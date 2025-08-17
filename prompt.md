# Amazon Shopping Backend – Low-Level Design

## Architectural Overview

- The backend is designed as a scalable, fault-tolerant microservices architecture using Spring Boot and containerization.
- Each domain (user, product catalog, inventory, cart, order, payment, shipping, notifications, reviews) is an independent service.
- Services run in Docker containers on Kubernetes for automated deployment, scaling, and failover.
- Communication uses REST (JSON) for external APIs, gRPC (Protobuf) for efficient inter-service calls, and Kafka for asynchronous event streaming.
- Multi-tenancy is supported by carrying a tenant ID in each request and isolating data per tenant in each service’s data store.
- The system targets 99.99% uptime by deploying replicas across multiple availability zones, using clustered databases with replication, and resilient patterns (retries, circuit breakers).

## Authentication & Authorization Service

- Built with Spring Boot + Spring Security, this service handles user login, logout, and token issuance.
- It maintains user credentials in a MySQL database (tenant-specific schema or tenant_id column) with secure password hashing.
- Implements OAuth2/JWT tokens with embedded tenant ID and roles/permissions for access control.
- Provides endpoints for login, token refresh, and user session management.
- Stateless design: tokens are self-contained, so no session storage is needed, enabling horizontal scaling.
- Integrates with the API Gateway to validate tokens on each request, propagating user and tenant context to other services.

## User Management Service

- Manages user profiles, addresses, and preferences.
- Uses Spring Boot and MySQL (users table with tenant_id).
- Provides CRUD APIs for user data (with input validation, DTOs, and Swagger documentation).
- Associates each user with a specific tenant; tenant ID is taken from the JWT context or request header.
- Includes features like email verification and password reset via secure tokens.
- Caches frequent reads (e.g. user profile) in Redis to improve performance.

## Product Catalog Service

- Handles product data (names, descriptions, images, price, categories).
- Uses MongoDB for flexible schemas (e.g. varied product attributes per category) and fast lookup.
- Exposes read-heavy endpoints for product search and listing; uses Redis to cache hot product info.
- Publishes product events (ProductCreated, ProductUpdated, ProductDeleted) to Kafka so other services (e.g. search, inventory) can react.
- Supports multi-tenant product segregation: each product document includes tenant_id or is stored in a tenant-specific collection.
- Implements pagination, filtering, and full-text search capabilities (e.g. MongoDB text indexes or integration with a search service).

## Inventory Service

- Tracks stock levels and availability for each product.
- Uses a relational MySQL store to ensure transactional consistency (e.g. row locking on stock decrement).
- Subscribes to Order events from Kafka; on OrderCreated event it atomically reduces stock or rejects the order if insufficient.
- Uses Redis as a caching layer for quick stock lookups and for handling high-concurrency reservations.
- Offers APIs for inventory management (adjust stock, set restock thresholds) for admin users.
- Ensures strong consistency for inventory counts and uses database replication for high availability.

## Shopping Cart Service

- Manages the customer’s shopping cart items before checkout.
- Stores carts in Redis for low-latency access and TTL (auto-expiry of old carts), with a fallback persistent store (MySQL) for recovery.
- Provides APIs to add, update, and remove items from the cart; calculates subtotal, taxes, and applies promotions.
- Each cart is tied to a user (hence to a tenant); keys in Redis include user_id and tenant_id.
- On order placement, the cart contents are validated and then cleared.
- Maintains idempotency keys to handle network retries without duplicating cart operations.

## Order Management Service

- Orchestrates the checkout process and maintains order lifecycle.
- Uses Spring Boot with MySQL for order records (with ACID transactions for creating an order).
- Implements a Saga pattern for distributed transactions: on checkout it creates an Order and then sends events/commands to Inventory and Payment.
- Publishes events like OrderCreated, OrderConfirmed, and OrderCancelled to Kafka. Other services (inventory, shipping) consume these events.
- Ensures idempotent order processing by using unique order IDs and idempotency tokens for incoming commands.
- Retries and compensations: if payment fails after order creation, triggers compensating actions (e.g. restock via Inventory service).
- Exposes APIs to query order status; access is restricted to the ordering user (tenant isolation).

## Payment Service

- Handles payment processing through external gateways (credit card, digital wallets).
- Uses MySQL to log payment transactions (status, amount, transaction IDs).
- Exposes an API endpoint that the Order service calls to execute payment; the call is idempotent and includes order ID and payment details.
- Validates payment details and communicates with third-party payment providers via secure, tokenized integration.
- On success or failure, emits events (PaymentSucceeded, PaymentFailed) to Kafka, which the Order service consumes to update the order status.
- Highly secure: no long-term storage of raw payment info (only tokens and references), following PCI DSS considerations.

## Shipping Service

- Manages shipment scheduling and tracking.
- Uses MySQL (or a NoSQL store) to keep shipment records (carrier info, tracking number, status).
- Subscribes to OrderConfirmed events: when an order is ready, it creates a shipment entry.
- Interfaces with external carrier APIs or logistics partners (this could be another microservice or external integration).
- Publishes shipment events (OrderShipped, OrderDelivered) to Kafka for notifications and order updates.
- Provides endpoints to update or query shipment status, scoped to the tenant and order.

## Notification Service

- Sends communications (email, SMS, push) to users based on events.
- Stateless, built with Spring Boot; it processes Kafka messages (e.g. OrderCreated, PaymentSucceeded, OrderShipped).
- Contains templating logic for different notification types; templates can be tenant-specific or customizable per tenant.
- Integrates with email/SMS providers (e.g. Amazon SES, Twilio) for delivery.
- Implements retry logic and a dead-letter queue for failed notifications.
- Supports user preferences (opt-in/out) fetched from the User Management service.

## Review & Rating Service

- Allows customers to submit reviews and ratings for products.
- Uses MongoDB for storing review documents (rating, comments, timestamp) with product and user references.
- Provides CRUD APIs for reviews; enforces one review per user per product.
- Publishes events when a new review is added so that product ratings can be recalculated or aggregated.
- Moderation features: flagging or approval workflows (could integrate with a manual review process).
- Multi-tenant: reviews are tied to products within a tenant’s catalog.

## Data Storage & Multi-Tenancy

- **Relational DB (MySQL)** clusters host transactional data (users, orders, inventory, payments, shipments). Each database is set up with master–slave replication across availability zones for failover. Use separate schemas or a `tenant_id` column per table to isolate tenants. Enforce row-level security or database features for tenant isolation.
- **NoSQL (MongoDB)** clusters store semi-structured data (products, reviews). Similarly, shard or partition data by tenant or include `tenant_id` in each document. Use replica sets for high availability.
- **Redis** clusters handle caching and fast storage (carts, sessions, quick lookups). Deploy Redis Cluster with master–replica nodes. Configure eviction policies and persistence (RDB/AOF) for data reliability.
- All data stores are configured with encryption at rest and TLS encryption in transit.
- Automated backups, point-in-time recovery, and regular disaster recovery drills ensure data durability and help meet the 99.99% availability target.

## Communication & Integration

- **Synchronous communication** via RESTful APIs (Spring MVC/REST) or gRPC (Protobuf) for internal service-to-service calls. Services expose well-defined endpoints (documented with OpenAPI/Swagger).
- **Asynchronous messaging** with Apache Kafka as the event bus. Key topics include user-events, order-events, inventory-events, payment-events, shipping-events, and notification-events. Producers publish events; consumers subscribe and react, enabling loose coupling.
- Use an **API Gateway** (e.g. Spring Cloud Gateway or Kong) at the front door: it routes requests, performs SSL termination, authentication (validating JWTs), and implements rate limiting per tenant.
- **Service Discovery** is handled by Kubernetes DNS; each service is reachable within the cluster by its service name and namespace.
- **Correlation IDs**: Every incoming request and Kafka message carries a correlation ID (passed via HTTP headers or Kafka metadata) to trace workflows end-to-end across microservices.

## High Availability & Scalability

- Deploy all microservices in Kubernetes across multiple availability zones. Each service runs multiple replicas (stateless pods) behind a Kubernetes Service (load balancer).
- Enable **Horizontal Pod Autoscaling** based on CPU, memory, or custom metrics (e.g. request rate) to handle load spikes.
- Use **Redis clusters** and **Kafka clusters** with replication for fault tolerance.
- Design services to be **stateless**: any local state (caches, sessions) is in Redis or external stores; services can restart without losing user context.
- Implement **circuit breakers** (with resilience4j or Spring Cloud Circuit Breaker) to prevent cascading failures when downstream services are unhealthy.
- Use **bulkheads and fallback methods** in critical services: isolate resources (threads, connections) so failures in one part (e.g. payments) do not impact others.
- Perform **blue/green or canary deployments** via Kubernetes (Helm). Run parallel environments and switch traffic gradually or route a fraction to new versions for zero-downtime releases.
- To meet 99.99% availability, ensure no single point of failure: multiple K8s nodes, replicated databases, and code-level resiliency (retries with exponential backoff).

## Monitoring & Logging

- **Metrics**: Each service is instrumented with Micrometer. Prometheus scrapes metrics endpoints. Key metrics include request rates, latencies, error rates, and resource usage. Grafana dashboards visualize service health, resource utilization, and SLA compliance.
- **Logging**: Services log structured JSON messages to stdout. Use a sidecar or DaemonSet (Filebeat/Logstash) to ship logs to Elasticsearch (ELK stack) or to Kafka (with Logstash) for aggregation and analysis. Logs include timestamps, service names, request IDs, and tenant IDs.
- **Tracing**: Implement distributed tracing (using OpenTelemetry/Jaeger). Propagate correlation/request IDs in HTTP headers and Kafka metadata to stitch together traces across microservices.
- **Alerting**: Use Prometheus Alertmanager to notify on SLA breaches (e.g. high error rate, service downtime). Configure alerts (via email, Slack, PagerDuty) for anomalies.
- **Health Checks**: Kubernetes liveness and readiness probes ensure unhealthy pods are restarted or removed from service until healthy.

## CI/CD & Deployment

- **Version Control**: All services have separate Git repositories (or well-structured monorepo). Use GitHub (or GitLab) for source control with branch protections.
- **Continuous Integration**: Jenkins or GitHub Actions pipelines run on each commit: static code analysis (SonarQube), unit tests, and build Docker images.
- **Security Scanning**: Integrate image vulnerability scanning (Aqua Trivy, Snyk) in the CI pipeline. Builds fail if critical vulnerabilities are detected.
- **Containerization**: Package each service as a Docker image. Tag images with semantic versioning and push to a container registry (Docker Hub, AWS ECR, etc.).
- **Infrastructure as Code**: Use Helm charts to define Kubernetes deployments (Deployments, Services, ConfigMaps, Secrets). Use Helm values files for different environments (dev/stage/prod) to parameterize configurations (image tags, resource limits, feature flags).
- **Blue/Green & Canary Deployments**: Use Helm and Kubernetes strategies for zero-downtime releases. For blue/green, maintain two live environments and switch load balancer traffic. For canary, gradually shift a percentage of traffic to new pods.
- **Rollback**: Use Helm and Kubernetes rollback features to revert to previous versions if any issue is detected post-deployment.
- **Documentation**: Auto-generate and publish API contracts (OpenAPI/Swagger) for each service. Maintain runbooks and architecture docs in the repos.

## Code Structure & Testing

- **SOLID Principles**: Follow clean architecture. Each microservice’s codebase has clear layers (Controller, Service, Repository/DAO). Use dependency injection and interfaces to decouple components and facilitate testing.
- **Domain-Driven Design (DDD)**: Each service owns its data and domain logic (bounded context). Entities and DTOs include tenantId where needed for multi-tenancy.
- **Unit & Integration Tests**: Write comprehensive unit tests (JUnit, Mockito) for business logic. Use Testcontainers or embedded databases for integration tests (e.g. testing repositories or REST endpoints).
- **API Documentation**: Use SpringDoc or Swagger annotations to generate interactive API docs. Host Swagger UI for each service, linked from a central developer portal.
- **Feature Toggles**: Implement feature flags (using a library or config) so new features can be enabled/disabled per tenant or environment without redeploying.
- **Code Reviews & CI**: Enforce pull-request reviews and coding standards (Checkstyle/PMD) in CI. Use pair programming or mob sessions for complex modules to ensure design quality.
- **Logging & Error Handling**: Provide consistent error response formats. Implement global exception handlers in each service. Log errors with full context (including correlation IDs and parameters) to aid debugging.

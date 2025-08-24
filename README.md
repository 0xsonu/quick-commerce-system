# Amazon Shopping Backend

A production-ready, scalable, multi-tenant e-commerce backend system built with Spring Boot microservices architecture. This system implements a complete shopping platform with advanced features including distributed transactions, event-driven architecture, comprehensive monitoring, and enterprise-grade security.

## 🏗️ Architecture Overview

The system follows Domain-Driven Design (DDD) principles with microservices architecture, implementing both synchronous (gRPC) and asynchronous (Kafka) communication patterns.

### 🔧 Core Services

- **API Gateway** (Port 8080) - Request routing, authentication, rate limiting, and protocol translation
- **Auth Service** (Port 8082) - JWT-based authentication, authorization, and token management
- **User Service** (Port 8083) - User profile management, address management, and caching

### 📦 Catalog Services

- **Product Service** (Port 8084) - Product catalog with MongoDB, full-text search, and Redis caching
- **Inventory Service** (Port 8085) - Real-time stock management with optimistic locking and reservations

### 🛒 Commerce Services

- **Cart Service** (Port 8086) - Redis-primary shopping cart with MySQL backup and gRPC integration
- **Order Service** (Port 8087) - Order processing with Saga pattern, idempotency, and event sourcing
- **Payment Service** (Port 8088) - Secure payment processing with external gateway integration

### 🚚 Fulfillment Services

- **Shipping Service** (Port 8089) - Shipment tracking with carrier API integration
- **Notification Service** (Port 8090) - Multi-channel notifications (email, SMS, push)

### ⭐ Engagement Services

- **Review Service** (Port 8091) - Product reviews and ratings with moderation

## 🛠️ Technology Stack (LTS Versions)

### Core Technologies

- **Java**: OpenJDK 21 LTS (Latest LTS, supported until 2031)
- **Spring Boot**: 3.2.x (Latest stable with Java 21 support)
- **Spring Security**: 6.2.x (Included with Spring Boot 3.2.x)
- **Spring Cloud Gateway**: 4.0.x (API Gateway implementation)

### Databases & Storage

- **MySQL**: 8.0 LTS (Primary RDBMS for transactional data)
- **MongoDB**: 7.0 Community (Document storage for products and reviews)
- **Redis**: 7.2 (Caching, session storage, and cart primary storage)

### Messaging & Communication

- **Apache Kafka**: 3.6 (Event streaming and asynchronous communication)
- **gRPC**: 1.58.0 (High-performance internal service communication)
- **Protocol Buffers**: 3.24.0 (Efficient serialization for gRPC)

### Infrastructure & Deployment

- **Docker**: 24.0+ (Containerization)
- **Kubernetes**: 1.28+ (Container orchestration)
- **Helm**: 3.13+ (Kubernetes package management)

### Monitoring & Observability

- **Prometheus**: Metrics collection and alerting
- **Grafana**: Monitoring dashboards and visualization
- **Jaeger**: Distributed tracing
- **ELK Stack**: Centralized logging (Elasticsearch, Logstash, Kibana)

## 🚀 Key Features Implemented

### ✅ Multi-Tenant Architecture

- **Complete tenant isolation** at database, application, and API levels
- **Automatic tenant context** propagation across all services
- **Tenant-aware repositories** with automatic filtering
- **JWT-based tenant validation** in all requests

### ✅ Advanced Security

- **JWT authentication** with refresh token support
- **Role-based authorization** (RBAC)
- **Secure password hashing** with BCrypt
- **Token cleanup jobs** for expired and revoked tokens
- **PCI DSS compliance** preparation for payment data

### ✅ High-Performance Communication

- **gRPC internal communication** for efficient service-to-service calls
- **Protocol Buffers** for type-safe, efficient serialization
- **REST APIs** for external client communication
- **Automatic protocol translation** in API Gateway

### ✅ Event-Driven Architecture

- **Kafka event streaming** for loose coupling between services
- **Event sourcing** for order processing and audit trails
- **Saga pattern** for distributed transactions
- **Event replay and recovery** mechanisms

### ✅ Advanced Caching Strategy

- **Redis caching** for frequently accessed data
- **Cache-aside pattern** with automatic invalidation
- **Shopping cart primary storage** in Redis with MySQL backup
- **Product catalog caching** with TTL management

### ✅ Robust Order Processing

- **Distributed transaction management** using Saga pattern
- **Idempotency and duplicate prevention** with token validation
- **Unique order number generation** with collision handling
- **Inventory reservation** with automatic timeout and release
- **Order state machine** with proper status transitions

### ✅ Comprehensive Data Management

- **Multi-database architecture** (MySQL, MongoDB, Redis)
- **Optimistic locking** for inventory management
- **ACID transactions** within service boundaries
- **Eventual consistency** across service boundaries
- **Database connection pooling** and health checks

### ✅ Production-Ready Features

- **Health checks** for all services and dependencies
- **Graceful shutdown** handling
- **Circuit breaker** patterns for resilience
- **Rate limiting** per tenant
- **Correlation ID** tracking across requests
- **Structured JSON logging** with MDC context

## 📋 Prerequisites

- **Java 21 LTS** (OpenJDK recommended)
- **Maven 3.9+** for building
- **Docker 24.0+** and **Docker Compose** for infrastructure
- **Git** for version control
- **8GB+ RAM** recommended for running all services locally

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd amazon-shopping-backend
```

### 2. Start Infrastructure Services

```bash
# Start all infrastructure services (MySQL, MongoDB, Redis, Kafka, Zookeeper, etc.)
docker-compose up -d

# Wait for services to be ready (this may take 2-3 minutes)
docker-compose ps

# Check service health
docker-compose logs -f mysql mongodb redis kafka
```

**Infrastructure Services Started:**

- **MySQL** (Port 3306) - Primary database for transactional data
- **MongoDB** (Port 27017) - Document storage for products and reviews
- **Redis** (Port 6379) - Caching and session storage
- **Apache Kafka** (Port 9092) - Event streaming
- **Zookeeper** (Port 2181) - Kafka coordination
- **Kafka UI** (Port 8080) - Kafka management interface
- **Prometheus** (Port 9090) - Metrics collection
- **Grafana** (Port 3000) - Monitoring dashboards
- **Jaeger** (Port 16686) - Distributed tracing

### 3. Build All Services

```bash
# Build all modules including shared libraries
mvn clean install -DskipTests

# Or use the build script
chmod +x scripts/build-all.sh
./scripts/build-all.sh
```

**Build Order:**

1. `shared-models` - Common data models and DTOs
2. `shared-utils` - Utility classes and helpers
3. `shared-security` - Security components and tenant context
4. `shared-proto` - Protocol Buffer definitions for gRPC
5. All microservices in dependency order

### 4. Run Services

#### Option A: Use Provided Scripts (Recommended)

```bash
# Make scripts executable
chmod +x scripts/*.sh

# Start infrastructure first
./scripts/start-infrastructure.sh

# Start services in recommended order
./scripts/start-service.sh auth-service      # Authentication first
./scripts/start-service.sh user-service      # User management
./scripts/start-service.sh product-service   # Product catalog
./scripts/start-service.sh inventory-service # Inventory management
./scripts/start-service.sh cart-service      # Shopping cart
./scripts/start-service.sh order-service     # Order processing
./scripts/start-service.sh api-gateway       # Gateway last
```

#### Option B: Manual Service Startup

```bash
# Start services individually (in separate terminals)
cd services/auth/auth-service && mvn spring-boot:run
cd services/user/user-service && mvn spring-boot:run
cd services/catalog/product-service && mvn spring-boot:run
cd services/catalog/inventory-service && mvn spring-boot:run
cd services/commerce/cart-service && mvn spring-boot:run
cd services/commerce/order-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

### 5. Verify System Health

```bash
# Check API Gateway health
curl http://localhost:8080/actuator/health

# Check individual service health
curl http://localhost:8082/actuator/health  # Auth Service
curl http://localhost:8083/actuator/health  # User Service
curl http://localhost:8084/actuator/health  # Product Service
curl http://localhost:8085/actuator/health  # Inventory Service
curl http://localhost:8086/actuator/health  # Cart Service
curl http://localhost:8087/actuator/health  # Order Service
```

### 6. Access Management Interfaces

- **Kafka UI**: http://localhost:8080 (Kafka topic management)
- **Prometheus**: http://localhost:9090 (Metrics and alerts)
- **Grafana**: http://localhost:3000 (admin/admin - Monitoring dashboards)
- **Jaeger**: http://localhost:16686 (Distributed tracing)

## 📜 Available Scripts

The project includes comprehensive utility scripts in the `scripts/` directory:

### Build Scripts

- **`build-all.sh`** - Builds all modules including shared libraries and services in correct dependency order
- **`clean-all.sh`** - Cleans all Maven targets and Docker images

### Infrastructure Scripts

- **`start-infrastructure.sh`** - Starts all infrastructure services using Docker Compose
- **`stop-infrastructure.sh`** - Stops all infrastructure services
- **`reset-infrastructure.sh`** - Resets all data (databases, caches, topics)

### Service Management Scripts

- **`start-service.sh <service-name>`** - Starts a specific microservice with proper environment
- **`stop-service.sh <service-name>`** - Gracefully stops a specific service
- **`restart-service.sh <service-name>`** - Restarts a service with zero downtime

### Development Scripts

- **`run-tests.sh`** - Runs all unit and integration tests
- **`generate-proto.sh`** - Regenerates Protocol Buffer classes
- **`check-health.sh`** - Checks health of all running services

### Usage Examples

```bash
# Make scripts executable (one time)
chmod +x scripts/*.sh

# Complete system startup
./scripts/build-all.sh
./scripts/start-infrastructure.sh
./scripts/start-service.sh auth-service
./scripts/start-service.sh user-service
./scripts/start-service.sh product-service
./scripts/start-service.sh inventory-service
./scripts/start-service.sh cart-service
./scripts/start-service.sh order-service
./scripts/start-service.sh api-gateway

# Check system health
./scripts/check-health.sh

# Run comprehensive tests
./scripts/run-tests.sh

# Available service names:
# auth-service, user-service, product-service, inventory-service,
# cart-service, order-service, payment-service, shipping-service,
# notification-service, review-service, api-gateway
```

## 🔧 Development Setup

### 🗄️ Database Architecture

#### MySQL Databases (Automatically Created)

The Docker Compose configuration creates tenant-isolated databases:

- **`auth_service`** - User authentication, JWT tokens, refresh tokens
- **`user_service`** - User profiles, addresses, preferences, caching metadata
- **`inventory_service`** - Stock levels, reservations, transactions, optimistic locking
- **`cart_service`** - Cart backup storage, recovery data
- **`order_service`** - Orders, order items, saga state, idempotency tokens
- **`payment_service`** - Payment transactions, gateway responses (tokenized)
- **`shipping_service`** - Shipment tracking, carrier data, delivery status

#### MongoDB Collections

- **`products`** - Product catalog with flexible schemas, full-text search indexes
- **`reviews`** - Product reviews, ratings, moderation status

#### Redis Usage Patterns

- **Session Storage**: `session:{tenant_id}:{user_id}` - User session data
- **Caching Layer**: `cache:{service}:{tenant_id}:{key}` - Service-specific caches
- **Shopping Cart Primary**: `cart:{tenant_id}:{user_id}` - Real-time cart data
- **Inventory Reservations**: `reservation:{product_id}:{reservation_id}` - Temporary stock holds
- **Rate Limiting**: `rate_limit:{tenant_id}:{user_id}` - API rate limiting counters

### 📨 Kafka Event Architecture

#### Topics and Event Types

**`product-events`** - Product lifecycle management

```json
{
  "eventType": "ProductCreated|ProductUpdated|ProductDeleted",
  "tenantId": "tenant_123",
  "productId": "product_456",
  "timestamp": "2024-01-01T00:00:00Z",
  "correlationId": "corr_789"
}
```

**`order-events`** - Order processing workflow

```json
{
  "eventType": "OrderCreated|OrderConfirmed|OrderCancelled|OrderShipped|OrderDelivered",
  "tenantId": "tenant_123",
  "orderId": 12345,
  "userId": 67890,
  "orderNumber": "ORD-20240101-A1B2-C3D4E5F6",
  "totalAmount": { "amount": 29999, "currency": "USD" },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

**`payment-events`** - Payment processing status

```json
{
  "eventType": "PaymentInitiated|PaymentSucceeded|PaymentFailed|PaymentRefunded",
  "tenantId": "tenant_123",
  "orderId": 12345,
  "paymentId": "pay_789",
  "amount": { "amount": 29999, "currency": "USD" },
  "status": "COMPLETED|FAILED|PENDING"
}
```

**`inventory-events`** - Stock management

```json
{
  "eventType": "StockReserved|StockReleased|StockUpdated|LowStockAlert",
  "tenantId": "tenant_123",
  "productId": "product_456",
  "quantity": 5,
  "reservationId": "res_123",
  "availableStock": 95
}
```

**`notification-events`** - Multi-channel notifications

```json
{
  "eventType": "EmailNotification|SMSNotification|PushNotification",
  "tenantId": "tenant_123",
  "userId": 67890,
  "templateId": "order_confirmation",
  "channel": "EMAIL|SMS|PUSH",
  "priority": "HIGH|MEDIUM|LOW"
}
```

### 🔄 gRPC Service Communication

#### Internal Service APIs

**Product Service gRPC**

```protobuf
service ProductService {
  rpc GetProduct(GetProductRequest) returns (GetProductResponse);
  rpc ValidateProduct(ValidateProductRequest) returns (ValidateProductResponse);
  rpc GetProductsByIds(GetProductsByIdsRequest) returns (GetProductsByIdsResponse);
}
```

**Inventory Service gRPC**

```protobuf
service InventoryService {
  rpc CheckAvailability(CheckAvailabilityRequest) returns (CheckAvailabilityResponse);
  rpc ReserveInventory(ReserveInventoryRequest) returns (ReserveInventoryResponse);
  rpc ReleaseInventory(ReleaseInventoryRequest) returns (ReleaseInventoryResponse);
  rpc GetStockLevel(GetStockLevelRequest) returns (GetStockLevelResponse);
}
```

**User Service gRPC**

```protobuf
service UserService {
  rpc GetUser(GetUserRequest) returns (GetUserResponse);
  rpc GetUserAddresses(GetUserAddressesRequest) returns (GetUserAddressesResponse);
  rpc ValidateUser(ValidateUserRequest) returns (ValidateUserResponse);
}
```

**Order Service gRPC**

```protobuf
service OrderService {
  rpc GetOrder(GetOrderRequest) returns (GetOrderResponse);
  rpc ValidateOrder(ValidateOrderRequest) returns (ValidateOrderResponse);
  rpc UpdateOrderStatus(UpdateOrderStatusRequest) returns (UpdateOrderStatusResponse);
}
```

### 🏗️ Multi-Tenant Data Isolation

#### Database Level Isolation

```sql
-- All tables include tenant_id for row-level security
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    -- ... other columns
    INDEX idx_tenant_id (tenant_id)
);
```

#### Application Level Isolation

```java
// Automatic tenant filtering in repositories
@Repository
public interface OrderRepository extends TenantAwareRepository<Order, Long> {
    // Automatically filters by tenant_id from TenantContext
    List<Order> findByUserId(Long userId);
}
```

#### API Level Isolation

```java
// Tenant context extraction from JWT
@RestController
public class OrderController {
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestHeader("X-User-ID") String userId,
        @RequestBody CreateOrderRequest request) {
        // Tenant context automatically set and validated
    }
}
```

## ⚙️ Configuration

### 🔐 Environment Variables

#### Core Security Configuration

```bash
# JWT Configuration
JWT_SECRET=your-256-bit-production-secret-key
JWT_EXPIRATION=86400000  # 24 hours in milliseconds
JWT_REFRESH_EXPIRATION=604800000  # 7 days in milliseconds

# Encryption Keys
ENCRYPTION_KEY=your-aes-256-encryption-key
ENCRYPTION_SALT=your-encryption-salt
```

#### Database Configuration

```bash
# MySQL Configuration
MYSQL_HOST=mysql-host
MYSQL_PORT=3306
MYSQL_USERNAME=ecommerce_user
MYSQL_PASSWORD=secure_password
MYSQL_MAX_POOL_SIZE=20
MYSQL_MIN_IDLE=5

# MongoDB Configuration
MONGODB_HOST=mongo-host
MONGODB_PORT=27017
MONGODB_DATABASE=ecommerce_catalog
MONGODB_USERNAME=mongo_user
MONGODB_PASSWORD=mongo_password

# Redis Configuration
REDIS_HOST=redis-host
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
REDIS_MAX_CONNECTIONS=50
REDIS_TIMEOUT=2000
```

#### Messaging Configuration

```bash
# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka-host1:9092,kafka-host2:9092
KAFKA_CONSUMER_GROUP_ID=ecommerce-backend
KAFKA_AUTO_OFFSET_RESET=earliest
KAFKA_ENABLE_AUTO_COMMIT=false
KAFKA_MAX_POLL_RECORDS=500

# gRPC Configuration
GRPC_SERVER_PORT=9090
GRPC_CLIENT_TIMEOUT=5000
GRPC_MAX_MESSAGE_SIZE=4194304  # 4MB
```

#### External Service Integration

```bash
# Payment Gateway
PAYMENT_GATEWAY_URL=https://api.stripe.com/v1
PAYMENT_GATEWAY_API_KEY=sk_live_your_stripe_key
PAYMENT_WEBHOOK_SECRET=whsec_your_webhook_secret

# Email Service (AWS SES)
EMAIL_SERVICE_URL=https://email.us-east-1.amazonaws.com
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_REGION=us-east-1

# SMS Service (Twilio)
SMS_SERVICE_URL=https://api.twilio.com/2010-04-01
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=+1234567890

# Shipping Carriers
FEDEX_API_KEY=your_fedex_api_key
UPS_API_KEY=your_ups_api_key
DHL_API_KEY=your_dhl_api_key
```

#### Monitoring Configuration

```bash
# Prometheus
PROMETHEUS_ENABLED=true
PROMETHEUS_PORT=9090
PROMETHEUS_SCRAPE_INTERVAL=15s

# Jaeger Tracing
JAEGER_ENABLED=true
JAEGER_ENDPOINT=http://jaeger:14268/api/traces
JAEGER_SAMPLER_TYPE=probabilistic
JAEGER_SAMPLER_PARAM=0.1

# Logging
LOG_LEVEL=INFO
LOG_FORMAT=JSON
LOG_FILE_PATH=/var/log/ecommerce
```

### 📋 Service-Specific Configuration

#### API Gateway (`api-gateway/src/main/resources/application.yml`)

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://auth-service:8082
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@tenantKeyResolver}"

        - id: user-service
          uri: http://user-service:8083
          predicates:
            - Path=/api/v1/users/**
          filters:
            - AuthenticationFilter
            - TenantContextFilter

grpc:
  client:
    auth-service:
      address: static://auth-service:9082
      negotiation-type: plaintext
```

#### Order Service (`services/commerce/order-service/src/main/resources/application.yml`)

```yaml
server:
  port: 8087

spring:
  application:
    name: order-service

  datasource:
    url: jdbc:mysql://localhost:3306/order_service_db
    username: ${DB_USERNAME:ecommerce_user}
    password: ${DB_PASSWORD:ecommerce_pass}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: order-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

grpc:
  server:
    port: 9087
  client:
    inventory-service:
      address: static://inventory-service:9085
      negotiation-type: plaintext
    payment-service:
      address: static://payment-service:9088
      negotiation-type: plaintext
    user-service:
      address: static://user-service:9083
      negotiation-type: plaintext

app:
  order:
    number-prefix: "ORD"
    default-currency: "USD"
    tax-rate: 0.08
    free-shipping-threshold: 100.00
    default-shipping-cost: 9.99

  saga:
    timeout-seconds: 300
    max-retry-attempts: 3
    retry-delay-seconds: 5
```

#### Cart Service (`services/commerce/cart-service/src/main/resources/application.yml`)

```yaml
server:
  port: 8086

spring:
  application:
    name: cart-service

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 50
        max-idle: 10
        min-idle: 5

  datasource:
    url: jdbc:mysql://localhost:3306/cart_service_db
    username: ${DB_USERNAME:ecommerce_user}
    password: ${DB_PASSWORD:ecommerce_pass}

grpc:
  client:
    product-service:
      address: static://product-service:9084
      negotiation-type: plaintext
    inventory-service:
      address: static://inventory-service:9085
      negotiation-type: plaintext

app:
  cart:
    ttl-hours: 168 # 7 days
    max-items: 100
    backup-interval-minutes: 30
```

### 🔧 Development vs Production Configuration

#### Development (`application-dev.yml`)

```yaml
logging:
  level:
    com.ecommerce: DEBUG
    org.springframework.kafka: INFO

spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

#### Production (`application-prod.yml`)

```yaml
logging:
  level:
    com.ecommerce: INFO
    org.springframework.kafka: WARN

spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

## 📊 Monitoring and Observability

### 📈 Prometheus Metrics Collection

**Access**: http://localhost:9090

#### Service-Level Metrics

All services expose comprehensive metrics at `/actuator/prometheus`:

```yaml
# HTTP Request Metrics
http_server_requests_seconds_count{method="POST",uri="/api/v1/orders",status="201"}
http_server_requests_seconds_sum{method="POST",uri="/api/v1/orders",status="201"}

# JVM Metrics
jvm_memory_used_bytes{area="heap",id="PS Eden Space"}
jvm_gc_pause_seconds{action="end of minor GC",cause="Allocation Failure"}
jvm_threads_live_threads

# Database Connection Pool Metrics
hikaricp_connections_active{pool="HikariPool-1"}
hikaricp_connections_idle{pool="HikariPool-1"}
hikaricp_connections_pending{pool="HikariPool-1"}

# Redis Metrics
lettuce_command_completion_seconds{command="GET",local="localhost:6379"}
lettuce_command_active_connections{local="localhost:6379"}

# Kafka Metrics
kafka_producer_record_send_total{client_id="order-service-producer"}
kafka_consumer_records_consumed_total{client_id="order-service-consumer"}

# Custom Business Metrics
orders_created_total{tenant_id="tenant_123",status="PENDING"}
cart_items_added_total{tenant_id="tenant_123"}
payment_transactions_total{tenant_id="tenant_123",status="COMPLETED"}
inventory_reservations_total{product_id="product_456",status="ACTIVE"}
```

#### Custom Metrics Implementation

```java
@Component
public class OrderMetrics {
    private final Counter ordersCreated = Counter.builder("orders_created_total")
        .description("Total orders created")
        .tag("service", "order-service")
        .register(Metrics.globalRegistry);

    private final Timer orderProcessingTime = Timer.builder("order_processing_seconds")
        .description("Order processing time")
        .register(Metrics.globalRegistry);

    public void incrementOrdersCreated(String tenantId, String status) {
        ordersCreated.increment(Tags.of("tenant_id", tenantId, "status", status));
    }
}
```

### 📊 Grafana Dashboards

**Access**: http://localhost:3000 (admin/admin)

#### Pre-configured Dashboards

1. **System Overview Dashboard**

   - Service health status
   - Request rates and response times
   - Error rates by service
   - Infrastructure resource usage

2. **Service-Level Dashboard** (per microservice)

   - HTTP request metrics (rate, duration, errors)
   - gRPC call metrics
   - Database query performance
   - Cache hit/miss ratios

3. **JVM Performance Dashboard**

   - Heap and non-heap memory usage
   - Garbage collection metrics
   - Thread pool utilization
   - Class loading statistics

4. **Database Performance Dashboard**

   - Connection pool metrics
   - Query execution times
   - Slow query analysis
   - Transaction rollback rates

5. **Business Intelligence Dashboard**
   - Order conversion rates
   - Revenue metrics by tenant
   - Product performance analytics
   - User engagement metrics

#### Sample Dashboard Queries

```promql
# Request rate per service
rate(http_server_requests_seconds_count[5m])

# 95th percentile response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate percentage
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) /
rate(http_server_requests_seconds_count[5m]) * 100

# JVM heap usage percentage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### 🔍 Distributed Tracing with Jaeger

**Access**: http://localhost:16686

#### Trace Context Propagation

```java
@RestController
public class OrderController {

    @Autowired
    private Tracer tracer;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        Span span = tracer.nextSpan()
            .name("create-order")
            .tag("tenant.id", TenantContext.getTenantId())
            .tag("user.id", TenantContext.getUserId())
            .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Business logic with automatic trace propagation
            return orderService.createOrder(request);
        } finally {
            span.end();
        }
    }
}
```

#### Trace Features

- **Cross-service correlation** via correlation IDs
- **gRPC call tracing** with automatic instrumentation
- **Database query tracing** with SQL statement capture
- **Kafka message tracing** with producer/consumer correlation
- **Error and exception tracking** with stack traces

### 📋 Kafka Management

**Access**: http://localhost:8080 (Kafka UI)

#### Available Features

- **Topic Management**: Create, delete, configure topics
- **Message Browser**: View messages with filtering and search
- **Consumer Group Monitoring**: Lag monitoring and offset management
- **Schema Registry**: Manage Avro/JSON schemas (if enabled)
- **Connector Management**: Kafka Connect connector status

#### Key Topics to Monitor

```bash
# Order processing pipeline
order-events (partitions: 3, replication: 1)
payment-events (partitions: 3, replication: 1)
inventory-events (partitions: 3, replication: 1)
shipping-events (partitions: 3, replication: 1)

# Catalog and user events
product-events (partitions: 2, replication: 1)
user-events (partitions: 2, replication: 1)

# Notification events
notification-events (partitions: 2, replication: 1)
```

### 🚨 Health Checks and Alerting

#### Service Health Endpoints

```bash
# Comprehensive health check
curl http://localhost:8087/actuator/health

# Response format
{
  "status": "UP",
  "components": {
    "db": {"status": "UP", "details": {"database": "MySQL", "validationQuery": "isValid()"}},
    "redis": {"status": "UP", "details": {"version": "7.2.0"}},
    "kafka": {"status": "UP", "details": {"clusterId": "kafka-cluster"}},
    "diskSpace": {"status": "UP", "details": {"total": 499963174912, "free": 91943821312}}
  }
}
```

#### Custom Health Indicators

```java
@Component
public class OrderServiceHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Check critical dependencies
            boolean dbHealthy = checkDatabaseConnection();
            boolean kafkaHealthy = checkKafkaConnection();
            boolean grpcHealthy = checkGrpcServices();

            if (dbHealthy && kafkaHealthy && grpcHealthy) {
                return Health.up()
                    .withDetail("database", "Connected")
                    .withDetail("kafka", "Connected")
                    .withDetail("grpc", "All services available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", dbHealthy ? "UP" : "DOWN")
                    .withDetail("kafka", kafkaHealthy ? "UP" : "DOWN")
                    .withDetail("grpc", grpcHealthy ? "UP" : "DOWN")
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

### 📝 Structured Logging

#### Log Format and Context

```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "service": "order-service",
  "traceId": "abc123def456",
  "spanId": "789ghi012",
  "tenantId": "tenant_123",
  "userId": "user_456",
  "correlationId": "corr_789",
  "message": "Order created successfully",
  "orderId": 12345,
  "orderNumber": "ORD-20240101-A1B2-C3D4E5F6",
  "processingTimeMs": 150
}
```

#### Log Aggregation with ELK Stack

- **Elasticsearch**: Log storage and indexing
- **Logstash**: Log parsing and enrichment
- **Kibana**: Log visualization and search
- **Filebeat**: Log shipping from containers

## 📚 API Documentation

### 🔗 Interactive API Documentation

Each service exposes comprehensive OpenAPI 3.0 documentation:

#### Swagger UI Endpoints

- **API Gateway**: http://localhost:8080/swagger-ui.html
- **Auth Service**: http://localhost:8082/swagger-ui.html
- **User Service**: http://localhost:8083/swagger-ui.html
- **Product Service**: http://localhost:8084/swagger-ui.html
- **Inventory Service**: http://localhost:8085/swagger-ui.html
- **Cart Service**: http://localhost:8086/swagger-ui.html
- **Order Service**: http://localhost:8087/swagger-ui.html

#### OpenAPI JSON Specifications

- **API Gateway**: http://localhost:8080/v3/api-docs
- **Auth Service**: http://localhost:8082/v3/api-docs
- **User Service**: http://localhost:8083/v3/api-docs
- **Product Service**: http://localhost:8084/v3/api-docs
- **Inventory Service**: http://localhost:8085/v3/api-docs
- **Cart Service**: http://localhost:8086/v3/api-docs
- **Order Service**: http://localhost:8087/v3/api-docs

### 🔐 Authentication Flow

#### 1. User Login

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "securePassword123",
  "tenantId": "tenant_123"
}

# Response
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_token_here",
  "expiresIn": 86400,
  "tokenType": "Bearer",
  "user": {
    "id": 12345,
    "username": "user@example.com",
    "roles": ["CUSTOMER"]
  }
}
```

#### 2. Token Refresh

```bash
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh_token_here"
}
```

#### 3. Using Authenticated Requests

```bash
# All subsequent requests require these headers
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-Tenant-ID: tenant_123
X-User-ID: 12345
```

### 🛒 Core API Examples

#### Product Catalog

```bash
# Search products
GET /api/v1/products?search=smartphone&category=electronics&page=0&size=20
Authorization: Bearer <token>
X-Tenant-ID: tenant_123

# Get product details
GET /api/v1/products/product_456
Authorization: Bearer <token>
X-Tenant-ID: tenant_123
```

#### Shopping Cart

```bash
# Add item to cart
POST /api/v1/cart/items
Authorization: Bearer <token>
X-Tenant-ID: tenant_123
X-User-ID: 12345
Content-Type: application/json

{
  "productId": "product_456",
  "sku": "SKU123",
  "quantity": 2
}

# Get cart contents
GET /api/v1/cart
Authorization: Bearer <token>
X-Tenant-ID: tenant_123
X-User-ID: 12345
```

#### Order Processing

```bash
# Create order with idempotency
POST /api/v1/orders
Authorization: Bearer <token>
X-Tenant-ID: tenant_123
X-User-ID: 12345
Idempotency-Key: unique_request_id_123
Content-Type: application/json

{
  "userId": 12345,
  "items": [
    {
      "productId": "product_456",
      "sku": "SKU123",
      "productName": "Smartphone",
      "quantity": 1,
      "unitPrice": 299.99
    }
  ],
  "billingAddress": {
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "US"
  },
  "shippingAddress": {
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "US"
  }
}

# Response
{
  "success": true,
  "data": {
    "id": 67890,
    "orderNumber": "ORD-20240101-A1B2-C3D4E5F6",
    "userId": 12345,
    "status": "PENDING",
    "subtotal": 299.99,
    "taxAmount": 24.00,
    "shippingAmount": 9.99,
    "totalAmount": 333.98,
    "currency": "USD",
    "items": [...],
    "createdAt": "2024-01-01T12:00:00Z"
  },
  "message": "Order created successfully"
}
```

### 🔄 gRPC API Documentation

#### Protocol Buffer Definitions

**Common Types** (`shared-proto/src/main/proto/common.proto`):

```protobuf
message TenantContext {
  string tenant_id = 1;
  string user_id = 2;
  string correlation_id = 3;
}

message Money {
  int64 amount_cents = 1;  // Amount in cents
  string currency = 2;
}

message Address {
  string street_address = 1;
  string city = 2;
  string state = 3;
  string postal_code = 4;
  string country = 5;
}
```

**Product Service gRPC**:

```protobuf
service ProductService {
  rpc GetProduct(GetProductRequest) returns (GetProductResponse);
  rpc ValidateProduct(ValidateProductRequest) returns (ValidateProductResponse);
  rpc GetProductsByIds(GetProductsByIdsRequest) returns (GetProductsByIdsResponse);
}
```

**Inventory Service gRPC**:

```protobuf
service InventoryService {
  rpc CheckAvailability(CheckAvailabilityRequest) returns (CheckAvailabilityResponse);
  rpc ReserveInventory(ReserveInventoryRequest) returns (ReserveInventoryResponse);
  rpc ReleaseInventory(ReleaseInventoryRequest) returns (ReleaseInventoryResponse);
}
```

### 📋 Error Response Format

#### Standard Error Response

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "path": "/api/v1/orders",
  "correlationId": "corr_123",
  "fieldErrors": {
    "userId": "User ID is required",
    "items": "Order items cannot be empty"
  }
}
```

#### Idempotency Error Response

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 409,
  "error": "Idempotency Error",
  "message": "Request is still being processed",
  "path": "/api/v1/orders"
}
```

#### Duplicate Order Error Response

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 409,
  "error": "Duplicate Order",
  "message": "Duplicate order request detected",
  "existingOrderId": 67890,
  "path": "/api/v1/orders"
}
```

### 📖 API Usage Guidelines

#### Rate Limiting

- **Per tenant**: 1000 requests per minute
- **Per user**: 100 requests per minute
- **Burst capacity**: 2x the rate limit for short bursts

#### Idempotency

- Use `Idempotency-Key` header for order creation and payment processing
- Keys should be unique per request and can be reused for retries
- Cached responses are returned for completed requests with the same key

#### Pagination

```bash
# Standard pagination parameters
GET /api/v1/products?page=0&size=20&sort=name&direction=asc

# Response includes pagination metadata
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

## 🧪 Testing

### 🔬 Unit Tests

#### Running Unit Tests

```bash
# Run all unit tests across all modules
mvn test

# Run tests for specific service
cd services/commerce/order-service && mvn test

# Run tests with coverage report
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=OrderServiceTest

# Run specific test method
mvn test -Dtest=OrderServiceTest#testCreateOrder
```

#### Test Coverage Requirements

- **Minimum coverage**: 80% line coverage
- **Critical paths**: 95% coverage (order processing, payment, inventory)
- **Service layer**: 90% coverage
- **Repository layer**: 85% coverage

#### Unit Test Examples

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_WithValidRequest_ShouldCreateOrder() {
        // Given
        CreateOrderRequest request = createValidOrderRequest();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrderWithIdempotency_ExistingToken_ShouldReturnCachedResult() {
        // Given
        String idempotencyKey = "test-key-123";
        OrderResponse cachedResponse = createOrderResponse();

        IdempotencyService.IdempotencyValidationResult validationResult =
            new IdempotencyService.IdempotencyValidationResult(null, true, cachedResponse);

        when(idempotencyService.validateIdempotencyToken(eq(idempotencyKey), any(), any()))
            .thenReturn(validationResult);

        // When
        OrderResponse response = orderService.createOrderWithIdempotency(request, idempotencyKey);

        // Then
        assertThat(response).isEqualTo(cachedResponse);
        verify(orderRepository, never()).save(any());
    }
}
```

### 🔗 Integration Tests

#### Running Integration Tests

```bash
# Run integration tests (requires Docker)
mvn verify

# Run integration tests for specific service
cd services/commerce/order-service && mvn verify

# Run with Testcontainers (automatic Docker management)
mvn verify -Dspring.profiles.active=test

# Run integration tests with specific database
mvn verify -Dtest.database=mysql
```

#### Integration Test Configuration

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("order_service_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Test
    @Transactional
    void createOrder_EndToEndFlow_ShouldProcessSuccessfully() {
        // Given
        CreateOrderRequest request = createValidOrderRequest();

        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertThat(response.getId()).isNotNull();

        // Verify database persistence
        Optional<Order> savedOrder = orderRepository.findById(response.getId());
        assertThat(savedOrder).isPresent();

        // Verify Kafka event publishing
        ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofSeconds(5));
        assertThat(records).hasSize(1);
    }
}
```

### 🌐 End-to-End API Tests

#### REST Assured Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderControllerE2ETest {

    @LocalServerPort
    private int port;

    @Test
    void createOrder_CompleteUserJourney_ShouldSucceed() {
        // 1. Authenticate user
        String accessToken = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("accessToken");

        // 2. Add items to cart
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .header("X-Tenant-ID", "tenant_123")
            .header("X-User-ID", "12345")
            .body(addToCartRequest)
        .when()
            .post("/api/v1/cart/items")
        .then()
            .statusCode(200);

        // 3. Create order
        ValidatableResponse response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .header("X-Tenant-ID", "tenant_123")
            .header("X-User-ID", "12345")
            .header("Idempotency-Key", "test-key-" + UUID.randomUUID())
            .body(createOrderRequest)
        .when()
            .post("/api/v1/orders")
        .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.orderNumber", matchesPattern("ORD-\\d{8}-[A-F0-9]{4}-[A-Z0-9]{8}"))
            .body("data.status", equalTo("PENDING"));

        // 4. Verify order can be retrieved
        String orderId = response.extract().path("data.id").toString();

        given()
            .header("Authorization", "Bearer " + accessToken)
            .header("X-Tenant-ID", "tenant_123")
            .header("X-User-ID", "12345")
        .when()
            .get("/api/v1/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.id", equalTo(Integer.parseInt(orderId)));
    }
}
```

### ⚡ Performance Tests

#### Load Testing with JMeter

```bash
# Run load tests (requires JMeter)
cd load-tests/
./run-load-test.sh order-creation-test.jmx

# Available test plans:
# - user-authentication.jmx (1000 concurrent users)
# - product-search.jmx (500 concurrent searches)
# - order-creation.jmx (200 concurrent orders)
# - cart-operations.jmx (1000 concurrent cart operations)
```

#### Performance Test Configuration

```xml
<!-- order-creation-test.jmx -->
<TestPlan>
  <ThreadGroup>
    <stringProp name="ThreadGroup.num_threads">200</stringProp>
    <stringProp name="ThreadGroup.ramp_time">60</stringProp>
    <stringProp name="ThreadGroup.duration">300</stringProp>
  </ThreadGroup>

  <HTTPSamplerProxy>
    <stringProp name="HTTPSampler.domain">localhost</stringProp>
    <stringProp name="HTTPSampler.port">8080</stringProp>
    <stringProp name="HTTPSampler.path">/api/v1/orders</stringProp>
    <stringProp name="HTTPSampler.method">POST</stringProp>
  </HTTPSamplerProxy>
</TestPlan>
```

#### Gatling Performance Tests

```scala
class OrderCreationSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val scn = scenario("Order Creation")
    .exec(
      http("Login")
        .post("/api/v1/auth/login")
        .body(StringBody("""{"username":"test@example.com","password":"password"}"""))
        .check(jsonPath("$.accessToken").saveAs("token"))
    )
    .exec(
      http("Create Order")
        .post("/api/v1/orders")
        .header("Authorization", "Bearer ${token}")
        .header("X-Tenant-ID", "tenant_123")
        .header("X-User-ID", "12345")
        .header("Idempotency-Key", "key-${__UUID}")
        .body(RawFileBody("order-request.json"))
        .check(status.is(201))
    )

  setUp(
    scn.inject(rampUsers(200) during (60 seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(1000),
     global.successfulRequests.percent.gt(95)
   )
}
```

### 🧪 Test Data Management

#### Test Data Builders

```java
public class OrderTestDataBuilder {

    public static CreateOrderRequest createValidOrderRequest() {
        return CreateOrderRequest.builder()
            .userId(12345L)
            .items(List.of(createOrderItem()))
            .billingAddress(createAddress())
            .shippingAddress(createAddress())
            .currency("USD")
            .build();
    }

    public static CreateOrderItemRequest createOrderItem() {
        return CreateOrderItemRequest.builder()
            .productId("product_123")
            .sku("SKU123")
            .productName("Test Product")
            .quantity(2)
            .unitPrice(new BigDecimal("29.99"))
            .build();
    }

    public static AddressDto createAddress() {
        return AddressDto.builder()
            .streetAddress("123 Test St")
            .city("Test City")
            .state("TS")
            .postalCode("12345")
            .country("US")
            .build();
    }
}
```

#### Database Test Fixtures

```java
@TestConfiguration
public class TestDataConfiguration {

    @Bean
    @Primary
    public TestDataLoader testDataLoader() {
        return new TestDataLoader();
    }
}

@Component
public class TestDataLoader {

    public void loadTestData() {
        // Load test users
        createTestUser("test@example.com", "tenant_123");

        // Load test products
        createTestProduct("product_123", "Test Product", new BigDecimal("29.99"));

        // Load test inventory
        createTestInventory("product_123", 100);
    }
}
```

### 📊 Test Reporting

#### Coverage Reports

```bash
# Generate coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html

# Generate aggregate coverage report for all modules
mvn clean test jacoco:report-aggregate
```

#### Test Results

- **Surefire Reports**: `target/surefire-reports/`
- **Failsafe Reports**: `target/failsafe-reports/`
- **JaCoCo Coverage**: `target/site/jacoco/`
- **Performance Reports**: `load-tests/results/`

## 🚀 Deployment

### 🐳 Docker Images

#### Building Service Images

```bash
# Build all service images
./scripts/build-docker-images.sh

# Build specific service image
docker build -t ecommerce/auth-service:latest services/auth/auth-service/
docker build -t ecommerce/user-service:latest services/user/user-service/
docker build -t ecommerce/product-service:latest services/catalog/product-service/
docker build -t ecommerce/inventory-service:latest services/catalog/inventory-service/
docker build -t ecommerce/cart-service:latest services/commerce/cart-service/
docker build -t ecommerce/order-service:latest services/commerce/order-service/
docker build -t ecommerce/api-gateway:latest api-gateway/

# Build with specific tag
docker build -t ecommerce/order-service:v1.2.0 services/commerce/order-service/
```

#### Multi-stage Dockerfile Example

```dockerfile
# services/commerce/order-service/Dockerfile
FROM openjdk:21-jdk-slim as builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY ../../../shared-models ./shared-models
COPY ../../../shared-utils ./shared-utils
COPY ../../../shared-security ./shared-security
COPY ../../../shared-proto ./shared-proto

RUN ./mvnw clean package -DskipTests

FROM openjdk:21-jre-slim

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

WORKDIR /app
COPY --from=builder /app/target/order-service-*.jar app.jar

EXPOSE 8087 9087

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8087/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

#### Docker Compose for Local Development

```yaml
# docker-compose.override.yml for development
version: "3.8"

services:
  order-service:
    build:
      context: ./services/commerce/order-service
      dockerfile: Dockerfile
    ports:
      - "8087:8087"
      - "9087:9087"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=mysql
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - mysql
      - redis
      - kafka
    volumes:
      - ./logs:/var/log/ecommerce
```

### ☸️ Kubernetes Deployment

#### Helm Charts Structure

```
helm/
├── ecommerce-backend/          # Umbrella chart
│   ├── Chart.yaml
│   ├── values.yaml
│   └── charts/
│       ├── auth-service/
│       ├── user-service/
│       ├── product-service/
│       ├── inventory-service/
│       ├── cart-service/
│       ├── order-service/
│       └── api-gateway/
├── infrastructure/             # Infrastructure components
│   ├── mysql/
│   ├── mongodb/
│   ├── redis/
│   ├── kafka/
│   └── monitoring/
└── shared/                     # Shared templates
    ├── configmap.yaml
    ├── secret.yaml
    └── service.yaml
```

#### Installing with Helm

```bash
# Add required Helm repositories
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install infrastructure components first
helm install mysql bitnami/mysql -f helm/infrastructure/mysql/values.yaml
helm install mongodb bitnami/mongodb -f helm/infrastructure/mongodb/values.yaml
helm install redis bitnami/redis -f helm/infrastructure/redis/values.yaml
helm install kafka bitnami/kafka -f helm/infrastructure/kafka/values.yaml

# Install monitoring stack
helm install prometheus prometheus-community/kube-prometheus-stack \
  -f helm/infrastructure/monitoring/prometheus-values.yaml

# Install application services
helm install ecommerce-backend helm/ecommerce-backend \
  --set global.environment=production \
  --set global.imageTag=v1.2.0

# Install specific service
helm install order-service helm/ecommerce-backend/charts/order-service \
  --set image.tag=v1.2.0 \
  --set replicaCount=3
```

#### Kubernetes Manifests Example

```yaml
# helm/ecommerce-backend/charts/order-service/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "order-service.fullname" . }}
  labels:
    {{- include "order-service.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "order-service.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "order-service.selectorLabels" . | nindent 8 }}
    spec:
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: 8087
          protocol: TCP
        - name: grpc
          containerPort: 9087
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: {{ .Values.global.environment }}
        - name: DB_HOST
          value: {{ .Values.database.host }}
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: {{ include "order-service.fullname" . }}-db
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: {{ include "order-service.fullname" . }}-db
              key: password
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: http
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: http
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          {{- toYaml .Values.resources | nindent 12 }}
```

#### Environment-Specific Values

```yaml
# helm/ecommerce-backend/values-production.yaml
global:
  environment: production
  imageTag: v1.2.0

orderService:
  replicaCount: 3
  image:
    repository: ecommerce/order-service
    tag: v1.2.0

  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"

  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70
    targetMemoryUtilizationPercentage: 80

  database:
    host: mysql-primary.database.svc.cluster.local
    port: 3306
    name: order_service_db

  redis:
    host: redis-master.cache.svc.cluster.local
    port: 6379

  kafka:
    bootstrapServers: kafka.messaging.svc.cluster.local:9092
```

### 🔄 CI/CD Pipeline

#### GitHub Actions Workflow

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run tests
        run: mvn clean test

      - name: Run integration tests
        run: mvn verify -Dspring.profiles.active=test

      - name: Generate coverage report
        run: mvn jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run Snyk security scan
        uses: snyk/actions/maven@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}

      - name: Run OWASP dependency check
        run: mvn org.owasp:dependency-check-maven:check

  build-and-push:
    needs: [test, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push images
        run: |
          ./scripts/build-and-push-images.sh ${{ github.sha }}

  deploy-staging:
    needs: build-and-push
    runs-on: ubuntu-latest
    environment: staging

    steps:
      - name: Deploy to staging
        run: |
          helm upgrade --install ecommerce-backend helm/ecommerce-backend \
            --set global.imageTag=${{ github.sha }} \
            --set global.environment=staging \
            -f helm/ecommerce-backend/values-staging.yaml

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main'

    steps:
      - name: Deploy to production
        run: |
          helm upgrade --install ecommerce-backend helm/ecommerce-backend \
            --set global.imageTag=${{ github.sha }} \
            --set global.environment=production \
            -f helm/ecommerce-backend/values-production.yaml
```

### 🔧 Production Configuration

#### Resource Requirements

```yaml
# Minimum production resource requirements
resources:
  api-gateway:
    requests: { memory: "256Mi", cpu: "100m" }
    limits: { memory: "512Mi", cpu: "200m" }

  auth-service:
    requests: { memory: "512Mi", cpu: "200m" }
    limits: { memory: "1Gi", cpu: "400m" }

  order-service:
    requests: { memory: "512Mi", cpu: "250m" }
    limits: { memory: "1Gi", cpu: "500m" }

  cart-service:
    requests: { memory: "256Mi", cpu: "150m" }
    limits: { memory: "512Mi", cpu: "300m" }
```

#### High Availability Setup

```yaml
# Production HA configuration
replicaCount: 3

affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app.kubernetes.io/name
                operator: In
                values:
                  - order-service
          topologyKey: kubernetes.io/hostname

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

## 🔐 Security

### 🏢 Multi-Tenancy Architecture

The system implements comprehensive tenant isolation at multiple levels:

#### Database Level Isolation

```sql
-- All tables include tenant_id for row-level security
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    -- ... other columns
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_tenant_user (tenant_id, user_id)
);

-- Automatic tenant filtering in all queries
SELECT * FROM orders WHERE tenant_id = ? AND user_id = ?;
```

#### Application Level Isolation

```java
// Tenant context automatically injected
@Component
public class TenantAwareRepositoryImpl<T, ID> implements TenantAwareRepository<T, ID> {

    @Override
    public List<T> findAll() {
        String tenantId = TenantContext.getTenantId();
        return entityManager.createQuery(
            "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.tenantId = :tenantId"
        ).setParameter("tenantId", tenantId).getResultList();
    }
}

// Automatic tenant context propagation
@RestController
public class OrderController {

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestHeader("X-User-ID") String userId,
        @RequestBody CreateOrderRequest request) {

        // TenantContext automatically set from headers
        // All subsequent operations are tenant-scoped
        return orderService.createOrder(request);
    }
}
```

#### API Level Isolation

```java
// JWT token validation with tenant verification
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) {

        String token = extractToken(request);
        if (jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.getClaimsFromToken(token);

            String tenantId = claims.get("tenant_id", String.class);
            String userId = claims.getSubject();

            // Verify tenant ID matches request header
            String requestTenantId = request.getHeader("X-Tenant-ID");
            if (!tenantId.equals(requestTenantId)) {
                throw new TenantAccessDeniedException("Tenant ID mismatch");
            }

            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(userId);
        }

        filterChain.doFilter(request, response);
    }
}
```

### 🔑 Authentication & Authorization

#### JWT Token Structure

```json
{
  "sub": "12345", // User ID
  "tenant_id": "tenant_123", // Tenant ID
  "roles": ["CUSTOMER", "ADMIN"], // User roles
  "permissions": [
    // Fine-grained permissions
    "order:create",
    "order:read",
    "cart:manage"
  ],
  "iat": 1640995200, // Issued at
  "exp": 1641081600, // Expires at
  "jti": "jwt_id_123" // JWT ID for revocation
}
```

#### Role-Based Access Control (RBAC)

```java
@PreAuthorize("hasRole('CUSTOMER') and @tenantSecurityService.canAccessOrder(#orderId)")
@GetMapping("/orders/{orderId}")
public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
    return orderService.getOrder(orderId);
}

@PreAuthorize("hasRole('ADMIN') and @tenantSecurityService.isSameTenant(#tenantId)")
@GetMapping("/admin/orders")
public ResponseEntity<List<OrderResponse>> getAllOrders(@RequestParam String tenantId) {
    return orderService.getAllOrdersForTenant(tenantId);
}
```

#### Refresh Token Management

```java
@Service
public class RefreshTokenService {

    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        // Validate refresh token
        if (!isValidRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        // Optionally rotate refresh token
        String newRefreshToken = shouldRotateRefreshToken() ?
            generateNewRefreshToken(user) : refreshToken;

        // Revoke old refresh token if rotated
        if (!newRefreshToken.equals(refreshToken)) {
            revokeRefreshToken(refreshToken);
        }

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
```

### 🛡️ Data Protection

#### Password Security

```java
@Service
public class PasswordService {

    private final BCryptPasswordEncoder passwordEncoder =
        new BCryptPasswordEncoder(12); // Strong cost factor

    public String hashPassword(String plainPassword) {
        // Additional validation
        validatePasswordStrength(plainPassword);

        return passwordEncoder.encode(plainPassword);
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < 12) {
            throw new WeakPasswordException("Password must be at least 12 characters");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException("Password must contain uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException("Password must contain lowercase letter");
        }

        if (!password.matches(".*[0-9].*")) {
            throw new WeakPasswordException("Password must contain number");
        }

        if (!password.matches(".*[!@#$%^&*()].*")) {
            throw new WeakPasswordException("Password must contain special character");
        }
    }
}
```

#### PCI DSS Compliance for Payment Data

```java
@Service
public class PaymentSecurityService {

    // Never store raw payment data
    public PaymentToken tokenizePaymentMethod(PaymentMethodRequest request) {
        // Use external tokenization service (Stripe, etc.)
        TokenizationRequest tokenRequest = TokenizationRequest.builder()
            .cardNumber(request.getCardNumber())  // This is immediately discarded
            .expiryMonth(request.getExpiryMonth())
            .expiryYear(request.getExpiryYear())
            .cvv(request.getCvv())  // Never logged or stored
            .build();

        // Get token from payment gateway
        PaymentToken token = paymentGateway.tokenize(tokenRequest);

        // Store only the token, never raw data
        return PaymentToken.builder()
            .tokenId(token.getTokenId())
            .last4Digits(token.getLast4Digits())
            .cardType(token.getCardType())
            .expiryMonth(token.getExpiryMonth())
            .expiryYear(token.getExpiryYear())
            .build();
    }

    // All payment operations use tokens
    public PaymentResult processPayment(String paymentToken, Money amount) {
        return paymentGateway.charge(paymentToken, amount);
    }
}
```

#### Encryption at Rest and in Transit

```yaml
# Database encryption configuration
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce?useSSL=true&requireSSL=true&verifyServerCertificate=true
    hikari:
      connection-init-sql: "SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO'"

  jpa:
    properties:
      hibernate:
        # Enable column-level encryption for sensitive data
        type:
          descriptor:
            sql:
              BasicBinder: TRACE
```

```java
// Field-level encryption for sensitive data
@Entity
public class UserProfile {

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
}

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
```

### 🔒 API Security

#### Rate Limiting

```java
@Component
public class TenantRateLimitingFilter implements Filter {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = httpRequest.getHeader("X-Tenant-ID");
        String userId = httpRequest.getHeader("X-User-ID");

        // Tenant-level rate limiting (1000 requests per minute)
        String tenantKey = "rate_limit:tenant:" + tenantId;
        if (isRateLimitExceeded(tenantKey, 1000, 60)) {
            throw new RateLimitExceededException("Tenant rate limit exceeded");
        }

        // User-level rate limiting (100 requests per minute)
        String userKey = "rate_limit:user:" + tenantId + ":" + userId;
        if (isRateLimitExceeded(userKey, 100, 60)) {
            throw new RateLimitExceededException("User rate limit exceeded");
        }

        chain.doFilter(request, response);
    }
}
```

#### Input Validation and Sanitization

```java
@RestController
@Validated
public class OrderController {

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("X-Tenant-ID") @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String tenantId,
        @RequestHeader("X-User-ID") @Pattern(regexp = "^[0-9]+$") String userId,
        @Valid @RequestBody CreateOrderRequest request) {

        // Additional business validation
        orderValidationService.validateOrderRequest(request);

        return orderService.createOrder(request);
    }
}

@Component
public class OrderValidationService {

    public void validateOrderRequest(CreateOrderRequest request) {
        // Validate order items
        if (request.getItems().size() > 100) {
            throw new ValidationException("Too many items in order");
        }

        // Validate monetary amounts
        for (CreateOrderItemRequest item : request.getItems()) {
            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Invalid item price");
            }

            if (item.getQuantity() <= 0 || item.getQuantity() > 1000) {
                throw new ValidationException("Invalid item quantity");
            }
        }

        // Validate addresses
        validateAddress(request.getBillingAddress());
        validateAddress(request.getShippingAddress());
    }
}
```

### 🔐 Security Headers and CORS

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
                .and()
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Using JWT tokens
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("https://*.example.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:

- Create an issue in the repository
- Check the documentation in the `docs/` directory
- Review the troubleshooting guide

## 🔧 Troubleshooting

### 🚨 Common Issues and Solutions

#### 1. Services Not Starting

```bash
# Check infrastructure services status
docker-compose ps

# Check service logs
docker-compose logs -f mysql
docker-compose logs -f kafka
docker-compose logs -f redis

# Restart infrastructure
docker-compose down
docker-compose up -d

# Wait for services to be ready
./scripts/check-health.sh
```

#### 2. Database Connection Errors

```bash
# Check MySQL connectivity
docker exec -it mysql mysql -u ecommerce_user -p

# Verify database creation
docker exec -it mysql mysql -u root -p -e "SHOW DATABASES;"

# Check connection pool settings
curl http://localhost:8087/actuator/metrics/hikaricp.connections.active

# Common fixes:
# - Increase connection timeout in application.yml
# - Check firewall settings
# - Verify credentials in environment variables
```

#### 3. Kafka Connection Issues

```bash
# Check Kafka cluster health
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Check consumer group status
docker exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Verify topic creation
curl http://localhost:8080  # Kafka UI

# Common fixes:
# - Ensure Zookeeper is running first
# - Check KAFKA_ADVERTISED_LISTENERS configuration
# - Verify topic auto-creation settings
```

#### 4. Redis Connection Issues

```bash
# Test Redis connectivity
docker exec -it redis redis-cli ping

# Check Redis memory usage
docker exec -it redis redis-cli info memory

# Monitor Redis operations
docker exec -it redis redis-cli monitor

# Common fixes:
# - Check Redis password configuration
# - Verify Redis maxmemory settings
# - Check connection pool configuration
```

#### 5. gRPC Communication Errors

```bash
# Test gRPC service health
grpcurl -plaintext localhost:9087 grpc.health.v1.Health/Check

# List available gRPC services
grpcurl -plaintext localhost:9087 list

# Common fixes:
# - Verify gRPC server port configuration
# - Check service discovery settings
# - Ensure Protocol Buffer compatibility
```

#### 6. Port Conflicts

```bash
# Check port usage
netstat -tulpn | grep :8087
lsof -i :8087

# Kill process using port
kill -9 $(lsof -t -i:8087)

# Use alternative ports
export SERVER_PORT=8097
mvn spring-boot:run
```

### 🏥 Health Checks and Diagnostics

#### Service Health Endpoints

```bash
# Comprehensive health check
curl http://localhost:8087/actuator/health | jq

# Detailed health information
curl http://localhost:8087/actuator/health/db | jq
curl http://localhost:8087/actuator/health/redis | jq
curl http://localhost:8087/actuator/health/kafka | jq

# Liveness and readiness probes
curl http://localhost:8087/actuator/health/liveness
curl http://localhost:8087/actuator/health/readiness
```

#### Performance Metrics

```bash
# JVM metrics
curl http://localhost:8087/actuator/metrics/jvm.memory.used | jq
curl http://localhost:8087/actuator/metrics/jvm.gc.pause | jq

# HTTP request metrics
curl http://localhost:8087/actuator/metrics/http.server.requests | jq

# Database metrics
curl http://localhost:8087/actuator/metrics/hikaricp.connections | jq

# Custom business metrics
curl http://localhost:8087/actuator/metrics/orders.created.total | jq
```

#### Log Analysis

```bash
# View structured logs
docker-compose logs -f order-service | jq

# Filter logs by level
docker-compose logs order-service | jq 'select(.level == "ERROR")'

# Filter logs by tenant
docker-compose logs order-service | jq 'select(.tenantId == "tenant_123")'

# Search for specific correlation ID
docker-compose logs order-service | jq 'select(.correlationId == "corr_123")'
```

### 🔍 Performance Troubleshooting

#### Memory Issues

```bash
# Generate heap dump
curl -X POST http://localhost:8087/actuator/heapdump

# Analyze GC performance
curl http://localhost:8087/actuator/metrics/jvm.gc.pause | jq

# Check memory pools
curl http://localhost:8087/actuator/metrics/jvm.memory.used | jq

# JVM flags for debugging
export JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof"
```

#### Database Performance

```bash
# Check slow queries
docker exec -it mysql mysql -u root -p -e "
  SELECT query_time, lock_time, rows_sent, rows_examined, sql_text
  FROM mysql.slow_log
  ORDER BY query_time DESC
  LIMIT 10;"

# Monitor connection pool
curl http://localhost:8087/actuator/metrics/hikaricp.connections.active

# Check database locks
docker exec -it mysql mysql -u root -p -e "SHOW PROCESSLIST;"
```

#### Network Issues

```bash
# Test service-to-service connectivity
curl -v http://product-service:8084/actuator/health

# Check DNS resolution
nslookup product-service
dig product-service

# Test gRPC connectivity
grpcurl -plaintext product-service:9084 grpc.health.v1.Health/Check
```

### 📊 Monitoring and Alerting

#### Prometheus Queries for Troubleshooting

```promql
# High error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1

# High response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1

# Memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8

# Database connection pool exhaustion
hikaricp_connections_active / hikaricp_connections_max > 0.9

# Kafka consumer lag
kafka_consumer_lag_sum > 1000
```

#### Grafana Dashboard Alerts

- **Service Down**: No metrics received for 2 minutes
- **High Error Rate**: >5% error rate for 5 minutes
- **High Response Time**: 95th percentile >1s for 5 minutes
- **Memory Usage**: >80% heap usage for 10 minutes
- **Database Issues**: Connection pool >90% for 5 minutes

### 🛠️ Development Troubleshooting

#### IDE Setup Issues

```bash
# Regenerate Protocol Buffer classes
./scripts/generate-proto.sh

# Refresh Maven dependencies
mvn dependency:resolve
mvn clean compile

# Fix IDE indexing issues
rm -rf .idea/
mvn idea:idea
```

#### Test Failures

```bash
# Run tests with debug output
mvn test -X

# Run specific failing test
mvn test -Dtest=OrderServiceTest#testCreateOrder

# Run tests with Testcontainers debug
mvn test -Dtestcontainers.reuse.enable=true -Dlogging.level.org.testcontainers=DEBUG
```

#### Build Issues

```bash
# Clean and rebuild everything
mvn clean install -DskipTests

# Build with dependency tree
mvn dependency:tree

# Resolve version conflicts
mvn dependency:resolve-sources
mvn versions:display-dependency-updates
```

### 📞 Support and Resources

#### Getting Help

1. **Check the logs** first using structured log queries
2. **Review health endpoints** for service status
3. **Check Prometheus metrics** for performance issues
4. **Use Jaeger tracing** to debug distributed calls
5. **Create GitHub issue** with logs and reproduction steps

#### Useful Commands Reference

```bash
# Quick health check all services
./scripts/check-health.sh

# View all service logs
./scripts/view-logs.sh

# Restart specific service
./scripts/restart-service.sh order-service

# Reset all data (development only)
./scripts/reset-infrastructure.sh

# Generate load test
./scripts/run-load-test.sh order-creation
```

#### Documentation Links

- **API Documentation**: http://localhost:8087/swagger-ui.html
- **Metrics Dashboard**: http://localhost:3000 (Grafana)
- **Distributed Tracing**: http://localhost:16686 (Jaeger)
- **Kafka Management**: http://localhost:8080 (Kafka UI)
- **Service Health**: http://localhost:8087/actuator/health

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow the existing code style and patterns
- Write comprehensive tests for new features
- Update documentation for API changes
- Ensure all tests pass before submitting PR
- Add appropriate logging and monitoring

### Code Review Process

- All changes require review from at least one maintainer
- Automated tests must pass
- Security scan must pass
- Performance impact must be evaluated

---

## 📞 Support

For support and questions:

- **Create an issue** in the GitHub repository
- **Check the troubleshooting guide** above
- **Review the API documentation** at service endpoints
- **Monitor system health** using provided dashboards

---

**Built with ❤️ using Spring Boot, Java 21, and modern cloud-native technologies.**`

```

```

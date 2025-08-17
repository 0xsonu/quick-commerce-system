# Amazon Shopping Backend

A scalable, multi-tenant e-commerce backend system built with Spring Boot microservices architecture.

## Architecture Overview

This system consists of the following microservices organized by business domain:

### Core Services

- **API Gateway** (Port 8081) - Request routing and authentication
- **Auth Service** (Port 8082) - Authentication and authorization
- **User Service** (Port 8083) - User profile management

### Catalog Services

- **Product Service** (Port 8084) - Product catalog management
- **Inventory Service** (Port 8085) - Stock management

### Commerce Services

- **Cart Service** (Port 8086) - Shopping cart operations
- **Order Service** (Port 8087) - Order processing
- **Payment Service** (Port 8088) - Payment processing

### Fulfillment Services

- **Shipping Service** (Port 8089) - Shipment tracking
- **Notification Service** (Port 8090) - Multi-channel notifications

### Engagement Services

- **Review Service** (Port 8091) - Product reviews and ratings

## Technology Stack

- **Java**: OpenJDK 21 LTS
- **Spring Boot**: 3.2.x
- **Spring Security**: 6.2.x
- **MySQL**: 8.0 (Primary database)
- **MongoDB**: 7.0 (Document storage)
- **Redis**: 7.2 (Caching and session storage)
- **Apache Kafka**: 3.6 (Event streaming)
- **Docker**: Containerization
- **Kubernetes**: Orchestration
- **Prometheus**: Metrics collection
- **Grafana**: Monitoring dashboards
- **Jaeger**: Distributed tracing

## Prerequisites

- Java 21 LTS
- Maven 3.9+
- Docker and Docker Compose
- Git

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd amazon-shopping-backend
```

### 2. Start Infrastructure Services

```bash
# Start all infrastructure services (MySQL, MongoDB, Redis, Kafka, etc.)
docker-compose up -d

# Wait for services to be ready (check health status)
docker-compose ps
```

### 3. Build All Services

```bash
# Build all modules
mvn clean install -DskipTests

# Or build individual services
cd auth-service && mvn clean install -DskipTests
```

### 4. Run Services

You can run services individually:

```bash
# Start API Gateway
cd api-gateway && mvn spring-boot:run

# Start Auth Service
cd auth-service && mvn spring-boot:run

# Start other services as needed...
```

Or use the provided script to start any service:

```bash
# Make scripts executable
chmod +x scripts/*.sh

# Start a specific service
./scripts/start-service.sh api-gateway
./scripts/start-service.sh auth-service
./scripts/start-service.sh user-service
# ... etc

# Available services:
# api-gateway, auth-service, user-service, product-service,
# inventory-service, cart-service, order-service, payment-service,
# shipping-service, notification-service, review-service
```

## Available Scripts

The project includes several utility scripts in the `scripts/` directory:

- **`build-all.sh`** - Builds all modules including shared libraries and services
- **`start-infrastructure.sh`** - Starts all infrastructure services (MySQL, MongoDB, Redis, Kafka, etc.) using Docker Compose
- **`start-service.sh <service-name>`** - Starts a specific microservice

```bash
# Make scripts executable
chmod +x scripts/*.sh

# Build everything
./scripts/build-all.sh

# Start infrastructure
./scripts/start-infrastructure.sh

# Start a specific service
./scripts/start-service.sh auth-service
```

## Development Setup

### Database Setup

The Docker Compose configuration automatically creates the required databases:

- `auth_service` - Authentication data
- `user_service` - User profiles and addresses
- `inventory_service` - Stock levels and transactions
- `cart_service` - Cart backup storage
- `order_service` - Orders and order items
- `payment_service` - Payment transactions
- `shipping_service` - Shipment tracking

### MongoDB Collections

- `products` - Product catalog
- `reviews` - Product reviews and ratings

### Redis Usage

- Session storage
- Caching layer
- Shopping cart primary storage

### Kafka Topics

The system uses the following Kafka topics for event-driven communication:

- `product-events` - Product lifecycle events
- `order-events` - Order processing events
- `payment-events` - Payment status events
- `inventory-events` - Stock level changes
- `notification-events` - Notification triggers

## Configuration

### Environment Variables

Key environment variables for production:

```bash
# JWT Configuration
JWT_SECRET=your-production-secret-key

# Database URLs
MYSQL_URL=jdbc:mysql://mysql-host:3306/
MONGODB_URL=mongodb://mongo-host:27017/
REDIS_URL=redis://redis-host:6379

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092

# External Services
PAYMENT_GATEWAY_URL=https://payment-provider.com/api
EMAIL_SERVICE_URL=https://email-provider.com/api
SMS_SERVICE_URL=https://sms-provider.com/api
```

### Service Configuration

Each service can be configured via `application.yml` or environment variables. See individual service directories for specific configuration options.

## Monitoring and Observability

### Prometheus Metrics

Access Prometheus at: http://localhost:9090

All services expose metrics at `/actuator/prometheus`

### Grafana Dashboards

Access Grafana at: http://localhost:3000

- Username: admin
- Password: admin

### Distributed Tracing

Access Jaeger UI at: http://localhost:16686

### Kafka Management

Access Kafka UI at: http://localhost:8080

## API Documentation

Each service exposes OpenAPI documentation at:

- `http://localhost:<port>/swagger-ui.html`
- `http://localhost:<port>/v3/api-docs`

## Testing

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run tests for specific service
cd user-service && mvn test
```

### Integration Tests

```bash
# Run integration tests (requires Docker)
mvn verify

# Run integration tests for specific service
cd user-service && mvn verify
```

### Load Testing

Load testing scripts are available in the `load-tests/` directory.

## Deployment

### Docker Images

Build Docker images for services:

```bash
# Build specific service image
docker build -t ecommerce/auth-service:latest auth-service/
docker build -t ecommerce/user-service:latest user-service/
# ... etc for other services
```

### Kubernetes Deployment

Helm charts are available in the `helm/` directory:

```bash
# Install all services
helm install ecommerce-backend helm/ecommerce-backend

# Install specific service
helm install auth-service helm/auth-service
```

## Security

### Multi-Tenancy

The system implements tenant isolation at multiple levels:

- Database level (tenant_id columns)
- Application level (tenant context)
- API level (JWT token validation)

### Authentication

- JWT-based authentication
- Refresh token support
- Role-based authorization

### Data Protection

- Password hashing with BCrypt
- PCI DSS compliance for payment data
- Encryption at rest and in transit

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

## Troubleshooting

### Common Issues

1. **Services not starting**: Check if all infrastructure services are running
2. **Database connection errors**: Verify database credentials and connectivity
3. **Kafka connection issues**: Ensure Kafka and Zookeeper are healthy
4. **Port conflicts**: Check if ports are already in use

### Health Checks

All services expose health endpoints:

- `http://localhost:<port>/actuator/health`

### Logs

Service logs are structured JSON format and can be viewed with:

```bash
# View logs for specific service
docker-compose logs -f <service-name>

# View all logs
docker-compose logs -f
```

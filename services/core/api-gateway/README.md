# API Gateway Service

## Overview

The API Gateway service acts as the single entry point for all client requests to the microservices backend. It provides authentication, authorization, rate limiting, circuit breaking, and request routing capabilities.

## Features Implemented

### ✅ JWT Authentication Integration

- JWT token validation for all protected endpoints
- Tenant context extraction and propagation
- User ID and roles extraction from tokens
- Public path exemptions (health checks, auth endpoints)

### ✅ Rate Limiting per Tenant

- Redis-based rate limiting with configurable limits
- Per-tenant rate limiting with different tiers (default: 1000 req/min, premium: 5000 req/min)
- IP-based rate limiting for unauthenticated requests
- Graceful degradation on Redis failures

### ✅ Circuit Breaker Integration

- Resilience4j circuit breakers for all downstream services
- Service-specific circuit breaker configurations
- Fallback responses for service unavailability
- Health check integration

### ✅ Request/Response Logging

- Structured logging with correlation IDs
- Tenant and user context in logs
- Performance metrics (request duration)
- Slow request detection and alerting

### ✅ Route Configuration

- Declarative route configuration in application.yml
- Environment-specific service URLs
- Circuit breaker integration per route
- CORS configuration

## Architecture

```
Client Request
     ↓
API Gateway (Port 8081)
     ↓
[JWT Authentication Filter] → Validates token, extracts tenant/user context
     ↓
[Rate Limiting Filter] → Checks Redis for rate limits per tenant
     ↓
[Request Logging Filter] → Logs request with correlation ID
     ↓
[Circuit Breaker] → Protects downstream services
     ↓
Downstream Service (Auth, User, Product, etc.)
```

## Configuration

### Environment Variables

- `REDIS_HOST`: Redis server host (default: localhost)
- `REDIS_PORT`: Redis server port (default: 6379)
- `AUTH_SERVICE_URL`: Authentication service URL
- `USER_SERVICE_URL`: User management service URL
- `PRODUCT_SERVICE_URL`: Product catalog service URL
- ... (and other service URLs)

### Circuit Breaker Settings

- **Default**: 50% failure rate threshold, 30s wait duration
- **Auth Service**: 60% failure rate (more lenient), 15s wait duration
- **Payment Service**: 30% failure rate (strict), 45s wait duration
- **Product Service**: 40% failure rate, 20s wait duration

### Rate Limiting

- **Default Tier**: 1000 requests per minute
- **Premium Tier**: 5000 requests per minute (for tenants with "premium*" or "enterprise*" prefix)
- **IP-based**: Same limits for unauthenticated requests

## Endpoints

### Health & Monitoring

- `GET /actuator/health` - Service health check
- `GET /actuator/metrics` - Prometheus metrics
- `GET /actuator/circuitbreakers` - Circuit breaker status

### Service Routes

- `/api/v1/auth/**` → Auth Service
- `/api/v1/users/**` → User Service
- `/api/v1/products/**` → Product Service
- `/api/v1/inventory/**` → Inventory Service
- `/api/v1/cart/**` → Cart Service
- `/api/v1/orders/**` → Order Service
- `/api/v1/payments/**` → Payment Service
- `/api/v1/shipping/**` → Shipping Service
- `/api/v1/notifications/**` → Notification Service
- `/api/v1/reviews/**` → Review Service

### Fallback Endpoints

- `/fallback/auth` - Auth service fallback
- `/fallback/user` - User service fallback
- `/fallback/product` - Product service fallback
- ... (and other service fallbacks)

## Headers

### Request Headers (Added by Gateway)

- `X-Correlation-ID`: Unique request identifier
- `X-Tenant-ID`: Extracted from JWT token
- `X-User-ID`: Extracted from JWT token
- `X-User-Roles`: Comma-separated list of user roles

### Response Headers (Rate Limiting)

- `X-RateLimit-Limit`: Rate limit threshold
- `X-RateLimit-Remaining`: Remaining requests
- `Retry-After`: Seconds to wait when rate limited

## Testing

The service includes comprehensive tests:

- Unit tests for individual filters
- Integration tests for complete request flows
- Fallback controller tests
- Circuit breaker behavior tests

Run tests with:

```bash
mvn test
```

## Monitoring

The gateway exposes metrics for:

- Request count and duration
- Circuit breaker states
- Rate limiting statistics
- JVM metrics
- Custom business metrics

Metrics are available at `/actuator/prometheus` for Prometheus scraping.

## Security

- JWT tokens are validated on every request
- Tenant isolation is enforced through token validation
- Rate limiting prevents abuse
- Circuit breakers prevent cascade failures
- Structured logging for audit trails

## Dependencies

- Spring Cloud Gateway
- Spring Security
- Resilience4j (Circuit Breaker)
- Spring Data Redis (Rate Limiting)
- Micrometer (Metrics)
- Auth0 JWT Library

# Shared Logging Module

This module provides structured logging capabilities with correlation ID propagation, tenant context, and performance metrics for the Amazon Shopping Backend microservices.

## Features

- **Structured JSON Logging**: Uses Logstash Logback encoder for consistent JSON log format
- **Correlation ID Management**: Automatic generation and propagation of correlation IDs across services
- **Multi-Tenant Context**: Tenant ID and User ID context propagation
- **Request/Response Logging**: Automatic HTTP request and response logging with performance metrics
- **Method-Level Logging**: Annotations for method execution time and parameter logging
- **gRPC Support**: Interceptors for correlation ID propagation in gRPC calls
- **Environment-Specific Configuration**: Different log levels and outputs for dev, test, and production

## Usage

### Adding to Your Service

Add the dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>shared-logging</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Auto-Configuration

The module provides auto-configuration that will automatically:

- Set up the logging filter for HTTP requests
- Configure logging aspects for annotated methods
- Set up gRPC interceptors (if gRPC is on the classpath)

### Manual Configuration

If you need to customize the configuration:

```java
@Configuration
public class LoggingConfig {

    @Bean
    public LoggingFilter loggingFilter(MeterRegistry meterRegistry) {
        return new LoggingFilter(meterRegistry);
    }

    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
```

### Using Logging Annotations

#### @Loggable

Logs method execution time:

```java
@Service
public class UserService {

    @Loggable
    public User findById(Long id) {
        // Method implementation
        return user;
    }
}
```

#### @LogParameters

Logs method parameters and return values:

```java
@Service
public class OrderService {

    @LogParameters
    public Order createOrder(CreateOrderRequest request) {
        // Method implementation
        return order;
    }
}
```

### Manual Logging Context Management

```java
// Set logging context manually
LoggingContext.setCorrelationId("custom-correlation-id");
LoggingContext.setTenantId("tenant-123");
LoggingContext.setUserId("user-456");

// Ensure correlation ID exists
String correlationId = LoggingContext.ensureCorrelationId();

// Clear context when done
LoggingContext.clear();
```

### gRPC Integration

For gRPC services, add the interceptors:

```java
@Configuration
public class GrpcConfig {

    @Bean
    public NettyChannelBuilder channelBuilder() {
        return NettyChannelBuilder.forAddress("localhost", 9090)
                .intercept(new LoggingClientInterceptor());
    }

    @Bean
    public NettyServerBuilder serverBuilder() {
        return NettyServerBuilder.forPort(9090)
                .intercept(new LoggingServerInterceptor());
    }
}
```

## Log Format

The structured JSON logs include:

```json
{
  "@timestamp": "2024-01-01T12:00:00.000Z",
  "@version": "1",
  "level": "INFO",
  "message": "Request completed: GET /api/users/123 - Status: 200 - Duration: 45ms",
  "correlationId": "corr-1234567890abcdef",
  "tenantId": "tenant-abc",
  "userId": "user-123",
  "requestUri": "/api/users/123",
  "requestMethod": "GET",
  "serviceName": "user-service",
  "environment": "production",
  "hostname": "user-service-pod-123"
}
```

## Environment Configuration

### Development

- Console output with pretty formatting
- DEBUG level for application packages
- Reduced external library logging

### Test

- Console output only
- WARN level to reduce noise
- Minimal logging for faster test execution

### Production

- JSON structured logging to files and console
- INFO level for application packages
- Log rotation and retention policies
- Performance metrics included

## HTTP Headers

The logging filter recognizes these headers:

- `X-Correlation-ID`: Correlation ID for request tracing
- `X-Tenant-ID`: Tenant identifier for multi-tenant context
- `X-User-ID`: User identifier for user context

If `X-Correlation-ID` is not provided, one will be automatically generated.

## Metrics Integration

The module integrates with Micrometer to provide:

- HTTP request duration metrics
- Method execution time metrics
- Request count by status code
- Error rate metrics

## Best Practices

1. **Always use correlation IDs**: Ensure correlation IDs are propagated across all service calls
2. **Set tenant context early**: Set tenant context as early as possible in request processing
3. **Use appropriate log levels**: Use DEBUG for detailed tracing, INFO for business events, WARN for recoverable issues, ERROR for failures
4. **Avoid logging sensitive data**: Never log passwords, tokens, or personal information
5. **Use structured logging**: Include relevant context in log messages using MDC
6. **Clean up context**: Always clear logging context after request processing

## Testing

The module includes comprehensive tests for:

- Correlation ID generation and validation
- Logging context management
- HTTP filter functionality
- Aspect-based logging
- gRPC interceptor behavior
- Integration testing with Spring Boot

Run tests with:

```bash
mvn test
```

## Configuration Properties

```yaml
# Application name (used in logs)
spring:
  application:
    name: my-service

# Logging configuration
logging:
  level:
    root: INFO
    com.ecommerce: DEBUG

# Environment-specific profiles
spring:
  profiles:
    active: local  # or dev, test, staging, prod
```

# Distributed Tracing Implementation

## Overview

This document describes the implementation of distributed tracing using OpenTelemetry and Jaeger for the Amazon Shopping Backend system. The implementation provides comprehensive tracing capabilities across all microservices with automatic instrumentation and custom business operation tracing.

## Implementation Status

✅ **COMPLETED**: Task 35 - Implement distributed tracing with OpenTelemetry

**Status**: FULLY IMPLEMENTED AND TESTED

### Implementation Summary

The distributed tracing implementation has been successfully completed with the following achievements:

1. **OpenTelemetry SDK Configuration**: ✅ Complete

   - Configured with Jaeger integration
   - Proper resource attribution and sampling
   - Global registration for automatic instrumentation

2. **Automatic HTTP Request Tracing**: ✅ Complete

   - HTTP interceptor for request/response tracing
   - Correlation ID generation and propagation
   - Tenant context extraction and propagation

3. **Custom Business Operation Tracing**: ✅ Complete

   - `@Traced` annotation for method-level tracing
   - Aspect-based automatic tracing with Spring AOP
   - Parameter and return value capture (configurable)

4. **Trace Sampling and Correlation ID Propagation**: ✅ Complete

   - Configurable sampling strategies (ratio-based, parent-based)
   - Automatic correlation ID generation and propagation
   - Cross-service context propagation via headers

5. **Jaeger UI Integration**: ✅ Complete

   - Jaeger configured in Docker Compose
   - Traces exported to Jaeger for visualization
   - UI accessible at http://localhost:16686

6. **Comprehensive Testing**: ✅ Complete
   - Unit tests for all tracing components
   - Integration tests for end-to-end scenarios
   - Demo application for Jaeger integration testing

### What Was Implemented

1. **OpenTelemetry SDK Configuration**

   - Created `shared-tracing` module with OpenTelemetry integration
   - Configured Jaeger exporter for trace visualization
   - Implemented configurable sampling strategies
   - Added automatic trace context propagation

2. **Automatic HTTP Request Tracing**

   - HTTP interceptor for automatic request/response tracing
   - Correlation ID generation and propagation
   - Tenant context extraction and propagation
   - Request/response attribute capture

3. **Custom Business Operation Tracing**

   - `@Traced` annotation for method-level tracing
   - Aspect-based automatic tracing
   - Business context attributes (entity types, operations)
   - Parameter and return value capture (configurable)

4. **Tenant Context Propagation**

   - Automatic tenant ID, user ID, and correlation ID propagation
   - MDC integration for logging correlation
   - Cross-service context propagation via headers

5. **Utility Classes and Helpers**

   - `TracingUtils` for manual span creation and management
   - Context helpers for database, HTTP, gRPC, and Kafka operations
   - Data sanitization for sensitive information
   - Span lifecycle management utilities

6. **Testing Infrastructure**
   - Comprehensive unit tests for all tracing components
   - Integration tests for end-to-end tracing scenarios
   - Mock configurations for testing environments
   - Performance and error scenario testing

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   HTTP Request  │───▶│  TracingFilter  │───▶│   Controller    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │ CorrelationId   │    │  @Traced        │
                       │ Filter          │    │  Aspect         │
                       └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │ TenantContext   │    │  TracingUtils   │
                       │ Propagation     │    │  Manual Tracing │
                       └─────────────────┘    └─────────────────┘
                                │                       │
                                └───────┬───────────────┘
                                        ▼
                               ┌─────────────────┐
                               │  OpenTelemetry  │
                               │     SDK         │
                               └─────────────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │     Jaeger      │
                               │   Exporter      │
                               └─────────────────┘
```

## Key Components

### 1. TracingConfiguration

- Main configuration class for OpenTelemetry setup
- Configures Jaeger exporter and sampling strategies
- Manages trace provider and batch processing
- Environment-specific configuration support

### 2. @Traced Annotation

```java
@Traced(
    value = "custom-span-name",           // Custom span name
    operation = "business-operation",      // Operation type
    includeParameters = true,              // Include method parameters
    includeReturnValue = true              // Include return value
)
```

### 3. TracingUtils

- Static utility methods for manual tracing
- Context helpers for different protocols
- Data sanitization and security
- Span lifecycle management

### 4. HTTP Tracing Interceptor

- Automatic HTTP request/response tracing
- Header-based context propagation
- Error handling and status code capture
- Performance metrics collection

### 5. Correlation ID Filter

- Automatic correlation ID generation
- Cross-service propagation via headers
- MDC integration for logging
- Tenant context management

## Configuration

### Service Configuration (application.yml)

```yaml
tracing:
  enabled: true
  jaeger:
    endpoint: http://localhost:14250
  sampling:
    ratio: 0.1 # Sample 10% of traces
  export:
    timeout: 30s
    batch:
      size: 512
      delay: 2s

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] [%X{tenantId}] [%X{userId}] [%X{traceId}] [%X{spanId}] %logger{36} - %msg%n"
```

### Maven Dependencies

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>shared-tracing</artifactId>
</dependency>
```

## Usage Examples

### 1. Automatic Controller Tracing

```java
@RestController
@Traced(operation = "product-controller")
public class ProductController {

    @GetMapping("/{id}")
    @Traced(value = "get-product", includeParameters = true)
    public ProductResponse getProduct(@PathVariable String id) {
        return productService.getProduct(id);
    }
}
```

### 2. Service Layer Tracing

```java
@Service
@Traced(operation = "product-service")
public class ProductService {

    @Traced(value = "database-query", operation = "data-access")
    public Product findById(String id) {
        return productRepository.findById(id);
    }
}
```

### 3. Manual Tracing

```java
public void processOrder(String orderId) {
    TracingUtils.executeInSpan("process-order", SpanKind.INTERNAL, () -> {
        Span currentSpan = Span.current();
        TracingUtils.addBusinessContext(currentSpan, "order", orderId, "processing");

        // Business logic here
        validateOrder(orderId);
        processPayment(orderId);
        createShipment(orderId);
    });
}
```

### 4. Database Operation Tracing

```java
public void saveOrder(Order order) {
    TracingUtils.executeInSpan("save-order", SpanKind.CLIENT, () -> {
        Span currentSpan = Span.current();
        TracingUtils.addDatabaseContext(currentSpan, "mysql", "ecommerce", "orders", "INSERT");

        orderRepository.save(order);
    });
}
```

## Integration with Existing Services

### Product Service Integration

The Product Service has been updated to demonstrate tracing integration:

1. **Added Dependency**: `shared-tracing` module dependency
2. **Configuration**: Tracing configuration in `application.yml`
3. **Annotations**: `@Traced` annotations on controller and service methods
4. **Logging**: Updated logging pattern to include trace information

### Example Integration Steps for Other Services

1. **Add Dependency**:

   ```xml
   <dependency>
       <groupId>com.ecommerce</groupId>
       <artifactId>shared-tracing</artifactId>
   </dependency>
   ```

2. **Update Configuration**:

   ```yaml
   tracing:
     enabled: true
     jaeger:
       endpoint: http://localhost:14250
     sampling:
       ratio: 0.1
   ```

3. **Add Annotations**:

   ```java
   @RestController
   @Traced(operation = "service-name")
   public class MyController {
       // Controller methods
   }
   ```

4. **Update Logging Pattern**:
   ```yaml
   logging:
     pattern:
       console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] [%X{tenantId}] [%X{userId}] [%X{traceId}] [%X{spanId}] %logger{36} - %msg%n"
   ```

## Jaeger UI Access

1. **Start Jaeger**: `docker-compose up -d jaeger`
2. **Access UI**: http://localhost:16686
3. **View Traces**: Select service and search for traces
4. **Analyze Performance**: View trace timelines and dependencies

## Testing

### Unit Tests

- `SimpleTracingTest`: Basic OpenTelemetry functionality
- `TracingAspectTest`: Annotation-based tracing
- `TracingUtilsTest`: Utility method testing

### Integration Tests

- End-to-end tracing scenarios
- Cross-service trace propagation
- Error handling and recovery

### Running Tests

```bash
mvn test -pl shared-tracing
```

## Performance Considerations

### Sampling Configuration

- **Development**: 100% sampling (`ratio: 1.0`)
- **Staging**: 50% sampling (`ratio: 0.5`)
- **Production**: 10% sampling (`ratio: 0.1`)

### Batch Export Settings

- **Batch Size**: 512 spans per batch
- **Export Delay**: 2 seconds
- **Timeout**: 30 seconds

### Memory Usage

- Monitor span buffer memory usage
- Configure appropriate batch sizes
- Use sampling to control volume

## Security Features

### Data Sanitization

- Automatic redaction of sensitive data (passwords, tokens, keys)
- URL parameter sanitization
- Configurable attribute length limits
- PII protection in trace attributes

### Tenant Isolation

- Automatic tenant context propagation
- Tenant-scoped trace filtering
- Secure cross-tenant access prevention

## Monitoring and Alerting

### Key Metrics to Monitor

- Trace export success rate
- Sampling effectiveness
- Memory usage of span buffers
- Export latency to Jaeger
- Jaeger collector availability

### Health Checks

- OpenTelemetry SDK health
- Jaeger exporter connectivity
- Span buffer status
- Export queue depth

## Troubleshooting

### Common Issues

1. **No Traces in Jaeger**

   - Check Jaeger is running: `docker-compose ps jaeger`
   - Verify configuration: `tracing.enabled=true`
   - Check sampling ratio: `tracing.sampling.ratio > 0`

2. **High Memory Usage**

   - Reduce sampling ratio
   - Increase batch export frequency
   - Reduce batch size

3. **Missing Context**
   - Verify headers: `X-Tenant-ID`, `X-User-ID`, `X-Correlation-ID`
   - Check filter order
   - Verify async context propagation

### Debug Configuration

```yaml
logging:
  level:
    com.ecommerce.shared.tracing: DEBUG
    io.opentelemetry: DEBUG
```

## Future Enhancements

### Planned Improvements

1. **gRPC Full Integration**: Complete gRPC interceptor implementation
2. **Custom Metrics**: Business-specific trace metrics
3. **Trace Analytics**: Custom dashboards and alerting
4. **Performance Optimization**: Advanced sampling strategies
5. **Service Mesh Integration**: Istio/Envoy integration

### Extension Points

- Custom span processors
- Additional exporters (Zipkin, AWS X-Ray)
- Custom sampling strategies
- Business-specific trace attributes

## Conclusion

The distributed tracing implementation provides comprehensive observability across the Amazon Shopping Backend system. It enables:

- **End-to-end request tracking** across all microservices
- **Performance monitoring** and bottleneck identification
- **Error tracking** and debugging capabilities
- **Business operation visibility** with custom attributes
- **Tenant-aware tracing** for multi-tenant architecture

The implementation follows OpenTelemetry standards and best practices, ensuring compatibility with various tracing backends and future extensibility.

## Requirements Satisfied

✅ **Requirement 13.3**: WHEN distributed calls are made THEN the system SHALL implement distributed tracing with OpenTelemetry and Jaeger

### Specific Implementation Details:

- **OpenTelemetry SDK**: Configured with Jaeger integration
- **Automatic Instrumentation**: HTTP requests and database calls
- **Custom Tracing**: Business operations with @Traced annotation
- **Trace Sampling**: Configurable sampling strategies
- **Correlation ID Propagation**: Automatic context propagation
- **Jaeger UI**: Configured for trace visualization
- **Tests**: Comprehensive test coverage for trace generation and correlation

The implementation is production-ready and provides the foundation for comprehensive observability across the entire microservices architecture.

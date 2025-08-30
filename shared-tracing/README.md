# Shared Tracing Module

This module provides comprehensive distributed tracing capabilities using OpenTelemetry and Jaeger for the e-commerce microservices platform.

## Features

- **Automatic HTTP Request Tracing**: All HTTP requests are automatically traced
- **gRPC Tracing**: Automatic tracing for gRPC service calls
- **Custom Business Operation Tracing**: Use `@Traced` annotation for custom tracing
- **Tenant Context Propagation**: Automatic propagation of tenant, user, and correlation IDs
- **Correlation ID Management**: Automatic generation and propagation of correlation IDs
- **Sampling Configuration**: Configurable sampling strategies
- **Jaeger Integration**: Export traces to Jaeger for visualization

## Quick Start

### 1. Add Dependency

Add the shared-tracing dependency to your service:

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>shared-tracing</artifactId>
</dependency>
```

### 2. Configuration

Add tracing configuration to your `application.yml`:

```yaml
tracing:
  enabled: true
  jaeger:
    endpoint: http://localhost:14250
  otlp:
    endpoint: http://localhost:4317
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

### 3. Start Jaeger

Jaeger is already configured in the Docker Compose file:

```bash
docker-compose up -d jaeger
```

Access Jaeger UI at: http://localhost:16686

## Usage

### Automatic Tracing

HTTP requests and gRPC calls are automatically traced. No additional code required.

### Custom Business Operation Tracing

Use the `@Traced` annotation to trace specific methods:

```java
@Service
@Traced(operation = "user-service")
public class UserService {

    @Traced(value = "get-user", includeParameters = true)
    public User getUser(String userId) {
        // Method implementation
    }

    @Traced(value = "create-user", operation = "user-creation", includeReturnValue = true)
    public User createUser(CreateUserRequest request) {
        // Method implementation
    }
}
```

### Manual Tracing

For more control, use `TracingUtils`:

```java
@Service
public class OrderService {

    public void processOrder(String orderId) {
        TracingUtils.executeInSpan("process-order", SpanKind.INTERNAL, () -> {
            // Add business context
            Span currentSpan = Span.current();
            TracingUtils.addBusinessContext(currentSpan, "order", orderId, "processing");

            // Your business logic here
            validateOrder(orderId);
            processPayment(orderId);
            createShipment(orderId);
        });
    }

    private void validateOrder(String orderId) {
        TracingUtils.executeInSpan("validate-order", SpanKind.INTERNAL, () -> {
            // Validation logic
        });
    }
}
```

### Database Operation Tracing

```java
public void saveOrder(Order order) {
    TracingUtils.executeInSpan("save-order", SpanKind.CLIENT, () -> {
        Span currentSpan = Span.current();
        TracingUtils.addDatabaseContext(currentSpan, "mysql", "ecommerce", "orders", "INSERT");

        // Database operation
        orderRepository.save(order);
    });
}
```

### HTTP Client Tracing

```java
public ProductResponse getProduct(String productId) {
    return TracingUtils.executeInSpan("get-product-http", SpanKind.CLIENT, () -> {
        Span currentSpan = Span.current();
        TracingUtils.addHttpContext(currentSpan, "GET", "/api/v1/products/" + productId, 200);

        // HTTP client call
        return restTemplate.getForObject("/api/v1/products/" + productId, ProductResponse.class);
    });
}
```

### Kafka Message Tracing

```java
public void publishOrderEvent(OrderEvent event) {
    TracingUtils.executeInSpan("publish-order-event", SpanKind.PRODUCER, () -> {
        Span currentSpan = Span.current();
        TracingUtils.addKafkaContext(currentSpan, "order-events", "publish", 0, 12345L);

        // Kafka publish
        kafkaTemplate.send("order-events", event);
    });
}
```

## Annotation Options

The `@Traced` annotation supports several options:

```java
@Traced(
    value = "custom-span-name",           // Custom span name
    operation = "business-operation",      // Operation type
    includeParameters = true,              // Include method parameters as attributes
    includeReturnValue = true              // Include return value as attribute
)
```

## Context Propagation

The tracing system automatically propagates:

- **Tenant ID**: From `X-Tenant-ID` header
- **User ID**: From `X-User-ID` header
- **Correlation ID**: From `X-Correlation-ID` header (generated if not present)
- **Trace Context**: OpenTelemetry W3C trace context

## Sampling Strategies

Configure sampling to control trace volume:

```yaml
tracing:
  sampling:
    ratio: 0.1 # Sample 10% of traces
    parent-based: true # Use parent-based sampling
    rate-limit:
      enabled: true
      max-per-second: 100 # Maximum traces per second
```

## Performance Considerations

- **Sampling**: Use appropriate sampling ratios for production (typically 1-10%)
- **Batch Export**: Configure batch size and delay for optimal performance
- **Async Processing**: Trace export is asynchronous and doesn't block requests
- **Memory Usage**: Monitor memory usage with high-volume tracing

## Troubleshooting

### No Traces in Jaeger

1. Check Jaeger is running: `docker-compose ps jaeger`
2. Verify configuration: `tracing.enabled=true`
3. Check sampling ratio: `tracing.sampling.ratio > 0`
4. Verify Jaeger endpoint: `tracing.jaeger.endpoint`

### High Memory Usage

1. Reduce sampling ratio
2. Increase batch export frequency
3. Reduce batch size
4. Check for trace leaks (spans not properly closed)

### Missing Context

1. Verify headers are being passed: `X-Tenant-ID`, `X-User-ID`, `X-Correlation-ID`
2. Check filter order: `CorrelationIdFilter` should run early
3. Verify async operations propagate context

## Testing

The module includes comprehensive tests:

```bash
mvn test -pl shared-tracing
```

### Integration Testing

Use `InMemorySpanExporter` for testing:

```java
@SpringBootTest
class TracingTest {

    @Autowired
    private InMemorySpanExporter spanExporter;

    @Test
    void testTracing() {
        // Execute traced operation
        service.tracedMethod();

        // Verify spans
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        assertEquals("expected-span-name", spans.get(0).getName());
    }
}
```

## Best Practices

1. **Use Meaningful Span Names**: Choose descriptive names that indicate the operation
2. **Add Business Context**: Include relevant business attributes (entity IDs, operation types)
3. **Trace Critical Paths**: Focus on important business operations and external calls
4. **Avoid Over-Tracing**: Don't trace every method - focus on boundaries and critical operations
5. **Handle Errors**: Ensure spans are properly closed even when exceptions occur
6. **Monitor Performance**: Track the overhead of tracing in production
7. **Use Sampling**: Configure appropriate sampling for production environments

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

## Monitoring

Monitor tracing system health:

- **Trace Export Success Rate**: Monitor failed exports
- **Sampling Effectiveness**: Ensure appropriate trace volume
- **Memory Usage**: Monitor span buffer memory usage
- **Export Latency**: Track time to export traces
- **Jaeger Health**: Monitor Jaeger collector availability

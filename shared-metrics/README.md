# Shared Metrics Module

This module provides comprehensive metrics collection capabilities for all microservices in the e-commerce platform. It includes JVM metrics, business metrics, database metrics, cache metrics, and method-level timing metrics.

## Features

- **JVM Metrics**: Comprehensive JVM monitoring including GC, heap, threads, and class loading
- **Business Metrics**: Domain-specific metrics for orders, payments, carts, products, inventory, users, reviews, notifications, and shipping
- **Database Metrics**: Connection pool monitoring, query timing, and transaction metrics
- **Cache Metrics**: Redis operation metrics including hit/miss ratios and operation timing
- **Method-Level Timing**: Automatic timing of service, controller, and repository methods
- **Custom Annotations**: `@Timed` and `@BusinessMetric` for fine-grained control

## Quick Start

### 1. Add Dependency

Add the shared-metrics dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>shared-metrics</artifactId>
</dependency>
```

### 2. Configure Metrics

Add metrics configuration to your `application.yml`:

```yaml
# Metrics Configuration
ecommerce:
  metrics:
    enabled: true
    application-name: your-service-name
    application-version: 1.0.0
    environment: ${ENVIRONMENT:development}
    jvm-metrics-enabled: true
    business-metrics-enabled: true
    database-metrics-enabled: true
    cache-metrics-enabled: true
    method-timing-enabled: true

# Actuator endpoints for Prometheus
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 3. Use Business Metrics

Inject the `BusinessMetricsCollector` in your services:

```java
@Service
public class OrderService {

    private final BusinessMetricsCollector businessMetricsCollector;

    public OrderService(BusinessMetricsCollector businessMetricsCollector) {
        this.businessMetricsCollector = businessMetricsCollector;
    }

    @BusinessMetric(value = "order_created", tags = {"operation", "create"})
    @Timed(value = "order.creation.time", description = "Time taken to create an order")
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Your business logic here

        // Record business metrics
        businessMetricsCollector.recordOrderCreated(tenantId, orderValue);

        return orderResponse;
    }
}
```

## Available Metrics

### JVM Metrics

- `jvm.memory.heap.utilization` - Heap memory utilization percentage
- `jvm.memory.nonheap.utilization` - Non-heap memory utilization percentage
- `jvm.threads.deadlocked` - Number of deadlocked threads
- `jvm.process.cpu.usage` - Process CPU usage
- `jvm.system.cpu.usage` - System CPU usage
- `jvm.uptime` - JVM uptime in milliseconds
- `jvm.processors.available` - Number of available processors
- `jvm.system.load.average` - System load average

### Business Metrics

#### Order Service

- `business.orders.created` - Number of orders created
- `business.orders.value` - Order value distribution
- `business.orders.status.changed` - Order status changes
- `business.orders.processing.time` - Order processing time

#### Payment Service

- `business.payments.attempts` - Payment attempts (success/failure)
- `business.payments.amount` - Payment amount distribution
- `business.payments.processing.time` - Payment processing time

#### Cart Service

- `business.cart.operations` - Cart operations (add, remove, update, checkout)
- `business.cart.value` - Cart value distribution
- `business.cart.abandonment` - Cart abandonment tracking

#### Product Service

- `business.products.views` - Product views by category
- `business.products.searches` - Product searches
- `business.products.search.results` - Search result counts

#### Inventory Service

- `business.inventory.operations` - Inventory operations (reserve, release, update)
- `business.inventory.stock.level` - Current stock levels
- `business.inventory.stock.out` - Stock out events

#### User Service

- `business.users.registrations` - User registrations
- `business.users.logins` - User login attempts

#### Review Service

- `business.reviews.submitted` - Reviews submitted
- `business.reviews.rating` - Review ratings distribution

#### Notification Service

- `business.notifications.sent` - Notifications sent by channel

#### Shipping Service

- `business.shipments.created` - Shipments created
- `business.shipments.status.updated` - Shipment status updates

### Database Metrics

- `database.connections.active` - Active database connections
- `database.connections.idle` - Idle database connections
- `database.connections.total` - Total database connections
- `database.connections.pending` - Threads awaiting connection
- `database.query.duration` - Database query execution time
- `database.query.executions` - Database query executions
- `database.transaction.duration` - Database transaction duration
- `database.slow.queries` - Slow database queries
- `database.deadlocks` - Database deadlocks
- `database.rollbacks` - Database transaction rollbacks

### Cache Metrics

- `cache.operations` - Cache operations (hit/miss by operation)
- `cache.operation.duration` - Cache operation duration
- `cache.evictions` - Cache evictions
- `cache.size` - Cache size
- `cache.memory.usage` - Cache memory usage
- `redis.connections.active` - Active Redis connections
- `redis.command.duration` - Redis command execution time
- `redis.pipeline.duration` - Redis pipeline execution time

### Method-Level Metrics

- `service.method.duration` - Service method execution time
- `controller.method.duration` - Controller method execution time
- `repository.method.duration` - Repository method execution time
- `method.execution.time` - Custom timed method execution time

## Annotations

### @Timed

Use `@Timed` to add timing metrics to any method:

```java
@Timed(value = "custom.operation.time",
       description = "Time taken for custom operation",
       extraTags = {"type", "important"})
public void customOperation() {
    // Method implementation
}
```

### @BusinessMetric

Use `@BusinessMetric` to record business events:

```java
@BusinessMetric(value = "user_action",
                tags = {"action", "login", "source", "web"},
                recordTiming = true)
public void userLogin(String userId) {
    // Method implementation
}
```

## Configuration Properties

All configuration properties are under the `ecommerce.metrics` prefix:

| Property                   | Default                   | Description                        |
| -------------------------- | ------------------------- | ---------------------------------- |
| `enabled`                  | `true`                    | Enable/disable metrics collection  |
| `application-name`         | `ecommerce-service`       | Application name for tagging       |
| `application-version`      | `1.0.0`                   | Application version for tagging    |
| `environment`              | `development`             | Environment name for tagging       |
| `jvm-metrics-enabled`      | `true`                    | Enable JVM metrics collection      |
| `business-metrics-enabled` | `true`                    | Enable business metrics collection |
| `database-metrics-enabled` | `true`                    | Enable database metrics collection |
| `cache-metrics-enabled`    | `true`                    | Enable cache metrics collection    |
| `method-timing-enabled`    | `true`                    | Enable method-level timing         |
| `percentiles`              | `[0.5, 0.75, 0.95, 0.99]` | Percentiles for timing metrics     |
| `histogram-buckets`        | `[0.001, 0.005, ...]`     | Histogram buckets for timing       |

## Integration with Monitoring Stack

The metrics are exposed via Micrometer and can be scraped by Prometheus. The metrics endpoint is available at:

```
http://your-service:port/actuator/prometheus
```

### Prometheus Configuration

Add your services to Prometheus configuration:

```yaml
scrape_configs:
  - job_name: "order-service"
    static_configs:
      - targets: ["order-service:8086"]
    metrics_path: "/actuator/prometheus"
    scrape_interval: 15s
```

### Grafana Dashboards

The metrics are designed to work with Grafana dashboards. Key dashboard panels include:

- **JVM Overview**: Heap usage, GC activity, thread count
- **Business Metrics**: Order rates, payment success rates, cart operations
- **Database Performance**: Query timing, connection pool usage
- **Cache Performance**: Hit ratios, operation timing
- **Service Performance**: Method timing, error rates

## Testing

The module includes comprehensive unit tests for all collectors and aspects. Run tests with:

```bash
mvn test -f shared-metrics/pom.xml
```

## Best Practices

1. **Use Appropriate Tags**: Add meaningful tags to metrics for better filtering and aggregation
2. **Monitor Business KPIs**: Focus on metrics that matter to your business
3. **Set Up Alerts**: Configure alerts for critical metrics like error rates and response times
4. **Regular Review**: Regularly review and optimize your metrics collection
5. **Performance Impact**: Be mindful of the performance impact of extensive metrics collection

## Troubleshooting

### Common Issues

1. **Metrics Not Appearing**: Check that the actuator endpoints are exposed and Prometheus is configured correctly
2. **High Memory Usage**: Reduce the number of tags or increase the metrics retention period
3. **Missing Business Metrics**: Ensure the `BusinessMetricsCollector` is properly injected

### Debug Configuration

Enable debug logging for metrics:

```yaml
logging:
  level:
    com.ecommerce.shared.metrics: DEBUG
    io.micrometer: DEBUG
```

## Contributing

When adding new metrics:

1. Add the metric to the appropriate collector
2. Write unit tests for the new metric
3. Update this README with the new metric documentation
4. Consider the performance impact of the new metric

## License

This module is part of the e-commerce platform and follows the same licensing terms.

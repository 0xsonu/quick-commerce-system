# Shared Health Module

This module provides comprehensive health check capabilities for all microservices in the e-commerce platform. It includes custom health indicators for databases, caches, messaging systems, and JVM monitoring, along with Kubernetes-ready probe endpoints.

## Features

- **Custom Health Indicators**: Database, Redis, MongoDB, Kafka, and JVM health checks
- **Composite Health Aggregation**: Combines multiple health indicators with degraded status support
- **Kubernetes Probes**: Liveness, readiness, and startup probe endpoints
- **Detailed Health Endpoints**: Component-specific health information
- **Configurable Thresholds**: Customizable warning and critical thresholds
- **Async Health Checks**: Non-blocking health check execution with timeouts

## Health Indicators

### Database Health Indicator

- Checks database connectivity and query performance
- Monitors connection pool status (HikariCP support)
- Configurable slow query and connection timeout thresholds
- Returns DEGRADED status for slow operations

### Redis Health Indicator

- Validates Redis connectivity with ping/pong
- Tests read/write operations with TTL
- Monitors cache hit ratio and memory usage
- Provides Redis server information

### MongoDB Health Indicator

- Checks MongoDB connectivity and server status
- Monitors connection pool and memory usage
- Tests collection operations
- Provides operation counters and database metrics

### Kafka Health Indicator

- Validates Kafka cluster connectivity
- Tests producer and consumer functionality
- Monitors cluster information and node count
- Configurable operation timeouts

### JVM Health Indicator

- Monitors heap and non-heap memory usage
- Tracks garbage collection performance
- Monitors thread count and deadlock detection
- Provides system metrics (CPU, load average)
- Configurable memory and GC thresholds

### Composite Health Indicator

- Aggregates all health indicators asynchronously
- Supports UP, DEGRADED, and DOWN statuses
- Provides detailed component breakdown
- Handles timeouts and exceptions gracefully

## Endpoints

### Standard Actuator Endpoints

- `GET /actuator/health` - Basic health status
- `GET /actuator/health/detailed` - Detailed health with all components

### Component-Specific Endpoints

- `GET /actuator/health/component/{componentName}` - Individual component health
- `GET /actuator/health/components` - List of available components

### Kubernetes Probe Endpoints

- `GET /actuator/health/liveness` - Liveness probe (application running)
- `GET /actuator/health/readiness` - Readiness probe (ready for traffic)
- `GET /actuator/health/startup` - Startup probe (finished starting)

## Configuration

### Maven Dependency

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>shared-health</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Application Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      show-components: always
      probes:
        enabled: true
  health:
    # Enable/disable specific health indicators
    db:
      enabled: true
    redis:
      enabled: true
    mongo:
      enabled: true
    kafka:
      enabled: true
    jvm:
      enabled: true
    composite:
      enabled: true

# Custom health thresholds
health:
  thresholds:
    memory:
      warning: 0.85 # 85%
      critical: 0.95 # 95%
    gc:
      warning: 1000 # 1 second average GC time
    threads:
      warning: 500 # thread count
    database:
      slow_query: 1000 # 1 second
      connection_timeout: 5000 # 5 seconds
    redis:
      slow_operation: 500 # 500ms
    mongo:
      slow_operation: 1000 # 1 second
    kafka:
      slow_operation: 2000 # 2 seconds
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ecommerce-service
spec:
  template:
    spec:
      containers:
        - name: ecommerce-service
          image: ecommerce/service:latest
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
          startupProbe:
            httpGet:
              path: /actuator/health/startup
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 30
```

## Health Status Levels

### UP

- All components are functioning normally
- All thresholds are within acceptable ranges
- System is ready to serve traffic

### DEGRADED

- Some components are experiencing issues but system is still functional
- Performance may be reduced but service is available
- Examples: High memory usage, slow database queries, cache misses

### DOWN

- Critical components are failing
- System cannot serve traffic reliably
- Examples: Database connection failure, critical memory shortage, deadlocks

## Usage Examples

### Basic Health Check

```java
@RestController
public class HealthController {

    @Autowired
    private CompositeHealthIndicator compositeHealthIndicator;

    @GetMapping("/health")
    public ResponseEntity<Health> getHealth() {
        Health health = compositeHealthIndicator.health();
        return ResponseEntity.ok(health);
    }
}
```

### Component-Specific Health

```java
@Service
public class HealthService {

    @Autowired
    private CompositeHealthIndicator compositeHealthIndicator;

    public boolean isDatabaseHealthy() {
        Health dbHealth = compositeHealthIndicator.getComponentHealth("database");
        return Status.UP.equals(dbHealth.getStatus());
    }
}
```

### Custom Health Indicator

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Custom health check logic
        boolean isHealthy = checkCustomComponent();

        if (isHealthy) {
            return Health.up()
                    .withDetail("custom.status", "OK")
                    .build();
        } else {
            return Health.down()
                    .withDetail("custom.status", "FAILED")
                    .build();
        }
    }
}
```

## Testing

The module includes comprehensive tests:

- **Unit Tests**: Individual health indicator testing with mocks
- **Integration Tests**: Real database/cache connectivity using Testcontainers
- **Controller Tests**: REST endpoint testing with MockMvc
- **Performance Tests**: Health check timeout and async behavior

### Running Tests

```bash
# Run all tests
mvn test

# Run integration tests only
mvn test -Dtest="*IntegrationTest"

# Run with Testcontainers
mvn test -Dspring.profiles.active=test
```

## Monitoring Integration

### Prometheus Metrics

Health indicators automatically expose metrics to Prometheus:

- `health_check_duration_seconds` - Health check execution time
- `health_check_status` - Current health status (0=DOWN, 1=UP, 0.5=DEGRADED)

### Grafana Dashboards

Pre-configured dashboards include:

- Service health overview
- Component-specific health trends
- JVM health metrics
- Database/cache performance

### Alerting

Recommended Prometheus alerting rules:

- Service down for > 1 minute
- Service degraded for > 5 minutes
- High memory usage (> 90%)
- High GC time (> 2 seconds average)

## Best Practices

1. **Probe Configuration**:

   - Set appropriate timeouts for your service startup time
   - Use different thresholds for liveness vs readiness
   - Consider startup time when setting failure thresholds

2. **Health Check Performance**:

   - Keep health checks lightweight and fast
   - Use connection pooling for database checks
   - Implement circuit breakers for external dependencies

3. **Status Interpretation**:

   - Use DEGRADED for performance issues that don't require restart
   - Reserve DOWN for critical failures requiring intervention
   - Provide meaningful error messages in health details

4. **Monitoring**:
   - Monitor health check execution time
   - Alert on status changes and trends
   - Use health metrics for capacity planning

## Troubleshooting

### Common Issues

1. **Health checks timing out**:

   - Increase timeout values in Kubernetes probes
   - Check database connection pool configuration
   - Monitor health check execution time

2. **False positive failures**:

   - Review threshold configurations
   - Check for resource contention
   - Verify external dependency availability

3. **Startup probe failures**:
   - Increase failure threshold for slow-starting services
   - Check application initialization order
   - Monitor startup time trends

### Debug Endpoints

- `GET /actuator/health/detailed` - Full health information
- `GET /actuator/metrics` - Health-related metrics
- `GET /actuator/info` - Application information

## Contributing

When adding new health indicators:

1. Extend `HealthIndicator` interface
2. Add appropriate configuration properties
3. Include comprehensive unit and integration tests
4. Update documentation and examples
5. Consider performance impact and timeout handling

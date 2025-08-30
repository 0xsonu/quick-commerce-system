# Grafana Monitoring Setup

This directory contains the complete Grafana configuration for the Amazon Shopping Backend monitoring infrastructure.

## Overview

Grafana is configured with comprehensive dashboards covering:

- System-level monitoring (CPU, memory, disk, network)
- Service-level RED metrics (Rate, Errors, Duration)
- JVM performance analytics (heap, GC, threads)
- Database performance (MySQL, MongoDB, Redis)
- Business intelligence metrics (revenue, conversion, orders)

## Architecture

```
Grafana
├── Prometheus-Main (15-day retention)
├── Prometheus-Federation (1-year retention)
└── Jaeger (distributed tracing)
```

## Dashboards

### 1. System Overview Dashboard

- **UID**: `system-overview`
- **Folder**: System Monitoring
- **Metrics**:
  - CPU usage across all nodes
  - Memory utilization
  - Disk I/O performance
  - Network throughput
- **Data Source**: Prometheus-Main (Node Exporter)

### 2. Service-Level RED Metrics Dashboard

- **UID**: `service-red-metrics`
- **Folder**: Service Monitoring
- **Metrics**:
  - Request Rate (requests per second)
  - Error Rate (5xx errors percentage)
  - Duration (response time percentiles)
  - HTTP status code distribution
  - Requests by endpoint
- **Data Source**: Prometheus-Main (Micrometer metrics)
- **Variables**: Service selector (multi-select)

### 3. JVM Performance Dashboard

- **UID**: `jvm-performance`
- **Folder**: Service Monitoring
- **Metrics**:
  - Heap memory usage (used, committed, max)
  - Non-heap memory usage
  - Garbage collection time and frequency
  - Thread count (current, peak)
  - Class loading statistics
- **Data Source**: Prometheus-Main (JVM metrics)
- **Variables**: Service selector (multi-select)

### 4. Database Performance Dashboard

- **UID**: `database-performance`
- **Folder**: Service Monitoring
- **Metrics**:
  - **MySQL**: Connection pool status, query duration, query rate
  - **MongoDB**: Operation rate, operation duration
  - **Redis**: Cache hit ratio, operation rate, connection pool
- **Data Source**: Prometheus-Main (Database metrics)
- **Variables**: Service selector (multi-select)

### 5. Business Intelligence Dashboard

- **UID**: `business-intelligence`
- **Folder**: Business Intelligence
- **Metrics**:
  - Daily revenue and order count
  - Average order value
  - Cart-to-order conversion rate
  - Revenue and order trends
  - Order status distribution
  - Product category views
  - Payment failure rate
- **Data Source**: Prometheus-Main (Business metrics)
- **Variables**: Tenant selector (multi-select)

## Configuration Files

### Datasources (`datasources/prometheus.yml`)

- **Prometheus-Main**: Primary metrics (15-day retention)
- **Prometheus-Federation**: Long-term storage (1-year retention)
- **Jaeger**: Distributed tracing integration

### Dashboard Provisioning (`dashboards/dashboard.yml`)

- Automatic dashboard loading from JSON files
- Organized into folders by category
- Auto-refresh every 10 seconds

## Access Information

- **URL**: http://localhost:3000
- **Username**: admin
- **Password**: admin (configurable via `GF_SECURITY_ADMIN_PASSWORD`)

## Dashboard Features

### Variables and Templating

- Service filtering across all service dashboards
- Tenant filtering for business metrics
- Dynamic legend formatting
- Auto-refresh capabilities

### Alerting Integration

- Thresholds configured for critical metrics
- Color-coded visualizations
- Integration with Prometheus Alertmanager

### Performance Optimization

- Efficient query patterns
- Appropriate time ranges
- Caching strategies
- Federation for long-term data

## Metric Naming Conventions

### HTTP Metrics

```
http_server_requests_seconds_count{service, uri, method, status}
http_server_requests_seconds_bucket{service, uri, method, status, le}
http_server_requests_seconds_sum{service, uri, method, status}
```

### JVM Metrics

```
jvm_memory_used_bytes{service, area, id}
jvm_memory_committed_bytes{service, area, id}
jvm_memory_max_bytes{service, area, id}
jvm_gc_collection_seconds_count{service, gc}
jvm_gc_collection_seconds_sum{service, gc}
jvm_threads_current{service}
jvm_classes_loaded{service}
```

### Database Metrics

```
# MySQL/HikariCP
hikaricp_connections_active{service}
hikaricp_connections_idle{service}
hikaricp_connections_pending{service}
spring_data_repository_invocations_seconds_bucket{service, repository, method, le}

# MongoDB
mongodb_operations_total{service, operation}
mongodb_operation_duration_seconds_bucket{service, operation, le}

# Redis
cache_gets_total{service, cache, result}
lettuce_command_completion_total{service, command}
```

### Business Metrics

```
ecommerce_order_created_total{tenant_id, status}
ecommerce_order_total_amount_sum{tenant_id}
ecommerce_cart_created_total{tenant_id}
ecommerce_product_views_total{tenant_id, category}
ecommerce_payment_attempted_total{tenant_id}
ecommerce_payment_failed_total{tenant_id}
```

## Customization

### Adding New Dashboards

1. Create JSON dashboard file in `dashboards/` directory
2. Use appropriate UID and tags
3. Configure variables for filtering
4. Test with sample data

### Modifying Existing Dashboards

1. Edit JSON files directly or use Grafana UI
2. Export updated dashboards to maintain version control
3. Update documentation as needed

### Adding New Data Sources

1. Update `datasources/prometheus.yml`
2. Configure appropriate access and security settings
3. Test connectivity and data availability

## Troubleshooting

### Common Issues

1. **Dashboard not loading**: Check JSON syntax and file permissions
2. **No data in panels**: Verify Prometheus targets and metric names
3. **Variables not working**: Check query syntax and label names
4. **Performance issues**: Review query complexity and time ranges

### Debugging Queries

1. Use Prometheus UI to test queries
2. Check metric availability and labels
3. Verify time ranges and aggregation functions
4. Monitor Grafana logs for errors

## Maintenance

### Regular Tasks

1. Monitor dashboard performance
2. Update metric queries as services evolve
3. Archive old dashboards
4. Review and optimize slow queries
5. Update documentation

### Backup and Recovery

1. Export dashboard JSON files regularly
2. Backup Grafana database
3. Document custom configurations
4. Test restore procedures

## Integration with Other Tools

### Prometheus Alertmanager

- Dashboards include alert thresholds
- Visual indicators for alert states
- Links to alert management

### Jaeger Tracing

- Trace correlation from metrics
- Deep-dive debugging capabilities
- Service dependency mapping

### ELK Stack

- Log correlation from metrics
- Unified observability experience
- Cross-platform troubleshooting

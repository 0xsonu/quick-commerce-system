# Prometheus Monitoring Infrastructure

This directory contains the complete Prometheus monitoring infrastructure for the Amazon Shopping Backend system.

## Architecture Overview

The monitoring infrastructure consists of:

1. **Prometheus Main Server** (15-day retention)

   - Primary metrics collection and short-term storage
   - Real-time alerting and monitoring
   - Port: 9090

2. **Prometheus Federation Server** (1-year retention)

   - Long-term metrics storage via federation
   - Historical data analysis and reporting
   - Port: 9091

3. **Pushgateway**

   - Batch job metrics collection
   - Short-lived job monitoring
   - Port: 9093

4. **Node Exporter**

   - System-level metrics (CPU, memory, disk, network)
   - Host monitoring
   - Port: 9100

5. **Blackbox Exporter**
   - External endpoint monitoring
   - HTTP/HTTPS, TCP, gRPC probes
   - Port: 9115

## Configuration Files

### prometheus.yml

Main Prometheus server configuration with:

- Service discovery for all microservices
- Infrastructure monitoring (databases, message brokers)
- Blackbox probes for health checks
- 15-day data retention

### prometheus-federation.yml

Federation server configuration with:

- Federated scraping from main Prometheus
- Selective metric aggregation
- 1-year data retention
- Long-term storage optimization

### blackbox.yml

Blackbox exporter configuration with modules for:

- HTTP/HTTPS endpoint monitoring
- TCP connection checks
- gRPC service health checks
- Custom probe configurations

### rules/

Alert rule definitions organized by category:

- **infrastructure.yml**: System-level alerts (CPU, memory, disk)
- **application.yml**: Application-level alerts (errors, latency, JVM)
- **business.yml**: Business-level alerts (orders, payments, inventory)

## Metrics Collection

### Infrastructure Metrics

- **Node Exporter**: System metrics (CPU, memory, disk, network)
- **Database Metrics**: MySQL, MongoDB, Redis performance
- **Message Broker**: Kafka cluster health and performance

### Application Metrics

- **HTTP Metrics**: Request rates, response times, error rates
- **JVM Metrics**: Heap usage, GC performance, thread counts
- **Custom Business Metrics**: Orders, payments, inventory levels

### External Monitoring

- **Health Checks**: All service endpoints via Blackbox Exporter
- **gRPC Health**: Internal service communication monitoring
- **Database Connectivity**: Connection pool and query performance

## Data Retention Strategy

### Short-term Storage (Main Prometheus)

- **Retention**: 15 days
- **Purpose**: Real-time monitoring and alerting
- **Resolution**: High-resolution data (15s intervals)
- **Use Cases**:
  - Incident response
  - Real-time dashboards
  - Immediate alerting

### Long-term Storage (Federation)

- **Retention**: 1 year
- **Purpose**: Historical analysis and capacity planning
- **Resolution**: Aggregated data (60s intervals)
- **Use Cases**:
  - Trend analysis
  - Capacity planning
  - Performance baselines
  - Compliance reporting

## Alert Rules

### Infrastructure Alerts

- Service availability (up/down status)
- High CPU usage (>80% for 5 minutes)
- High memory usage (>85% for 5 minutes)
- Disk space usage (>85% for 5 minutes)
- Database connection limits

### Application Alerts

- High error rates (>5% for 2 minutes)
- High response times (>1s 95th percentile)
- JVM heap usage (>85% for 5 minutes)
- High GC time (>10% CPU time)
- Slow database queries (>2s 95th percentile)
- Low cache hit rates (<80% for 10 minutes)

### Business Alerts

- High order failure rates (>2% for 5 minutes)
- High payment failure rates (>1% for 3 minutes)
- Low inventory stock (<10 units)
- High cart abandonment (>70% for 30 minutes)
- Review moderation queue (>100 pending)
- Notification delivery failures (>5% for 5 minutes)

## Usage Instructions

### Starting the Infrastructure

```bash
# Start all monitoring services
docker-compose up -d prometheus prometheus-federation pushgateway node-exporter blackbox-exporter grafana

# Verify services are running
./scripts/test-monitoring.sh
```

### Accessing Services

- **Prometheus Main**: http://localhost:9090
- **Prometheus Federation**: http://localhost:9091
- **Pushgateway**: http://localhost:9093
- **Node Exporter**: http://localhost:9100/metrics
- **Blackbox Exporter**: http://localhost:9115
- **Grafana**: http://localhost:3000 (admin/admin)

### Pushing Custom Metrics

```bash
# Push a custom metric to Pushgateway
echo "custom_metric{label=\"value\"} 42" | curl --data-binary @- http://localhost:9093/metrics/job/my_job/instance/my_instance

# Push multiple metrics
cat <<EOF | curl --data-binary @- http://localhost:9093/metrics/job/batch_job
batch_processed_total 1234
batch_errors_total 5
batch_duration_seconds 45.2
EOF
```

### Querying Metrics

```bash
# Query current metric values
curl "http://localhost:9090/api/v1/query?query=up"

# Query metric over time range
curl "http://localhost:9090/api/v1/query_range?query=up&start=2024-01-01T00:00:00Z&end=2024-01-01T01:00:00Z&step=15s"

# Query federation server for historical data
curl "http://localhost:9091/api/v1/query?query=up"
```

### Configuration Reload

```bash
# Reload Prometheus configuration without restart
curl -X POST http://localhost:9090/-/reload
curl -X POST http://localhost:9091/-/reload
```

## Monitoring Best Practices

### Metric Naming

- Use consistent naming conventions
- Include units in metric names (e.g., `_seconds`, `_bytes`, `_total`)
- Use labels for dimensions, not metric names

### Alert Design

- Set appropriate thresholds based on SLA requirements
- Use multiple severity levels (critical, warning, info)
- Include runbook links in alert annotations
- Avoid alert fatigue with proper grouping and suppression

### Performance Optimization

- Use recording rules for expensive queries
- Implement proper label cardinality limits
- Monitor Prometheus resource usage
- Use federation for long-term storage

### Security Considerations

- Secure Prometheus endpoints in production
- Use authentication and authorization
- Encrypt communication between components
- Regularly update monitoring components

## Troubleshooting

### Common Issues

1. **High Memory Usage**

   - Check metric cardinality
   - Review retention settings
   - Monitor ingestion rate

2. **Missing Metrics**

   - Verify service discovery configuration
   - Check network connectivity
   - Review scrape target health

3. **Alert Fatigue**

   - Review alert thresholds
   - Implement proper grouping
   - Use inhibition rules

4. **Federation Issues**
   - Check federation query configuration
   - Verify network connectivity between servers
   - Monitor federation lag

### Debugging Commands

```bash
# Check Prometheus configuration
curl http://localhost:9090/api/v1/status/config

# List all targets
curl http://localhost:9090/api/v1/targets

# Check rule evaluation
curl http://localhost:9090/api/v1/rules

# View runtime information
curl http://localhost:9090/api/v1/status/runtimeinfo
```

## Integration with Other Systems

### Grafana Dashboards

- System overview dashboard
- Service-specific dashboards
- Business metrics dashboard
- JVM performance dashboard

### Alertmanager Integration

- Configure alert routing
- Set up notification channels
- Implement escalation policies
- Create silence management

### Log Correlation

- Use correlation IDs for tracing
- Link metrics with log entries
- Implement distributed tracing
- Create unified observability

## Maintenance

### Regular Tasks

- Monitor disk usage for time series data
- Review and update alert rules
- Clean up unused metrics and labels
- Update monitoring components
- Backup configuration files

### Capacity Planning

- Monitor ingestion rates
- Plan for metric growth
- Scale federation servers
- Optimize storage usage

### Security Updates

- Keep components updated
- Review access controls
- Monitor for security vulnerabilities
- Implement security best practices

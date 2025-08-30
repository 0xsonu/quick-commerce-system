# ELK Stack for Ecommerce Microservices Log Aggregation

This directory contains the complete ELK (Elasticsearch, Logstash, Kibana) stack configuration for aggregating and analyzing logs from the ecommerce microservices platform.

## Overview

The ELK stack provides:

- **Elasticsearch**: Distributed search and analytics engine for log storage
- **Logstash**: Data processing pipeline for log parsing and enrichment
- **Kibana**: Visualization and exploration interface for logs
- **Filebeat**: Lightweight log shipper for collecting logs from containers and files

## Architecture

```
Microservices → Filebeat → Logstash → Elasticsearch → Kibana
                    ↓
              Docker Logs → Logstash → Elasticsearch → Kibana
```

## Features

### Log Processing

- **Structured JSON log parsing** from Spring Boot applications
- **Multi-line support** for Java stack traces
- **Field extraction** for correlation IDs, tenant IDs, user IDs
- **HTTP request/response logging** with performance metrics
- **Database query logging** with timing information
- **Business metrics extraction** from application logs

### Storage and Retention

- **Index Lifecycle Management (ILM)** with automatic rollover
- **Configurable retention policies**:
  - Application logs: 30 days
  - Error logs: 90 days
  - Audit logs: 1 year
- **Compression and optimization** for storage efficiency

### Monitoring and Alerting

- **Real-time dashboards** for log analysis
- **Error rate monitoring** with automatic alerts
- **Service health monitoring** based on log activity
- **Performance monitoring** with response time analysis
- **Custom alerting** based on log patterns

## Configuration Files

### Elasticsearch

- `elasticsearch/elasticsearch.yml` - Main Elasticsearch configuration
- `elasticsearch/setup-ilm-policies.sh` - Index lifecycle management setup
- `elasticsearch/watchers/` - Alerting configurations

### Logstash

- `logstash/config/logstash.yml` - Logstash configuration
- `logstash/pipeline/logstash.conf` - Log processing pipeline
- `logstash/templates/` - Elasticsearch index templates

### Kibana

- `kibana/kibana.yml` - Kibana configuration
- `kibana/dashboards/` - Pre-built dashboard configurations
- `kibana/setup-dashboards.sh` - Dashboard setup script

### Filebeat

- `filebeat/filebeat.yml` - Filebeat configuration for log shipping

## Quick Start

### 1. Start the ELK Stack

```bash
# Start all services including ELK stack
docker-compose up -d elasticsearch logstash kibana filebeat

# Wait for services to be ready
./scripts/test-elk-stack.sh
```

### 2. Set up Index Lifecycle Management

```bash
# Set up ILM policies and index templates
./docker/elasticsearch/setup-ilm-policies.sh
```

### 3. Configure Kibana Dashboards

```bash
# Set up index patterns and dashboards
./docker/kibana/setup-dashboards.sh
```

### 4. Set up Alerting (Optional)

```bash
# Set up log-based alerting
./docker/elasticsearch/setup-watchers.sh
```

## Service Endpoints

- **Elasticsearch**: http://localhost:9200
- **Kibana**: http://localhost:5601
- **Logstash**: http://localhost:9600 (API), 5044 (Beats), 5000 (TCP/UDP)
- **Filebeat**: http://localhost:5066 (Health)

## Log Format Requirements

### Structured JSON Logs (Recommended)

Your microservices should output logs in JSON format:

```json
{
  "@timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.ecommerce.orderservice.OrderController",
  "thread": "http-nio-8080-exec-1",
  "message": "Order created successfully",
  "correlationId": "abc123",
  "tenantId": "tenant1",
  "userId": "user123",
  "http": {
    "method": "POST",
    "path": "/api/v1/orders",
    "status": 201,
    "duration": 150,
    "userAgent": "Mozilla/5.0..."
  },
  "order": {
    "id": 12345,
    "total": 99.99
  }
}
```

### Exception Logs

For error logs with exceptions:

```json
{
  "@timestamp": "2024-01-01T12:00:00.000Z",
  "level": "ERROR",
  "logger": "com.ecommerce.orderservice.OrderService",
  "message": "Failed to process order",
  "correlationId": "abc123",
  "tenantId": "tenant1",
  "exception": {
    "class": "java.lang.RuntimeException",
    "message": "Database connection failed",
    "stackTrace": "java.lang.RuntimeException: Database connection failed\n\tat com.ecommerce..."
  }
}
```

## Kibana Dashboards

### Pre-built Dashboards

1. **Ecommerce Logs Overview**

   - Log levels distribution
   - Logs by service
   - Timeline view
   - Error logs table
   - HTTP response times

2. **Service Health Dashboard**

   - Service availability
   - Error rates by service
   - Response time percentiles
   - Request volume

3. **Error Analysis Dashboard**
   - Exception types
   - Error trends
   - Stack trace analysis
   - Error correlation

### Custom Visualizations

Create custom visualizations for:

- Business metrics (orders, payments, etc.)
- User activity patterns
- Tenant-specific analytics
- Performance bottlenecks

## Alerting Configuration

### Built-in Alerts

1. **High Error Rate Alert**

   - Triggers when error count > 10 in 5 minutes
   - Sends email notifications
   - Groups by service

2. **Service Down Alert**

   - Triggers when no logs received for 5 minutes
   - Identifies potentially down services
   - Immediate notification

3. **Slow Response Alert**
   - Triggers when response time > 5 seconds
   - Monitors performance degradation
   - Service-specific analysis

### Custom Alerting

Create custom alerts for:

- Business rule violations
- Security events
- Resource utilization
- SLA breaches

## Performance Tuning

### Elasticsearch

```yaml
# elasticsearch.yml
indices.memory.index_buffer_size: 20%
indices.memory.min_index_buffer_size: 96mb
thread_pool.write.queue_size: 1000
```

### Logstash

```yaml
# logstash.yml
pipeline.workers: 4
pipeline.batch.size: 250
pipeline.batch.delay: 50
```

### Index Management

- **Hot tier**: Recent logs (0-7 days) - fast SSD storage
- **Warm tier**: Older logs (7-30 days) - slower storage, compressed
- **Cold tier**: Archive logs (30+ days) - cheapest storage
- **Delete**: Automatic cleanup after retention period

## Monitoring

### Health Checks

```bash
# Elasticsearch cluster health
curl http://localhost:9200/_cluster/health

# Logstash node stats
curl http://localhost:9600/_node/stats

# Kibana status
curl http://localhost:5601/api/status
```

### Metrics to Monitor

- **Elasticsearch**: Cluster health, index size, query performance
- **Logstash**: Processing rate, queue size, error rate
- **Kibana**: Response time, user activity
- **Filebeat**: Harvester status, registry size

## Troubleshooting

### Common Issues

1. **Elasticsearch won't start**

   - Check memory settings (`ES_JAVA_OPTS`)
   - Verify disk space
   - Check file permissions

2. **Logstash parsing errors**

   - Check pipeline configuration
   - Validate JSON format
   - Review grok patterns

3. **Missing logs in Kibana**

   - Verify index patterns
   - Check time range
   - Confirm log shipping

4. **High memory usage**
   - Adjust JVM heap sizes
   - Optimize index settings
   - Review retention policies

### Debug Commands

```bash
# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices?v

# View Logstash pipeline stats
curl http://localhost:9600/_node/stats/pipelines

# Test log parsing
echo '{"test": "log"}' | nc localhost 5000

# Check Filebeat registry
docker exec ecommerce-filebeat filebeat export config
```

## Security Considerations

### Development Environment

- Security disabled for ease of development
- No authentication required
- All services accessible without encryption

### Production Recommendations

- Enable X-Pack security
- Configure SSL/TLS encryption
- Set up role-based access control
- Use secure passwords and certificates
- Network segmentation and firewalls

## Backup and Recovery

### Elasticsearch Snapshots

```bash
# Create snapshot repository
curl -X PUT "localhost:9200/_snapshot/backup_repo" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/backup"
  }
}'

# Create snapshot
curl -X PUT "localhost:9200/_snapshot/backup_repo/snapshot_1"
```

### Configuration Backup

- Backup all configuration files
- Version control pipeline configurations
- Document custom dashboards and visualizations

## Scaling

### Horizontal Scaling

- Add more Elasticsearch nodes
- Scale Logstash workers
- Distribute Filebeat across multiple hosts

### Vertical Scaling

- Increase memory allocation
- Use faster storage (SSD)
- Optimize JVM settings

## Integration with Microservices

### Spring Boot Configuration

Add to your `application.yml`:

```yaml
logging:
  pattern:
    console: '{"@timestamp":"%d{yyyy-MM-dd''T''HH:mm:ss.SSSZ}","level":"%level","logger":"%logger","thread":"%thread","message":"%message","correlationId":"${correlationId:-}","tenantId":"${tenantId:-}","userId":"${userId:-}"}%n'
  level:
    com.ecommerce: DEBUG
    org.springframework.web: INFO
```

### Logback Configuration

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <appender name="TCP" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5000</destination>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="TCP"/>
    </root>
</configuration>
```

## Testing

Run the comprehensive test suite:

```bash
./scripts/test-elk-stack.sh
```

This will verify:

- Service health and connectivity
- Log ingestion and processing
- Index creation and management
- Dashboard functionality
- Alerting configuration

## Support and Maintenance

### Regular Tasks

- Monitor disk usage and clean up old indices
- Update index templates and mappings
- Review and optimize queries
- Update alerting rules
- Backup configurations

### Upgrades

- Follow Elastic Stack upgrade procedures
- Test in staging environment first
- Backup data and configurations
- Monitor performance after upgrades

For more information, see the [Elastic Stack documentation](https://www.elastic.co/guide/index.html).

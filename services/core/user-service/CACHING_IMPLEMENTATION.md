# User Service Caching Implementation

## Overview

This document describes the comprehensive caching strategy implemented for the User Management Service, including cache-aside pattern, cache warming strategies, and proper invalidation mechanisms.

## Features Implemented

### 1. Enhanced Cache Configuration

- **Multiple Cache Types**: Different TTL configurations for different cache types
  - User profiles: 2 hours TTL
  - User profile responses: 45 minutes TTL
  - Search results: 15 minutes TTL
  - User count: 5 minutes TTL
- **Redis Integration**: Full Redis integration with proper serialization
- **Cache Prefixing**: All cache keys are prefixed with "user-service:" for namespace isolation

### 2. Cache-Aside Pattern Implementation

- **UserCacheService**: Dedicated service for managing cache operations
- **Cache Miss Handling**: Automatic fallback to database when cache misses occur
- **Cache Population**: Automatic cache population after database reads
- **Error Resilience**: Graceful degradation when cache is unavailable

### 3. Comprehensive Cache Invalidation

- **Update Operations**: Cache invalidation on user profile updates
- **Email Changes**: Special handling for email changes with dual cache invalidation
- **Delete Operations**: Complete cache cleanup on user deletion
- **Selective Invalidation**: Targeted cache eviction for specific keys

### 4. Cache Warming Strategies

- **Application Startup**: Automatic cache warming on application startup
- **Scheduled Refresh**: Periodic cache refresh every 2 hours
- **Manual Warming**: API endpoints for manual cache warming
- **Tenant-Specific**: Cache warming per tenant with recent user prioritization

### 5. Cache Health Monitoring

- **Health Checks**: Regular cache health monitoring
- **Statistics Collection**: Cache hit/miss ratio tracking
- **Performance Monitoring**: Cache operation timing and metrics
- **Alerting**: Logging for cache failures and performance issues

## API Endpoints

### Cache Management Endpoints

```http
POST /api/v1/users/cache/warmup
Headers: X-Tenant-ID: {tenantId}
Description: Manually trigger cache warm-up for a tenant

GET /api/v1/users/cache/health
Description: Check cache health status

GET /api/v1/users/cache/stats
Description: Get cache statistics (size, hit/miss ratios)
```

## Cache Keys Structure

- **User Profile by Auth ID**: `user-service:user-profiles:{tenantId}:user:{authUserId}`
- **User Profile by Email**: `user-service:user-profiles:{tenantId}:email:{email}`
- **User Profile by ID**: `user-service:user-profiles:{tenantId}:id:{userId}`
- **Search Results**: `user-service:user-search:{tenantId}:search:{searchTerm}`
- **User Count**: `user-service:user-count:{tenantId}`

## Scheduled Tasks

### Cache Warm-up Scheduler

- **Startup Warm-up**: Runs on application startup for all tenants
- **Periodic Refresh**: Every 2 hours for active tenants
- **Health Monitoring**: Every 30 minutes
- **Statistics Logging**: Every hour

## Configuration

### Redis Configuration

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### Cache TTL Settings

- **Default TTL**: 30 minutes
- **User Profiles**: 2 hours (frequently accessed)
- **Search Results**: 15 minutes (data changes frequently)
- **User Count**: 5 minutes (changes frequently)

## Performance Benefits

1. **Reduced Database Load**: Frequent user profile reads served from cache
2. **Improved Response Times**: Sub-millisecond cache responses vs database queries
3. **Better Scalability**: Cache can handle higher concurrent loads
4. **Fault Tolerance**: Graceful degradation when cache is unavailable

## Testing

### Unit Tests

- **UserCacheServiceTest**: Comprehensive cache service testing
- **CacheWarmupSchedulerTest**: Scheduler functionality testing
- **UserServiceTest**: Updated to include caching behavior

### Integration Tests

- **UserCacheIntegrationTest**: End-to-end caching with real Redis
- **Cache Performance Tests**: Response time comparisons
- **Cache Invalidation Tests**: Verification of proper cache cleanup

## Monitoring and Observability

### Metrics Exposed

- Cache hit/miss ratios
- Cache operation timing
- Cache size and memory usage
- Error rates and failure counts

### Logging

- Cache operations (debug level)
- Cache failures (warn level)
- Performance statistics (info level)
- Health check results

## Error Handling

- **Cache Unavailable**: Automatic fallback to database
- **Serialization Errors**: Graceful error handling with logging
- **Network Timeouts**: Configurable timeouts with retry logic
- **Memory Pressure**: Automatic eviction based on TTL and LRU

## Future Enhancements

1. **Cache Metrics Dashboard**: Grafana dashboard for cache monitoring
2. **Distributed Caching**: Redis Cluster support for high availability
3. **Cache Preloading**: Predictive cache warming based on usage patterns
4. **A/B Testing**: Cache configuration experiments
5. **Multi-Level Caching**: L1 (in-memory) + L2 (Redis) caching strategy

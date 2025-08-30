package com.ecommerce.shared.metrics.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Collector for Redis cache metrics including hit/miss ratios and operation timing
 */
@Component
public class CacheMetricsCollector {

    private final MeterRegistry meterRegistry;

    public CacheMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit(String cacheName, String operation) {
        Counter.builder("cache.operations")
                .description("Cache operations")
                .tag("cache", cacheName)
                .tag("operation", operation) // get, put, delete, exists
                .tag("result", "hit")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss(String cacheName, String operation) {
        Counter.builder("cache.operations")
                .description("Cache operations")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .tag("result", "miss")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache operation time
     */
    public void recordCacheOperationTime(String cacheName, String operation, long durationMs) {
        Timer.builder("cache.operation.duration")
                .description("Cache operation duration")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record cache eviction
     */
    public void recordCacheEviction(String cacheName, String reason) {
        Counter.builder("cache.evictions")
                .description("Cache evictions")
                .tag("cache", cacheName)
                .tag("reason", reason) // ttl, memory, manual
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache size
     */
    public void recordCacheSize(String cacheName, long size) {
        meterRegistry.gauge("cache.size", 
            io.micrometer.core.instrument.Tags.of("cache", cacheName), 
            size);
    }

    /**
     * Record cache memory usage
     */
    public void recordCacheMemoryUsage(String cacheName, long memoryBytes) {
        meterRegistry.gauge("cache.memory.usage", 
            io.micrometer.core.instrument.Tags.of("cache", cacheName), 
            memoryBytes);
    }

    /**
     * Record Redis-specific metrics
     */
    public void recordRedisConnectionPoolMetrics(int activeConnections, int idleConnections, int totalConnections) {
        meterRegistry.gauge("redis.connections.active", activeConnections);
        meterRegistry.gauge("redis.connections.idle", idleConnections);
        meterRegistry.gauge("redis.connections.total", totalConnections);
    }

    /**
     * Record Redis command execution
     */
    public void recordRedisCommand(String command, long durationMs, boolean success) {
        Timer.builder("redis.command.duration")
                .description("Redis command execution time")
                .tag("command", command)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record Redis pipeline operation
     */
    public void recordRedisPipeline(int commandCount, long durationMs, boolean success) {
        Timer.builder("redis.pipeline.duration")
                .description("Redis pipeline execution time")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        DistributionSummary.builder("redis.pipeline.commands")
                .register(meterRegistry)
                .record(commandCount);
    }

    /**
     * Record cache warming operation
     */
    public void recordCacheWarming(String cacheName, int itemsWarmed, long durationMs) {
        Timer.builder("cache.warming.duration")
                .description("Cache warming duration")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        DistributionSummary.builder("cache.warming.items")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .record(itemsWarmed);
    }

    /**
     * Record cache serialization metrics
     */
    public void recordCacheSerialization(String cacheName, String operation, long durationMs, long sizeBytes) {
        Timer.builder("cache.serialization.duration")
                .description("Cache serialization/deserialization time")
                .tag("cache", cacheName)
                .tag("operation", operation) // serialize, deserialize
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        DistributionSummary.builder("cache.serialization.size")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .register(meterRegistry)
                .record(sizeBytes);
    }

    /**
     * Record distributed cache metrics
     */
    public void recordDistributedCacheOperation(String cacheName, String node, String operation, boolean success) {
        Counter.builder("cache.distributed.operations")
                .description("Distributed cache operations")
                .tag("cache", cacheName)
                .tag("node", node)
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Calculate and record hit ratio
     */
    public void calculateHitRatio(String cacheName) {
        // This would typically be called periodically to calculate hit ratios
        // The actual calculation would be based on the accumulated hit/miss counters
        meterRegistry.gauge("cache.hit.ratio", 
            io.micrometer.core.instrument.Tags.of("cache", cacheName), 
            this, collector -> calculateHitRatioValue());
    }

    private double calculateHitRatioValue() {
        // Implementation would calculate hit ratio from counters
        // This is a placeholder - actual implementation would query the meter registry
        return 0.0;
    }
}
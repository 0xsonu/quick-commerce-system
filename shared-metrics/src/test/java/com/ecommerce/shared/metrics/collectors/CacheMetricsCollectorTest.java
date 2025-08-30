package com.ecommerce.shared.metrics.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheMetricsCollectorTest {

    private MeterRegistry meterRegistry;
    private CacheMetricsCollector cacheMetricsCollector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        cacheMetricsCollector = new CacheMetricsCollector(meterRegistry);
    }

    @Test
    void shouldRecordCacheHit() {
        // Given
        String cacheName = "user-cache";
        String operation = "get";

        // When
        cacheMetricsCollector.recordCacheHit(cacheName, operation);

        // Then
        Counter counter = meterRegistry.find("cache.operations")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .tag("result", "hit")
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordCacheMiss() {
        // Given
        String cacheName = "product-cache";
        String operation = "get";

        // When
        cacheMetricsCollector.recordCacheMiss(cacheName, operation);

        // Then
        Counter counter = meterRegistry.find("cache.operations")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .tag("result", "miss")
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordCacheOperationTime() {
        // Given
        String cacheName = "order-cache";
        String operation = "put";
        long durationMs = 25L;

        // When
        cacheMetricsCollector.recordCacheOperationTime(cacheName, operation, durationMs);

        // Then
        Timer timer = meterRegistry.find("cache.operation.duration")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordCacheEviction() {
        // Given
        String cacheName = "session-cache";
        String reason = "ttl";

        // When
        cacheMetricsCollector.recordCacheEviction(cacheName, reason);

        // Then
        Counter counter = meterRegistry.find("cache.evictions")
                .tag("cache", cacheName)
                .tag("reason", reason)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordCacheSize() {
        // Given
        String cacheName = "inventory-cache";
        long size = 1500L;

        // When
        cacheMetricsCollector.recordCacheSize(cacheName, size);

        // Then
        Gauge gauge = meterRegistry.find("cache.size")
                .tag("cache", cacheName)
                .gauge();
        assertNotNull(gauge);
        assertEquals(size, gauge.value());
    }

    @Test
    void shouldRecordCacheMemoryUsage() {
        // Given
        String cacheName = "cart-cache";
        long memoryBytes = 1048576L; // 1MB

        // When
        cacheMetricsCollector.recordCacheMemoryUsage(cacheName, memoryBytes);

        // Then
        Gauge gauge = meterRegistry.find("cache.memory.usage")
                .tag("cache", cacheName)
                .gauge();
        assertNotNull(gauge);
        assertEquals(memoryBytes, gauge.value());
    }

    @Test
    void shouldRecordRedisConnectionPoolMetrics() {
        // Given
        int activeConnections = 5;
        int idleConnections = 10;
        int totalConnections = 15;

        // When
        cacheMetricsCollector.recordRedisConnectionPoolMetrics(activeConnections, idleConnections, totalConnections);

        // Then
        Gauge activeGauge = meterRegistry.find("redis.connections.active").gauge();
        Gauge idleGauge = meterRegistry.find("redis.connections.idle").gauge();
        Gauge totalGauge = meterRegistry.find("redis.connections.total").gauge();

        assertNotNull(activeGauge);
        assertNotNull(idleGauge);
        assertNotNull(totalGauge);

        assertEquals(activeConnections, activeGauge.value());
        assertEquals(idleConnections, idleGauge.value());
        assertEquals(totalConnections, totalGauge.value());
    }

    @Test
    void shouldRecordRedisCommand() {
        // Given
        String command = "SET";
        long durationMs = 5L;
        boolean success = true;

        // When
        cacheMetricsCollector.recordRedisCommand(command, durationMs, success);

        // Then
        Timer timer = meterRegistry.find("redis.command.duration")
                .tag("command", command)
                .tag("success", String.valueOf(success))
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordRedisPipeline() {
        // Given
        int commandCount = 10;
        long durationMs = 50L;
        boolean success = true;

        // When
        cacheMetricsCollector.recordRedisPipeline(commandCount, durationMs, success);

        // Then
        Timer timer = meterRegistry.find("redis.pipeline.duration")
                .tag("success", String.valueOf(success))
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        DistributionSummary summary = meterRegistry.find("redis.pipeline.commands").summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(commandCount, summary.totalAmount());
    }

    @Test
    void shouldRecordCacheWarming() {
        // Given
        String cacheName = "product-cache";
        int itemsWarmed = 1000;
        long durationMs = 5000L;

        // When
        cacheMetricsCollector.recordCacheWarming(cacheName, itemsWarmed, durationMs);

        // Then
        Timer timer = meterRegistry.find("cache.warming.duration")
                .tag("cache", cacheName)
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        DistributionSummary summary = meterRegistry.find("cache.warming.items")
                .tag("cache", cacheName)
                .summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(itemsWarmed, summary.totalAmount());
    }

    @Test
    void shouldRecordCacheSerialization() {
        // Given
        String cacheName = "user-cache";
        String operation = "serialize";
        long durationMs = 15L;
        long sizeBytes = 2048L;

        // When
        cacheMetricsCollector.recordCacheSerialization(cacheName, operation, durationMs, sizeBytes);

        // Then
        Timer timer = meterRegistry.find("cache.serialization.duration")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        DistributionSummary summary = meterRegistry.find("cache.serialization.size")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(sizeBytes, summary.totalAmount());
    }

    @Test
    void shouldRecordDistributedCacheOperation() {
        // Given
        String cacheName = "distributed-cache";
        String node = "node-1";
        String operation = "replicate";
        boolean success = true;

        // When
        cacheMetricsCollector.recordDistributedCacheOperation(cacheName, node, operation, success);

        // Then
        Counter counter = meterRegistry.find("cache.distributed.operations")
                .tag("cache", cacheName)
                .tag("node", node)
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordFailedRedisCommand() {
        // Given
        String command = "GET";
        long durationMs = 100L;
        boolean success = false;

        // When
        cacheMetricsCollector.recordRedisCommand(command, durationMs, success);

        // Then
        Timer timer = meterRegistry.find("redis.command.duration")
                .tag("command", command)
                .tag("success", "false")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordMultipleCacheOperations() {
        // Given
        String cacheName = "test-cache";

        // When
        cacheMetricsCollector.recordCacheHit(cacheName, "get");
        cacheMetricsCollector.recordCacheHit(cacheName, "get");
        cacheMetricsCollector.recordCacheMiss(cacheName, "get");

        // Then
        Counter hitCounter = meterRegistry.find("cache.operations")
                .tag("cache", cacheName)
                .tag("operation", "get")
                .tag("result", "hit")
                .counter();
        Counter missCounter = meterRegistry.find("cache.operations")
                .tag("cache", cacheName)
                .tag("operation", "get")
                .tag("result", "miss")
                .counter();

        assertNotNull(hitCounter);
        assertNotNull(missCounter);
        assertEquals(2, hitCounter.count());
        assertEquals(1, missCounter.count());
    }
}
package com.ecommerce.shared.health.indicator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class JvmHealthIndicatorTest {

    private JvmHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new JvmHealthIndicator();
    }

    @Test
    void shouldReturnHealthWithJvmMetrics() {
        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isIn(Status.UP, new Status("DEGRADED"));
        
        // Memory details
        assertThat(health.getDetails()).containsKey("memory.heap.used");
        assertThat(health.getDetails()).containsKey("memory.heap.max");
        assertThat(health.getDetails()).containsKey("memory.heap.usage_percent");
        assertThat(health.getDetails()).containsKey("memory.nonheap.used");
        assertThat(health.getDetails()).containsKey("memory.nonheap.max");
        assertThat(health.getDetails()).containsKey("memory.nonheap.usage_percent");
        
        // GC details
        assertThat(health.getDetails()).containsKey("gc.total.time");
        assertThat(health.getDetails()).containsKey("gc.total.count");
        assertThat(health.getDetails()).containsKey("gc.average.time");
        
        // Thread details
        assertThat(health.getDetails()).containsKey("threads.count");
        assertThat(health.getDetails()).containsKey("threads.daemon");
        assertThat(health.getDetails()).containsKey("threads.peak");
        assertThat(health.getDetails()).containsKey("threads.deadlocked");
        
        // Runtime details
        assertThat(health.getDetails()).containsKey("runtime.uptime");
        assertThat(health.getDetails()).containsKey("runtime.jvm.version");
        assertThat(health.getDetails()).containsKey("runtime.jvm.vendor");
        
        // System details
        assertThat(health.getDetails()).containsKey("system.processors");
        assertThat(health.getDetails()).containsKey("timestamp");
    }

    @Test
    void shouldFormatBytesCorrectly() {
        // This is tested indirectly through the health check
        // The formatBytes method is private, so we test its output through health()
        Health health = healthIndicator.health();
        
        String heapUsed = (String) health.getDetails().get("memory.heap.used");
        assertThat(heapUsed).matches("\\d+\\.\\d+ [KMGTPE]?B");
    }

    @Test
    void shouldFormatDurationCorrectly() {
        // This is tested indirectly through the health check
        // The formatDuration method is private, so we test its output through health()
        Health health = healthIndicator.health();
        
        String uptime = (String) health.getDetails().get("runtime.uptime");
        assertThat(uptime).matches("\\d+[dhms].*");
    }

    @Test
    void shouldCalculateMemoryUsagePercentage() {
        Health health = healthIndicator.health();
        
        String heapUsagePercent = (String) health.getDetails().get("memory.heap.usage_percent");
        assertThat(heapUsagePercent).matches("\\d+\\.\\d+%");
        
        // Extract percentage value and verify it's reasonable
        double percentage = Double.parseDouble(heapUsagePercent.replace("%", ""));
        assertThat(percentage).isBetween(0.0, 100.0);
    }

    @Test
    void shouldDetectHighMemoryUsage() {
        // This test is challenging because we can't easily simulate high memory usage
        // We can only verify that the logic would work by checking the structure
        Health health = healthIndicator.health();
        
        // If memory usage is high, there should be a warning
        if (health.getDetails().containsKey("memory.warning")) {
            assertThat(health.getStatus().getCode()).isIn("DEGRADED", "DOWN");
        }
    }

    @Test
    void shouldProvideGarbageCollectionMetrics() {
        Health health = healthIndicator.health();
        
        // Should have GC metrics
        assertThat(health.getDetails().get("gc.total.time")).isNotNull();
        assertThat(health.getDetails().get("gc.total.count")).isNotNull();
        assertThat(health.getDetails().get("gc.average.time")).isNotNull();
        
        // GC time should be formatted correctly
        String gcTime = (String) health.getDetails().get("gc.total.time");
        assertThat(gcTime).endsWith("ms");
        
        String avgGcTime = (String) health.getDetails().get("gc.average.time");
        assertThat(avgGcTime).endsWith("ms");
    }

    @Test
    void shouldProvideThreadMetrics() {
        Health health = healthIndicator.health();
        
        Integer threadCount = (Integer) health.getDetails().get("threads.count");
        Integer daemonThreadCount = (Integer) health.getDetails().get("threads.daemon");
        Integer peakThreadCount = (Integer) health.getDetails().get("threads.peak");
        Integer deadlockedThreads = (Integer) health.getDetails().get("threads.deadlocked");
        
        assertThat(threadCount).isPositive();
        assertThat(daemonThreadCount).isNotNegative();
        assertThat(peakThreadCount).isGreaterThanOrEqualTo(threadCount);
        assertThat(deadlockedThreads).isNotNegative();
    }

    @Test
    void shouldProvideSystemMetrics() {
        Health health = healthIndicator.health();
        
        Integer processors = (Integer) health.getDetails().get("system.processors");
        assertThat(processors).isPositive();
        
        // Load average might not be available on all systems
        if (health.getDetails().containsKey("system.load_average")) {
            String loadAverage = (String) health.getDetails().get("system.load_average");
            assertThat(loadAverage).isNotNull();
        }
    }
}
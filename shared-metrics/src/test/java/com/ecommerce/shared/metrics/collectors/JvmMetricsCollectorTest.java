package com.ecommerce.shared.metrics.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class JvmMetricsCollectorTest {

    private MeterRegistry meterRegistry;
    private JvmMetricsCollector jvmMetricsCollector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        jvmMetricsCollector = new JvmMetricsCollector(meterRegistry);
    }

    @Test
    void shouldRegisterJvmMetricsOnApplicationReady() {
        // When
        jvmMetricsCollector.registerMetrics();

        // Then
        assertNotNull(meterRegistry.find("jvm.memory.heap.utilization").gauge());
        assertNotNull(meterRegistry.find("jvm.memory.nonheap.utilization").gauge());
        assertNotNull(meterRegistry.find("jvm.threads.deadlocked").gauge());
        assertNotNull(meterRegistry.find("jvm.process.cpu.usage").gauge());
        assertNotNull(meterRegistry.find("jvm.system.cpu.usage").gauge());
        assertNotNull(meterRegistry.find("jvm.uptime").gauge());
        assertNotNull(meterRegistry.find("jvm.processors.available").gauge());
        assertNotNull(meterRegistry.find("jvm.system.load.average").gauge());
    }

    @Test
    void shouldRecordGcPause() {
        // Given
        String gcName = "G1 Young Generation";
        long pauseTimeMs = 50L;

        // When
        jvmMetricsCollector.recordGcPause(gcName, pauseTimeMs);

        // Then
        Timer timer = meterRegistry.find("jvm.gc.pause.custom")
                .tag("gc", gcName)
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordMemoryAllocation() {
        // Given
        long bytes = 1024L;

        // When
        jvmMetricsCollector.recordMemoryAllocation(bytes);

        // Then
        Counter counter = meterRegistry.find("jvm.memory.allocation.rate")
                .tag("unit", "bytes")
                .counter();
        assertNotNull(counter);
        assertEquals(bytes, counter.count());
    }

    @Test
    void shouldRecordThreadCreation() {
        // When
        jvmMetricsCollector.recordThreadCreation();

        // Then
        Counter counter = meterRegistry.find("jvm.threads.created").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordClassLoading() {
        // Given
        int classCount = 5;

        // When
        jvmMetricsCollector.recordClassLoading(classCount);

        // Then
        Counter counter = meterRegistry.find("jvm.classes.loaded.custom").counter();
        assertNotNull(counter);
        assertEquals(classCount, counter.count());
    }

    @Test
    void shouldRecordMultipleGcPauses() {
        // Given
        String gcName1 = "G1 Young Generation";
        String gcName2 = "G1 Old Generation";
        long pauseTime1 = 30L;
        long pauseTime2 = 100L;

        // When
        jvmMetricsCollector.recordGcPause(gcName1, pauseTime1);
        jvmMetricsCollector.recordGcPause(gcName2, pauseTime2);

        // Then
        Timer timer1 = meterRegistry.find("jvm.gc.pause.custom")
                .tag("gc", gcName1)
                .timer();
        Timer timer2 = meterRegistry.find("jvm.gc.pause.custom")
                .tag("gc", gcName2)
                .timer();
        
        assertNotNull(timer1);
        assertNotNull(timer2);
        assertEquals(1, timer1.count());
        assertEquals(1, timer2.count());
    }

    @Test
    void shouldRecordMultipleMemoryAllocations() {
        // Given
        long allocation1 = 512L;
        long allocation2 = 1024L;

        // When
        jvmMetricsCollector.recordMemoryAllocation(allocation1);
        jvmMetricsCollector.recordMemoryAllocation(allocation2);

        // Then
        Counter counter = meterRegistry.find("jvm.memory.allocation.rate")
                .tag("unit", "bytes")
                .counter();
        assertNotNull(counter);
        assertEquals(allocation1 + allocation2, counter.count());
    }

    @Test
    void shouldRecordMultipleThreadCreations() {
        // When
        jvmMetricsCollector.recordThreadCreation();
        jvmMetricsCollector.recordThreadCreation();
        jvmMetricsCollector.recordThreadCreation();

        // Then
        Counter counter = meterRegistry.find("jvm.threads.created").counter();
        assertNotNull(counter);
        assertEquals(3, counter.count());
    }
}
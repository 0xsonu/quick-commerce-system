package com.ecommerce.shared.metrics.collectors;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

/**
 * Collector for comprehensive JVM metrics including GC, heap, threads, and class loading
 */
@Component
public class JvmMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final OperatingSystemMXBean osMXBean;

    public JvmMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerMetrics() {
        // Register standard JVM metrics
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ClassLoaderMetrics().bindTo(meterRegistry);
        new JvmHeapPressureMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new UptimeMetrics().bindTo(meterRegistry);

        // Register custom JVM metrics
        registerCustomJvmMetrics();
    }

    private void registerCustomJvmMetrics() {
        // Heap utilization percentage
        meterRegistry.gauge("jvm.memory.heap.utilization", this, collector -> {
            long used = memoryMXBean.getHeapMemoryUsage().getUsed();
            long max = memoryMXBean.getHeapMemoryUsage().getMax();
            return max > 0 ? (double) used / max * 100 : 0;
        });

        // Non-heap utilization percentage
        meterRegistry.gauge("jvm.memory.nonheap.utilization", this, collector -> {
            long used = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            long max = memoryMXBean.getNonHeapMemoryUsage().getMax();
            return max > 0 ? (double) used / max * 100 : 0;
        });

        // Thread deadlock detection
        meterRegistry.gauge("jvm.threads.deadlocked", this, collector -> {
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
            return deadlockedThreads != null ? deadlockedThreads.length : 0;
        });

        // CPU usage
        meterRegistry.gauge("jvm.process.cpu.usage", this, collector -> {
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuLoad();
            }
            return -1;
        });

        // System CPU usage
        meterRegistry.gauge("jvm.system.cpu.usage", this, collector -> {
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osMXBean).getSystemCpuLoad();
            }
            return -1;
        });

        // JVM uptime
        meterRegistry.gauge("jvm.uptime", this, collector -> runtimeMXBean.getUptime());

        // Available processors
        meterRegistry.gauge("jvm.processors.available", this, collector -> osMXBean.getAvailableProcessors());

        // System load average
        meterRegistry.gauge("jvm.system.load.average", this, collector -> osMXBean.getSystemLoadAverage());
    }

    /**
     * Record GC pause time
     */
    public void recordGcPause(String gcName, long pauseTimeMs) {
        Timer.builder("jvm.gc.pause.custom")
                .description("Custom GC pause time tracking")
                .tag("gc", gcName)
                .register(meterRegistry)
                .record(pauseTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record memory allocation rate
     */
    public void recordMemoryAllocation(long bytes) {
        meterRegistry.counter("jvm.memory.allocation.rate", "unit", "bytes")
                .increment(bytes);
    }

    /**
     * Record thread creation
     */
    public void recordThreadCreation() {
        meterRegistry.counter("jvm.threads.created").increment();
    }

    /**
     * Record class loading
     */
    public void recordClassLoading(int classCount) {
        meterRegistry.counter("jvm.classes.loaded.custom").increment(classCount);
    }
}
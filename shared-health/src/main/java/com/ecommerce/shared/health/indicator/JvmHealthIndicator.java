package com.ecommerce.shared.health.indicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.time.Instant;
import java.util.List;

/**
 * Health indicator for JVM metrics including memory, GC, and thread monitoring
 */
@Component
public class JvmHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(JvmHealthIndicator.class);
    
    private final MemoryMXBean memoryMXBean;
    private final List<GarbageCollectorMXBean> gcMXBeans;
    private final ThreadMXBean threadMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final OperatingSystemMXBean osMXBean;
    
    // Health check thresholds
    private static final double MEMORY_WARNING_THRESHOLD = 0.85; // 85%
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.95; // 95%
    private static final long GC_TIME_WARNING_THRESHOLD = 1000; // 1 second
    private static final int THREAD_COUNT_WARNING_THRESHOLD = 500;
    private static final int DEADLOCK_CRITICAL = 1;

    public JvmHealthIndicator() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public Health health() {
        try {
            return checkJvmHealth();
        } catch (Exception e) {
            logger.error("JVM health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }

    private Health checkJvmHealth() {
        Health.Builder healthBuilder = Health.up();
        
        // Check memory usage
        MemoryStatus memoryStatus = checkMemoryUsage(healthBuilder);
        
        // Check garbage collection
        GcStatus gcStatus = checkGarbageCollection(healthBuilder);
        
        // Check thread status
        ThreadStatus threadStatus = checkThreads(healthBuilder);
        
        // Add runtime information
        addRuntimeInfo(healthBuilder);
        
        // Add system information
        addSystemInfo(healthBuilder);
        
        // Determine overall health status
        String overallStatus = determineOverallStatus(memoryStatus, gcStatus, threadStatus);
        if (!"UP".equals(overallStatus)) {
            healthBuilder.status(overallStatus);
        }
        
        healthBuilder.withDetail("timestamp", Instant.now().toString());
        
        return healthBuilder.build();
    }

    private MemoryStatus checkMemoryUsage(Health.Builder healthBuilder) {
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        
        // Calculate heap usage percentage
        double heapUsagePercent = (double) heapMemory.getUsed() / heapMemory.getMax();
        double nonHeapUsagePercent = nonHeapMemory.getMax() > 0 ? 
                (double) nonHeapMemory.getUsed() / nonHeapMemory.getMax() : 0;
        
        // Add memory details
        healthBuilder.withDetail("memory.heap.used", formatBytes(heapMemory.getUsed()))
                .withDetail("memory.heap.max", formatBytes(heapMemory.getMax()))
                .withDetail("memory.heap.usage_percent", String.format("%.2f%%", heapUsagePercent * 100))
                .withDetail("memory.nonheap.used", formatBytes(nonHeapMemory.getUsed()))
                .withDetail("memory.nonheap.max", formatBytes(nonHeapMemory.getMax()))
                .withDetail("memory.nonheap.usage_percent", String.format("%.2f%%", nonHeapUsagePercent * 100));
        
        // Determine memory status
        if (heapUsagePercent >= MEMORY_CRITICAL_THRESHOLD) {
            healthBuilder.withDetail("memory.warning", "Critical heap memory usage");
            return MemoryStatus.CRITICAL;
        } else if (heapUsagePercent >= MEMORY_WARNING_THRESHOLD) {
            healthBuilder.withDetail("memory.warning", "High heap memory usage");
            return MemoryStatus.WARNING;
        }
        
        return MemoryStatus.HEALTHY;
    }

    private GcStatus checkGarbageCollection(Health.Builder healthBuilder) {
        long totalGcTime = 0;
        long totalGcCount = 0;
        
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            long gcTime = gcBean.getCollectionTime();
            long gcCount = gcBean.getCollectionCount();
            
            totalGcTime += gcTime;
            totalGcCount += gcCount;
            
            healthBuilder.withDetail("gc." + gcBean.getName().toLowerCase().replace(" ", "_") + ".time", gcTime + "ms")
                    .withDetail("gc." + gcBean.getName().toLowerCase().replace(" ", "_") + ".count", gcCount);
        }
        
        healthBuilder.withDetail("gc.total.time", totalGcTime + "ms")
                .withDetail("gc.total.count", totalGcCount);
        
        // Calculate average GC time per collection
        double avgGcTime = totalGcCount > 0 ? (double) totalGcTime / totalGcCount : 0;
        healthBuilder.withDetail("gc.average.time", String.format("%.2fms", avgGcTime));
        
        // Determine GC status
        if (avgGcTime >= GC_TIME_WARNING_THRESHOLD) {
            healthBuilder.withDetail("gc.warning", "High average GC time");
            return GcStatus.WARNING;
        }
        
        return GcStatus.HEALTHY;
    }

    private ThreadStatus checkThreads(Health.Builder healthBuilder) {
        int threadCount = threadMXBean.getThreadCount();
        int daemonThreadCount = threadMXBean.getDaemonThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();
        
        healthBuilder.withDetail("threads.count", threadCount)
                .withDetail("threads.daemon", daemonThreadCount)
                .withDetail("threads.peak", peakThreadCount);
        
        // Check for deadlocks
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        int deadlockCount = deadlockedThreads != null ? deadlockedThreads.length : 0;
        healthBuilder.withDetail("threads.deadlocked", deadlockCount);
        
        // Determine thread status
        if (deadlockCount >= DEADLOCK_CRITICAL) {
            healthBuilder.withDetail("threads.warning", "Deadlocked threads detected");
            return ThreadStatus.CRITICAL;
        } else if (threadCount >= THREAD_COUNT_WARNING_THRESHOLD) {
            healthBuilder.withDetail("threads.warning", "High thread count");
            return ThreadStatus.WARNING;
        }
        
        return ThreadStatus.HEALTHY;
    }

    private void addRuntimeInfo(Health.Builder healthBuilder) {
        long uptime = runtimeMXBean.getUptime();
        String jvmVersion = runtimeMXBean.getVmVersion();
        String jvmVendor = runtimeMXBean.getVmVendor();
        
        healthBuilder.withDetail("runtime.uptime", formatDuration(uptime))
                .withDetail("runtime.jvm.version", jvmVersion)
                .withDetail("runtime.jvm.vendor", jvmVendor);
    }

    private void addSystemInfo(Health.Builder healthBuilder) {
        int availableProcessors = osMXBean.getAvailableProcessors();
        double systemLoadAverage = osMXBean.getSystemLoadAverage();
        
        healthBuilder.withDetail("system.processors", availableProcessors);
        
        if (systemLoadAverage >= 0) {
            healthBuilder.withDetail("system.load_average", String.format("%.2f", systemLoadAverage));
        }
        
        // Add additional system info if available (for some JVM implementations)
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osMXBean;
            
            healthBuilder.withDetail("system.cpu_usage", String.format("%.2f%%", sunOsBean.getProcessCpuLoad() * 100))
                    .withDetail("system.memory.total", formatBytes(sunOsBean.getTotalPhysicalMemorySize()))
                    .withDetail("system.memory.free", formatBytes(sunOsBean.getFreePhysicalMemorySize()));
        }
    }

    private String determineOverallStatus(MemoryStatus memoryStatus, GcStatus gcStatus, ThreadStatus threadStatus) {
        if (memoryStatus == MemoryStatus.CRITICAL || threadStatus == ThreadStatus.CRITICAL) {
            return "DOWN";
        } else if (memoryStatus == MemoryStatus.WARNING || gcStatus == GcStatus.WARNING || 
                   threadStatus == ThreadStatus.WARNING) {
            return "DEGRADED";
        }
        return "UP";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private enum MemoryStatus {
        HEALTHY, WARNING, CRITICAL
    }

    private enum GcStatus {
        HEALTHY, WARNING
    }

    private enum ThreadStatus {
        HEALTHY, WARNING, CRITICAL
    }
}
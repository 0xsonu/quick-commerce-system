package com.ecommerce.shared.health.indicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Composite health indicator that aggregates multiple health checks
 * and provides an overall system health status with degraded state support
 */
@Component
public class CompositeHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CompositeHealthIndicator.class);
    
    private final Map<String, HealthIndicator> healthIndicators;
    private final ExecutorService executorService;
    
    // Health check timeout
    private static final long HEALTH_CHECK_TIMEOUT_SECONDS = 10;

    public CompositeHealthIndicator(Map<String, HealthIndicator> healthIndicators) {
        this.healthIndicators = new HashMap<>(healthIndicators);
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "health-check-thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public Health health() {
        try {
            return performCompositeHealthCheck();
        } catch (Exception e) {
            logger.error("Composite health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }

    private Health performCompositeHealthCheck() {
        Map<String, CompletableFuture<Health>> healthFutures = new HashMap<>();
        
        // Start all health checks asynchronously
        for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
            String name = entry.getKey();
            HealthIndicator indicator = entry.getValue();
            
            CompletableFuture<Health> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return indicator.health();
                } catch (Exception e) {
                    logger.warn("Health check failed for {}", name, e);
                    return Health.down()
                            .withDetail("error", e.getMessage())
                            .withDetail("component", name)
                            .build();
                }
            }, executorService);
            
            healthFutures.put(name, future);
        }
        
        // Collect results with timeout
        Map<String, Health> healthResults = new HashMap<>();
        for (Map.Entry<String, CompletableFuture<Health>> entry : healthFutures.entrySet()) {
            String name = entry.getKey();
            CompletableFuture<Health> future = entry.getValue();
            
            try {
                Health health = future.get(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                healthResults.put(name, health);
            } catch (Exception e) {
                logger.warn("Health check timed out or failed for {}", name, e);
                healthResults.put(name, Health.down()
                        .withDetail("error", "Health check timed out or failed")
                        .withDetail("component", name)
                        .build());
            }
        }
        
        return aggregateHealthResults(healthResults);
    }

    private Health aggregateHealthResults(Map<String, Health> healthResults) {
        Health.Builder compositeHealthBuilder = Health.up();
        
        int upCount = 0;
        int degradedCount = 0;
        int downCount = 0;
        int totalCount = healthResults.size();
        
        // Analyze individual health statuses
        for (Map.Entry<String, Health> entry : healthResults.entrySet()) {
            String componentName = entry.getKey();
            Health componentHealth = entry.getValue();
            Status status = componentHealth.getStatus();
            
            // Add component details to composite health
            compositeHealthBuilder.withDetail(componentName, Map.of(
                    "status", status.getCode(),
                    "details", componentHealth.getDetails()
            ));
            
            // Count statuses
            if (Status.UP.equals(status)) {
                upCount++;
            } else if ("DEGRADED".equals(status.getCode())) {
                degradedCount++;
            } else {
                downCount++;
            }
        }
        
        // Add summary information
        compositeHealthBuilder.withDetail("summary", Map.of(
                "total", totalCount,
                "up", upCount,
                "degraded", degradedCount,
                "down", downCount
        ));
        
        // Determine overall status
        Status overallStatus = determineOverallStatus(upCount, degradedCount, downCount, totalCount);
        compositeHealthBuilder.status(overallStatus);
        
        // Add status explanation
        addStatusExplanation(compositeHealthBuilder, overallStatus, upCount, degradedCount, downCount, totalCount);
        
        compositeHealthBuilder.withDetail("timestamp", Instant.now().toString());
        
        return compositeHealthBuilder.build();
    }

    private Status determineOverallStatus(int upCount, int degradedCount, int downCount, int totalCount) {
        // If any critical component is down, system is down
        if (downCount > 0) {
            return Status.DOWN;
        }
        
        // If some components are degraded but none are down, system is degraded
        if (degradedCount > 0) {
            return new Status("DEGRADED");
        }
        
        // If all components are up, system is up
        if (upCount == totalCount) {
            return Status.UP;
        }
        
        // Default to degraded if we can't determine status clearly
        return new Status("DEGRADED");
    }

    private void addStatusExplanation(Health.Builder builder, Status status, 
                                    int upCount, int degradedCount, int downCount, int totalCount) {
        String explanation;
        
        if (Status.DOWN.equals(status)) {
            explanation = String.format("System is DOWN: %d component(s) are down, %d degraded, %d up", 
                    downCount, degradedCount, upCount);
        } else if ("DEGRADED".equals(status.getCode())) {
            explanation = String.format("System is DEGRADED: %d component(s) are degraded, %d up, %d down", 
                    degradedCount, upCount, downCount);
        } else {
            explanation = String.format("System is UP: All %d components are healthy", totalCount);
        }
        
        builder.withDetail("status_explanation", explanation);
    }

    /**
     * Get health status for a specific component
     */
    public Health getComponentHealth(String componentName) {
        HealthIndicator indicator = healthIndicators.get(componentName);
        if (indicator == null) {
            return Health.unknown()
                    .withDetail("error", "Component not found: " + componentName)
                    .build();
        }
        
        try {
            return indicator.health();
        } catch (Exception e) {
            logger.warn("Health check failed for component {}", componentName, e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("component", componentName)
                    .build();
        }
    }

    /**
     * Get the names of all registered health indicators
     */
    public java.util.Set<String> getComponentNames() {
        return healthIndicators.keySet();
    }
}
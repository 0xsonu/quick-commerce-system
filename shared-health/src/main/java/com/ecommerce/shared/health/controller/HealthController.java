package com.ecommerce.shared.health.controller;

import com.ecommerce.shared.health.indicator.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * REST controller for detailed health check endpoints
 */
@RestController
@RequestMapping("/actuator/health")
@ConditionalOnProperty(name = "management.endpoints.web.exposure.include", havingValue = "health", matchIfMissing = true)
public class HealthController {

    private final CompositeHealthIndicator compositeHealthIndicator;
    private final Map<String, HealthIndicator> healthIndicators;

    public HealthController(CompositeHealthIndicator compositeHealthIndicator,
                          Map<String, HealthIndicator> healthIndicators) {
        this.compositeHealthIndicator = compositeHealthIndicator;
        this.healthIndicators = healthIndicators;
    }

    /**
     * Get overall system health with all component details
     */
    @GetMapping("/detailed")
    public ResponseEntity<Health> getDetailedHealth() {
        Health health = compositeHealthIndicator.health();
        return ResponseEntity.ok(health);
    }

    /**
     * Get health status for a specific component
     */
    @GetMapping("/component/{componentName}")
    public ResponseEntity<Health> getComponentHealth(@PathVariable String componentName) {
        Health health = compositeHealthIndicator.getComponentHealth(componentName);
        
        if ("UNKNOWN".equals(health.getStatus().getCode())) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get list of all available health components
     */
    @GetMapping("/components")
    public ResponseEntity<Set<String>> getAvailableComponents() {
        Set<String> componentNames = compositeHealthIndicator.getComponentNames();
        return ResponseEntity.ok(componentNames);
    }

    /**
     * Kubernetes liveness probe endpoint
     * Returns 200 if the application is running, 503 if it should be restarted
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> getLivenessProbe() {
        // Liveness probe should only check if the application is running
        // It should not check external dependencies
        try {
            // Simple check - if we can respond, we're alive
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "timestamp", java.time.Instant.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", java.time.Instant.now().toString()
            ));
        }
    }

    /**
     * Kubernetes readiness probe endpoint
     * Returns 200 if the application is ready to serve traffic
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> getReadinessProbe() {
        Health health = compositeHealthIndicator.health();
        String status = health.getStatus().getCode();
        
        // Ready if UP or DEGRADED (can still serve traffic with degraded performance)
        if ("UP".equals(status) || "DEGRADED".equals(status)) {
            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "ready", true,
                    "timestamp", java.time.Instant.now().toString(),
                    "details", health.getDetails()
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                    "status", status,
                    "ready", false,
                    "timestamp", java.time.Instant.now().toString(),
                    "details", health.getDetails()
            ));
        }
    }

    /**
     * Kubernetes startup probe endpoint
     * Returns 200 when the application has finished starting up
     */
    @GetMapping("/startup")
    public ResponseEntity<Map<String, Object>> getStartupProbe() {
        Health health = compositeHealthIndicator.health();
        String status = health.getStatus().getCode();
        
        // Started if not DOWN (UP or DEGRADED means startup completed)
        if (!"DOWN".equals(status)) {
            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "started", true,
                    "timestamp", java.time.Instant.now().toString()
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                    "status", status,
                    "started", false,
                    "timestamp", java.time.Instant.now().toString(),
                    "details", health.getDetails()
            ));
        }
    }
}
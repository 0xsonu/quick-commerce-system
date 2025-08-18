package com.ecommerce.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for API Gateway
 */
@RestController
@RequestMapping("/api/v1/gateway")
public class HealthController {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return checkHealth()
            .map(healthStatus -> {
                if (healthStatus.get("status").equals("UP")) {
                    return ResponseEntity.ok(healthStatus);
                } else {
                    return ResponseEntity.status(503).body(healthStatus);
                }
            });
    }

    @GetMapping("/ready")
    public Mono<ResponseEntity<Map<String, Object>>> ready() {
        return checkReadiness()
            .map(readinessStatus -> {
                if (readinessStatus.get("status").equals("UP")) {
                    return ResponseEntity.ok(readinessStatus);
                } else {
                    return ResponseEntity.status(503).body(readinessStatus);
                }
            });
    }

    @GetMapping("/live")
    public Mono<ResponseEntity<Map<String, Object>>> live() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "UP");
        liveness.put("timestamp", Instant.now().toString());
        liveness.put("service", "api-gateway");
        return Mono.just(ResponseEntity.ok(liveness));
    }

    private Mono<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "api-gateway");
        health.put("timestamp", Instant.now().toString());

        return checkRedisHealth()
            .map(redisHealth -> {
                health.put("redis", redisHealth);
                
                // Overall status based on components
                boolean allHealthy = redisHealth.get("status").equals("UP");
                health.put("status", allHealthy ? "UP" : "DOWN");
                
                return health;
            })
            .onErrorReturn(createErrorHealth(health, "Health check failed"));
    }

    private Mono<Map<String, Object>> checkReadiness() {
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("service", "api-gateway");
        readiness.put("timestamp", Instant.now().toString());

        return checkRedisHealth()
            .map(redisHealth -> {
                readiness.put("redis", redisHealth);
                
                // Gateway is ready if Redis is available (for rate limiting)
                boolean ready = redisHealth.get("status").equals("UP");
                readiness.put("status", ready ? "UP" : "DOWN");
                
                return readiness;
            })
            .onErrorReturn(createErrorHealth(readiness, "Readiness check failed"));
    }

    private Mono<Map<String, Object>> checkRedisHealth() {
        Map<String, Object> redisHealth = new HashMap<>();
        
        return redisTemplate.opsForValue()
            .set("health:check", "ping")
            .timeout(Duration.ofSeconds(2))
            .then(redisTemplate.opsForValue().get("health:check"))
            .timeout(Duration.ofSeconds(2))
            .map(value -> {
                if ("ping".equals(value)) {
                    redisHealth.put("status", "UP");
                    redisHealth.put("details", "Redis connection successful");
                } else {
                    redisHealth.put("status", "DOWN");
                    redisHealth.put("details", "Redis ping failed");
                }
                return redisHealth;
            })
            .onErrorReturn(createRedisErrorHealth());
    }

    private Map<String, Object> createRedisErrorHealth() {
        Map<String, Object> redisHealth = new HashMap<>();
        redisHealth.put("status", "DOWN");
        redisHealth.put("details", "Redis connection failed");
        return redisHealth;
    }

    private Map<String, Object> createErrorHealth(Map<String, Object> health, String message) {
        health.put("status", "DOWN");
        health.put("error", message);
        return health;
    }
}
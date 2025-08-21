package com.ecommerce.shared.utils.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Custom Redis health indicator for comprehensive Redis health monitoring
 * Provides detailed health information including connection status and server info
 */
@Component
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnBean({RedisConnectionFactory.class, RedisTemplate.class})
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthIndicator.class);
    
    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory, 
                               RedisTemplate<String, Object> redisTemplate) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            return checkRedisHealth();
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
        }
    }

    private Health checkRedisHealth() {
        Map<String, Object> details = new HashMap<>();
        
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            long startTime = System.currentTimeMillis();
            
            // Test basic connectivity with ping
            String pong = connection.ping();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (!"PONG".equals(pong)) {
                return Health.down()
                        .withDetail("error", "Redis ping command failed")
                        .withDetail("expected", "PONG")
                        .withDetail("actual", pong)
                        .build();
            }
            
            details.put("responseTime", responseTime + "ms");
            details.put("status", "UP");
            
            // Get Redis server information
            addServerDetails(details, connection);
            
            // Get memory information
            addMemoryDetails(details, connection);
            
            // Get client information
            addClientDetails(details, connection);
            
            // Test basic operations
            testBasicOperations(details);
            
            // Determine health status based on response time
            if (responseTime > 5000) { // 5 seconds threshold
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "Redis response time too high")
                        .build();
            } else if (responseTime > 1000) { // 1 second warning threshold
                details.put("warning", "Redis response time is elevated");
            }
            
            return Health.up().withDetails(details).build();
            
        } catch (Exception e) {
            logger.error("Redis connectivity check failed", e);
            return Health.down()
                    .withDetail("error", "Redis connectivity failed")
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .withDetails(details)
                    .build();
        }
    }
    
    private void addServerDetails(Map<String, Object> details, RedisConnection connection) {
        try {
            Properties info = connection.info("server");
            
            Map<String, Object> serverDetails = new HashMap<>();
            serverDetails.put("version", info.getProperty("redis_version"));
            serverDetails.put("mode", info.getProperty("redis_mode"));
            serverDetails.put("os", info.getProperty("os"));
            serverDetails.put("uptime", info.getProperty("uptime_in_seconds") + " seconds");
            
            details.put("server", serverDetails);
            
        } catch (Exception e) {
            logger.debug("Could not retrieve Redis server details", e);
            details.put("serverInfo", "Not available");
        }
    }
    
    private void addMemoryDetails(Map<String, Object> details, RedisConnection connection) {
        try {
            Properties info = connection.info("memory");
            
            Map<String, Object> memoryDetails = new HashMap<>();
            memoryDetails.put("usedMemory", info.getProperty("used_memory_human"));
            memoryDetails.put("maxMemory", info.getProperty("maxmemory_human"));
            memoryDetails.put("memoryFragmentationRatio", info.getProperty("mem_fragmentation_ratio"));
            
            details.put("memory", memoryDetails);
            
            // Check for memory issues
            String fragRatio = info.getProperty("mem_fragmentation_ratio");
            if (fragRatio != null) {
                double ratio = Double.parseDouble(fragRatio);
                if (ratio > 1.5) {
                    details.put("warning", "High memory fragmentation ratio: " + ratio);
                }
            }
            
        } catch (Exception e) {
            logger.debug("Could not retrieve Redis memory details", e);
            details.put("memoryInfo", "Not available");
        }
    }
    
    private void addClientDetails(Map<String, Object> details, RedisConnection connection) {
        try {
            Properties info = connection.info("clients");
            
            Map<String, Object> clientDetails = new HashMap<>();
            clientDetails.put("connectedClients", info.getProperty("connected_clients"));
            clientDetails.put("blockedClients", info.getProperty("blocked_clients"));
            
            details.put("clients", clientDetails);
            
            // Check for potential client issues
            String blockedClients = info.getProperty("blocked_clients");
            if (blockedClients != null && Integer.parseInt(blockedClients) > 0) {
                details.put("warning", "There are blocked Redis clients: " + blockedClients);
            }
            
        } catch (Exception e) {
            logger.debug("Could not retrieve Redis client details", e);
            details.put("clientInfo", "Not available");
        }
    }
    
    private void testBasicOperations(Map<String, Object> details) {
        try {
            String testKey = "health:check:" + System.currentTimeMillis();
            String testValue = "test-value";
            
            // Test SET operation
            redisTemplate.opsForValue().set(testKey, testValue, 
                java.time.Duration.ofSeconds(10));
            
            // Test GET operation
            Object retrievedValue = redisTemplate.opsForValue().get(testKey);
            
            // Test DELETE operation
            redisTemplate.delete(testKey);
            
            boolean operationsSuccessful = testValue.equals(retrievedValue);
            details.put("basicOperations", operationsSuccessful ? "SUCCESS" : "FAILED");
            
            if (!operationsSuccessful) {
                details.put("warning", "Basic Redis operations test failed");
            }
            
        } catch (Exception e) {
            logger.debug("Redis basic operations test failed", e);
            details.put("basicOperations", "FAILED");
            details.put("operationsError", e.getMessage());
        }
    }
    
    /**
     * Test Redis performance with multiple operations
     */
    public boolean testPerformance() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Perform multiple operations to test performance
            for (int i = 0; i < 100; i++) {
                String key = "perf:test:" + i;
                redisTemplate.opsForValue().set(key, "value" + i, 
                    java.time.Duration.ofSeconds(10));
                redisTemplate.opsForValue().get(key);
                redisTemplate.delete(key);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Should complete 300 operations (100 sets, 100 gets, 100 deletes) in reasonable time
            return duration < 5000; // 5 seconds threshold
            
        } catch (Exception e) {
            logger.error("Redis performance test failed", e);
            return false;
        }
    }
}
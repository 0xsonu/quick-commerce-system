package com.ecommerce.shared.health.indicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Health indicator for Redis cache connectivity and performance
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthIndicator.class);
    
    private final RedisConnectionFactory connectionFactory;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Health check thresholds
    private static final long SLOW_OPERATION_THRESHOLD_MS = 500; // 500ms
    private static final String HEALTH_CHECK_KEY = "health:check:";

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory, 
                               RedisTemplate<String, Object> redisTemplate) {
        this.connectionFactory = connectionFactory;
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
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }

    private Health checkRedisHealth() {
        Health.Builder healthBuilder = Health.up();
        
        // Check basic connectivity
        Instant start = Instant.now();
        boolean isConnected = checkConnection();
        Duration connectionTime = Duration.between(start, Instant.now());
        
        if (!isConnected) {
            return Health.down()
                    .withDetail("error", "Unable to connect to Redis")
                    .withDetail("connectionTime", connectionTime.toMillis() + "ms")
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
        
        healthBuilder.withDetail("connectionTime", connectionTime.toMillis() + "ms");
        
        // Check read/write performance
        start = Instant.now();
        boolean operationsSuccessful = checkOperations();
        Duration operationTime = Duration.between(start, Instant.now());
        
        healthBuilder.withDetail("operationTime", operationTime.toMillis() + "ms");
        
        if (!operationsSuccessful) {
            healthBuilder.status("DEGRADED")
                    .withDetail("warning", "Redis operations are slow or failing");
        }
        
        // Add Redis server information
        addRedisServerInfo(healthBuilder);
        
        // Determine overall status
        if (connectionTime.toMillis() > SLOW_OPERATION_THRESHOLD_MS || 
            operationTime.toMillis() > SLOW_OPERATION_THRESHOLD_MS) {
            healthBuilder.status("DEGRADED");
        }
        
        healthBuilder.withDetail("timestamp", Instant.now().toString());
        
        return healthBuilder.build();
    }

    private boolean checkConnection() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Object pingResult = connection.ping();
            return pingResult != null;
        } catch (Exception e) {
            logger.warn("Redis connection check failed", e);
            return false;
        }
    }

    private boolean checkOperations() {
        try {
            String testKey = HEALTH_CHECK_KEY + System.currentTimeMillis();
            String testValue = "health-check-value";
            
            // Test write operation
            Instant start = Instant.now();
            redisTemplate.opsForValue().set(testKey, testValue, 10, TimeUnit.SECONDS);
            Duration writeTime = Duration.between(start, Instant.now());
            
            // Test read operation
            start = Instant.now();
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
            Duration readTime = Duration.between(start, Instant.now());
            
            // Clean up test key
            redisTemplate.delete(testKey);
            
            boolean operationsSuccessful = testValue.equals(retrievedValue) &&
                    writeTime.toMillis() < SLOW_OPERATION_THRESHOLD_MS &&
                    readTime.toMillis() < SLOW_OPERATION_THRESHOLD_MS;
            
            return operationsSuccessful;
        } catch (Exception e) {
            logger.warn("Redis operations check failed", e);
            return false;
        }
    }

    private void addRedisServerInfo(Health.Builder healthBuilder) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Properties info = connection.info();
            
            if (info != null) {
                // Add key Redis metrics
                healthBuilder.withDetail("redis.version", info.getProperty("redis_version"))
                        .withDetail("redis.mode", info.getProperty("redis_mode"))
                        .withDetail("connected_clients", info.getProperty("connected_clients"))
                        .withDetail("used_memory_human", info.getProperty("used_memory_human"))
                        .withDetail("used_memory_peak_human", info.getProperty("used_memory_peak_human"))
                        .withDetail("keyspace_hits", info.getProperty("keyspace_hits"))
                        .withDetail("keyspace_misses", info.getProperty("keyspace_misses"));
                
                // Calculate hit ratio
                String hits = info.getProperty("keyspace_hits");
                String misses = info.getProperty("keyspace_misses");
                if (hits != null && misses != null) {
                    try {
                        long hitsLong = Long.parseLong(hits);
                        long missesLong = Long.parseLong(misses);
                        long total = hitsLong + missesLong;
                        if (total > 0) {
                            double hitRatio = (double) hitsLong / total * 100;
                            healthBuilder.withDetail("hit_ratio_percent", String.format("%.2f", hitRatio));
                        }
                    } catch (NumberFormatException e) {
                        logger.debug("Could not calculate hit ratio", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve Redis server information", e);
        }
    }
}
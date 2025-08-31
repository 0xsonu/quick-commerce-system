package com.ecommerce.shared.health.indicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Duration;
import java.time.Instant;

/**
 * Health indicator for MongoDB connectivity and performance
 */
@Component
public class MongoHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(MongoHealthIndicator.class);
    
    private final MongoTemplate mongoTemplate;
    
    // Health check thresholds
    private static final long SLOW_OPERATION_THRESHOLD_MS = 1000; // 1 second

    public MongoHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            return checkMongoHealth();
        } catch (Exception e) {
            logger.error("MongoDB health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }

    private Health checkMongoHealth() {
        Health.Builder healthBuilder = Health.up();
        
        // Check basic connectivity and get server status
        Instant start = Instant.now();
        Document serverStatus = checkConnection();
        Duration connectionTime = Duration.between(start, Instant.now());
        
        if (serverStatus == null) {
            return Health.down()
                    .withDetail("error", "Unable to connect to MongoDB")
                    .withDetail("connectionTime", connectionTime.toMillis() + "ms")
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
        
        healthBuilder.withDetail("connectionTime", connectionTime.toMillis() + "ms");
        
        // Check database operations
        start = Instant.now();
        boolean operationsSuccessful = checkOperations();
        Duration operationTime = Duration.between(start, Instant.now());
        
        healthBuilder.withDetail("operationTime", operationTime.toMillis() + "ms");
        
        if (!operationsSuccessful) {
            healthBuilder.status("DEGRADED")
                    .withDetail("warning", "MongoDB operations are slow or failing");
        }
        
        // Add MongoDB server information
        addMongoServerInfo(healthBuilder, serverStatus);
        
        // Determine overall status
        if (connectionTime.toMillis() > SLOW_OPERATION_THRESHOLD_MS || 
            operationTime.toMillis() > SLOW_OPERATION_THRESHOLD_MS) {
            healthBuilder.status("DEGRADED");
        }
        
        healthBuilder.withDetail("timestamp", Instant.now().toString());
        
        return healthBuilder.build();
    }

    private Document checkConnection() {
        try {
            MongoDatabase database = mongoTemplate.getDb();
            return database.runCommand(new Document("serverStatus", 1));
        } catch (Exception e) {
            logger.warn("MongoDB connection check failed", e);
            return null;
        }
    }

    private boolean checkOperations() {
        try {
            // Test a simple count operation
            Instant start = Instant.now();
            mongoTemplate.getCollectionNames();
            Duration operationTime = Duration.between(start, Instant.now());
            
            return operationTime.toMillis() < SLOW_OPERATION_THRESHOLD_MS;
        } catch (Exception e) {
            logger.warn("MongoDB operations check failed", e);
            return false;
        }
    }

    private void addMongoServerInfo(Health.Builder healthBuilder, Document serverStatus) {
        try {
            // Add MongoDB version and basic info
            healthBuilder.withDetail("mongo.version", serverStatus.getString("version"))
                    .withDetail("mongo.uptime", serverStatus.getInteger("uptime", 0) + "s");
            
            // Add connection information
            Document connections = serverStatus.get("connections", Document.class);
            if (connections != null) {
                healthBuilder.withDetail("connections.current", connections.getInteger("current", 0))
                        .withDetail("connections.available", connections.getInteger("available", 0))
                        .withDetail("connections.totalCreated", connections.getInteger("totalCreated", 0));
            }
            
            // Add memory information
            Document mem = serverStatus.get("mem", Document.class);
            if (mem != null) {
                healthBuilder.withDetail("memory.resident", mem.getInteger("resident", 0) + "MB")
                        .withDetail("memory.virtual", mem.getInteger("virtual", 0) + "MB");
            }
            
            // Add operation counters
            Document opcounters = serverStatus.get("opcounters", Document.class);
            if (opcounters != null) {
                healthBuilder.withDetail("opcounters.insert", opcounters.getInteger("insert", 0))
                        .withDetail("opcounters.query", opcounters.getInteger("query", 0))
                        .withDetail("opcounters.update", opcounters.getInteger("update", 0))
                        .withDetail("opcounters.delete", opcounters.getInteger("delete", 0));
            }
            
            // Add database name
            healthBuilder.withDetail("database", mongoTemplate.getDb().getName());
            
        } catch (Exception e) {
            logger.debug("Could not retrieve MongoDB server information", e);
        }
    }
}
package com.ecommerce.shared.utils.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom MongoDB health indicator for comprehensive MongoDB health monitoring
 * Provides detailed health information including connection status and database info
 */
@Component
public class MongoHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(MongoHealthIndicator.class);
    
    private final MongoTemplate mongoTemplate;
    
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
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
        }
    }

    private Health checkMongoHealth() {
        Map<String, Object> details = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Test basic connectivity with ping command
            var result = mongoTemplate.getDb().runCommand(
                new org.bson.Document("ping", 1)
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (result.getDouble("ok") != 1.0) {
                return Health.down()
                        .withDetail("error", "MongoDB ping command failed")
                        .withDetail("result", result.toJson())
                        .build();
            }
            
            details.put("responseTime", responseTime + "ms");
            details.put("status", "UP");
            
            // Get database information
            addDatabaseDetails(details);
            
            // Get server status information
            addServerStatusDetails(details);
            
            // Determine health status based on response time
            if (responseTime > 5000) { // 5 seconds threshold
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "MongoDB response time too high")
                        .build();
            } else if (responseTime > 1000) { // 1 second warning threshold
                details.put("warning", "MongoDB response time is elevated");
            }
            
            return Health.up().withDetails(details).build();
            
        } catch (Exception e) {
            logger.error("MongoDB connectivity check failed", e);
            return Health.down()
                    .withDetail("error", "MongoDB connectivity failed")
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .withDetails(details)
                    .build();
        }
    }
    
    private void addDatabaseDetails(Map<String, Object> details) {
        try {
            String databaseName = mongoTemplate.getDb().getName();
            details.put("database", databaseName);
            
            // Get collection names to verify database access
            var collections = mongoTemplate.getDb().listCollectionNames();
            details.put("collections", collections.into(new java.util.ArrayList<>()).size());
            
        } catch (Exception e) {
            logger.debug("Could not retrieve database details", e);
            details.put("databaseInfo", "Not available");
        }
    }
    
    private void addServerStatusDetails(Map<String, Object> details) {
        try {
            // Get server status for additional health information
            var serverStatus = mongoTemplate.getDb().runCommand(
                new org.bson.Document("serverStatus", 1)
            );
            
            // Extract useful information
            if (serverStatus.containsKey("version")) {
                details.put("version", serverStatus.getString("version"));
            }
            
            if (serverStatus.containsKey("uptime")) {
                details.put("uptime", serverStatus.getInteger("uptime") + " seconds");
            }
            
            // Connection information
            if (serverStatus.containsKey("connections")) {
                var connections = serverStatus.get("connections", org.bson.Document.class);
                Map<String, Object> connectionDetails = new HashMap<>();
                connectionDetails.put("current", connections.getInteger("current"));
                connectionDetails.put("available", connections.getInteger("available"));
                connectionDetails.put("totalCreated", connections.getInteger("totalCreated"));
                details.put("connections", connectionDetails);
                
                // Check for potential connection issues
                int current = connections.getInteger("current", 0);
                int available = connections.getInteger("available", 0);
                
                if (available < 10) {
                    details.put("warning", "Low number of available MongoDB connections");
                }
            }
            
            // Memory information
            if (serverStatus.containsKey("mem")) {
                var mem = serverStatus.get("mem", org.bson.Document.class);
                Map<String, Object> memDetails = new HashMap<>();
                memDetails.put("resident", mem.getInteger("resident") + " MB");
                memDetails.put("virtual", mem.getInteger("virtual") + " MB");
                details.put("memory", memDetails);
            }
            
        } catch (Exception e) {
            logger.debug("Could not retrieve server status details", e);
            details.put("serverStatusInfo", "Not available");
        }
    }
    
    /**
     * Test if we can perform basic CRUD operations
     */
    public boolean testCrudOperations() {
        try {
            String testCollection = "health_check_test";
            
            // Insert a test document
            org.bson.Document testDoc = new org.bson.Document("test", true)
                    .append("timestamp", System.currentTimeMillis());
            
            mongoTemplate.getCollection(testCollection).insertOne(testDoc);
            
            // Read the document back
            org.bson.Document found = mongoTemplate.getCollection(testCollection)
                    .find(new org.bson.Document("test", true))
                    .first();
            
            // Clean up
            mongoTemplate.getCollection(testCollection)
                    .deleteOne(new org.bson.Document("test", true));
            
            return found != null;
            
        } catch (Exception e) {
            logger.error("MongoDB CRUD operations test failed", e);
            return false;
        }
    }
}
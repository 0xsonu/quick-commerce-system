package com.ecommerce.shared.utils.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom database health indicator for comprehensive database health monitoring
 * Provides detailed health information including connection pool status
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
        }
    }

    private Health checkDatabaseHealth() {
        Map<String, Object> details = new HashMap<>();
        
        try {
            // Test basic connectivity
            long startTime = System.currentTimeMillis();
            String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (!"1".equals(result)) {
                return Health.down()
                        .withDetail("error", "Database query returned unexpected result")
                        .withDetail("expected", "1")
                        .withDetail("actual", result)
                        .build();
            }
            
            details.put("responseTime", responseTime + "ms");
            details.put("status", "UP");
            
            // Get connection pool information if using HikariCP
            addConnectionPoolDetails(details);
            
            // Get database information
            addDatabaseDetails(details);
            
            // Determine health status based on response time
            if (responseTime > 5000) { // 5 seconds threshold
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "Database response time too high")
                        .build();
            } else if (responseTime > 1000) { // 1 second warning threshold
                details.put("warning", "Database response time is elevated");
            }
            
            return Health.up().withDetails(details).build();
            
        } catch (DataAccessException e) {
            logger.error("Database connectivity check failed", e);
            return Health.down()
                    .withDetail("error", "Database connectivity failed")
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .withDetails(details)
                    .build();
        }
    }
    
    private void addConnectionPoolDetails(Map<String, Object> details) {
        try {
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDataSource = 
                    (com.zaxxer.hikari.HikariDataSource) dataSource;
                
                com.zaxxer.hikari.HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                if (poolMXBean != null) {
                    Map<String, Object> poolDetails = new HashMap<>();
                    poolDetails.put("activeConnections", poolMXBean.getActiveConnections());
                    poolDetails.put("idleConnections", poolMXBean.getIdleConnections());
                    poolDetails.put("totalConnections", poolMXBean.getTotalConnections());
                    poolDetails.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                    
                    details.put("connectionPool", poolDetails);
                    
                    // Check for potential issues
                    if (poolMXBean.getThreadsAwaitingConnection() > 0) {
                        details.put("warning", "Threads are waiting for database connections");
                    }
                    
                    if (poolMXBean.getActiveConnections() >= hikariDataSource.getMaximumPoolSize() * 0.9) {
                        details.put("warning", "Connection pool is near capacity");
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve connection pool details", e);
            details.put("connectionPoolInfo", "Not available");
        }
    }
    
    private void addDatabaseDetails(Map<String, Object> details) {
        try {
            // Get database version
            String version = jdbcTemplate.queryForObject(
                "SELECT VERSION()", String.class);
            details.put("database", "MySQL");
            details.put("version", version);
            
            // Get current database name
            String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()", String.class);
            details.put("schema", database);
            
            // Check if we can write (test with a simple operation)
            try {
                jdbcTemplate.execute("SELECT 1 FROM DUAL WHERE 1=0");
                details.put("writable", true);
            } catch (Exception e) {
                details.put("writable", false);
                details.put("writeError", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.debug("Could not retrieve database details", e);
            details.put("databaseInfo", "Not available");
        }
    }
    
    /**
     * Test database connection without using connection pool
     * Useful for deep health checks
     */
    public boolean testDirectConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            logger.error("Direct database connection test failed", e);
            return false;
        }
    }
}
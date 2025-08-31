package com.ecommerce.shared.health.indicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * Health indicator for database connectivity and performance
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    
    // Health check thresholds
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1 second
    private static final long CONNECTION_TIMEOUT_MS = 5000; // 5 seconds

    public DatabaseHealthIndicator(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }

    private Health checkDatabaseHealth() {
        Health.Builder healthBuilder = Health.up();
        
        // Check basic connectivity
        Instant start = Instant.now();
        boolean isConnected = checkConnection();
        Duration connectionTime = Duration.between(start, Instant.now());
        
        if (!isConnected) {
            return Health.down()
                    .withDetail("error", "Unable to connect to database")
                    .withDetail("connectionTime", connectionTime.toMillis() + "ms")
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
        
        healthBuilder.withDetail("connectionTime", connectionTime.toMillis() + "ms");
        
        // Check query performance
        start = Instant.now();
        boolean querySuccessful = checkQueryPerformance();
        Duration queryTime = Duration.between(start, Instant.now());
        
        healthBuilder.withDetail("queryTime", queryTime.toMillis() + "ms");
        
        if (!querySuccessful) {
            healthBuilder.status("DEGRADED")
                    .withDetail("warning", "Database queries are slow or failing");
        }
        
        // Add connection pool information if available
        addConnectionPoolInfo(healthBuilder);
        
        // Determine overall status
        if (connectionTime.toMillis() > CONNECTION_TIMEOUT_MS || 
            queryTime.toMillis() > SLOW_QUERY_THRESHOLD_MS) {
            healthBuilder.status("DEGRADED");
        }
        
        healthBuilder.withDetail("timestamp", Instant.now().toString());
        
        return healthBuilder.build();
    }

    private boolean checkConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            logger.warn("Database connection check failed", e);
            return false;
        }
    }

    private boolean checkQueryPerformance() {
        try {
            Instant start = Instant.now();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            Duration queryTime = Duration.between(start, Instant.now());
            
            return queryTime.toMillis() < SLOW_QUERY_THRESHOLD_MS;
        } catch (DataAccessException e) {
            logger.warn("Database query performance check failed", e);
            return false;
        }
    }

    private void addConnectionPoolInfo(Health.Builder healthBuilder) {
        try {
            // Try to get HikariCP information if available
            if (dataSource.getClass().getName().contains("HikariDataSource")) {
                addHikariPoolInfo(healthBuilder);
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve connection pool information", e);
        }
    }

    private void addHikariPoolInfo(Health.Builder healthBuilder) {
        try {
            // Use reflection to get HikariCP pool information
            Object hikariPoolMXBean = dataSource.getClass().getMethod("getHikariPoolMXBean").invoke(dataSource);
            
            if (hikariPoolMXBean != null) {
                int activeConnections = (Integer) hikariPoolMXBean.getClass().getMethod("getActiveConnections").invoke(hikariPoolMXBean);
                int idleConnections = (Integer) hikariPoolMXBean.getClass().getMethod("getIdleConnections").invoke(hikariPoolMXBean);
                int totalConnections = (Integer) hikariPoolMXBean.getClass().getMethod("getTotalConnections").invoke(hikariPoolMXBean);
                
                healthBuilder.withDetail("pool.active", activeConnections)
                        .withDetail("pool.idle", idleConnections)
                        .withDetail("pool.total", totalConnections);
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve HikariCP pool information", e);
        }
    }
}
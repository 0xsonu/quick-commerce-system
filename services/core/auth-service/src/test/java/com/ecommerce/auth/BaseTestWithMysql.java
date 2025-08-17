package com.ecommerce.auth;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

/**
 * Base test class that provides MySQL Testcontainer configuration for all tests.
 * This ensures consistency across all test types (unit, integration, etc.)
 * Uses schema.sql for proper database initialization with exact JPA entity structure.
 * 
 * Features:
 * - Proper schema initialization using schema.sql
 * - Transactional rollback for test data cleanup
 * - Optimized container reuse for performance
 * - Proper database connection configuration
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.test.database.replace=none",
        "spring.sql.init.mode=never", // Let TestContainers handle initialization
        "spring.jpa.defer-datasource-initialization=false",
        "spring.jpa.show-sql=false", // Reduce test noise
        "logging.level.org.hibernate.SQL=WARN",
        "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN"
    }
)
@Testcontainers
@Transactional // Enable transactional rollback for test data cleanup
public abstract class BaseTestWithMysql {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("auth_service_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("schema.sql") // Use schema.sql for proper initialization
            .withReuse(true) // Reuse container for performance
            .withStartupTimeout(Duration.ofMinutes(2)) // Reasonable startup timeout
            .withCommand("--innodb-buffer-pool-size=64M", // Optimize for test environment
                        "--innodb-log-file-size=16M",
                        "--innodb-flush-log-at-trx-commit=2", // Better performance for tests
                        "--sync-binlog=0", // Disable for tests
                        "--max-connections=50") // Limit connections for test environment
            .withTmpFs(Map.of("/var/lib/mysql", "rw,noexec,nosuid,size=512m")); // Use tmpfs for better I/O performance

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database connection properties
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // JPA/Hibernate properties
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); // Validate schema matches entities
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");
        registry.add("spring.jpa.properties.hibernate.connection.characterEncoding", () -> "utf8");
        registry.add("spring.jpa.properties.hibernate.connection.useUnicode", () -> "true");
        
        // Optimized connection pool properties for test performance and reliability
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10"); // Increased for parallel tests
        registry.add("spring.datasource.hikari.minimum-idle", () -> "2"); // Maintain minimum connections
        registry.add("spring.datasource.hikari.connection-timeout", () -> "20000"); // Faster timeout for tests
        registry.add("spring.datasource.hikari.idle-timeout", () -> "300000"); // Shorter idle timeout
        registry.add("spring.datasource.hikari.max-lifetime", () -> "900000"); // Shorter max lifetime
        registry.add("spring.datasource.hikari.validation-timeout", () -> "5000"); // Quick validation
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000"); // Detect leaks quickly
        
        // Test-specific JWT configuration
        registry.add("jwt.secret", () -> "test-secret-key-for-testing-only-must-be-at-least-256-bits");
        registry.add("jwt.access-token.expiration", () -> "3600");
        registry.add("jwt.refresh-token.expiration", () -> "86400");
        registry.add("jwt.issuer", () -> "ecommerce-auth-service-test");
        
        // Disable security filters for testing
        registry.add("app.security.jwt.filter.enabled", () -> "false");
    }

    @BeforeAll
    static void setUp() {
        // Ensure container is started before tests with timeout handling
        if (!mysql.isRunning()) {
            try {
                mysql.start();
                // Wait for container to be fully ready
                mysql.waitingFor(org.testcontainers.containers.wait.strategy.Wait
                    .forLogMessage(".*ready for connections.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));
            } catch (Exception e) {
                throw new RuntimeException("Failed to start MySQL container for tests", e);
            }
        }
    }
}
package com.ecommerce.shared.metrics.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseMetricsCollectorTest {

    private MeterRegistry meterRegistry;
    private DatabaseMetricsCollector databaseMetricsCollector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        databaseMetricsCollector = new DatabaseMetricsCollector(meterRegistry);
    }

    @Test
    void shouldRecordQueryTime() {
        // Given
        String queryType = "SELECT";
        String tableName = "users";
        long executionTimeMs = 150L;

        // When
        databaseMetricsCollector.recordQueryTime(queryType, tableName, executionTimeMs);

        // Then
        Timer timer = meterRegistry.find("database.query.duration")
                .tag("query_type", queryType)
                .tag("table", tableName)
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordQueryExecution() {
        // Given
        String queryType = "INSERT";
        String tableName = "orders";
        boolean success = true;

        // When
        databaseMetricsCollector.recordQueryExecution(queryType, tableName, success);

        // Then
        Counter counter = meterRegistry.find("database.query.executions")
                .tag("query_type", queryType)
                .tag("table", tableName)
                .tag("success", String.valueOf(success))
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordConnectionAcquisitionTime() {
        // Given
        long acquisitionTimeMs = 50L;

        // When
        databaseMetricsCollector.recordConnectionAcquisitionTime(acquisitionTimeMs);

        // Then
        Timer timer = meterRegistry.find("database.connection.acquisition.time").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordConnectionTimeout() {
        // When
        databaseMetricsCollector.recordConnectionTimeout();

        // Then
        Counter counter = meterRegistry.find("database.connection.timeouts").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordTransactionTime() {
        // Given
        String transactionType = "READ_WRITE";
        long durationMs = 500L;
        boolean success = true;

        // When
        databaseMetricsCollector.recordTransactionTime(transactionType, durationMs, success);

        // Then
        Timer timer = meterRegistry.find("database.transaction.duration")
                .tag("type", transactionType)
                .tag("success", String.valueOf(success))
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordDeadlock() {
        // Given
        String tableName = "inventory";

        // When
        databaseMetricsCollector.recordDeadlock(tableName);

        // Then
        Counter counter = meterRegistry.find("database.deadlocks")
                .tag("table", tableName)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordRollback() {
        // Given
        String reason = "ValidationException";

        // When
        databaseMetricsCollector.recordRollback(reason);

        // Then
        Counter counter = meterRegistry.find("database.rollbacks")
                .tag("reason", reason)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordSlowQuery() {
        // Given
        String queryType = "SELECT";
        String tableName = "products";
        long executionTimeMs = 2500L;

        // When
        databaseMetricsCollector.recordSlowQuery(queryType, tableName, executionTimeMs);

        // Then
        Counter slowQueryCounter = meterRegistry.find("database.slow.queries")
                .tag("query_type", queryType)
                .tag("table", tableName)
                .counter();
        assertNotNull(slowQueryCounter);
        assertEquals(1, slowQueryCounter.count());

        // Should also record regular query time
        Timer timer = meterRegistry.find("database.query.duration")
                .tag("query_type", queryType)
                .tag("table", tableName)
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordBatchOperation() {
        // Given
        String operation = "INSERT";
        int batchSize = 100;
        long durationMs = 1000L;
        boolean success = true;

        // When
        databaseMetricsCollector.recordBatchOperation(operation, batchSize, durationMs, success);

        // Then
        Timer timer = meterRegistry.find("database.batch.duration")
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        DistributionSummary summary = meterRegistry.find("database.batch.size")
                .tag("operation", operation)
                .summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(batchSize, summary.totalAmount());
    }

    @Test
    void shouldRecordRepositoryMethod() {
        // Given
        String repositoryName = "UserRepository";
        String methodName = "findByEmail";
        long durationMs = 75L;
        boolean success = true;

        // When
        databaseMetricsCollector.recordRepositoryMethod(repositoryName, methodName, durationMs, success);

        // Then
        Timer timer = meterRegistry.find("database.repository.method.duration")
                .tag("repository", repositoryName)
                .tag("method", methodName)
                .tag("success", String.valueOf(success))
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordFailedQueryExecution() {
        // Given
        String queryType = "UPDATE";
        String tableName = "orders";
        boolean success = false;

        // When
        databaseMetricsCollector.recordQueryExecution(queryType, tableName, success);

        // Then
        Counter counter = meterRegistry.find("database.query.executions")
                .tag("query_type", queryType)
                .tag("table", tableName)
                .tag("success", "false")
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordFailedTransaction() {
        // Given
        String transactionType = "READ_WRITE";
        long durationMs = 300L;
        boolean success = false;

        // When
        databaseMetricsCollector.recordTransactionTime(transactionType, durationMs, success);

        // Then
        Timer timer = meterRegistry.find("database.transaction.duration")
                .tag("type", transactionType)
                .tag("success", "false")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }
}
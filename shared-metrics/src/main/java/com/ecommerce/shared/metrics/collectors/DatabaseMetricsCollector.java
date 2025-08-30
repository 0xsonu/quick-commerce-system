package com.ecommerce.shared.metrics.collectors;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Collector for database and connection pool metrics
 */
@Component
public class DatabaseMetricsCollector {

    private final MeterRegistry meterRegistry;
    
    @Autowired(required = false)
    private DataSource dataSource;

    public DatabaseMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerConnectionPoolMetrics() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            if (poolMXBean != null) {
                // Active connections
                meterRegistry.gauge("database.connections.active", poolMXBean, HikariPoolMXBean::getActiveConnections);

                // Idle connections
                meterRegistry.gauge("database.connections.idle", poolMXBean, HikariPoolMXBean::getIdleConnections);

                // Total connections
                meterRegistry.gauge("database.connections.total", poolMXBean, HikariPoolMXBean::getTotalConnections);

                // Threads awaiting connection
                meterRegistry.gauge("database.connections.pending", poolMXBean, HikariPoolMXBean::getThreadsAwaitingConnection);

                // Connection pool configuration
                meterRegistry.gauge("database.connections.max", hikariDataSource, ds -> ds.getMaximumPoolSize());

                meterRegistry.gauge("database.connections.min", hikariDataSource, ds -> ds.getMinimumIdle());
            }
        }
    }

    /**
     * Record database query execution time
     */
    public void recordQueryTime(String queryType, String tableName, long executionTimeMs) {
        Timer.builder("database.query.duration")
                .description("Database query execution time")
                .tag("query_type", queryType) // SELECT, INSERT, UPDATE, DELETE
                .tag("table", tableName)
                .register(meterRegistry)
                .record(executionTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record database query execution
     */
    public void recordQueryExecution(String queryType, String tableName, boolean success) {
        Counter.builder("database.query.executions")
                .description("Database query executions")
                .tag("query_type", queryType)
                .tag("table", tableName)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record database connection acquisition time
     */
    public void recordConnectionAcquisitionTime(long acquisitionTimeMs) {
        Timer.builder("database.connection.acquisition.time")
                .description("Database connection acquisition time")
                .register(meterRegistry)
                .record(acquisitionTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record database connection timeout
     */
    public void recordConnectionTimeout() {
        Counter.builder("database.connection.timeouts")
                .description("Database connection timeouts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record database transaction time
     */
    public void recordTransactionTime(String transactionType, long durationMs, boolean success) {
        Timer.builder("database.transaction.duration")
                .description("Database transaction duration")
                .tag("type", transactionType) // READ_ONLY, READ_WRITE
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record database deadlock
     */
    public void recordDeadlock(String tableName) {
        Counter.builder("database.deadlocks")
                .description("Database deadlocks")
                .tag("table", tableName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record database rollback
     */
    public void recordRollback(String reason) {
        Counter.builder("database.rollbacks")
                .description("Database transaction rollbacks")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record slow query
     */
    public void recordSlowQuery(String queryType, String tableName, long executionTimeMs) {
        Counter.builder("database.slow.queries")
                .description("Slow database queries")
                .tag("query_type", queryType)
                .tag("table", tableName)
                .register(meterRegistry)
                .increment();

        // Also record the execution time
        recordQueryTime(queryType, tableName, executionTimeMs);
    }

    /**
     * Record batch operation metrics
     */
    public void recordBatchOperation(String operation, int batchSize, long durationMs, boolean success) {
        Timer.builder("database.batch.duration")
                .description("Database batch operation duration")
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        DistributionSummary.builder("database.batch.size")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(batchSize);
    }

    /**
     * Record repository method execution
     */
    public void recordRepositoryMethod(String repositoryName, String methodName, long durationMs, boolean success) {
        Timer.builder("database.repository.method.duration")
                .description("Repository method execution time")
                .tag("repository", repositoryName)
                .tag("method", methodName)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
package com.ecommerce.shared.metrics.aspects;

import com.ecommerce.shared.metrics.collectors.DatabaseMetricsCollector;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect for database operation metrics collection
 */
@Aspect
@Component
public class DatabaseMetricsAspect {

    private final DatabaseMetricsCollector databaseMetricsCollector;

    public DatabaseMetricsAspect(DatabaseMetricsCollector databaseMetricsCollector) {
        this.databaseMetricsCollector = databaseMetricsCollector;
    }

    @Around("execution(* com.ecommerce..repository.*Repository.save*(..))")
    public Object recordSaveOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return recordDatabaseOperation(joinPoint, "INSERT", extractTableName(joinPoint));
    }

    @Around("execution(* com.ecommerce..repository.*Repository.find*(..))")
    public Object recordFindOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return recordDatabaseOperation(joinPoint, "SELECT", extractTableName(joinPoint));
    }

    @Around("execution(* com.ecommerce..repository.*Repository.delete*(..))")
    public Object recordDeleteOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return recordDatabaseOperation(joinPoint, "DELETE", extractTableName(joinPoint));
    }

    @Around("execution(* com.ecommerce..repository.*Repository.update*(..))")
    public Object recordUpdateOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return recordDatabaseOperation(joinPoint, "UPDATE", extractTableName(joinPoint));
    }

    @Around("execution(* com.ecommerce..repository.*Repository.count*(..))")
    public Object recordCountOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return recordDatabaseOperation(joinPoint, "COUNT", extractTableName(joinPoint));
    }

    @Around("execution(* com.ecommerce..repository.*Repository.exists*(..))")
    public Object recordExistsOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return recordDatabaseOperation(joinPoint, "EXISTS", extractTableName(joinPoint));
    }

    private Object recordDatabaseOperation(ProceedingJoinPoint joinPoint, String queryType, String tableName) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        Throwable exception = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            exception = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Record query execution
            databaseMetricsCollector.recordQueryExecution(queryType, tableName, success);
            
            // Record query timing
            databaseMetricsCollector.recordQueryTime(queryType, tableName, duration);
            
            // Record repository method timing
            String repositoryName = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            databaseMetricsCollector.recordRepositoryMethod(repositoryName, methodName, duration, success);
            
            // Record slow queries (threshold: 1 second)
            if (duration > 1000) {
                databaseMetricsCollector.recordSlowQuery(queryType, tableName, duration);
            }
        }
    }

    private String extractTableName(ProceedingJoinPoint joinPoint) {
        String repositoryName = joinPoint.getTarget().getClass().getSimpleName();
        
        // Extract table name from repository name
        // e.g., UserRepository -> user, OrderItemRepository -> order_item
        if (repositoryName.endsWith("Repository")) {
            String entityName = repositoryName.substring(0, repositoryName.length() - 10);
            return camelCaseToSnakeCase(entityName);
        }
        
        return "unknown";
    }

    private String camelCaseToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object recordTransactionalMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String transactionType = "READ_WRITE"; // Default assumption

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            // Record rollback
            databaseMetricsCollector.recordRollback(t.getClass().getSimpleName());
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            databaseMetricsCollector.recordTransactionTime(transactionType, duration, success);
        }
    }
}
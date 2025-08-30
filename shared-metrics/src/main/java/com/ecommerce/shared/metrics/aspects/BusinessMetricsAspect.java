package com.ecommerce.shared.metrics.aspects;

import com.ecommerce.shared.metrics.annotations.BusinessMetric;
import com.ecommerce.shared.metrics.collectors.BusinessMetricsCollector;
import com.ecommerce.shared.utils.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect for business metrics collection
 */
@Aspect
@Component
public class BusinessMetricsAspect {

    private final BusinessMetricsCollector businessMetricsCollector;

    public BusinessMetricsAspect(BusinessMetricsCollector businessMetricsCollector) {
        this.businessMetricsCollector = businessMetricsCollector;
    }

    @Around("@annotation(com.ecommerce.shared.metrics.annotations.BusinessMetric)")
    public Object recordBusinessMetric(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        BusinessMetric businessMetric = method.getAnnotation(BusinessMetric.class);

        String tenantId = TenantContext.getTenantId();
        String eventType = businessMetric.value();
        String[] tags = businessMetric.tags();

        long startTime = System.currentTimeMillis();
        boolean success = true;
        Throwable exception = null;

        try {
            Object result = joinPoint.proceed();
            
            // Record the business event
            businessMetricsCollector.recordBusinessEvent(tenantId, eventType, tags);
            
            return result;
        } catch (Throwable t) {
            success = false;
            exception = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Record timing if enabled
            if (businessMetric.recordTiming()) {
                String[] timingTags = new String[tags.length + 2];
                System.arraycopy(tags, 0, timingTags, 0, tags.length);
                timingTags[tags.length] = "success";
                timingTags[tags.length + 1] = String.valueOf(success);
                
                businessMetricsCollector.recordBusinessTimer(tenantId, eventType, duration, timingTags);
            }
        }
    }

    // Specific business operation aspects
    @Around("execution(* com.ecommerce..service.*Service.create*(..))")
    public Object recordCreateOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String tenantId = TenantContext.getTenantId();
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            Object result = joinPoint.proceed();
            businessMetricsCollector.recordBusinessEvent(tenantId, "entity_created", 
                "service", serviceName, "method", methodName);
            return result;
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            businessMetricsCollector.recordBusinessTimer(tenantId, "create_operation", duration,
                "service", serviceName, "method", methodName, "success", String.valueOf(success));
        }
    }

    @Around("execution(* com.ecommerce..service.*Service.update*(..))")
    public Object recordUpdateOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String tenantId = TenantContext.getTenantId();
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            Object result = joinPoint.proceed();
            businessMetricsCollector.recordBusinessEvent(tenantId, "entity_updated", 
                "service", serviceName, "method", methodName);
            return result;
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            businessMetricsCollector.recordBusinessTimer(tenantId, "update_operation", duration,
                "service", serviceName, "method", methodName, "success", String.valueOf(success));
        }
    }

    @Around("execution(* com.ecommerce..service.*Service.delete*(..))")
    public Object recordDeleteOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String tenantId = TenantContext.getTenantId();
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            Object result = joinPoint.proceed();
            businessMetricsCollector.recordBusinessEvent(tenantId, "entity_deleted", 
                "service", serviceName, "method", methodName);
            return result;
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            businessMetricsCollector.recordBusinessTimer(tenantId, "delete_operation", duration,
                "service", serviceName, "method", methodName, "success", String.valueOf(success));
        }
    }

    @Around("execution(* com.ecommerce..service.*Service.find*(..))")
    public Object recordFindOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String tenantId = TenantContext.getTenantId();
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            Object result = joinPoint.proceed();
            businessMetricsCollector.recordBusinessEvent(tenantId, "entity_queried", 
                "service", serviceName, "method", methodName);
            return result;
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            businessMetricsCollector.recordBusinessTimer(tenantId, "find_operation", duration,
                "service", serviceName, "method", methodName, "success", String.valueOf(success));
        }
    }
}
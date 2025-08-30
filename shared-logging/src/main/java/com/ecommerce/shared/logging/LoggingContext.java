package com.ecommerce.shared.logging;

import org.slf4j.MDC;

/**
 * Utility class for managing logging context with correlation ID, tenant ID, and user ID
 */
public class LoggingContext {
    
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String TENANT_ID_KEY = "tenantId";
    public static final String USER_ID_KEY = "userId";
    public static final String REQUEST_URI_KEY = "requestUri";
    public static final String REQUEST_METHOD_KEY = "requestMethod";
    public static final String REQUEST_START_TIME_KEY = "requestStartTime";
    public static final String SERVICE_NAME_KEY = "serviceName";
    
    /**
     * Set correlation ID in MDC
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }
    
    /**
     * Get correlation ID from MDC
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }
    
    /**
     * Set tenant ID in MDC
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null) {
            MDC.put(TENANT_ID_KEY, tenantId);
        }
    }
    
    /**
     * Get tenant ID from MDC
     */
    public static String getTenantId() {
        return MDC.get(TENANT_ID_KEY);
    }
    
    /**
     * Set user ID in MDC
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId);
        }
    }
    
    /**
     * Get user ID from MDC
     */
    public static String getUserId() {
        return MDC.get(USER_ID_KEY);
    }
    
    /**
     * Set request URI in MDC
     */
    public static void setRequestUri(String requestUri) {
        if (requestUri != null) {
            MDC.put(REQUEST_URI_KEY, requestUri);
        }
    }
    
    /**
     * Set request method in MDC
     */
    public static void setRequestMethod(String requestMethod) {
        if (requestMethod != null) {
            MDC.put(REQUEST_METHOD_KEY, requestMethod);
        }
    }
    
    /**
     * Set request start time in MDC
     */
    public static void setRequestStartTime(long startTime) {
        MDC.put(REQUEST_START_TIME_KEY, String.valueOf(startTime));
    }
    
    /**
     * Get request start time from MDC
     */
    public static Long getRequestStartTime() {
        String startTime = MDC.get(REQUEST_START_TIME_KEY);
        return startTime != null ? Long.valueOf(startTime) : null;
    }
    
    /**
     * Set service name in MDC
     */
    public static void setServiceName(String serviceName) {
        if (serviceName != null) {
            MDC.put(SERVICE_NAME_KEY, serviceName);
        }
    }
    
    /**
     * Clear all logging context
     */
    public static void clear() {
        MDC.clear();
    }
    
    /**
     * Clear specific keys from logging context
     */
    public static void clearRequestContext() {
        MDC.remove(REQUEST_URI_KEY);
        MDC.remove(REQUEST_METHOD_KEY);
        MDC.remove(REQUEST_START_TIME_KEY);
    }
    
    /**
     * Initialize logging context with correlation ID if not present
     */
    public static String ensureCorrelationId() {
        String correlationId = getCorrelationId();
        if (correlationId == null) {
            correlationId = CorrelationIdGenerator.generate();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }
}
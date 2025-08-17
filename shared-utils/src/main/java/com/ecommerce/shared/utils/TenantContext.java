package com.ecommerce.shared.utils;

/**
 * Thread-local context holder for tenant information
 */
public class TenantContext {
    
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    public static String getCorrelationId() {
        return CORRELATION_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        CORRELATION_ID.remove();
    }

    public static boolean hasTenantId() {
        return TENANT_ID.get() != null;
    }

    public static boolean hasUserId() {
        return USER_ID.get() != null;
    }

    public static boolean hasCorrelationId() {
        return CORRELATION_ID.get() != null;
    }
}
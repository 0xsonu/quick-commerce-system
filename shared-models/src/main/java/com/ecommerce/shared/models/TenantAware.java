package com.ecommerce.shared.models;

/**
 * Interface for entities that are tenant-aware
 */
public interface TenantAware {
    String getTenantId();
    void setTenantId(String tenantId);
}
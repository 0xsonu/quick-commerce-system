package com.ecommerce.shared.security;

import com.ecommerce.shared.models.TenantAware;
import com.ecommerce.shared.utils.TenantContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.util.StringUtils;

/**
 * JPA Entity Listener for automatic tenant ID setting
 */
public class TenantAwareEntityListener {

    @PrePersist
    public void setTenantIdOnCreate(Object entity) {
        if (entity instanceof TenantAware) {
            TenantAware tenantAware = (TenantAware) entity;
            
            // Auto-set tenant ID from context if not already set
            if (!StringUtils.hasText(tenantAware.getTenantId()) && TenantContext.hasTenantId()) {
                tenantAware.setTenantId(TenantContext.getTenantId());
            }
        }
    }

    @PreUpdate
    public void validateTenantIdOnUpdate(Object entity) {
        if (entity instanceof TenantAware) {
            TenantAware tenantAware = (TenantAware) entity;
            
            // Ensure tenant ID is not changed during update
            if (StringUtils.hasText(tenantAware.getTenantId()) && TenantContext.hasTenantId()) {
                String currentTenantId = TenantContext.getTenantId();
                if (!currentTenantId.equals(tenantAware.getTenantId())) {
                    throw new IllegalStateException("Cannot modify entity from different tenant");
                }
            }
        }
    }
}
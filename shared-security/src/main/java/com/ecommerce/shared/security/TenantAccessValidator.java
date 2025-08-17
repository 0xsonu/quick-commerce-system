package com.ecommerce.shared.security;

import com.ecommerce.shared.models.TenantAware;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shared.utils.exception.TenantAccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validator for tenant access control
 */
@Component
public class TenantAccessValidator {

    /**
     * Validate that the entity belongs to the current tenant
     */
    public void validateTenantAccess(TenantAware entity) {
        if (entity == null) {
            return;
        }

        String currentTenantId = TenantContext.getTenantId();
        String entityTenantId = entity.getTenantId();

        if (!StringUtils.hasText(currentTenantId)) {
            throw new TenantAccessDeniedException("No tenant context available");
        }

        if (!StringUtils.hasText(entityTenantId)) {
            throw new TenantAccessDeniedException("Entity has no tenant ID");
        }

        if (!currentTenantId.equals(entityTenantId)) {
            throw new TenantAccessDeniedException("Access denied: entity belongs to different tenant");
        }
    }

    /**
     * Validate tenant ID matches current context
     */
    public void validateTenantId(String tenantId) {
        String currentTenantId = TenantContext.getTenantId();

        if (!StringUtils.hasText(currentTenantId)) {
            throw new TenantAccessDeniedException("No tenant context available");
        }

        if (!StringUtils.hasText(tenantId)) {
            throw new TenantAccessDeniedException("Tenant ID is required");
        }

        if (!currentTenantId.equals(tenantId)) {
            throw new TenantAccessDeniedException("Access denied: invalid tenant ID");
        }
    }

    /**
     * Get current tenant ID or throw exception if not available
     */
    public String requireCurrentTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new TenantAccessDeniedException("No tenant context available");
        }
        return tenantId;
    }

    /**
     * Set tenant ID on entity if not already set
     */
    public void ensureTenantId(TenantAware entity) {
        if (entity == null) {
            return;
        }

        if (!StringUtils.hasText(entity.getTenantId())) {
            String currentTenantId = requireCurrentTenantId();
            entity.setTenantId(currentTenantId);
        } else {
            validateTenantAccess(entity);
        }
    }
}
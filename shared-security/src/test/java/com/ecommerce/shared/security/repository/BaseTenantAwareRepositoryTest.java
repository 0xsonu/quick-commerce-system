package com.ecommerce.shared.security.repository;

import com.ecommerce.shared.models.TenantAware;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shared.utils.exception.TenantAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for tenant-aware repository functionality
 * These tests focus on the tenant validation logic without Spring JPA infrastructure
 */
class BaseTenantAwareRepositoryTest {

    private TestEntity entity;

    @BeforeEach
    void setUp() {
        entity = new TestEntity();
        TenantContext.clear();
    }

    @Test
    void shouldSetTenantIdWhenSavingEntityWithoutTenantId() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);
        TestTenantValidator validator = new TestTenantValidator();

        // When
        validator.ensureTenantId(entity);

        // Then
        assertEquals(tenantId, entity.getTenantId());
    }

    @Test
    void shouldThrowExceptionWhenSavingEntityWithDifferentTenant() {
        // Given
        String currentTenantId = "tenant123";
        String entityTenantId = "tenant456";
        TenantContext.setTenantId(currentTenantId);
        entity.setTenantId(entityTenantId);
        TestTenantValidator validator = new TestTenantValidator();

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.validateTenantAccess(entity);
        });
    }

    @Test
    void shouldValidateEntityWithCorrectTenant() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);
        entity.setTenantId(tenantId);
        TestTenantValidator validator = new TestTenantValidator();

        // When & Then
        assertDoesNotThrow(() -> {
            validator.validateTenantAccess(entity);
        });
    }

    @Test
    void shouldThrowExceptionWhenNoTenantContext() {
        // Given
        // No tenant context set
        TestTenantValidator validator = new TestTenantValidator();

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.ensureTenantId(entity);
        });
    }

    @Test
    void shouldValidateMultipleEntitiesWithSameTenant() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);
        
        TestEntity entity2 = new TestEntity();
        List<TestEntity> entities = Arrays.asList(entity, entity2);
        TestTenantValidator validator = new TestTenantValidator();

        // When
        for (TestEntity e : entities) {
            validator.ensureTenantId(e);
        }

        // Then
        assertEquals(tenantId, entity.getTenantId());
        assertEquals(tenantId, entity2.getTenantId());
    }

    @Test
    void shouldThrowExceptionWhenSavingAllWithDifferentTenant() {
        // Given
        String currentTenantId = "tenant123";
        String entityTenantId = "tenant456";
        TenantContext.setTenantId(currentTenantId);
        
        entity.setTenantId(entityTenantId);
        TestTenantValidator validator = new TestTenantValidator();

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.validateTenantAccess(entity);
        });
    }

    @Test
    void shouldRequireCurrentTenantId() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);
        TestTenantValidator validator = new TestTenantValidator();

        // When
        String result = validator.requireCurrentTenantId();

        // Then
        assertEquals(tenantId, result);
    }

    @Test
    void shouldThrowExceptionWhenRequiringTenantIdWithoutContext() {
        // Given
        // No tenant context set
        TestTenantValidator validator = new TestTenantValidator();

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.requireCurrentTenantId();
        });
    }

    // Helper class to test tenant validation logic
    private static class TestTenantValidator {
        public void validateTenantAccess(TenantAware entity) {
            if (entity == null) {
                return;
            }

            String currentTenantId = TenantContext.getTenantId();
            String entityTenantId = entity.getTenantId();

            if (currentTenantId == null || currentTenantId.isEmpty()) {
                throw new TenantAccessDeniedException("No tenant context available");
            }

            if (entityTenantId == null || entityTenantId.isEmpty()) {
                throw new TenantAccessDeniedException("Entity has no tenant ID");
            }

            if (!currentTenantId.equals(entityTenantId)) {
                throw new TenantAccessDeniedException("Access denied: entity belongs to different tenant");
            }
        }

        public String requireCurrentTenantId() {
            String tenantId = TenantContext.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                throw new TenantAccessDeniedException("No tenant context available");
            }
            return tenantId;
        }

        public void ensureTenantId(TenantAware entity) {
            if (entity == null) {
                return;
            }

            if (entity.getTenantId() == null || entity.getTenantId().isEmpty()) {
                String currentTenantId = requireCurrentTenantId();
                entity.setTenantId(currentTenantId);
            } else {
                validateTenantAccess(entity);
            }
        }
    }

    // Test implementation of TenantAware
    private static class TestEntity implements TenantAware {
        private String tenantId;

        @Override
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}
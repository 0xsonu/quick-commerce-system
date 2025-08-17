package com.ecommerce.shared.security;

import com.ecommerce.shared.models.TenantAware;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shared.utils.exception.TenantAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantAccessValidatorTest {

    private TenantAccessValidator validator;
    private TestTenantAwareEntity entity;

    @BeforeEach
    void setUp() {
        validator = new TenantAccessValidator();
        entity = new TestTenantAwareEntity();
        TenantContext.clear();
    }

    @Test
    void shouldValidateValidTenantAccess() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);
        entity.setTenantId(tenantId);

        // When & Then
        assertDoesNotThrow(() -> validator.validateTenantAccess(entity));
    }

    @Test
    void shouldThrowExceptionForDifferentTenant() {
        // Given
        TenantContext.setTenantId("tenant123");
        entity.setTenantId("tenant456");

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.validateTenantAccess(entity);
        });
    }

    @Test
    void shouldThrowExceptionForNoTenantContext() {
        // Given
        entity.setTenantId("tenant123");
        // No tenant context set

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.validateTenantAccess(entity);
        });
    }

    @Test
    void shouldThrowExceptionForEntityWithoutTenantId() {
        // Given
        TenantContext.setTenantId("tenant123");
        // Entity has no tenant ID

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.validateTenantAccess(entity);
        });
    }

    @Test
    void shouldHandleNullEntity() {
        // Given
        TenantContext.setTenantId("tenant123");

        // When & Then
        assertDoesNotThrow(() -> validator.validateTenantAccess(null));
    }

    @Test
    void shouldValidateMatchingTenantId() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);

        // When & Then
        assertDoesNotThrow(() -> validator.validateTenantId(tenantId));
    }

    @Test
    void shouldThrowExceptionForMismatchedTenantId() {
        // Given
        TenantContext.setTenantId("tenant123");

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.validateTenantId("tenant456");
        });
    }

    @Test
    void shouldRequireCurrentTenantId() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);

        // When
        String result = validator.requireCurrentTenantId();

        // Then
        assertEquals(tenantId, result);
    }

    @Test
    void shouldThrowExceptionWhenRequiringTenantIdWithoutContext() {
        // Given
        // No tenant context set

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.requireCurrentTenantId();
        });
    }

    @Test
    void shouldEnsureTenantIdOnEntityWithoutTenantId() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);

        // When
        validator.ensureTenantId(entity);

        // Then
        assertEquals(tenantId, entity.getTenantId());
    }

    @Test
    void shouldValidateEntityWithCorrectTenantId() {
        // Given
        String tenantId = "tenant123";
        TenantContext.setTenantId(tenantId);
        entity.setTenantId(tenantId);

        // When & Then
        assertDoesNotThrow(() -> validator.ensureTenantId(entity));
        assertEquals(tenantId, entity.getTenantId());
    }

    @Test
    void shouldThrowExceptionWhenEnsuringTenantIdWithWrongTenant() {
        // Given
        TenantContext.setTenantId("tenant123");
        entity.setTenantId("tenant456");

        // When & Then
        assertThrows(TenantAccessDeniedException.class, () -> {
            validator.ensureTenantId(entity);
        });
    }

    @Test
    void shouldHandleNullEntityWhenEnsuring() {
        // Given
        TenantContext.setTenantId("tenant123");

        // When & Then
        assertDoesNotThrow(() -> validator.ensureTenantId(null));
    }

    // Test implementation of TenantAware
    private static class TestTenantAwareEntity implements TenantAware {
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
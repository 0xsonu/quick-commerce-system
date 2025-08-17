package com.ecommerce.shared.security.repository;

import com.ecommerce.shared.models.TenantAware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface with tenant-aware operations
 */
@NoRepositoryBean
public interface TenantAwareRepository<T extends TenantAware, ID> extends JpaRepository<T, ID> {

    /**
     * Find entity by ID within current tenant context
     */
    Optional<T> findByIdAndTenantId(ID id, String tenantId);

    /**
     * Find all entities within current tenant context
     */
    List<T> findByTenantId(String tenantId);

    /**
     * Delete entity by ID within current tenant context
     */
    void deleteByIdAndTenantId(ID id, String tenantId);

    /**
     * Check if entity exists by ID within current tenant context
     */
    boolean existsByIdAndTenantId(ID id, String tenantId);

    /**
     * Count entities within current tenant context
     */
    long countByTenantId(String tenantId);
}
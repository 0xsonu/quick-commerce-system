package com.ecommerce.shared.utils.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * Find entity by tenant ID and entity ID
     */
    Optional<T> findByTenantIdAndId(String tenantId, ID id);

    /**
     * Find all entities by tenant ID
     */
    List<T> findByTenantId(String tenantId);

    /**
     * Find all entities by tenant ID with pagination
     */
    Page<T> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Delete entity by tenant ID and entity ID
     */
    void deleteByTenantIdAndId(String tenantId, ID id);

    /**
     * Check if entity exists by tenant ID and entity ID
     */
    boolean existsByTenantIdAndId(String tenantId, ID id);

    /**
     * Count entities by tenant ID
     */
    long countByTenantId(String tenantId);
}
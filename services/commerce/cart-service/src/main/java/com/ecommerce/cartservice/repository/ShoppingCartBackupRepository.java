package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.entity.ShoppingCartBackup;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MySQL repository for shopping cart backup operations
 */
@Repository
public interface ShoppingCartBackupRepository extends TenantAwareRepository<ShoppingCartBackup, Long> {

    /**
     * Find cart backup by tenant and user ID
     */
    Optional<ShoppingCartBackup> findByTenantIdAndUserId(String tenantId, String userId);

    /**
     * Delete cart backup by tenant and user ID
     */
    void deleteByTenantIdAndUserId(String tenantId, String userId);

    /**
     * Check if cart backup exists by tenant and user ID
     */
    boolean existsByTenantIdAndUserId(String tenantId, String userId);

    /**
     * Find expired cart backups
     */
    List<ShoppingCartBackup> findByUpdatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Find expired cart backups for specific tenant
     */
    List<ShoppingCartBackup> findByTenantIdAndUpdatedAtBefore(String tenantId, LocalDateTime cutoffDate);
}
package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.TenantBrandingConfig;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantBrandingConfigRepository extends TenantAwareRepository<TenantBrandingConfig, Long> {

    @Query("SELECT tbc FROM TenantBrandingConfig tbc WHERE tbc.tenantId = :tenantId AND tbc.isActive = true")
    Optional<TenantBrandingConfig> findActiveBrandingByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT tbc FROM TenantBrandingConfig tbc WHERE tbc.tenantId = :tenantId")
    Optional<TenantBrandingConfig> findByTenantIdOptional(@Param("tenantId") String tenantId);
}
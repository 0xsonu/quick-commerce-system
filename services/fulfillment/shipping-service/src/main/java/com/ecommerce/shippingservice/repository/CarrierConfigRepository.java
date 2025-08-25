package com.ecommerce.shippingservice.repository;

import com.ecommerce.shared.security.repository.TenantAwareRepository;
import com.ecommerce.shippingservice.entity.CarrierConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarrierConfigRepository extends TenantAwareRepository<CarrierConfig, Long> {

    Optional<CarrierConfig> findByCarrierName(String carrierName);

    List<CarrierConfig> findByIsActiveTrue();

    @Query("SELECT cc FROM CarrierConfig cc WHERE cc.tenantId = :tenantId AND cc.carrierName = :carrierName AND cc.isActive = true")
    Optional<CarrierConfig> findActiveCarrierConfig(@Param("tenantId") String tenantId, @Param("carrierName") String carrierName);

    @Query("SELECT cc FROM CarrierConfig cc WHERE cc.tenantId = :tenantId AND cc.isActive = true")
    List<CarrierConfig> findAllActiveCarriers(@Param("tenantId") String tenantId);

    boolean existsByCarrierNameAndIsActiveTrue(String carrierName);
}
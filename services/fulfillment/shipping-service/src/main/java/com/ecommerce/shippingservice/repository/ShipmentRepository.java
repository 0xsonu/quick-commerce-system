package com.ecommerce.shippingservice.repository;

import com.ecommerce.shared.security.repository.TenantAwareRepository;
import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends TenantAwareRepository<Shipment, Long> {

    Optional<Shipment> findByShipmentNumber(String shipmentNumber);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findByOrderId(Long orderId);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    Page<Shipment> findByCarrierName(String carrierName, Pageable pageable);

    @Query("SELECT s FROM Shipment s WHERE s.tenantId = :tenantId AND s.status IN :statuses")
    List<Shipment> findByStatusIn(@Param("tenantId") String tenantId, @Param("statuses") List<ShipmentStatus> statuses);

    @Query("SELECT s FROM Shipment s WHERE s.tenantId = :tenantId AND s.estimatedDeliveryDate BETWEEN :startDate AND :endDate")
    List<Shipment> findByEstimatedDeliveryDateBetween(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT s FROM Shipment s WHERE s.tenantId = :tenantId AND s.status = :status AND s.carrierName = :carrierName")
    List<Shipment> findByStatusAndCarrierName(
        @Param("tenantId") String tenantId,
        @Param("status") ShipmentStatus status, 
        @Param("carrierName") String carrierName
    );

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.tenantId = :tenantId AND s.status = :status")
    long countByStatus(@Param("tenantId") String tenantId, @Param("status") ShipmentStatus status);

    @Query("SELECT s FROM Shipment s WHERE s.tenantId = :tenantId AND s.status IN ('IN_TRANSIT', 'OUT_FOR_DELIVERY') AND s.estimatedDeliveryDate < :date")
    List<Shipment> findOverdueShipments(@Param("tenantId") String tenantId, @Param("date") LocalDate date);

    Page<Shipment> findByStatusInAndTrackingNumberIsNotNull(List<ShipmentStatus> statuses, Pageable pageable);
}
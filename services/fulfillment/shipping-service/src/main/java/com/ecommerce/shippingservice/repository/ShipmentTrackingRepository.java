package com.ecommerce.shippingservice.repository;

import com.ecommerce.shippingservice.entity.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    List<ShipmentTracking> findByShipmentIdOrderByEventTimeDesc(Long shipmentId);

    List<ShipmentTracking> findByShipmentIdAndStatusOrderByEventTimeDesc(Long shipmentId, String status);

    Optional<ShipmentTracking> findByCarrierEventId(String carrierEventId);

    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId ORDER BY st.eventTime DESC")
    List<ShipmentTracking> findLatestTrackingEvents(@Param("shipmentId") Long shipmentId);

    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId AND st.eventTime >= :since ORDER BY st.eventTime DESC")
    List<ShipmentTracking> findTrackingEventsSince(@Param("shipmentId") Long shipmentId, @Param("since") LocalDateTime since);

    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.tenantId = :tenantId AND st.eventTime BETWEEN :startTime AND :endTime")
    List<ShipmentTracking> findTrackingEventsBetween(
        @Param("tenantId") String tenantId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    boolean existsByCarrierEventId(String carrierEventId);
}
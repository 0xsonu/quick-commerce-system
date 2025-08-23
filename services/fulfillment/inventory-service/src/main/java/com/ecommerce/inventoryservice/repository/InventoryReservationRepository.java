package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.shared.utils.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends TenantAwareRepository<InventoryReservation, Long> {

    /**
     * Find reservation by tenant ID and reservation ID with optimistic locking
     */
    @Lock(LockModeType.OPTIMISTIC)
    Optional<InventoryReservation> findByTenantIdAndReservationId(String tenantId, String reservationId);

    /**
     * Find reservation by tenant ID and order ID with optimistic locking
     */
    @Lock(LockModeType.OPTIMISTIC)
    Optional<InventoryReservation> findByTenantIdAndOrderId(String tenantId, String orderId);

    /**
     * Find all reservations for a specific inventory item
     */
    List<InventoryReservation> findByTenantIdAndInventoryItemId(String tenantId, Long inventoryItemId);

    /**
     * Find reservations by tenant ID and status
     */
    Page<InventoryReservation> findByTenantIdAndStatus(String tenantId, 
                                                      InventoryReservation.ReservationStatus status, 
                                                      Pageable pageable);

    /**
     * Find all active reservations for an order
     */
    @Query("SELECT r FROM InventoryReservation r WHERE r.tenantId = :tenantId " +
           "AND r.orderId = :orderId AND r.status = 'ACTIVE'")
    List<InventoryReservation> findActiveReservationsByOrder(@Param("tenantId") String tenantId, 
                                                            @Param("orderId") String orderId);

    /**
     * Find expired reservations that need cleanup
     */
    @Query("SELECT r FROM InventoryReservation r WHERE r.tenantId = :tenantId " +
           "AND r.status = 'ACTIVE' AND r.expiresAt < :currentTime")
    List<InventoryReservation> findExpiredReservations(@Param("tenantId") String tenantId, 
                                                      @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find all expired reservations across all tenants for cleanup job
     */
    @Query("SELECT r FROM InventoryReservation r WHERE r.status = 'ACTIVE' AND r.expiresAt < :currentTime")
    List<InventoryReservation> findAllExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Get total reserved quantity for an inventory item
     */
    @Query("SELECT COALESCE(SUM(r.reservedQuantity), 0) FROM InventoryReservation r " +
           "WHERE r.tenantId = :tenantId AND r.inventoryItemId = :inventoryItemId " +
           "AND r.status = 'ACTIVE'")
    Integer getTotalReservedQuantity(@Param("tenantId") String tenantId, 
                                   @Param("inventoryItemId") Long inventoryItemId);

    /**
     * Check if reservation exists for order
     */
    boolean existsByTenantIdAndOrderId(String tenantId, String orderId);

    /**
     * Check if reservation exists by reservation ID
     */
    boolean existsByTenantIdAndReservationId(String tenantId, String reservationId);

    /**
     * Update reservation status in bulk
     */
    @Modifying
    @Query("UPDATE InventoryReservation r SET r.status = :newStatus " +
           "WHERE r.tenantId = :tenantId AND r.status = :currentStatus " +
           "AND r.expiresAt < :currentTime")
    int updateExpiredReservations(@Param("tenantId") String tenantId,
                                 @Param("currentStatus") InventoryReservation.ReservationStatus currentStatus,
                                 @Param("newStatus") InventoryReservation.ReservationStatus newStatus,
                                 @Param("currentTime") LocalDateTime currentTime);

    /**
     * Count reservations by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, InventoryReservation.ReservationStatus status);

    /**
     * Find reservations expiring within a time window
     */
    @Query("SELECT r FROM InventoryReservation r WHERE r.tenantId = :tenantId " +
           "AND r.status = 'ACTIVE' " +
           "AND r.expiresAt BETWEEN :startTime AND :endTime " +
           "ORDER BY r.expiresAt ASC")
    List<InventoryReservation> findReservationsExpiringBetween(@Param("tenantId") String tenantId,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime);

    /**
     * Get reservation statistics for a tenant
     */
    @Query("SELECT r.status, COUNT(r), SUM(r.reservedQuantity) " +
           "FROM InventoryReservation r WHERE r.tenantId = :tenantId " +
           "GROUP BY r.status")
    List<Object[]> getReservationStatistics(@Param("tenantId") String tenantId);
}
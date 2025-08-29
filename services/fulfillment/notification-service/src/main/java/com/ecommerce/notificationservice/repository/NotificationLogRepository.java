package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.NotificationLog;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends TenantAwareRepository<NotificationLog, Long> {

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.tenantId = :tenantId AND nl.userId = :userId " +
           "ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findByTenantIdAndUserId(
            @Param("tenantId") String tenantId,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.status = :status " +
           "AND nl.retryCount < :maxRetries AND nl.createdAt > :cutoffTime")
    List<NotificationLog> findFailedNotificationsForRetry(
            @Param("status") NotificationStatus status,
            @Param("maxRetries") Integer maxRetries,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.tenantId = :tenantId " +
           "AND nl.status = :status AND nl.createdAt >= :fromDate")
    Long countByTenantIdAndStatusAndCreatedAtAfter(
            @Param("tenantId") String tenantId,
            @Param("status") NotificationStatus status,
            @Param("fromDate") LocalDateTime fromDate);
}
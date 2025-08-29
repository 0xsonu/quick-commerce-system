package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationPreference;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends TenantAwareRepository<NotificationPreference, Long> {

    @Query("SELECT np FROM NotificationPreference np WHERE np.tenantId = :tenantId " +
           "AND np.userId = :userId AND np.notificationType = :notificationType " +
           "AND np.channel = :channel")
    Optional<NotificationPreference> findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            @Param("tenantId") String tenantId,
            @Param("userId") Long userId,
            @Param("notificationType") NotificationType notificationType,
            @Param("channel") NotificationChannel channel);

    @Query("SELECT np FROM NotificationPreference np WHERE np.tenantId = :tenantId " +
           "AND np.userId = :userId")
    List<NotificationPreference> findByTenantIdAndUserId(
            @Param("tenantId") String tenantId,
            @Param("userId") Long userId);

    @Query("SELECT np FROM NotificationPreference np WHERE np.tenantId = :tenantId " +
           "AND np.userId = :userId AND np.notificationType = :notificationType " +
           "AND np.isEnabled = true")
    List<NotificationPreference> findEnabledPreferencesByUserAndType(
            @Param("tenantId") String tenantId,
            @Param("userId") Long userId,
            @Param("notificationType") NotificationType notificationType);
}
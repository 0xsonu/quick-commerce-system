package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationTemplate;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends TenantAwareRepository<NotificationTemplate, Long> {

    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.tenantId = :tenantId " +
           "AND nt.templateKey = :templateKey AND nt.channel = :channel AND nt.isActive = true")
    Optional<NotificationTemplate> findByTenantIdAndTemplateKeyAndChannel(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey,
            @Param("channel") NotificationChannel channel);

    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.tenantId = :tenantId AND nt.isActive = true")
    List<NotificationTemplate> findActiveTemplatesByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.tenantId = :tenantId " +
           "AND nt.templateKey = :templateKey AND nt.isActive = true")
    List<NotificationTemplate> findByTenantIdAndTemplateKey(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey);
}
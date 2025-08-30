package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationTemplateVersion;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateVersionRepository extends TenantAwareRepository<NotificationTemplateVersion, Long> {

    @Query("SELECT ntv FROM NotificationTemplateVersion ntv WHERE ntv.tenantId = :tenantId " +
           "AND ntv.templateKey = :templateKey AND ntv.channel = :channel AND ntv.isActive = true")
    Optional<NotificationTemplateVersion> findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey,
            @Param("channel") NotificationChannel channel);

    @Query("SELECT ntv FROM NotificationTemplateVersion ntv WHERE ntv.tenantId = :tenantId " +
           "AND ntv.templateKey = :templateKey AND ntv.channel = :channel " +
           "ORDER BY ntv.versionNumber DESC")
    List<NotificationTemplateVersion> findAllVersionsByTenantIdAndTemplateKeyAndChannel(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey,
            @Param("channel") NotificationChannel channel);

    @Query("SELECT MAX(ntv.versionNumber) FROM NotificationTemplateVersion ntv WHERE ntv.tenantId = :tenantId " +
           "AND ntv.templateKey = :templateKey AND ntv.channel = :channel")
    Optional<Integer> findMaxVersionNumber(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey,
            @Param("channel") NotificationChannel channel);

    @Query("SELECT ntv FROM NotificationTemplateVersion ntv WHERE ntv.tenantId = :tenantId " +
           "AND ntv.templateKey = :templateKey AND ntv.channel = :channel " +
           "AND ntv.versionNumber = :versionNumber")
    Optional<NotificationTemplateVersion> findByTenantIdAndTemplateKeyAndChannelAndVersion(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey,
            @Param("channel") NotificationChannel channel,
            @Param("versionNumber") Integer versionNumber);

    @Query("SELECT ntv FROM NotificationTemplateVersion ntv WHERE ntv.tenantId = :tenantId " +
           "AND ntv.isPublished = true ORDER BY ntv.publishedAt DESC")
    List<NotificationTemplateVersion> findPublishedVersionsByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT ntv FROM NotificationTemplateVersion ntv WHERE ntv.tenantId = :tenantId " +
           "AND ntv.templateKey = :templateKey AND ntv.isPublished = true " +
           "ORDER BY ntv.publishedAt DESC")
    List<NotificationTemplateVersion> findPublishedVersionsByTenantIdAndTemplateKey(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey);
}
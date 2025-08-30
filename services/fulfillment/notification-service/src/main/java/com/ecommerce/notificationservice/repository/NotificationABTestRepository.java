package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.NotificationABTest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationABTestRepository extends TenantAwareRepository<NotificationABTest, Long> {

    @Query("SELECT nat FROM NotificationABTest nat WHERE nat.tenantId = :tenantId " +
           "AND nat.templateKey = :templateKey AND nat.channel = :channel " +
           "AND nat.isActive = true AND nat.startDate <= :now " +
           "AND (nat.endDate IS NULL OR nat.endDate > :now)")
    Optional<NotificationABTest> findActiveTestByTenantIdAndTemplateKeyAndChannel(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey,
            @Param("channel") NotificationChannel channel,
            @Param("now") LocalDateTime now);

    @Query("SELECT nat FROM NotificationABTest nat WHERE nat.tenantId = :tenantId " +
           "AND nat.isActive = true AND nat.startDate <= :now " +
           "AND (nat.endDate IS NULL OR nat.endDate > :now)")
    List<NotificationABTest> findActiveTestsByTenantId(
            @Param("tenantId") String tenantId,
            @Param("now") LocalDateTime now);

    @Query("SELECT nat FROM NotificationABTest nat WHERE nat.tenantId = :tenantId " +
           "AND nat.templateKey = :templateKey ORDER BY nat.createdAt DESC")
    List<NotificationABTest> findAllTestsByTenantIdAndTemplateKey(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey);

    @Query("SELECT nat FROM NotificationABTest nat WHERE nat.tenantId = :tenantId " +
           "AND nat.testName = :testName")
    Optional<NotificationABTest> findByTenantIdAndTestName(
            @Param("tenantId") String tenantId,
            @Param("testName") String testName);

    @Query("SELECT nat FROM NotificationABTest nat WHERE nat.isActive = true " +
           "AND nat.endDate IS NOT NULL AND nat.endDate <= :now")
    List<NotificationABTest> findExpiredActiveTests(@Param("now") LocalDateTime now);

    @Query("SELECT nat FROM NotificationABTest nat WHERE nat.tenantId = :tenantId " +
           "ORDER BY nat.createdAt DESC")
    List<NotificationABTest> findAllTestsByTenantId(@Param("tenantId") String tenantId);
}
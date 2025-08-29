package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.NotificationLog;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NotificationDeliveryTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDeliveryTrackingService.class);

    private final NotificationLogRepository logRepository;

    @Autowired
    public NotificationDeliveryTrackingService(NotificationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * Get notification delivery status by ID
     */
    public Optional<NotificationDeliveryStatus> getDeliveryStatus(Long notificationId) {
        String tenantId = TenantContext.getTenantId();
        
        return logRepository.findById(notificationId)
            .filter(log -> log.getTenantId().equals(tenantId))
            .map(this::mapToDeliveryStatus);
    }

    /**
     * Get notification delivery history for a user
     */
    public Page<NotificationDeliveryStatus> getUserNotificationHistory(Long userId, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        
        Page<NotificationLog> logs = logRepository.findByTenantIdAndUserId(tenantId, userId, pageable);
        return logs.map(this::mapToDeliveryStatus);
    }

    /**
     * Get delivery statistics for a tenant
     */
    public NotificationDeliveryStats getDeliveryStats(LocalDateTime fromDate, LocalDateTime toDate) {
        String tenantId = TenantContext.getTenantId();
        
        // Get counts by status
        Long sentCount = logRepository.countByTenantIdAndStatusAndCreatedAtAfter(
            tenantId, NotificationStatus.SENT, fromDate);
        Long failedCount = logRepository.countByTenantIdAndStatusAndCreatedAtAfter(
            tenantId, NotificationStatus.FAILED, fromDate);
        Long pendingCount = logRepository.countByTenantIdAndStatusAndCreatedAtAfter(
            tenantId, NotificationStatus.PENDING, fromDate);
        Long retryingCount = logRepository.countByTenantIdAndStatusAndCreatedAtAfter(
            tenantId, NotificationStatus.RETRYING, fromDate);

        Long totalCount = sentCount + failedCount + pendingCount + retryingCount;
        
        double successRate = totalCount > 0 ? (double) sentCount / totalCount * 100 : 0.0;
        double failureRate = totalCount > 0 ? (double) failedCount / totalCount * 100 : 0.0;

        return new NotificationDeliveryStats(
            totalCount, sentCount, failedCount, pendingCount, retryingCount,
            successRate, failureRate, fromDate, toDate
        );
    }

    /**
     * Get failed notifications that need attention
     */
    public List<NotificationDeliveryStatus> getFailedNotifications(int maxRetries, LocalDateTime since) {
        List<NotificationLog> failedLogs = logRepository.findFailedNotificationsForRetry(maxRetries, since);
        
        return failedLogs.stream()
            .map(this::mapToDeliveryStatus)
            .collect(Collectors.toList());
    }

    /**
     * Update delivery status (for external webhook callbacks)
     */
    @Transactional
    public void updateDeliveryStatus(Long notificationId, NotificationStatus status, String statusMessage) {
        String tenantId = TenantContext.getTenantId();
        
        Optional<NotificationLog> logOpt = logRepository.findById(notificationId);
        if (logOpt.isPresent()) {
            NotificationLog log = logOpt.get();
            
            // Verify tenant access
            if (!log.getTenantId().equals(tenantId)) {
                logger.warn("Attempted to update notification from different tenant: notificationId={}, " +
                           "requestTenant={}, actualTenant={}", notificationId, tenantId, log.getTenantId());
                return;
            }
            
            log.setStatus(status);
            if (status == NotificationStatus.SENT) {
                log.setSentAt(LocalDateTime.now());
            }
            if (statusMessage != null) {
                log.setErrorMessage(statusMessage);
            }
            
            logRepository.save(log);
            
            logger.info("Updated notification delivery status: id={}, status={}", notificationId, status);
        } else {
            logger.warn("Attempted to update non-existent notification: id={}", notificationId);
        }
    }

    /**
     * Map NotificationLog to NotificationDeliveryStatus
     */
    private NotificationDeliveryStatus mapToDeliveryStatus(NotificationLog log) {
        return new NotificationDeliveryStatus(
            log.getId(),
            log.getUserId(),
            log.getNotificationType(),
            log.getChannel(),
            log.getRecipient(),
            log.getSubject(),
            log.getStatus(),
            log.getErrorMessage(),
            log.getRetryCount(),
            log.getCreatedAt(),
            log.getSentAt(),
            log.getUpdatedAt()
        );
    }

    /**
     * Notification delivery status DTO
     */
    public static class NotificationDeliveryStatus {
        private Long id;
        private Long userId;
        private com.ecommerce.notificationservice.entity.NotificationType notificationType;
        private com.ecommerce.notificationservice.entity.NotificationChannel channel;
        private String recipient;
        private String subject;
        private NotificationStatus status;
        private String errorMessage;
        private Integer retryCount;
        private LocalDateTime createdAt;
        private LocalDateTime sentAt;
        private LocalDateTime updatedAt;

        public NotificationDeliveryStatus(Long id, Long userId, 
                                        com.ecommerce.notificationservice.entity.NotificationType notificationType,
                                        com.ecommerce.notificationservice.entity.NotificationChannel channel,
                                        String recipient, String subject, NotificationStatus status,
                                        String errorMessage, Integer retryCount, LocalDateTime createdAt,
                                        LocalDateTime sentAt, LocalDateTime updatedAt) {
            this.id = id;
            this.userId = userId;
            this.notificationType = notificationType;
            this.channel = channel;
            this.recipient = recipient;
            this.subject = subject;
            this.status = status;
            this.errorMessage = errorMessage;
            this.retryCount = retryCount;
            this.createdAt = createdAt;
            this.sentAt = sentAt;
            this.updatedAt = updatedAt;
        }

        // Getters
        public Long getId() { return id; }
        public Long getUserId() { return userId; }
        public com.ecommerce.notificationservice.entity.NotificationType getNotificationType() { return notificationType; }
        public com.ecommerce.notificationservice.entity.NotificationChannel getChannel() { return channel; }
        public String getRecipient() { return recipient; }
        public String getSubject() { return subject; }
        public NotificationStatus getStatus() { return status; }
        public String getErrorMessage() { return errorMessage; }
        public Integer getRetryCount() { return retryCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getSentAt() { return sentAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    /**
     * Notification delivery statistics DTO
     */
    public static class NotificationDeliveryStats {
        private Long totalNotifications;
        private Long sentNotifications;
        private Long failedNotifications;
        private Long pendingNotifications;
        private Long retryingNotifications;
        private Double successRate;
        private Double failureRate;
        private LocalDateTime fromDate;
        private LocalDateTime toDate;

        public NotificationDeliveryStats(Long totalNotifications, Long sentNotifications,
                                       Long failedNotifications, Long pendingNotifications,
                                       Long retryingNotifications, Double successRate,
                                       Double failureRate, LocalDateTime fromDate, LocalDateTime toDate) {
            this.totalNotifications = totalNotifications;
            this.sentNotifications = sentNotifications;
            this.failedNotifications = failedNotifications;
            this.pendingNotifications = pendingNotifications;
            this.retryingNotifications = retryingNotifications;
            this.successRate = successRate;
            this.failureRate = failureRate;
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        // Getters
        public Long getTotalNotifications() { return totalNotifications; }
        public Long getSentNotifications() { return sentNotifications; }
        public Long getFailedNotifications() { return failedNotifications; }
        public Long getPendingNotifications() { return pendingNotifications; }
        public Long getRetryingNotifications() { return retryingNotifications; }
        public Double getSuccessRate() { return successRate; }
        public Double getFailureRate() { return failureRate; }
        public LocalDateTime getFromDate() { return fromDate; }
        public LocalDateTime getToDate() { return toDate; }
    }
}
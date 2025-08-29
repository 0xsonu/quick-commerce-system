package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.NotificationLog;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.notificationservice.service.provider.NotificationException;
import com.ecommerce.notificationservice.service.provider.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationRetryService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRetryService.class);

    @Value("${notification.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${notification.retry.initial-delay:1000}")
    private long initialDelay;

    @Value("${notification.retry.max-delay:30000}")
    private long maxDelay;

    @Value("${notification.retry.multiplier:2.0}")
    private double backoffMultiplier;

    private final NotificationLogRepository logRepository;
    private final Map<String, NotificationProvider> providers;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public NotificationRetryService(NotificationLogRepository logRepository,
                                  List<NotificationProvider> providerList,
                                  KafkaTemplate<String, Object> kafkaTemplate) {
        this.logRepository = logRepository;
        this.providers = providerList.stream()
            .collect(java.util.stream.Collectors.toMap(
                provider -> provider.getChannel().name(), 
                provider -> provider));
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Retry failed notifications with exponential backoff (public method)
     */
    @Async
    public void retryNotification(Long notificationId) {
        try {
            retryNotificationInternal(notificationId);
        } catch (Exception e) {
            logger.error("Failed to retry notification: id={}", notificationId, e);
        }
    }

    /**
     * Internal retry method with exponential backoff
     */
    @Retryable(
        retryFor = {NotificationException.class, RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 30000)
    )
    private void retryNotificationInternal(Long notificationId) throws NotificationException {
        NotificationLog log = logRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (log.getRetryCount() >= maxRetryAttempts) {
            logger.warn("Max retry attempts reached for notification: {}", notificationId);
            sendToDeadLetterQueue(log);
            return;
        }

        try {
            log.setStatus(NotificationStatus.RETRYING);
            log.setRetryCount(log.getRetryCount() + 1);
            logRepository.save(log);

            NotificationProvider provider = providers.get(log.getChannel().name());
            if (provider == null || !provider.isAvailable()) {
                throw new NotificationException("Provider not available for channel: " + log.getChannel());
            }

            boolean sent = provider.sendNotification(log.getRecipient(), log.getSubject(), log.getContent());
            
            if (sent) {
                log.setStatus(NotificationStatus.SENT);
                log.setSentAt(LocalDateTime.now());
                log.setErrorMessage(null);
                logger.info("Notification retry successful: id={}, attempt={}", 
                           notificationId, log.getRetryCount());
            } else {
                throw new NotificationException("Provider returned false on retry");
            }

        } catch (Exception e) {
            log.setStatus(NotificationStatus.FAILED);
            log.setErrorMessage("Retry failed: " + e.getMessage());
            logger.error("Notification retry failed: id={}, attempt={}", 
                        notificationId, log.getRetryCount(), e);
            throw e;
        } finally {
            logRepository.save(log);
        }
    }

    /**
     * Recovery method called when all retry attempts are exhausted
     */
    @Recover
    public void recoverFromRetryFailure(Exception ex, Long notificationId) {
        logger.error("All retry attempts exhausted for notification: {}", notificationId, ex);
        
        NotificationLog log = logRepository.findById(notificationId).orElse(null);
        if (log != null) {
            sendToDeadLetterQueue(log);
        }
    }

    /**
     * Send failed notification to dead letter queue
     */
    private void sendToDeadLetterQueue(NotificationLog log) {
        try {
            DeadLetterNotification deadLetter = new DeadLetterNotification(
                log.getId(),
                log.getTenantId(),
                log.getUserId(),
                log.getNotificationType(),
                log.getChannel(),
                log.getRecipient(),
                log.getSubject(),
                log.getContent(),
                log.getRetryCount(),
                log.getErrorMessage(),
                LocalDateTime.now()
            );

            kafkaTemplate.send("notification-dead-letter", deadLetter);
            
            log.setStatus(NotificationStatus.FAILED);
            log.setErrorMessage("Sent to dead letter queue after " + log.getRetryCount() + " attempts");
            logRepository.save(log);
            
            logger.warn("Notification sent to dead letter queue: id={}, retries={}", 
                       log.getId(), log.getRetryCount());
                       
        } catch (Exception e) {
            logger.error("Failed to send notification to dead letter queue: id={}", log.getId(), e);
        }
    }

    /**
     * Scheduled task to retry failed notifications
     */
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void processFailedNotifications() {
        logger.debug("Processing failed notifications for retry");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        List<NotificationLog> failedNotifications = logRepository
            .findFailedNotificationsForRetry(maxRetryAttempts, cutoffTime);

        for (NotificationLog log : failedNotifications) {
            try {
                retryNotification(log.getId());
            } catch (Exception e) {
                logger.error("Error processing failed notification: id={}", log.getId(), e);
            }
        }
        
        if (!failedNotifications.isEmpty()) {
            logger.info("Processed {} failed notifications for retry", failedNotifications.size());
        }
    }

    /**
     * Dead letter notification DTO for Kafka
     */
    public static class DeadLetterNotification {
        private Long originalNotificationId;
        private String tenantId;
        private Long userId;
        private String notificationType;
        private String channel;
        private String recipient;
        private String subject;
        private String content;
        private Integer retryCount;
        private String lastError;
        private LocalDateTime failedAt;

        public DeadLetterNotification() {}

        public DeadLetterNotification(Long originalNotificationId, String tenantId, Long userId,
                                    com.ecommerce.notificationservice.entity.NotificationType notificationType,
                                    com.ecommerce.notificationservice.entity.NotificationChannel channel,
                                    String recipient, String subject, String content,
                                    Integer retryCount, String lastError, LocalDateTime failedAt) {
            this.originalNotificationId = originalNotificationId;
            this.tenantId = tenantId;
            this.userId = userId;
            this.notificationType = notificationType.name();
            this.channel = channel.name();
            this.recipient = recipient;
            this.subject = subject;
            this.content = content;
            this.retryCount = retryCount;
            this.lastError = lastError;
            this.failedAt = failedAt;
        }

        // Getters and setters
        public Long getOriginalNotificationId() { return originalNotificationId; }
        public void setOriginalNotificationId(Long originalNotificationId) { this.originalNotificationId = originalNotificationId; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getNotificationType() { return notificationType; }
        public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
        
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Integer getRetryCount() { return retryCount; }
        public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
        
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
        
        public LocalDateTime getFailedAt() { return failedAt; }
        public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    }
}
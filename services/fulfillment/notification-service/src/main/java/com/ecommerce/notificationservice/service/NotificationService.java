package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.notificationservice.repository.NotificationPreferenceRepository;
import com.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import com.ecommerce.notificationservice.service.provider.NotificationException;
import com.ecommerce.notificationservice.service.provider.NotificationProvider;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationLogRepository logRepository;
    private final NotificationTemplateEngine templateEngine;
    private final Map<NotificationChannel, NotificationProvider> providers;

    @Autowired
    public NotificationService(NotificationTemplateRepository templateRepository,
                             NotificationPreferenceRepository preferenceRepository,
                             NotificationLogRepository logRepository,
                             NotificationTemplateEngine templateEngine,
                             List<NotificationProvider> providerList) {
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.logRepository = logRepository;
        this.templateEngine = templateEngine;
        this.providers = providerList.stream()
            .collect(Collectors.toMap(NotificationProvider::getChannel, provider -> provider));
    }

    /**
     * Send a notification asynchronously
     */
    @Async
    public void sendNotificationAsync(NotificationRequest request) {
        sendNotification(request);
    }

    /**
     * Send a notification synchronously
     */
    public NotificationResponse sendNotification(NotificationRequest request) {
        String tenantId = TenantContext.getTenantId();
        
        // Check user preferences
        if (!isNotificationEnabled(tenantId, request.getUserId(), 
                                  request.getNotificationType(), request.getChannel())) {
            logger.info("Notification disabled by user preference: userId={}, type={}, channel={}", 
                       request.getUserId(), request.getNotificationType(), request.getChannel());
            return createSkippedResponse(request, "Disabled by user preference");
        }

        // Create notification log entry
        NotificationLog log = new NotificationLog(tenantId, request.getUserId(), 
                                                 request.getNotificationType(), 
                                                 request.getChannel(), request.getRecipient());
        
        try {
            // Process template if template key is provided
            String subject = request.getSubject();
            String content = request.getContent();
            
            if (request.getTemplateKey() != null) {
                Optional<NotificationTemplate> templateOpt = templateRepository
                    .findByTenantIdAndTemplateKeyAndChannel(tenantId, request.getTemplateKey(), 
                                                          request.getChannel());
                
                if (templateOpt.isPresent()) {
                    NotificationTemplate template = templateOpt.get();
                    subject = templateEngine.processSubject(template.getSubject(), 
                                                          request.getTemplateVariables());
                    content = templateEngine.processContent(template.getContent(), 
                                                          request.getTemplateVariables());
                } else {
                    logger.warn("Template not found: tenantId={}, templateKey={}, channel={}", 
                               tenantId, request.getTemplateKey(), request.getChannel());
                }
            }

            log.setSubject(subject);
            log.setContent(content);
            log.setMetadata(request.getMetadata());

            // Get provider and send notification
            NotificationProvider provider = providers.get(request.getChannel());
            if (provider == null || !provider.isAvailable()) {
                throw new NotificationException("Provider not available for channel: " + request.getChannel());
            }

            boolean sent = provider.sendNotification(request.getRecipient(), subject, content);
            
            if (sent) {
                log.setStatus(NotificationStatus.SENT);
                log.setSentAt(LocalDateTime.now());
                logger.info("Notification sent successfully: userId={}, type={}, channel={}", 
                           request.getUserId(), request.getNotificationType(), request.getChannel());
            } else {
                log.setStatus(NotificationStatus.FAILED);
                log.setErrorMessage("Provider returned false");
            }

        } catch (NotificationException e) {
            log.setStatus(NotificationStatus.FAILED);
            log.setErrorMessage(e.getMessage());
            logger.error("Failed to send notification: userId={}, type={}, channel={}", 
                        request.getUserId(), request.getNotificationType(), request.getChannel(), e);
        } catch (Exception e) {
            log.setStatus(NotificationStatus.FAILED);
            log.setErrorMessage("Unexpected error: " + e.getMessage());
            logger.error("Unexpected error sending notification: userId={}, type={}, channel={}", 
                        request.getUserId(), request.getNotificationType(), request.getChannel(), e);
        }

        // Save log
        log = logRepository.save(log);
        
        return mapToResponse(log);
    }

    /**
     * Check if notification is enabled for user
     */
    private boolean isNotificationEnabled(String tenantId, Long userId, 
                                        NotificationType notificationType, NotificationChannel channel) {
        Optional<NotificationPreference> preferenceOpt = preferenceRepository
            .findByTenantIdAndUserIdAndNotificationTypeAndChannel(tenantId, userId, notificationType, channel);
        
        // If no preference is set, default to enabled
        return preferenceOpt.map(NotificationPreference::getIsEnabled).orElse(true);
    }

    /**
     * Create a response for skipped notifications
     */
    private NotificationResponse createSkippedResponse(NotificationRequest request, String reason) {
        NotificationResponse response = new NotificationResponse();
        response.setUserId(request.getUserId());
        response.setNotificationType(request.getNotificationType());
        response.setChannel(request.getChannel());
        response.setRecipient(request.getRecipient());
        response.setStatus(NotificationStatus.PENDING); // Use PENDING to indicate it was not processed
        response.setErrorMessage(reason);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    /**
     * Map NotificationLog to NotificationResponse
     */
    private NotificationResponse mapToResponse(NotificationLog log) {
        NotificationResponse response = new NotificationResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setNotificationType(log.getNotificationType());
        response.setChannel(log.getChannel());
        response.setRecipient(log.getRecipient());
        response.setSubject(log.getSubject());
        response.setStatus(log.getStatus());
        response.setErrorMessage(log.getErrorMessage());
        response.setSentAt(log.getSentAt());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
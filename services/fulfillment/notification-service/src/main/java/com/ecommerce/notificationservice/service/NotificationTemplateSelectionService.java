package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationTemplate;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for selecting appropriate notification templates based on tenant, type, and channel
 */
@Service
public class NotificationTemplateSelectionService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationTemplateSelectionService.class);

    private final NotificationTemplateRepository templateRepository;

    @Autowired
    public NotificationTemplateSelectionService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Select the appropriate template key for a notification
     * Uses caching to improve performance for frequently accessed templates
     */
    @Cacheable(value = "notificationTemplates", key = "#tenantId + '_' + #notificationType + '_' + #channel")
    public String selectTemplate(String tenantId, NotificationType notificationType, NotificationChannel channel) {
        logger.debug("Selecting template for tenantId={}, type={}, channel={}", 
                    tenantId, notificationType, channel);

        // First, try to find tenant-specific template
        String templateKey = findTenantSpecificTemplate(tenantId, notificationType, channel);
        
        if (templateKey != null) {
            logger.debug("Found tenant-specific template: {}", templateKey);
            return templateKey;
        }

        // Fall back to default template
        templateKey = getDefaultTemplateKey(notificationType, channel);
        logger.debug("Using default template: {}", templateKey);
        
        return templateKey;
    }

    /**
     * Find tenant-specific template
     */
    private String findTenantSpecificTemplate(String tenantId, NotificationType notificationType, 
                                            NotificationChannel channel) {
        // Try exact match first
        String exactTemplateKey = buildTemplateKey(notificationType, channel);
        Optional<NotificationTemplate> template = templateRepository
            .findByTenantIdAndTemplateKeyAndChannel(tenantId, exactTemplateKey, channel);
        
        if (template.isPresent() && template.get().getIsActive()) {
            return exactTemplateKey;
        }

        // Try with tenant-specific prefix
        String tenantTemplateKey = buildTenantTemplateKey(tenantId, notificationType, channel);
        template = templateRepository
            .findByTenantIdAndTemplateKeyAndChannel(tenantId, tenantTemplateKey, channel);
        
        if (template.isPresent() && template.get().getIsActive()) {
            return tenantTemplateKey;
        }

        return null;
    }

    /**
     * Get default template key for notification type and channel
     */
    private String getDefaultTemplateKey(NotificationType notificationType, NotificationChannel channel) {
        return buildTemplateKey(notificationType, channel);
    }

    /**
     * Build standard template key
     */
    private String buildTemplateKey(NotificationType notificationType, NotificationChannel channel) {
        return notificationType.name().toLowerCase() + "_" + channel.name().toLowerCase();
    }

    /**
     * Build tenant-specific template key
     */
    private String buildTenantTemplateKey(String tenantId, NotificationType notificationType, 
                                        NotificationChannel channel) {
        return tenantId + "_" + buildTemplateKey(notificationType, channel);
    }

    /**
     * Check if a template exists and is active
     */
    public boolean templateExists(String tenantId, String templateKey, NotificationChannel channel) {
        Optional<NotificationTemplate> template = templateRepository
            .findByTenantIdAndTemplateKeyAndChannel(tenantId, templateKey, channel);
        
        return template.isPresent() && template.get().getIsActive();
    }

    /**
     * Get template priority order for fallback logic
     * This method defines the order in which templates should be tried
     */
    public String[] getTemplatePriorityOrder(String tenantId, NotificationType notificationType, 
                                           NotificationChannel channel) {
        return new String[] {
            // 1. Tenant-specific custom template
            buildTenantTemplateKey(tenantId, notificationType, channel),
            // 2. Standard template for type and channel
            buildTemplateKey(notificationType, channel),
            // 3. Generic template for channel
            "generic_" + channel.name().toLowerCase(),
            // 4. Fallback template
            "default_" + channel.name().toLowerCase()
        };
    }

    /**
     * Select template with fallback logic
     */
    public String selectTemplateWithFallback(String tenantId, NotificationType notificationType, 
                                           NotificationChannel channel) {
        String[] templateOrder = getTemplatePriorityOrder(tenantId, notificationType, channel);
        
        for (String templateKey : templateOrder) {
            if (templateExists(tenantId, templateKey, channel)) {
                logger.debug("Selected template with fallback: {}", templateKey);
                return templateKey;
            }
        }

        // If no template found, return the default key anyway
        // The notification service will handle missing templates gracefully
        String defaultKey = buildTemplateKey(notificationType, channel);
        logger.warn("No template found for tenantId={}, type={}, channel={}. Using default: {}", 
                   tenantId, notificationType, channel, defaultKey);
        
        return defaultKey;
    }
}
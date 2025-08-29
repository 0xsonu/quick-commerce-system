package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationPreferenceRequest;
import com.ecommerce.notificationservice.entity.NotificationPreference;
import com.ecommerce.notificationservice.repository.NotificationPreferenceRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Autowired
    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Update or create a notification preference
     */
    public NotificationPreference updatePreference(NotificationPreferenceRequest request) {
        String tenantId = TenantContext.getTenantId();
        
        Optional<NotificationPreference> existingOpt = preferenceRepository
            .findByTenantIdAndUserIdAndNotificationTypeAndChannel(
                tenantId, request.getUserId(), request.getNotificationType(), request.getChannel());

        NotificationPreference preference;
        if (existingOpt.isPresent()) {
            preference = existingOpt.get();
            preference.setIsEnabled(request.getIsEnabled());
        } else {
            preference = new NotificationPreference(tenantId, request.getUserId(), 
                                                  request.getNotificationType(), 
                                                  request.getChannel(), request.getIsEnabled());
        }

        return preferenceRepository.save(preference);
    }

    /**
     * Get all preferences for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getUserPreferences(Long userId) {
        String tenantId = TenantContext.getTenantId();
        return preferenceRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    /**
     * Get enabled preferences for a user and notification type
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getEnabledPreferences(Long userId, 
                                                             com.ecommerce.notificationservice.entity.NotificationType notificationType) {
        String tenantId = TenantContext.getTenantId();
        return preferenceRepository.findEnabledPreferencesByUserAndType(tenantId, userId, notificationType);
    }

    /**
     * Delete a preference
     */
    public void deletePreference(Long userId, 
                               com.ecommerce.notificationservice.entity.NotificationType notificationType,
                               com.ecommerce.notificationservice.entity.NotificationChannel channel) {
        String tenantId = TenantContext.getTenantId();
        
        Optional<NotificationPreference> preferenceOpt = preferenceRepository
            .findByTenantIdAndUserIdAndNotificationTypeAndChannel(tenantId, userId, notificationType, channel);
        
        preferenceOpt.ifPresent(preferenceRepository::delete);
    }
}
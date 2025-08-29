package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.client.UserServiceClient;
import com.ecommerce.notificationservice.dto.NotificationPreferenceRequest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationPreference;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.repository.NotificationPreferenceRepository;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.userservice.proto.UserServiceProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserServiceClient userServiceClient;

    @Autowired
    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository,
                                       UserServiceClient userServiceClient) {
        this.preferenceRepository = preferenceRepository;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Update or create a notification preference
     */
    @CacheEvict(value = "userPreferences", key = "#request.userId")
    public NotificationPreference updatePreference(NotificationPreferenceRequest request) {
        String tenantId = TenantContext.getTenantId();
        
        // Validate user exists and is active
        if (!userServiceClient.validateUser(request.getUserId())) {
            throw new IllegalArgumentException("User not found or inactive: " + request.getUserId());
        }
        
        Optional<NotificationPreference> existingOpt = preferenceRepository
            .findByTenantIdAndUserIdAndNotificationTypeAndChannel(
                tenantId, request.getUserId(), request.getNotificationType(), request.getChannel());

        NotificationPreference preference;
        if (existingOpt.isPresent()) {
            preference = existingOpt.get();
            preference.setIsEnabled(request.getIsEnabled());
            logger.info("Updated notification preference: userId={}, type={}, channel={}, enabled={}", 
                       request.getUserId(), request.getNotificationType(), request.getChannel(), request.getIsEnabled());
        } else {
            preference = new NotificationPreference(tenantId, request.getUserId(), 
                                                  request.getNotificationType(), 
                                                  request.getChannel(), request.getIsEnabled());
            logger.info("Created notification preference: userId={}, type={}, channel={}, enabled={}", 
                       request.getUserId(), request.getNotificationType(), request.getChannel(), request.getIsEnabled());
        }

        return preferenceRepository.save(preference);
    }

    /**
     * Get all preferences for a user
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "userPreferences", key = "#userId")
    public List<NotificationPreference> getUserPreferences(Long userId) {
        String tenantId = TenantContext.getTenantId();
        
        // Validate user exists
        if (!userServiceClient.validateUser(userId)) {
            logger.warn("Attempted to get preferences for invalid user: {}", userId);
            return Collections.emptyList();
        }
        
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
    @CacheEvict(value = "userPreferences", key = "#userId")
    public void deletePreference(Long userId, 
                               com.ecommerce.notificationservice.entity.NotificationType notificationType,
                               com.ecommerce.notificationservice.entity.NotificationChannel channel) {
        String tenantId = TenantContext.getTenantId();
        
        Optional<NotificationPreference> preferenceOpt = preferenceRepository
            .findByTenantIdAndUserIdAndNotificationTypeAndChannel(tenantId, userId, notificationType, channel);
        
        if (preferenceOpt.isPresent()) {
            preferenceRepository.delete(preferenceOpt.get());
            logger.info("Deleted notification preference: userId={}, type={}, channel={}", 
                       userId, notificationType, channel);
        }
    }

    /**
     * Check if a specific notification is enabled for a user
     */
    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(Long userId, NotificationType notificationType, NotificationChannel channel) {
        String tenantId = TenantContext.getTenantId();
        
        // First validate user exists and is active
        if (!userServiceClient.validateUser(userId)) {
            logger.warn("Notification check for invalid user: {}", userId);
            return false;
        }
        
        Optional<NotificationPreference> preferenceOpt = preferenceRepository
            .findByTenantIdAndUserIdAndNotificationTypeAndChannel(tenantId, userId, notificationType, channel);
        
        // If no preference is set, default to enabled
        boolean enabled = preferenceOpt.map(NotificationPreference::getIsEnabled).orElse(true);
        
        logger.debug("Notification preference check: userId={}, type={}, channel={}, enabled={}", 
                    userId, notificationType, channel, enabled);
        
        return enabled;
    }

    /**
     * Bulk opt-out user from all notifications
     */
    @CacheEvict(value = "userPreferences", key = "#userId")
    public void optOutAllNotifications(Long userId) {
        String tenantId = TenantContext.getTenantId();
        
        // Validate user exists
        if (!userServiceClient.validateUser(userId)) {
            throw new IllegalArgumentException("User not found or inactive: " + userId);
        }
        
        // Create preferences to disable all notification types and channels
        for (NotificationType type : NotificationType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                Optional<NotificationPreference> existingOpt = preferenceRepository
                    .findByTenantIdAndUserIdAndNotificationTypeAndChannel(tenantId, userId, type, channel);
                
                NotificationPreference preference;
                if (existingOpt.isPresent()) {
                    preference = existingOpt.get();
                    preference.setIsEnabled(false);
                } else {
                    preference = new NotificationPreference(tenantId, userId, type, channel, false);
                }
                
                preferenceRepository.save(preference);
            }
        }
        
        logger.info("User opted out of all notifications: userId={}", userId);
    }

    /**
     * Bulk opt-in user to default notifications
     */
    @CacheEvict(value = "userPreferences", key = "#userId")
    public void optInDefaultNotifications(Long userId) {
        String tenantId = TenantContext.getTenantId();
        
        // Validate user exists
        if (!userServiceClient.validateUser(userId)) {
            throw new IllegalArgumentException("User not found or inactive: " + userId);
        }
        
        // Define default enabled notifications
        Map<NotificationType, Set<NotificationChannel>> defaultPreferences = getDefaultNotificationPreferences();
        
        for (Map.Entry<NotificationType, Set<NotificationChannel>> entry : defaultPreferences.entrySet()) {
            NotificationType type = entry.getKey();
            Set<NotificationChannel> channels = entry.getValue();
            
            for (NotificationChannel channel : channels) {
                Optional<NotificationPreference> existingOpt = preferenceRepository
                    .findByTenantIdAndUserIdAndNotificationTypeAndChannel(tenantId, userId, type, channel);
                
                NotificationPreference preference;
                if (existingOpt.isPresent()) {
                    preference = existingOpt.get();
                    preference.setIsEnabled(true);
                } else {
                    preference = new NotificationPreference(tenantId, userId, type, channel, true);
                }
                
                preferenceRepository.save(preference);
            }
        }
        
        logger.info("User opted in to default notifications: userId={}", userId);
    }

    /**
     * Get user contact information for notifications
     */
    @Transactional(readOnly = true)
    public UserContactInfo getUserContactInfo(Long userId) {
        try {
            UserServiceProtos.User user = userServiceClient.getUser(userId);
            List<UserServiceProtos.UserAddress> addresses = userServiceClient.getUserAddresses(userId);
            
            return new UserContactInfo(
                user.getEmail(),
                user.getPhone(),
                addresses.stream()
                    .filter(addr -> "BILLING".equals(addr.getType()) || "SHIPPING".equals(addr.getType()))
                    .collect(Collectors.toList())
            );
        } catch (Exception e) {
            logger.error("Failed to get user contact info: userId={}", userId, e);
            throw new RuntimeException("Failed to get user contact information", e);
        }
    }

    /**
     * Get default notification preferences for new users
     */
    private Map<NotificationType, Set<NotificationChannel>> getDefaultNotificationPreferences() {
        Map<NotificationType, Set<NotificationChannel>> defaults = new HashMap<>();
        
        // Order notifications - enable email by default
        defaults.put(NotificationType.ORDER_CREATED, Set.of(NotificationChannel.EMAIL));
        defaults.put(NotificationType.ORDER_CONFIRMED, Set.of(NotificationChannel.EMAIL));
        defaults.put(NotificationType.ORDER_SHIPPED, Set.of(NotificationChannel.EMAIL));
        defaults.put(NotificationType.ORDER_DELIVERED, Set.of(NotificationChannel.EMAIL));
        
        // Payment notifications - enable email by default
        defaults.put(NotificationType.PAYMENT_SUCCEEDED, Set.of(NotificationChannel.EMAIL));
        defaults.put(NotificationType.PAYMENT_FAILED, Set.of(NotificationChannel.EMAIL));
        
        // Marketing notifications - disabled by default (opt-in required)
        defaults.put(NotificationType.PROMOTIONAL, Set.of());
        defaults.put(NotificationType.NEWSLETTER, Set.of());
        
        return defaults;
    }

    /**
     * User contact information DTO
     */
    public static class UserContactInfo {
        private String email;
        private String phone;
        private List<UserServiceProtos.UserAddress> addresses;

        public UserContactInfo(String email, String phone, List<UserServiceProtos.UserAddress> addresses) {
            this.email = email;
            this.phone = phone;
            this.addresses = addresses;
        }

        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public List<UserServiceProtos.UserAddress> getAddresses() { return addresses; }
    }
}
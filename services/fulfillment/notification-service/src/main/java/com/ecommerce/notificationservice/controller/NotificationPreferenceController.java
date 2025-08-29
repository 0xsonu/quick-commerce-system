package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationPreferenceRequest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationPreference;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.service.NotificationPreferenceService;
import com.ecommerce.shared.utils.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @Autowired
    public NotificationPreferenceController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    /**
     * Update notification preference
     */
    @PutMapping
    public ResponseEntity<ApiResponse<NotificationPreference>> updatePreference(
            @Valid @RequestBody NotificationPreferenceRequest request) {
        
        NotificationPreference preference = preferenceService.updatePreference(request);
        
        return ResponseEntity.ok(ApiResponse.success(preference, "Preference updated successfully"));
    }

    /**
     * Get user preferences
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationPreference>>> getUserPreferences(
            @PathVariable Long userId) {
        
        List<NotificationPreference> preferences = preferenceService.getUserPreferences(userId);
        
        return ResponseEntity.ok(ApiResponse.success(preferences, "Preferences retrieved successfully"));
    }

    /**
     * Get enabled preferences for user and notification type
     */
    @GetMapping("/user/{userId}/enabled")
    public ResponseEntity<ApiResponse<List<NotificationPreference>>> getEnabledPreferences(
            @PathVariable Long userId,
            @RequestParam NotificationType notificationType) {
        
        List<NotificationPreference> preferences = preferenceService.getEnabledPreferences(userId, notificationType);
        
        return ResponseEntity.ok(ApiResponse.success(preferences, "Enabled preferences retrieved successfully"));
    }

    /**
     * Delete preference
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<String>> deletePreference(
            @PathVariable Long userId,
            @RequestParam NotificationType notificationType,
            @RequestParam NotificationChannel channel) {
        
        preferenceService.deletePreference(userId, notificationType, channel);
        
        return ResponseEntity.ok(ApiResponse.success("Preference deleted successfully", 
                                                    "Preference deleted successfully"));
    }

    /**
     * Check if notification is enabled for user
     */
    @GetMapping("/user/{userId}/check")
    public ResponseEntity<ApiResponse<Boolean>> isNotificationEnabled(
            @PathVariable Long userId,
            @RequestParam NotificationType notificationType,
            @RequestParam NotificationChannel channel) {
        
        boolean enabled = preferenceService.isNotificationEnabled(userId, notificationType, channel);
        
        return ResponseEntity.ok(ApiResponse.success(enabled, "Preference check completed"));
    }

    /**
     * Opt out user from all notifications
     */
    @PostMapping("/user/{userId}/opt-out-all")
    public ResponseEntity<ApiResponse<String>> optOutAllNotifications(@PathVariable Long userId) {
        
        preferenceService.optOutAllNotifications(userId);
        
        return ResponseEntity.ok(ApiResponse.success("User opted out successfully", 
                                                    "User has been opted out of all notifications"));
    }

    /**
     * Opt in user to default notifications
     */
    @PostMapping("/user/{userId}/opt-in-default")
    public ResponseEntity<ApiResponse<String>> optInDefaultNotifications(@PathVariable Long userId) {
        
        preferenceService.optInDefaultNotifications(userId);
        
        return ResponseEntity.ok(ApiResponse.success("User opted in successfully", 
                                                    "User has been opted in to default notifications"));
    }

    /**
     * Get user contact information
     */
    @GetMapping("/user/{userId}/contact-info")
    public ResponseEntity<ApiResponse<NotificationPreferenceService.UserContactInfo>> getUserContactInfo(
            @PathVariable Long userId) {
        
        NotificationPreferenceService.UserContactInfo contactInfo = preferenceService.getUserContactInfo(userId);
        
        return ResponseEntity.ok(ApiResponse.success(contactInfo, "Contact information retrieved successfully"));
    }
}
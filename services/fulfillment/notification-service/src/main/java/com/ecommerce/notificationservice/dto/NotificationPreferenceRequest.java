package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationType;
import jakarta.validation.constraints.NotNull;

public class NotificationPreferenceRequest {

    @NotNull
    private Long userId;

    @NotNull
    private NotificationType notificationType;

    @NotNull
    private NotificationChannel channel;

    @NotNull
    private Boolean isEnabled;

    // Constructors
    public NotificationPreferenceRequest() {}

    public NotificationPreferenceRequest(Long userId, NotificationType notificationType,
                                       NotificationChannel channel, Boolean isEnabled) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
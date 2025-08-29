package com.ecommerce.notificationservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.ecommerce.shared.models.TenantAware;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id", "notification_type", "channel"}))
public class NotificationPreference extends BaseEntity implements TenantAware {

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @NotNull
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    // Constructors
    public NotificationPreference() {}

    public NotificationPreference(String tenantId, Long userId, NotificationType notificationType, 
                                NotificationChannel channel, Boolean isEnabled) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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
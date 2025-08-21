package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.entity.UserPreferences;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Response DTO for user preferences information
 */
public class UserPreferencesResponse {

    private Long id;
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean pushNotifications;
    private Boolean marketingEmails;
    private Boolean orderUpdates;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public UserPreferencesResponse() {}

    public UserPreferencesResponse(UserPreferences preferences) {
        this.id = preferences.getId();
        this.emailNotifications = preferences.getEmailNotifications();
        this.smsNotifications = preferences.getSmsNotifications();
        this.pushNotifications = preferences.getPushNotifications();
        this.marketingEmails = preferences.getMarketingEmails();
        this.orderUpdates = preferences.getOrderUpdates();
        this.createdAt = preferences.getCreatedAt();
        this.updatedAt = preferences.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getSmsNotifications() {
        return smsNotifications;
    }

    public void setSmsNotifications(Boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
    }

    public Boolean getPushNotifications() {
        return pushNotifications;
    }

    public void setPushNotifications(Boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }

    public Boolean getMarketingEmails() {
        return marketingEmails;
    }

    public void setMarketingEmails(Boolean marketingEmails) {
        this.marketingEmails = marketingEmails;
    }

    public Boolean getOrderUpdates() {
        return orderUpdates;
    }

    public void setOrderUpdates(Boolean orderUpdates) {
        this.orderUpdates = orderUpdates;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
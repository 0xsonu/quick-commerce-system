package com.ecommerce.notificationservice.service.provider;

import com.ecommerce.notificationservice.entity.NotificationChannel;

/**
 * Interface for notification providers (Email, SMS, Push, etc.)
 */
public interface NotificationProvider {

    /**
     * Get the channel this provider supports
     * 
     * @return The notification channel
     */
    NotificationChannel getChannel();

    /**
     * Send a notification
     * 
     * @param recipient The recipient (email address, phone number, etc.)
     * @param subject The subject/title of the notification
     * @param content The content/body of the notification
     * @return true if sent successfully, false otherwise
     * @throws NotificationException if sending fails
     */
    boolean sendNotification(String recipient, String subject, String content) throws NotificationException;

    /**
     * Check if this provider is available/configured
     * 
     * @return true if the provider is ready to send notifications
     */
    boolean isAvailable();
}
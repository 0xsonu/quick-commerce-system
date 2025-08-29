package com.ecommerce.notificationservice.service.provider;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SmsNotificationProviderTest {

    private SmsNotificationProvider smsProvider;

    @BeforeEach
    void setUp() {
        smsProvider = new SmsNotificationProvider();
        ReflectionTestUtils.setField(smsProvider, "accountSid", "test-sid");
        ReflectionTestUtils.setField(smsProvider, "authToken", "test-token");
        ReflectionTestUtils.setField(smsProvider, "fromNumber", "+1234567890");
        ReflectionTestUtils.setField(smsProvider, "smsEnabled", true);
        ReflectionTestUtils.setField(smsProvider, "twilioInitialized", false); // Not initialized for testing
    }

    @Test
    void getChannel_ShouldReturnSms() {
        // Act & Assert
        assertEquals(NotificationChannel.SMS, smsProvider.getChannel());
    }

    @Test
    void isAvailable_WhenNotInitialized_ShouldReturnFalse() {
        // Act & Assert
        assertFalse(smsProvider.isAvailable());
    }

    @Test
    void isAvailable_WhenDisabled_ShouldReturnFalse() {
        // Arrange
        ReflectionTestUtils.setField(smsProvider, "smsEnabled", false);
        ReflectionTestUtils.setField(smsProvider, "twilioInitialized", true);

        // Act & Assert
        assertFalse(smsProvider.isAvailable());
    }

    @Test
    void isAvailable_WhenFromNumberEmpty_ShouldReturnFalse() {
        // Arrange
        ReflectionTestUtils.setField(smsProvider, "fromNumber", "");
        ReflectionTestUtils.setField(smsProvider, "twilioInitialized", true);

        // Act & Assert
        assertFalse(smsProvider.isAvailable());
    }

    @Test
    void sendNotification_WhenNotAvailable_ShouldThrowException() {
        // Act & Assert
        NotificationException exception = assertThrows(NotificationException.class, () -> 
            smsProvider.sendNotification("+1987654321", "Subject", "Content"));
        
        assertEquals("SMS provider is not available", exception.getMessage());
    }

    @Test
    void initializeTwilio_WhenConfigurationIncomplete_ShouldNotInitialize() {
        // Arrange
        SmsNotificationProvider provider = new SmsNotificationProvider();
        ReflectionTestUtils.setField(provider, "smsEnabled", true);
        ReflectionTestUtils.setField(provider, "accountSid", "");
        ReflectionTestUtils.setField(provider, "authToken", "test-token");

        // Act
        provider.initializeTwilio();

        // Assert
        Boolean initialized = (Boolean) ReflectionTestUtils.getField(provider, "twilioInitialized");
        assertFalse(initialized);
    }

    @Test
    void initializeTwilio_WhenDisabled_ShouldNotInitialize() {
        // Arrange
        SmsNotificationProvider provider = new SmsNotificationProvider();
        ReflectionTestUtils.setField(provider, "smsEnabled", false);
        ReflectionTestUtils.setField(provider, "accountSid", "test-sid");
        ReflectionTestUtils.setField(provider, "authToken", "test-token");

        // Act
        provider.initializeTwilio();

        // Assert
        Boolean initialized = (Boolean) ReflectionTestUtils.getField(provider, "twilioInitialized");
        assertFalse(initialized);
    }
}
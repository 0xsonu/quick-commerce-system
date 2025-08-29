package com.ecommerce.notificationservice.integration;

import com.ecommerce.notificationservice.client.UserServiceClient;
import com.ecommerce.notificationservice.dto.NotificationPreferenceRequest;
import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.notificationservice.repository.NotificationPreferenceRepository;
import com.ecommerce.notificationservice.service.NotificationPreferenceService;
import com.ecommerce.notificationservice.service.NotificationRetryService;
import com.ecommerce.notificationservice.service.NotificationService;
import com.ecommerce.notificationservice.service.provider.NotificationException;
import com.ecommerce.notificationservice.service.provider.NotificationProvider;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.userservice.proto.UserServiceProtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "notification.retry.max-attempts=2",
    "notification.retry.initial-delay=100",
    "notification.retry.max-delay=1000",
    "notification.retry.multiplier=2.0"
})
@Transactional
class NotificationReliabilityIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationPreferenceService preferenceService;

    @Autowired
    private NotificationRetryService retryService;

    @Autowired
    private NotificationLogRepository logRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean(name = "emailProvider")
    private NotificationProvider emailProvider;

    @MockBean(name = "smsProvider")
    private NotificationProvider smsProvider;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Set up tenant context
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("456");

        // Mock providers
        when(emailProvider.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(smsProvider.getChannel()).thenReturn(NotificationChannel.SMS);
        when(emailProvider.isAvailable()).thenReturn(true);
        when(smsProvider.isAvailable()).thenReturn(true);

        // Mock user service
        when(userServiceClient.validateUser(anyLong())).thenReturn(true);
        when(userServiceClient.getUser(anyLong())).thenReturn(
            UserServiceProtos.User.newBuilder()
                .setId(456L)
                .setEmail("test@example.com")
                .setPhone("+1234567890")
                .setIsActive(true)
                .build()
        );
        when(userServiceClient.getUserAddresses(anyLong())).thenReturn(Arrays.asList());
    }

    @Test
    void testNotificationWithUserPreferences_Enabled() {
        // Given
        Long userId = 456L;
        
        // Set user preference to enabled
        NotificationPreferenceRequest prefRequest = new NotificationPreferenceRequest();
        prefRequest.setUserId(userId);
        prefRequest.setNotificationType(NotificationType.ORDER_CREATED);
        prefRequest.setChannel(NotificationChannel.EMAIL);
        prefRequest.setIsEnabled(true);
        preferenceService.updatePreference(prefRequest);

        // Mock successful email sending
        try {
            when(emailProvider.sendNotification(anyString(), anyString(), anyString())).thenReturn(true);
        } catch (NotificationException e) {
            // This won't happen in the mock, but we need to handle the checked exception
        }

        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Order Created");
        request.setContent("Your order has been created");

        // When
        notificationService.sendNotification(request);

        // Then
        List<NotificationLog> logs = logRepository.findAll();
        assertEquals(1, logs.size());
        assertEquals(NotificationStatus.SENT, logs.get(0).getStatus());
        try {
            verify(emailProvider).sendNotification("test@example.com", "Order Created", "Your order has been created");
        } catch (NotificationException e) {
            // This won't happen in the mock, but we need to handle the checked exception
        }
    }

    @Test
    void testNotificationWithUserPreferences_Disabled() {
        // Given
        Long userId = 456L;
        
        // Set user preference to disabled
        NotificationPreferenceRequest prefRequest = new NotificationPreferenceRequest();
        prefRequest.setUserId(userId);
        prefRequest.setNotificationType(NotificationType.ORDER_CREATED);
        prefRequest.setChannel(NotificationChannel.EMAIL);
        prefRequest.setIsEnabled(false);
        preferenceService.updatePreference(prefRequest);

        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Order Created");
        request.setContent("Your order has been created");

        // When
        notificationService.sendNotification(request);

        // Then
        List<NotificationLog> logs = logRepository.findAll();
        assertTrue(logs.isEmpty()); // No log should be created for disabled notifications
        try {
            verify(emailProvider, never()).sendNotification(anyString(), anyString(), anyString());
        } catch (NotificationException e) {
            // This won't happen in the mock, but we need to handle the checked exception
        }
    }

    @Test
    void testNotificationRetryOnFailure() throws InterruptedException {
        // Given
        Long userId = 456L;
        
        // Mock email provider to fail initially, then succeed
        try {
            when(emailProvider.sendNotification(anyString(), anyString(), anyString()))
                .thenReturn(false) // First attempt fails
                .thenReturn(true); // Retry succeeds
        } catch (NotificationException e) {
            // This won't happen in the mock, but we need to handle the checked exception
        }

        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Order Created");
        request.setContent("Your order has been created");

        // When
        notificationService.sendNotification(request);

        // Wait for async retry to complete
        Thread.sleep(500);

        // Then
        List<NotificationLog> logs = logRepository.findAll();
        assertEquals(1, logs.size());
        
        NotificationLog log = logs.get(0);
        // The log should eventually be marked as sent after retry
        assertTrue(log.getRetryCount() > 0);
        
        // Verify retry was attempted
        try {
            verify(emailProvider, atLeast(2)).sendNotification(anyString(), anyString(), anyString());
        } catch (NotificationException e) {
            // This won't happen in the mock, but we need to handle the checked exception
        }
    }

    @Test
    void testOptOutAllNotifications() {
        // Given
        Long userId = 456L;

        // When
        preferenceService.optOutAllNotifications(userId);

        // Then
        List<NotificationPreference> preferences = preferenceRepository.findByTenantIdAndUserId("tenant123", userId);
        
        // Should have preferences for all notification types and channels, all disabled
        int expectedCount = NotificationType.values().length * NotificationChannel.values().length;
        assertEquals(expectedCount, preferences.size());
        
        preferences.forEach(pref -> {
            assertFalse(pref.getIsEnabled());
            assertEquals(userId, pref.getUserId());
            assertEquals("tenant123", pref.getTenantId());
        });
    }

    @Test
    void testOptInDefaultNotifications() {
        // Given
        Long userId = 456L;

        // When
        preferenceService.optInDefaultNotifications(userId);

        // Then
        List<NotificationPreference> preferences = preferenceRepository.findByTenantIdAndUserId("tenant123", userId);
        
        // Should have preferences for default notification types, all enabled
        assertFalse(preferences.isEmpty());
        
        preferences.forEach(pref -> {
            assertTrue(pref.getIsEnabled());
            assertEquals(userId, pref.getUserId());
            assertEquals("tenant123", pref.getTenantId());
        });

        // Verify that order and payment notifications are enabled by default
        assertTrue(preferences.stream().anyMatch(pref -> 
            pref.getNotificationType() == NotificationType.ORDER_CREATED && 
            pref.getChannel() == NotificationChannel.EMAIL));
        assertTrue(preferences.stream().anyMatch(pref -> 
            pref.getNotificationType() == NotificationType.PAYMENT_SUCCEEDED && 
            pref.getChannel() == NotificationChannel.EMAIL));
    }

    @Test
    void testNotificationDeadLetterQueue() throws InterruptedException {
        // Given
        Long userId = 456L;
        
        // Mock email provider to always fail
        try {
            when(emailProvider.sendNotification(anyString(), anyString(), anyString())).thenReturn(false);
        } catch (NotificationException e) {
            // This won't happen in the mock, but we need to handle the checked exception
        }

        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Order Created");
        request.setContent("Your order has been created");

        // When
        notificationService.sendNotification(request);

        // Wait for retries to complete
        Thread.sleep(2000);

        // Then
        List<NotificationLog> logs = logRepository.findAll();
        assertEquals(1, logs.size());
        
        NotificationLog log = logs.get(0);
        assertEquals(NotificationStatus.FAILED, log.getStatus());
        assertTrue(log.getRetryCount() >= 2); // Should have attempted retries
        
        // Verify dead letter queue was called
        verify(kafkaTemplate, atLeastOnce()).send(eq("notification-dead-letter"), any());
    }

    @Test
    void testUserContactInfoIntegration() {
        // Given
        Long userId = 456L;
        
        List<UserServiceProtos.UserAddress> addresses = Arrays.asList(
            UserServiceProtos.UserAddress.newBuilder()
                .setId(1L)
                .setType("BILLING")
                .setIsDefault(true)
                .build(),
            UserServiceProtos.UserAddress.newBuilder()
                .setId(2L)
                .setType("SHIPPING")
                .setIsDefault(false)
                .build()
        );
        
        when(userServiceClient.getUserAddresses(userId)).thenReturn(addresses);

        // When
        NotificationPreferenceService.UserContactInfo contactInfo = preferenceService.getUserContactInfo(userId);

        // Then
        assertNotNull(contactInfo);
        assertEquals("test@example.com", contactInfo.getEmail());
        assertEquals("+1234567890", contactInfo.getPhone());
        assertEquals(2, contactInfo.getAddresses().size());
        
        verify(userServiceClient).getUser(userId);
        verify(userServiceClient).getUserAddresses(userId);
    }

    @Test
    void testTenantIsolation() {
        // Given
        Long userId = 456L;
        
        // Create preference for tenant123
        TenantContext.setTenantId("tenant123");
        NotificationPreferenceRequest prefRequest = new NotificationPreferenceRequest();
        prefRequest.setUserId(userId);
        prefRequest.setNotificationType(NotificationType.ORDER_CREATED);
        prefRequest.setChannel(NotificationChannel.EMAIL);
        prefRequest.setIsEnabled(false);
        preferenceService.updatePreference(prefRequest);

        // Switch to different tenant
        TenantContext.setTenantId("tenant456");
        
        // When - check if notification is enabled for different tenant
        boolean enabled = preferenceService.isNotificationEnabled(userId, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL);

        // Then - should be enabled (default) since no preference exists for tenant456
        assertTrue(enabled);
        
        // Verify tenant isolation in repository
        List<NotificationPreference> tenant123Prefs = preferenceRepository.findByTenantIdAndUserId("tenant123", userId);
        List<NotificationPreference> tenant456Prefs = preferenceRepository.findByTenantIdAndUserId("tenant456", userId);
        
        assertEquals(1, tenant123Prefs.size());
        assertEquals(0, tenant456Prefs.size());
    }
}
package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.notificationservice.repository.NotificationPreferenceRepository;
import com.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import com.ecommerce.notificationservice.service.provider.NotificationException;
import com.ecommerce.notificationservice.service.provider.NotificationProvider;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private NotificationTemplateEngine templateEngine;

    @Mock
    private NotificationProvider emailProvider;

    @Mock
    private NotificationProvider smsProvider;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(emailProvider.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(smsProvider.getChannel()).thenReturn(NotificationChannel.SMS);
        
        List<NotificationProvider> providers = List.of(emailProvider, smsProvider);
        notificationService = new NotificationService(templateRepository, preferenceRepository, 
                                                    logRepository, templateEngine, providers);
        
        // Set up tenant context
        TenantContext.setTenantId("test-tenant");
    }

    @Test
    void sendNotification_WithTemplate_ShouldProcessTemplateAndSend() throws NotificationException {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setTemplateKey("order-created");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderNumber", "12345");
        request.setTemplateVariables(variables);

        NotificationTemplate template = new NotificationTemplate();
        template.setSubject("Order #{orderNumber} Created");
        template.setContent("Your order #{orderNumber} has been created successfully.");

        NotificationLog savedLog = new NotificationLog();
        savedLog.setId(1L);
        savedLog.setStatus(NotificationStatus.SENT);

        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            anyString(), anyLong(), any(), any())).thenReturn(Optional.empty());
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(
            anyString(), anyString(), any())).thenReturn(Optional.of(template));
        when(templateEngine.processSubject(anyString(), any())).thenReturn("Order #12345 Created");
        when(templateEngine.processContent(anyString(), any())).thenReturn("Your order #12345 has been created successfully.");
        when(emailProvider.isAvailable()).thenReturn(true);
        when(emailProvider.sendNotification(anyString(), anyString(), anyString())).thenReturn(true);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);

        // Act
        NotificationResponse response = notificationService.sendNotification(request);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationStatus.SENT, response.getStatus());
        verify(templateEngine).processSubject("Order #{orderNumber} Created", variables);
        verify(templateEngine).processContent("Your order #{orderNumber} has been created successfully.", variables);
        verify(emailProvider).sendNotification("test@example.com", "Order #12345 Created", 
                                              "Your order #12345 has been created successfully.");
        verify(logRepository).save(any(NotificationLog.class));
    }

    @Test
    void sendNotification_WithoutTemplate_ShouldSendDirectly() throws NotificationException {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setContent("Test Content");

        NotificationLog savedLog = new NotificationLog();
        savedLog.setId(1L);
        savedLog.setStatus(NotificationStatus.SENT);

        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            anyString(), anyLong(), any(), any())).thenReturn(Optional.empty());
        when(emailProvider.isAvailable()).thenReturn(true);
        when(emailProvider.sendNotification(anyString(), anyString(), anyString())).thenReturn(true);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);

        // Act
        NotificationResponse response = notificationService.sendNotification(request);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationStatus.SENT, response.getStatus());
        verify(emailProvider).sendNotification("test@example.com", "Test Subject", "Test Content");
        verify(logRepository).save(any(NotificationLog.class));
    }

    @Test
    void sendNotification_UserPreferenceDisabled_ShouldSkip() {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");

        NotificationPreference preference = new NotificationPreference();
        preference.setIsEnabled(false);

        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            anyString(), anyLong(), any(), any())).thenReturn(Optional.of(preference));

        // Act
        NotificationResponse response = notificationService.sendNotification(request);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationStatus.PENDING, response.getStatus());
        assertEquals("Disabled by user preference", response.getErrorMessage());
        verifyNoInteractions(emailProvider);
        verify(logRepository, never()).save(any(NotificationLog.class));
    }

    @Test
    void sendNotification_ProviderNotAvailable_ShouldFail() {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setContent("Test Content");

        NotificationLog savedLog = new NotificationLog();
        savedLog.setId(1L);
        savedLog.setStatus(NotificationStatus.FAILED);

        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            anyString(), anyLong(), any(), any())).thenReturn(Optional.empty());
        when(emailProvider.isAvailable()).thenReturn(false);
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);

        // Act
        NotificationResponse response = notificationService.sendNotification(request);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("Provider not available"));
        verify(emailProvider).isAvailable();
        verifyNoMoreInteractions(emailProvider);
        verify(logRepository).save(any(NotificationLog.class));
    }

    @Test
    void sendNotification_ProviderThrowsException_ShouldFail() throws NotificationException {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setContent("Test Content");

        NotificationLog savedLog = new NotificationLog();
        savedLog.setId(1L);
        savedLog.setStatus(NotificationStatus.FAILED);

        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            anyString(), anyLong(), any(), any())).thenReturn(Optional.empty());
        when(emailProvider.isAvailable()).thenReturn(true);
        doThrow(new NotificationException("SMTP server unavailable"))
            .when(emailProvider).sendNotification(anyString(), anyString(), anyString());
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);

        // Act
        NotificationResponse response = notificationService.sendNotification(request);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationStatus.FAILED, response.getStatus());
        assertEquals("SMTP server unavailable", response.getErrorMessage());
        verify(logRepository).save(any(NotificationLog.class));
    }
}
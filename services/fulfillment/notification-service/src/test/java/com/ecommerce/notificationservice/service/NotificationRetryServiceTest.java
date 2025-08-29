package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.notificationservice.service.provider.NotificationException;
import com.ecommerce.notificationservice.service.provider.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRetryServiceTest {

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private NotificationProvider emailProvider;

    @Mock
    private NotificationProvider smsProvider;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private NotificationRetryService retryService;

    @BeforeEach
    void setUp() {
        when(emailProvider.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(smsProvider.getChannel()).thenReturn(NotificationChannel.SMS);

        List<NotificationProvider> providers = Arrays.asList(emailProvider, smsProvider);
        retryService = new NotificationRetryService(logRepository, providers, kafkaTemplate);

        // Set test values for retry configuration
        ReflectionTestUtils.setField(retryService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(retryService, "initialDelay", 1000L);
        ReflectionTestUtils.setField(retryService, "maxDelay", 30000L);
        ReflectionTestUtils.setField(retryService, "backoffMultiplier", 2.0);
    }

    @Test
    void testRetryNotification_Success() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);
        log.setRetryCount(1);

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));
        when(emailProvider.isAvailable()).thenReturn(true);
        try {
            when(emailProvider.sendNotification(anyString(), anyString(), anyString())).thenReturn(true);
        } catch (NotificationException e) {
            // This won't happen in the mock, but we need to handle the checked exception
        }
        when(logRepository.save(any(NotificationLog.class))).thenReturn(log);

        // When
        retryService.retryNotification(notificationId);

        // Then
        verify(logRepository, times(2)).save(any(NotificationLog.class));
        
        // Verify the final state of the log
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(logCaptor.capture());
        
        List<NotificationLog> savedLogs = logCaptor.getAllValues();
        // Both captures are the same object, so check the final state
        NotificationLog finalLog = savedLogs.get(1);
        assertEquals(NotificationStatus.SENT, finalLog.getStatus());
        assertEquals(2, finalLog.getRetryCount());
        assertNotNull(finalLog.getSentAt());
    }

    @Test
    void testRetryNotification_FailureAfterMaxRetries() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);
        log.setRetryCount(3); // Already at max retries

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));

        // When
        retryService.retryNotification(notificationId);

        // Then
        verify(kafkaTemplate).send(eq("notification-dead-letter"), any());
        verify(logRepository).save(any(NotificationLog.class));
    }

    @Test
    void testRetryNotification_ProviderNotAvailable() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);
        log.setRetryCount(1);

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));
        when(emailProvider.isAvailable()).thenReturn(false);

        // When
        retryService.retryNotification(notificationId);

        // Then - The public method catches exceptions, so we verify the log was updated
        verify(logRepository, atLeastOnce()).save(any(NotificationLog.class));
    }

    @Test
    void testRetryNotification_NotificationNotFound() {
        // Given
        Long notificationId = 1L;
        when(logRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When - The public method catches exceptions, so no exception is thrown
        retryService.retryNotification(notificationId);

        // Then - Verify that the repository was called
        verify(logRepository).findById(notificationId);
    }

    @Test
    void testProcessFailedNotifications() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        List<NotificationLog> failedNotifications = Arrays.asList(
            createTestNotificationLog(1L),
            createTestNotificationLog(2L)
        );

        when(logRepository.findFailedNotificationsForRetry(eq(3), any(LocalDateTime.class)))
            .thenReturn(failedNotifications);

        // Mock the retry method to avoid actual retry logic in this test
        NotificationRetryService spyService = spy(retryService);
        doNothing().when(spyService).retryNotification(anyLong());

        // When
        spyService.processFailedNotifications();

        // Then
        verify(spyService).retryNotification(1L);
        verify(spyService).retryNotification(2L);
    }

    @Test
    void testSendToDeadLetterQueue() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);
        log.setRetryCount(3);
        log.setErrorMessage("Max retries exceeded");

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));
        when(logRepository.save(any(NotificationLog.class))).thenReturn(log);

        // When
        retryService.retryNotification(notificationId);

        // Then
        ArgumentCaptor<NotificationRetryService.DeadLetterNotification> deadLetterCaptor = 
            ArgumentCaptor.forClass(NotificationRetryService.DeadLetterNotification.class);
        verify(kafkaTemplate).send(eq("notification-dead-letter"), deadLetterCaptor.capture());

        NotificationRetryService.DeadLetterNotification deadLetter = deadLetterCaptor.getValue();
        assertEquals(notificationId, deadLetter.getOriginalNotificationId());
        assertEquals("tenant123", deadLetter.getTenantId());
        assertEquals(Long.valueOf(456L), deadLetter.getUserId());
        assertEquals("ORDER_CREATED", deadLetter.getNotificationType());
        assertEquals("EMAIL", deadLetter.getChannel());
        assertEquals(3, deadLetter.getRetryCount());
    }

    @Test
    void testRecoverFromRetryFailure() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);
        Exception testException = new RuntimeException("Test exception");

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));

        // When
        retryService.recoverFromRetryFailure(testException, notificationId);

        // Then
        verify(kafkaTemplate).send(eq("notification-dead-letter"), any());
    }

    private NotificationLog createTestNotificationLog(Long id) {
        NotificationLog log = new NotificationLog();
        log.setId(id);
        log.setTenantId("tenant123");
        log.setUserId(456L);
        log.setNotificationType(NotificationType.ORDER_CREATED);
        log.setChannel(NotificationChannel.EMAIL);
        log.setRecipient("test@example.com");
        log.setSubject("Test Subject");
        log.setContent("Test Content");
        log.setStatus(NotificationStatus.FAILED);
        log.setRetryCount(0);
        log.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        log.setUpdatedAt(LocalDateTime.now().minusMinutes(5));
        return log;
    }
}
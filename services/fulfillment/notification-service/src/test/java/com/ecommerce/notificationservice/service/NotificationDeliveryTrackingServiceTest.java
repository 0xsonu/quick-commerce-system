package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryTrackingServiceTest {

    @Mock
    private NotificationLogRepository logRepository;

    private NotificationDeliveryTrackingService trackingService;

    @BeforeEach
    void setUp() {
        trackingService = new NotificationDeliveryTrackingService(logRepository);
        
        // Set up tenant context
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("456");
    }

    @Test
    void testGetDeliveryStatus_Found() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));

        // When
        Optional<NotificationDeliveryTrackingService.NotificationDeliveryStatus> result = 
            trackingService.getDeliveryStatus(notificationId);

        // Then
        assertTrue(result.isPresent());
        NotificationDeliveryTrackingService.NotificationDeliveryStatus status = result.get();
        assertEquals(notificationId, status.getId());
        assertEquals(456L, status.getUserId());
        assertEquals(NotificationType.ORDER_CREATED, status.getNotificationType());
        assertEquals(NotificationChannel.EMAIL, status.getChannel());
        assertEquals("test@example.com", status.getRecipient());
        assertEquals(NotificationStatus.SENT, status.getStatus());
    }

    @Test
    void testGetDeliveryStatus_NotFound() {
        // Given
        Long notificationId = 1L;
        when(logRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When
        Optional<NotificationDeliveryTrackingService.NotificationDeliveryStatus> result = 
            trackingService.getDeliveryStatus(notificationId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testGetDeliveryStatus_DifferentTenant() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);
        log.setTenantId("different-tenant");

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));

        // When
        Optional<NotificationDeliveryTrackingService.NotificationDeliveryStatus> result = 
            trackingService.getDeliveryStatus(notificationId);

        // Then
        assertFalse(result.isPresent()); // Should be filtered out due to tenant mismatch
    }

    @Test
    void testGetUserNotificationHistory() {
        // Given
        Long userId = 456L;
        Pageable pageable = PageRequest.of(0, 10);
        
        List<NotificationLog> logs = Arrays.asList(
            createTestNotificationLog(1L),
            createTestNotificationLog(2L)
        );
        Page<NotificationLog> logPage = new PageImpl<>(logs, pageable, 2);

        when(logRepository.findByTenantIdAndUserId("tenant123", userId, pageable))
            .thenReturn(logPage);

        // When
        Page<NotificationDeliveryTrackingService.NotificationDeliveryStatus> result = 
            trackingService.getUserNotificationHistory(userId, pageable);

        // Then
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());
    }

    @Test
    void testGetDeliveryStats() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();

        when(logRepository.countByTenantIdAndStatusAndCreatedAtAfter(
            "tenant123", NotificationStatus.SENT, fromDate)).thenReturn(100L);
        when(logRepository.countByTenantIdAndStatusAndCreatedAtAfter(
            "tenant123", NotificationStatus.FAILED, fromDate)).thenReturn(10L);
        when(logRepository.countByTenantIdAndStatusAndCreatedAtAfter(
            "tenant123", NotificationStatus.PENDING, fromDate)).thenReturn(5L);
        when(logRepository.countByTenantIdAndStatusAndCreatedAtAfter(
            "tenant123", NotificationStatus.RETRYING, fromDate)).thenReturn(2L);

        // When
        NotificationDeliveryTrackingService.NotificationDeliveryStats stats = 
            trackingService.getDeliveryStats(fromDate, toDate);

        // Then
        assertEquals(117L, stats.getTotalNotifications());
        assertEquals(100L, stats.getSentNotifications());
        assertEquals(10L, stats.getFailedNotifications());
        assertEquals(5L, stats.getPendingNotifications());
        assertEquals(2L, stats.getRetryingNotifications());
        assertEquals(85.47, stats.getSuccessRate(), 0.01); // 100/117 * 100
        assertEquals(8.55, stats.getFailureRate(), 0.01); // 10/117 * 100
        assertEquals(fromDate, stats.getFromDate());
        assertEquals(toDate, stats.getToDate());
    }

    @Test
    void testGetDeliveryStats_NoNotifications() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();

        when(logRepository.countByTenantIdAndStatusAndCreatedAtAfter(anyString(), any(), any()))
            .thenReturn(0L);

        // When
        NotificationDeliveryTrackingService.NotificationDeliveryStats stats = 
            trackingService.getDeliveryStats(fromDate, toDate);

        // Then
        assertEquals(0L, stats.getTotalNotifications());
        assertEquals(0.0, stats.getSuccessRate());
        assertEquals(0.0, stats.getFailureRate());
    }

    @Test
    void testGetFailedNotifications() {
        // Given
        int maxRetries = 3;
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        
        List<NotificationLog> failedLogs = Arrays.asList(
            createFailedNotificationLog(1L),
            createFailedNotificationLog(2L)
        );

        when(logRepository.findFailedNotificationsForRetry(maxRetries, since))
            .thenReturn(failedLogs);

        // When
        List<NotificationDeliveryTrackingService.NotificationDeliveryStatus> result = 
            trackingService.getFailedNotifications(maxRetries, since);

        // Then
        assertEquals(2, result.size());
        assertEquals(NotificationStatus.FAILED, result.get(0).getStatus());
        assertEquals(NotificationStatus.FAILED, result.get(1).getStatus());
    }

    @Test
    void testUpdateDeliveryStatus_Success() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);
        log.setStatus(NotificationStatus.PENDING);

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));
        when(logRepository.save(any(NotificationLog.class))).thenReturn(log);

        // When
        trackingService.updateDeliveryStatus(notificationId, NotificationStatus.SENT, "Delivered successfully");

        // Then
        verify(logRepository).save(log);
        assertEquals(NotificationStatus.SENT, log.getStatus());
        assertEquals("Delivered successfully", log.getErrorMessage());
        assertNotNull(log.getSentAt());
    }

    @Test
    void testUpdateDeliveryStatus_DifferentTenant() {
        // Given
        Long notificationId = 1L;
        NotificationLog log = createTestNotificationLog(notificationId);
        log.setTenantId("different-tenant");

        when(logRepository.findById(notificationId)).thenReturn(Optional.of(log));

        // When
        trackingService.updateDeliveryStatus(notificationId, NotificationStatus.SENT, "Delivered successfully");

        // Then
        verify(logRepository, never()).save(any()); // Should not save due to tenant mismatch
    }

    @Test
    void testUpdateDeliveryStatus_NotFound() {
        // Given
        Long notificationId = 1L;
        when(logRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When
        trackingService.updateDeliveryStatus(notificationId, NotificationStatus.SENT, "Delivered successfully");

        // Then
        verify(logRepository, never()).save(any());
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
        log.setStatus(NotificationStatus.SENT);
        log.setRetryCount(0);
        log.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        log.setUpdatedAt(LocalDateTime.now().minusMinutes(5));
        log.setSentAt(LocalDateTime.now().minusMinutes(5));
        return log;
    }

    private NotificationLog createFailedNotificationLog(Long id) {
        NotificationLog log = createTestNotificationLog(id);
        log.setStatus(NotificationStatus.FAILED);
        log.setErrorMessage("Delivery failed");
        log.setSentAt(null);
        log.setRetryCount(2);
        return log;
    }
}
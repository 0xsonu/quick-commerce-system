package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationBatchServiceTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationBatchService notificationBatchService;

    @BeforeEach
    void setUp() {
        // Set test configuration values
        ReflectionTestUtils.setField(notificationBatchService, "batchSize", 5);
        ReflectionTestUtils.setField(notificationBatchService, "emailThrottlePerMinute", 10);
        ReflectionTestUtils.setField(notificationBatchService, "smsThrottlePerMinute", 5);
        ReflectionTestUtils.setField(notificationBatchService, "userThrottlePerHour", 3);
    }

    @Test
    void submitNotifications_ShouldQueueNotifications() {
        // Given
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(1L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL),
            createNotificationRequest(2L, NotificationType.ORDER_CONFIRMED, NotificationChannel.SMS)
        );

        // When
        notificationBatchService.submitNotifications(requests);

        // Then
        Map<String, Object> stats = notificationBatchService.getQueueStatistics();
        assertThat((Integer) stats.get("totalPending")).isEqualTo(2);
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> channelStats = (Map<String, Integer>) stats.get("channelQueues");
        assertThat(channelStats.get("EMAIL")).isEqualTo(1);
        assertThat(channelStats.get("SMS")).isEqualTo(1);
    }

    @Test
    void submitNotifications_ShouldQueueAllNotifications() {
        // Given - Create multiple requests
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(1L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL),
            createNotificationRequest(2L, NotificationType.ORDER_CONFIRMED, NotificationChannel.EMAIL),
            createNotificationRequest(3L, NotificationType.ORDER_SHIPPED, NotificationChannel.EMAIL)
        );

        // When
        notificationBatchService.submitNotifications(requests);

        // Then - All should be queued
        Map<String, Object> stats = notificationBatchService.getQueueStatistics();
        assertThat((Integer) stats.get("totalPending")).isEqualTo(3);
    }

    @Test
    void processBatch_ShouldSendNotifications() {
        // Given
        List<NotificationRequest> batch = List.of(
            createNotificationRequest(1L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL),
            createNotificationRequest(2L, NotificationType.ORDER_CONFIRMED, NotificationChannel.SMS)
        );

        // When
        notificationBatchService.processBatch(batch);

        // Then
        verify(notificationService, times(2)).sendNotificationAsync(any(NotificationRequest.class));
    }

    @Test
    void processBatch_ShouldHandleExceptions() {
        // Given
        List<NotificationRequest> batch = List.of(
            createNotificationRequest(1L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL)
        );
        
        doThrow(new RuntimeException("Send failed")).when(notificationService)
            .sendNotificationAsync(any(NotificationRequest.class));

        // When
        notificationBatchService.processBatch(batch);

        // Then - Should not throw exception
        verify(notificationService).sendNotificationAsync(any(NotificationRequest.class));
    }

    @Test
    void getQueueStatistics_ShouldReturnCorrectStats() {
        // Given
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(1L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL),
            createNotificationRequest(2L, NotificationType.ORDER_CONFIRMED, NotificationChannel.SMS),
            createNotificationRequest(3L, NotificationType.ORDER_SHIPPED, NotificationChannel.EMAIL)
        );
        
        notificationBatchService.submitNotifications(requests);

        // When
        Map<String, Object> stats = notificationBatchService.getQueueStatistics();

        // Then
        assertThat((Integer) stats.get("totalPending")).isEqualTo(3);
        assertThat((Integer) stats.get("batchSize")).isEqualTo(5);
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> channelStats = (Map<String, Integer>) stats.get("channelQueues");
        assertThat(channelStats.get("EMAIL")).isEqualTo(2);
        assertThat(channelStats.get("SMS")).isEqualTo(1);
    }

    @Test
    void forceProcessAll_ShouldProcessAllPendingNotifications() {
        // Given
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(1L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL),
            createNotificationRequest(2L, NotificationType.ORDER_CONFIRMED, NotificationChannel.SMS)
        );
        
        notificationBatchService.submitNotifications(requests);

        // When
        notificationBatchService.forceProcessAll();

        // Then
        verify(notificationService, times(2)).sendNotificationAsync(any(NotificationRequest.class));
        
        Map<String, Object> stats = notificationBatchService.getQueueStatistics();
        assertThat((Integer) stats.get("totalPending")).isEqualTo(0);
    }

    private NotificationRequest createNotificationRequest(Long userId, NotificationType type, NotificationChannel channel) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setNotificationType(type);
        request.setChannel(channel);
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setContent("Test Content");
        return request;
    }
}
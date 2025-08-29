package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.shared.models.events.*;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventNotificationServiceTest {

    @Mock
    private NotificationBatchService notificationBatchService;

    @Mock
    private NotificationTemplateSelectionService templateSelectionService;

    @InjectMocks
    private EventNotificationService eventNotificationService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContext.setTenantId("tenant1");
    }

    @Test
    void processOrderCreatedEvent_ShouldCreateNotificationRequests() {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();
        when(templateSelectionService.selectTemplate(eq("tenant1"), eq(NotificationType.ORDER_CREATED), any()))
            .thenReturn("order_created_email", "order_created_sms");

        // When
        eventNotificationService.processOrderCreatedEvent(event);

        // Then
        ArgumentCaptor<List<NotificationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationBatchService).submitNotifications(captor.capture());
        
        List<NotificationRequest> requests = captor.getValue();
        assertThat(requests).hasSize(2);
        
        // Verify email notification
        NotificationRequest emailRequest = requests.stream()
            .filter(r -> r.getChannel() == NotificationChannel.EMAIL)
            .findFirst().orElseThrow();
        
        assertThat(emailRequest.getUserId()).isEqualTo(456L);
        assertThat(emailRequest.getNotificationType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(emailRequest.getTemplateKey()).isEqualTo("order_created_email");
        assertThat(emailRequest.getTemplateVariables()).containsEntry("orderId", "order123");
        assertThat(emailRequest.getTemplateVariables()).containsEntry("totalAmount", BigDecimal.valueOf(59.98));
        
        // Verify SMS notification
        NotificationRequest smsRequest = requests.stream()
            .filter(r -> r.getChannel() == NotificationChannel.SMS)
            .findFirst().orElseThrow();
        
        assertThat(smsRequest.getUserId()).isEqualTo(456L);
        assertThat(smsRequest.getNotificationType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(smsRequest.getTemplateKey()).isEqualTo("order_created_sms");
    }

    @Test
    void processOrderConfirmedEvent_ShouldCreateNotificationRequests() {
        // Given
        OrderConfirmedEvent event = createOrderConfirmedEvent();
        when(templateSelectionService.selectTemplate(eq("tenant1"), eq(NotificationType.ORDER_CONFIRMED), any()))
            .thenReturn("order_confirmed_email", "order_confirmed_sms");

        // When
        eventNotificationService.processOrderConfirmedEvent(event);

        // Then
        ArgumentCaptor<List<NotificationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationBatchService).submitNotifications(captor.capture());
        
        List<NotificationRequest> requests = captor.getValue();
        assertThat(requests).hasSize(2);
        
        NotificationRequest emailRequest = requests.stream()
            .filter(r -> r.getChannel() == NotificationChannel.EMAIL)
            .findFirst().orElseThrow();
        
        assertThat(emailRequest.getTemplateVariables()).containsEntry("paymentId", "payment123");
    }

    @Test
    void processOrderShippedEvent_ShouldCreateNotificationRequests() {
        // Given
        OrderShippedEvent event = createOrderShippedEvent();
        when(templateSelectionService.selectTemplate(eq("tenant1"), eq(NotificationType.ORDER_SHIPPED), any()))
            .thenReturn("order_shipped_email", "order_shipped_sms");

        // When
        eventNotificationService.processOrderShippedEvent(event);

        // Then
        ArgumentCaptor<List<NotificationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationBatchService).submitNotifications(captor.capture());
        
        List<NotificationRequest> requests = captor.getValue();
        assertThat(requests).hasSize(2);
        
        NotificationRequest emailRequest = requests.stream()
            .filter(r -> r.getChannel() == NotificationChannel.EMAIL)
            .findFirst().orElseThrow();
        
        assertThat(emailRequest.getTemplateVariables()).containsEntry("trackingNumber", "TRACK123");
        assertThat(emailRequest.getTemplateVariables()).containsEntry("carrier", "UPS");
    }

    @Test
    void processOrderDeliveredEvent_ShouldCreateNotificationRequests() {
        // Given
        OrderDeliveredEvent event = createOrderDeliveredEvent();
        when(templateSelectionService.selectTemplate(eq("tenant1"), eq(NotificationType.ORDER_DELIVERED), any()))
            .thenReturn("order_delivered_email", "order_delivered_sms");

        // When
        eventNotificationService.processOrderDeliveredEvent(event);

        // Then
        verify(notificationBatchService).submitNotifications(any());
    }

    @Test
    void processOrderCancelledEvent_ShouldCreateNotificationRequests() {
        // Given
        OrderCancelledEvent event = createOrderCancelledEvent();
        when(templateSelectionService.selectTemplate(eq("tenant1"), eq(NotificationType.ORDER_CANCELLED), any()))
            .thenReturn("order_cancelled_email", "order_cancelled_sms");

        // When
        eventNotificationService.processOrderCancelledEvent(event);

        // Then
        ArgumentCaptor<List<NotificationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationBatchService).submitNotifications(captor.capture());
        
        List<NotificationRequest> requests = captor.getValue();
        NotificationRequest emailRequest = requests.stream()
            .filter(r -> r.getChannel() == NotificationChannel.EMAIL)
            .findFirst().orElseThrow();
        
        assertThat(emailRequest.getTemplateVariables()).containsEntry("reason", "Customer requested cancellation");
    }

    @Test
    void processInventoryLowStockEvent_ShouldCreateAdminNotificationRequest() {
        // Given
        InventoryReservationFailedEvent event = createInventoryReservationFailedEvent();
        when(templateSelectionService.selectTemplate(eq("tenant1"), eq(NotificationType.INVENTORY_LOW), any()))
            .thenReturn("inventory_low_email");

        // When
        eventNotificationService.processInventoryLowStockEvent(event);

        // Then
        ArgumentCaptor<List<NotificationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationBatchService).submitNotifications(captor.capture());
        
        List<NotificationRequest> requests = captor.getValue();
        assertThat(requests).hasSize(1);
        
        NotificationRequest request = requests.get(0);
        assertThat(request.getUserId()).isEqualTo(0L); // Admin user
        assertThat(request.getNotificationType()).isEqualTo(NotificationType.INVENTORY_LOW);
        assertThat(request.getRecipient()).isEqualTo("admin@ecommerce.com");
        assertThat(request.getTemplateVariables()).containsEntry("orderId", "order123");
        assertThat(request.getTemplateVariables()).containsKey("failedItems");
        assertThat(request.getTemplateVariables()).containsEntry("itemCount", 1);
    }

    private OrderCreatedEvent createOrderCreatedEvent() {
        List<OrderCreatedEvent.OrderItemData> items = List.of(
            new OrderCreatedEvent.OrderItemData("product1", "SKU1", 2, BigDecimal.valueOf(29.99))
        );
        
        OrderCreatedEvent event = new OrderCreatedEvent("tenant1", "order123", "456", 
                                                       items, BigDecimal.valueOf(59.98), "PENDING");
        event.setCorrelationId("corr123");
        return event;
    }

    private OrderConfirmedEvent createOrderConfirmedEvent() {
        List<OrderConfirmedEvent.OrderItemData> items = List.of(
            new OrderConfirmedEvent.OrderItemData("product1", "SKU1", 2, BigDecimal.valueOf(29.99))
        );
        
        OrderConfirmedEvent event = new OrderConfirmedEvent("tenant1", "order123", "456", 
                                                           items, BigDecimal.valueOf(59.98), "payment123");
        event.setCorrelationId("corr123");
        return event;
    }

    private OrderShippedEvent createOrderShippedEvent() {
        List<OrderShippedEvent.OrderItemData> items = List.of(
            new OrderShippedEvent.OrderItemData("product1", "SKU1", 2)
        );
        
        OrderShippedEvent event = new OrderShippedEvent("tenant1", "order123", "456", 
                                                       items, "TRACK123", "UPS", "2024-01-15");
        event.setCorrelationId("corr123");
        return event;
    }

    private OrderDeliveredEvent createOrderDeliveredEvent() {
        List<OrderDeliveredEvent.OrderItemData> items = List.of(
            new OrderDeliveredEvent.OrderItemData("product1", "SKU1", 2)
        );
        
        OrderDeliveredEvent event = new OrderDeliveredEvent("tenant1", "order123", "456", 
                                                           items, "TRACK123", "UPS", null, "John Doe");
        event.setCorrelationId("corr123");
        return event;
    }

    private OrderCancelledEvent createOrderCancelledEvent() {
        List<OrderCancelledEvent.OrderItemData> items = List.of(
            new OrderCancelledEvent.OrderItemData("product1", "SKU1", 2)
        );
        
        OrderCancelledEvent event = new OrderCancelledEvent("tenant1", "order123", "456", 
                                                           items, "Customer requested cancellation");
        event.setCorrelationId("corr123");
        return event;
    }

    private InventoryReservationFailedEvent createInventoryReservationFailedEvent() {
        List<InventoryReservationFailedEvent.FailedItemData> failedItems = List.of(
            new InventoryReservationFailedEvent.FailedItemData("product1", "SKU1", 10, 2, "Insufficient stock")
        );
        
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent("tenant1", "order123", 
                                                                                   failedItems, "Insufficient stock");
        event.setCorrelationId("corr123");
        return event;
    }
}
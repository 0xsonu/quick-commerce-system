package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.service.EventNotificationService;
import com.ecommerce.shared.models.events.*;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private EventNotificationService eventNotificationService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private NotificationEventConsumer notificationEventConsumer;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void handleOrderCreatedEvent_ShouldProcessEventAndAcknowledge() {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();

        // When
        notificationEventConsumer.handleOrderCreatedEvent(event, "order-events", 0, 123L, acknowledgment);

        // Then
        verify(eventNotificationService).processOrderCreatedEvent(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderCreatedEvent_ShouldNotAcknowledgeOnException() {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();
        doThrow(new RuntimeException("Processing failed")).when(eventNotificationService)
            .processOrderCreatedEvent(event);

        // When
        notificationEventConsumer.handleOrderCreatedEvent(event, "order-events", 0, 123L, acknowledgment);

        // Then
        verify(eventNotificationService).processOrderCreatedEvent(event);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void handleOrderConfirmedEvent_ShouldProcessEventAndAcknowledge() {
        // Given
        OrderConfirmedEvent event = createOrderConfirmedEvent();

        // When
        notificationEventConsumer.handleOrderConfirmedEvent(event, "order-events", 0, 123L, acknowledgment);

        // Then
        verify(eventNotificationService).processOrderConfirmedEvent(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderShippedEvent_ShouldProcessEventAndAcknowledge() {
        // Given
        OrderShippedEvent event = createOrderShippedEvent();

        // When
        notificationEventConsumer.handleOrderShippedEvent(event, "shipping-events", 0, 123L, acknowledgment);

        // Then
        verify(eventNotificationService).processOrderShippedEvent(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderDeliveredEvent_ShouldProcessEventAndAcknowledge() {
        // Given
        OrderDeliveredEvent event = createOrderDeliveredEvent();

        // When
        notificationEventConsumer.handleOrderDeliveredEvent(event, "shipping-events", 0, 123L, acknowledgment);

        // Then
        verify(eventNotificationService).processOrderDeliveredEvent(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderCancelledEvent_ShouldProcessEventAndAcknowledge() {
        // Given
        OrderCancelledEvent event = createOrderCancelledEvent();

        // When
        notificationEventConsumer.handleOrderCancelledEvent(event, "order-events", 0, 123L, acknowledgment);

        // Then
        verify(eventNotificationService).processOrderCancelledEvent(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleInventoryLowStockEvent_ShouldProcessEventAndAcknowledge() {
        // Given
        InventoryReservationFailedEvent event = createInventoryReservationFailedEvent();

        // When
        notificationEventConsumer.handleInventoryLowStockEvent(event, "inventory-events", 0, 123L, acknowledgment);

        // Then
        verify(eventNotificationService).processInventoryLowStockEvent(event);
        verify(acknowledgment).acknowledge();
    }

    private OrderCreatedEvent createOrderCreatedEvent() {
        List<OrderCreatedEvent.OrderItemData> items = List.of(
            new OrderCreatedEvent.OrderItemData("product1", "SKU1", 2, BigDecimal.valueOf(29.99))
        );
        
        OrderCreatedEvent event = new OrderCreatedEvent("tenant1", "order123", "user456", 
                                                       items, BigDecimal.valueOf(59.98), "PENDING");
        event.setCorrelationId("corr123");
        return event;
    }

    private OrderConfirmedEvent createOrderConfirmedEvent() {
        List<OrderConfirmedEvent.OrderItemData> items = List.of(
            new OrderConfirmedEvent.OrderItemData("product1", "SKU1", 2, BigDecimal.valueOf(29.99))
        );
        
        OrderConfirmedEvent event = new OrderConfirmedEvent("tenant1", "order123", "user456", 
                                                           items, BigDecimal.valueOf(59.98), "payment123");
        event.setCorrelationId("corr123");
        return event;
    }

    private OrderShippedEvent createOrderShippedEvent() {
        List<OrderShippedEvent.OrderItemData> items = List.of(
            new OrderShippedEvent.OrderItemData("product1", "SKU1", 2)
        );
        
        OrderShippedEvent event = new OrderShippedEvent("tenant1", "order123", "user456", 
                                                       items, "TRACK123", "UPS", "2024-01-15");
        event.setCorrelationId("corr123");
        return event;
    }

    private OrderDeliveredEvent createOrderDeliveredEvent() {
        List<OrderDeliveredEvent.OrderItemData> items = List.of(
            new OrderDeliveredEvent.OrderItemData("product1", "SKU1", 2)
        );
        
        OrderDeliveredEvent event = new OrderDeliveredEvent("tenant1", "order123", "user456", 
                                                           items, "TRACK123", "UPS", null, "John Doe");
        event.setCorrelationId("corr123");
        return event;
    }

    private OrderCancelledEvent createOrderCancelledEvent() {
        List<OrderCancelledEvent.OrderItemData> items = List.of(
            new OrderCancelledEvent.OrderItemData("product1", "SKU1", 2)
        );
        
        OrderCancelledEvent event = new OrderCancelledEvent("tenant1", "order123", "user456", 
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
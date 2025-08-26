package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shared.models.events.OrderConfirmedEvent;
import com.ecommerce.shippingservice.service.ShipmentOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private ShipmentOrchestrationService shipmentOrchestrationService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    private OrderConfirmedEvent orderConfirmedEvent;

    @BeforeEach
    void setUp() {
        // Create test order confirmed event
        List<OrderConfirmedEvent.OrderItemData> items = List.of(
            new OrderConfirmedEvent.OrderItemData("product-1", "SKU-001", 2, BigDecimal.valueOf(29.99)),
            new OrderConfirmedEvent.OrderItemData("product-2", "SKU-002", 1, BigDecimal.valueOf(49.99))
        );

        orderConfirmedEvent = new OrderConfirmedEvent(
            "tenant-123",
            "order-456",
            "user-789",
            items,
            BigDecimal.valueOf(109.97),
            "payment-abc"
        );
        orderConfirmedEvent.setCorrelationId("corr-123");
    }

    @Test
    void handleOrderConfirmedEvent_Success() {
        // When
        orderEventConsumer.handleOrderConfirmedEvent(
            orderConfirmedEvent,
            "order-events",
            0,
            100L,
            acknowledgment
        );

        // Then
        verify(shipmentOrchestrationService).processOrderConfirmed(orderConfirmedEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderConfirmedEvent_WrongEventType_ShouldIgnore() {
        // Given
        orderConfirmedEvent.setEventType("ORDER_CANCELLED");

        // When
        orderEventConsumer.handleOrderConfirmedEvent(
            orderConfirmedEvent,
            "order-events",
            0,
            100L,
            acknowledgment
        );

        // Then
        verify(shipmentOrchestrationService, never()).processOrderConfirmed(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderConfirmedEvent_ProcessingError_ShouldNotAcknowledge() {
        // Given
        doThrow(new RuntimeException("Processing failed"))
            .when(shipmentOrchestrationService).processOrderConfirmed(any());

        // When & Then
        try {
            orderEventConsumer.handleOrderConfirmedEvent(
                orderConfirmedEvent,
                "order-events",
                0,
                100L,
                acknowledgment
            );
        } catch (RuntimeException e) {
            // Expected
        }

        verify(shipmentOrchestrationService).processOrderConfirmed(orderConfirmedEvent);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void handleOrderConfirmedEvent_NullEvent_ShouldHandleGracefully() {
        // When & Then
        try {
            orderEventConsumer.handleOrderConfirmedEvent(
                null,
                "order-events",
                0,
                100L,
                acknowledgment
            );
        } catch (Exception e) {
            // Expected due to null event
        }

        verify(shipmentOrchestrationService, never()).processOrderConfirmed(any());
        verify(acknowledgment, never()).acknowledge();
    }
}
package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shared.models.events.OrderDeliveredEvent;
import com.ecommerce.shared.models.events.OrderShippedEvent;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentItem;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    @InjectMocks
    private ShipmentEventPublisher eventPublisher;

    private Shipment testShipment;

    @BeforeEach
    void setUp() {
        // Set up tenant context
        TenantContext.setTenantId("tenant-123");
        TenantContext.setCorrelationId("corr-456");

        // Create test shipment
        testShipment = new Shipment();
        testShipment.setId(1L);
        testShipment.setTenantId("tenant-123");
        testShipment.setOrderId(100L);
        testShipment.setShipmentNumber("SH123456");
        testShipment.setCarrierName("FedEx");
        testShipment.setTrackingNumber("1234567890");
        testShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        testShipment.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
        testShipment.setDeliveredAt(LocalDateTime.now());

        // Add test items
        ShipmentItem item1 = new ShipmentItem();
        item1.setProductId("product-1");
        item1.setSku("SKU-001");
        item1.setQuantity(2);
        testShipment.addItem(item1);

        ShipmentItem item2 = new ShipmentItem();
        item2.setProductId("product-2");
        item2.setSku("SKU-002");
        item2.setQuantity(1);
        testShipment.addItem(item2);

        // Mock Kafka template
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void publishOrderShippedEvent_Success() {
        // When
        eventPublisher.publishOrderShippedEvent(testShipment);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderShippedEvent> eventCaptor = ArgumentCaptor.forClass(OrderShippedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals("shipping-events", topicCaptor.getValue());
        assertEquals("tenant-123:100", keyCaptor.getValue());

        OrderShippedEvent event = eventCaptor.getValue();
        assertEquals("100", event.getOrderId());
        assertEquals("1234567890", event.getTrackingNumber());
        assertEquals("FedEx", event.getCarrier());
        assertEquals(2, event.getItems().size());
        assertEquals("corr-456", event.getCorrelationId());
    }

    @Test
    void publishOrderDeliveredEvent_Success() {
        // When
        eventPublisher.publishOrderDeliveredEvent(testShipment);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(OrderDeliveredEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals("shipping-events", topicCaptor.getValue());
        assertEquals("tenant-123:100", keyCaptor.getValue());

        OrderDeliveredEvent event = eventCaptor.getValue();
        assertEquals("100", event.getOrderId());
        assertEquals("1234567890", event.getTrackingNumber());
        assertEquals("FedEx", event.getCarrier());
        assertEquals(2, event.getItems().size());
        assertNotNull(event.getDeliveredAt());
    }

    @Test
    void publishOrderDeliveredEvent_WithSignature() {
        // Given
        String signature = "John Doe";

        // When
        eventPublisher.publishOrderDeliveredEvent(testShipment, signature);

        // Then
        ArgumentCaptor<OrderDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(OrderDeliveredEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

        OrderDeliveredEvent event = eventCaptor.getValue();
        assertEquals(signature, event.getDeliverySignature());
    }

    @Test
    void publishShipmentStatusUpdateEvent_Success() {
        // When
        eventPublisher.publishShipmentStatusUpdateEvent(testShipment, "CREATED", "IN_TRANSIT");

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ShipmentStatusUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ShipmentStatusUpdateEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals("shipping-events", topicCaptor.getValue());
        assertEquals("tenant-123:1", keyCaptor.getValue());

        ShipmentStatusUpdateEvent event = eventCaptor.getValue();
        assertEquals(1L, event.getShipmentId());
        assertEquals(100L, event.getOrderId());
        assertEquals("SH123456", event.getShipmentNumber());
        assertEquals("1234567890", event.getTrackingNumber());
        assertEquals("FedEx", event.getCarrierName());
        assertEquals("CREATED", event.getPreviousStatus());
        assertEquals("IN_TRANSIT", event.getNewStatus());
        assertNotNull(event.getStatusChangeTime());
    }

    @Test
    void publishOrderShippedEvent_NoTenantContext_ShouldGenerateCorrelationId() {
        // Given
        TenantContext.clear();
        TenantContext.setTenantId("tenant-123");

        // When
        eventPublisher.publishOrderShippedEvent(testShipment);

        // Then
        ArgumentCaptor<OrderShippedEvent> eventCaptor = ArgumentCaptor.forClass(OrderShippedEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

        OrderShippedEvent event = eventCaptor.getValue();
        assertNotNull(event.getCorrelationId());
        assertFalse(event.getCorrelationId().isEmpty());
    }

    @Test
    void publishOrderShippedEvent_KafkaError_ShouldLogError() {
        // Given
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);

        // When
        eventPublisher.publishOrderShippedEvent(testShipment);

        // Then - should not throw exception, just log error
        verify(kafkaTemplate).send(anyString(), anyString(), any(OrderShippedEvent.class));
    }

    @Test
    void publishOrderShippedEvent_NullShipment_ShouldHandleGracefully() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> eventPublisher.publishOrderShippedEvent(null));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
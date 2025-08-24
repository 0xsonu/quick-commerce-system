package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.shared.models.events.*;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @Mock
    private SendResult<String, DomainEvent> sendResult;

    private OrderEventPublisher eventPublisher;
    private Order testOrder;
    private final String orderEventsTopic = "order-events";

    @BeforeEach
    void setUp() {
        eventPublisher = new OrderEventPublisher(kafkaTemplate, orderEventsTopic);
        
        // Set up tenant context
        TenantContext.setTenantId("test-tenant");
        TenantContext.setUserId("test-user");
        
        // Set up correlation ID in MDC
        MDC.put("correlationId", "test-correlation-id");
        
        // Create test order
        testOrder = new Order("test-tenant", "ORD-123", 1L);
        testOrder.setId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setSubtotal(new BigDecimal("100.00"));
        testOrder.setTaxAmount(new BigDecimal("8.00"));
        testOrder.setShippingAmount(new BigDecimal("9.99"));
        testOrder.setTotalAmount(new BigDecimal("117.99"));
        testOrder.setCurrency("USD");
        
        // Add test items
        OrderItem item1 = new OrderItem("product-1", "SKU-1", "Product 1", 2, new BigDecimal("25.00"));
        OrderItem item2 = new OrderItem("product-2", "SKU-2", "Product 2", 1, new BigDecimal("50.00"));
        testOrder.addItem(item1);
        testOrder.addItem(item2);
        
        // Mock successful Kafka send (will be overridden in specific tests if needed)
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(sendResult);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any(DomainEvent.class))).thenReturn(future);
    }

    @Test
    void publishOrderCreated_ShouldPublishCorrectEvent() {
        // Act
        CompletableFuture<SendResult<String, DomainEvent>> result = eventPublisher.publishOrderCreated(testOrder);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        
        // Verify Kafka template was called
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertEquals(orderEventsTopic, topicCaptor.getValue());
        assertEquals("1", keyCaptor.getValue());
        
        DomainEvent capturedEvent = eventCaptor.getValue();
        assertInstanceOf(OrderCreatedEvent.class, capturedEvent);
        
        OrderCreatedEvent orderCreatedEvent = (OrderCreatedEvent) capturedEvent;
        assertEquals("1", orderCreatedEvent.getOrderId());
        assertEquals("1", orderCreatedEvent.getUserId());
        assertEquals("test-tenant", orderCreatedEvent.getTenantId());
        assertEquals("ORDER_CREATED", orderCreatedEvent.getEventType());
        assertEquals("test-correlation-id", orderCreatedEvent.getCorrelationId());
        assertEquals(new BigDecimal("117.99"), orderCreatedEvent.getTotalAmount());
        assertEquals("PENDING", orderCreatedEvent.getStatus());
        assertEquals(2, orderCreatedEvent.getItems().size());
    }

    @Test
    void publishOrderConfirmed_ShouldPublishCorrectEvent() {
        // Arrange
        String paymentId = "payment-123";

        // Act
        CompletableFuture<SendResult<String, DomainEvent>> result = eventPublisher.publishOrderConfirmed(testOrder, paymentId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(kafkaTemplate).send(eq(orderEventsTopic), eq("1"), eventCaptor.capture());
        
        DomainEvent capturedEvent = eventCaptor.getValue();
        assertInstanceOf(OrderConfirmedEvent.class, capturedEvent);
        
        OrderConfirmedEvent orderConfirmedEvent = (OrderConfirmedEvent) capturedEvent;
        assertEquals("1", orderConfirmedEvent.getOrderId());
        assertEquals("1", orderConfirmedEvent.getUserId());
        assertEquals("test-tenant", orderConfirmedEvent.getTenantId());
        assertEquals("ORDER_CONFIRMED", orderConfirmedEvent.getEventType());
        assertEquals(paymentId, orderConfirmedEvent.getPaymentId());
        assertEquals(new BigDecimal("117.99"), orderConfirmedEvent.getTotalAmount());
    }

    @Test
    void publishOrderProcessing_ShouldPublishCorrectEvent() {
        // Act
        CompletableFuture<SendResult<String, DomainEvent>> result = eventPublisher.publishOrderProcessing(testOrder);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(kafkaTemplate).send(eq(orderEventsTopic), eq("1"), eventCaptor.capture());
        
        DomainEvent capturedEvent = eventCaptor.getValue();
        assertInstanceOf(OrderProcessingEvent.class, capturedEvent);
        
        OrderProcessingEvent orderProcessingEvent = (OrderProcessingEvent) capturedEvent;
        assertEquals("1", orderProcessingEvent.getOrderId());
        assertEquals("1", orderProcessingEvent.getUserId());
        assertEquals("test-tenant", orderProcessingEvent.getTenantId());
        assertEquals("ORDER_PROCESSING", orderProcessingEvent.getEventType());
        assertEquals(2, orderProcessingEvent.getItems().size());
    }

    @Test
    void publishOrderShipped_ShouldPublishCorrectEvent() {
        // Arrange
        String trackingNumber = "TRACK-123";
        String carrier = "UPS";
        String estimatedDeliveryDate = "2024-01-15";

        // Act
        CompletableFuture<SendResult<String, DomainEvent>> result = eventPublisher.publishOrderShipped(
            testOrder, trackingNumber, carrier, estimatedDeliveryDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(kafkaTemplate).send(eq(orderEventsTopic), eq("1"), eventCaptor.capture());
        
        DomainEvent capturedEvent = eventCaptor.getValue();
        assertInstanceOf(OrderShippedEvent.class, capturedEvent);
        
        OrderShippedEvent orderShippedEvent = (OrderShippedEvent) capturedEvent;
        assertEquals("1", orderShippedEvent.getOrderId());
        assertEquals("1", orderShippedEvent.getUserId());
        assertEquals("test-tenant", orderShippedEvent.getTenantId());
        assertEquals("ORDER_SHIPPED", orderShippedEvent.getEventType());
        assertEquals(trackingNumber, orderShippedEvent.getTrackingNumber());
        assertEquals(carrier, orderShippedEvent.getCarrier());
        assertEquals(estimatedDeliveryDate, orderShippedEvent.getEstimatedDeliveryDate());
    }

    @Test
    void publishOrderDelivered_ShouldPublishCorrectEvent() {
        // Arrange
        String trackingNumber = "TRACK-123";
        String carrier = "UPS";
        LocalDateTime deliveredAt = LocalDateTime.now();
        String deliverySignature = "John Doe";

        // Act
        CompletableFuture<SendResult<String, DomainEvent>> result = eventPublisher.publishOrderDelivered(
            testOrder, trackingNumber, carrier, deliveredAt, deliverySignature);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(kafkaTemplate).send(eq(orderEventsTopic), eq("1"), eventCaptor.capture());
        
        DomainEvent capturedEvent = eventCaptor.getValue();
        assertInstanceOf(OrderDeliveredEvent.class, capturedEvent);
        
        OrderDeliveredEvent orderDeliveredEvent = (OrderDeliveredEvent) capturedEvent;
        assertEquals("1", orderDeliveredEvent.getOrderId());
        assertEquals("1", orderDeliveredEvent.getUserId());
        assertEquals("test-tenant", orderDeliveredEvent.getTenantId());
        assertEquals("ORDER_DELIVERED", orderDeliveredEvent.getEventType());
        assertEquals(trackingNumber, orderDeliveredEvent.getTrackingNumber());
        assertEquals(carrier, orderDeliveredEvent.getCarrier());
        assertEquals(deliveredAt, orderDeliveredEvent.getDeliveredAt());
        assertEquals(deliverySignature, orderDeliveredEvent.getDeliverySignature());
    }

    @Test
    void publishOrderCancelled_ShouldPublishCorrectEvent() {
        // Arrange
        String reason = "Customer requested cancellation";

        // Act
        CompletableFuture<SendResult<String, DomainEvent>> result = eventPublisher.publishOrderCancelled(testOrder, reason);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(kafkaTemplate).send(eq(orderEventsTopic), eq("1"), eventCaptor.capture());
        
        DomainEvent capturedEvent = eventCaptor.getValue();
        assertInstanceOf(OrderCancelledEvent.class, capturedEvent);
        
        OrderCancelledEvent orderCancelledEvent = (OrderCancelledEvent) capturedEvent;
        assertEquals("1", orderCancelledEvent.getOrderId());
        assertEquals("1", orderCancelledEvent.getUserId());
        assertEquals("test-tenant", orderCancelledEvent.getTenantId());
        assertEquals("ORDER_CANCELLED", orderCancelledEvent.getEventType());
        assertEquals(reason, orderCancelledEvent.getReason());
        assertEquals(2, orderCancelledEvent.getItems().size());
    }

    @Test
    void publishEvent_WithoutCorrelationId_ShouldGenerateOne() {
        // Arrange
        MDC.clear(); // Remove correlation ID

        // Act
        eventPublisher.publishOrderCreated(testOrder);

        // Assert
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(kafkaTemplate).send(eq(orderEventsTopic), eq("1"), eventCaptor.capture());
        
        DomainEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent.getCorrelationId());
        assertFalse(capturedEvent.getCorrelationId().isEmpty());
    }

    @Test
    void publishEvent_WithKafkaFailure_ShouldLogError() {
        // Arrange
        CompletableFuture<SendResult<String, DomainEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), any(DomainEvent.class))).thenReturn(failedFuture);

        // Act
        CompletableFuture<SendResult<String, DomainEvent>> result = eventPublisher.publishOrderCreated(testOrder);

        // Assert
        assertNotNull(result);
        assertTrue(result.isCompletedExceptionally());
    }
}
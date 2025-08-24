package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
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
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventReplayServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private SendResult<String, Object> sendResult;

    private OrderEventReplayService replayService;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        replayService = new OrderEventReplayService(orderRepository, eventPublisher);
        
        // Set up tenant context
        TenantContext.setTenantId("test-tenant");
        
        // Create test order
        testOrder = new Order("test-tenant", "ORD-123", 1L);
        testOrder.setId(1L);
        testOrder.setStatus(OrderStatus.CONFIRMED);
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
        
        // Mock successful event publishing
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(eventPublisher.publishOrderCreated(any(Order.class))).thenReturn((CompletableFuture) future);
        when(eventPublisher.publishOrderConfirmed(any(Order.class), anyString())).thenReturn((CompletableFuture) future);
        when(eventPublisher.publishOrderProcessing(any(Order.class))).thenReturn((CompletableFuture) future);
        when(eventPublisher.publishOrderShipped(any(Order.class), anyString(), anyString(), anyString())).thenReturn((CompletableFuture) future);
        when(eventPublisher.publishOrderDelivered(any(Order.class), anyString(), anyString(), any(LocalDateTime.class), anyString())).thenReturn((CompletableFuture) future);
        when(eventPublisher.publishOrderCancelled(any(Order.class), anyString())).thenReturn((CompletableFuture) future);
    }

    @Test
    void replayOrderEvents_WithConfirmedOrder_ShouldPublishCorrectEvents() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        CompletableFuture<Void> result = replayService.replayOrderEvents(1L);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.join());
        
        // Verify correct events were published
        verify(eventPublisher).publishOrderCreated(testOrder);
        verify(eventPublisher).publishOrderConfirmed(testOrder, "replay-payment-id");
        verify(eventPublisher, never()).publishOrderProcessing(testOrder);
        verify(eventPublisher, never()).publishOrderShipped(any(), anyString(), anyString(), anyString());
        verify(eventPublisher, never()).publishOrderDelivered(any(), anyString(), anyString(), any(), anyString());
        verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
    }

    @Test
    void replayOrderEvents_WithShippedOrder_ShouldPublishAllRelevantEvents() {
        // Arrange
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        CompletableFuture<Void> result = replayService.replayOrderEvents(1L);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.join());
        
        // Verify all relevant events were published
        verify(eventPublisher).publishOrderCreated(testOrder);
        verify(eventPublisher).publishOrderConfirmed(testOrder, "replay-payment-id");
        verify(eventPublisher).publishOrderProcessing(testOrder);
        verify(eventPublisher).publishOrderShipped(testOrder, "replay-tracking", "replay-carrier", null);
        verify(eventPublisher, never()).publishOrderDelivered(any(), anyString(), anyString(), any(), anyString());
        verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
    }

    @Test
    void replayOrderEvents_WithDeliveredOrder_ShouldPublishAllEvents() {
        // Arrange
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        CompletableFuture<Void> result = replayService.replayOrderEvents(1L);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.join());
        
        // Verify all events were published
        verify(eventPublisher).publishOrderCreated(testOrder);
        verify(eventPublisher).publishOrderConfirmed(testOrder, "replay-payment-id");
        verify(eventPublisher).publishOrderProcessing(testOrder);
        verify(eventPublisher).publishOrderShipped(testOrder, "replay-tracking", "replay-carrier", null);
        verify(eventPublisher).publishOrderDelivered(eq(testOrder), eq("replay-tracking"), eq("replay-carrier"), any(LocalDateTime.class), eq(null));
        verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
    }

    @Test
    void replayOrderEvents_WithCancelledOrder_ShouldPublishCancelledEvent() {
        // Arrange
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        CompletableFuture<Void> result = replayService.replayOrderEvents(1L);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.join());
        
        // Verify correct events were published
        verify(eventPublisher).publishOrderCreated(testOrder);
        verify(eventPublisher).publishOrderCancelled(testOrder, "Event replay");
        verify(eventPublisher, never()).publishOrderConfirmed(any(), anyString());
        verify(eventPublisher, never()).publishOrderProcessing(testOrder);
    }

    @Test
    void replayOrderEvents_WithPendingOrder_ShouldOnlyPublishCreatedEvent() {
        // Arrange
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        CompletableFuture<Void> result = replayService.replayOrderEvents(1L);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.join());
        
        // Verify only created event was published
        verify(eventPublisher).publishOrderCreated(testOrder);
        verify(eventPublisher, never()).publishOrderConfirmed(any(), anyString());
        verify(eventPublisher, never()).publishOrderProcessing(testOrder);
        verify(eventPublisher, never()).publishOrderShipped(any(), anyString(), anyString(), anyString());
        verify(eventPublisher, never()).publishOrderDelivered(any(), anyString(), anyString(), any(), anyString());
        verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
    }

    @Test
    void replayOrderEvents_WithNonExistentOrder_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        CompletableFuture<Void> result = replayService.replayOrderEvents(1L);
        assertThrows(RuntimeException.class, () -> result.join());
    }

    @Test
    void replayOrderEventsByDateRange_ShouldProcessAllOrdersInRange() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        Order order2 = new Order("test-tenant", "ORD-124", 2L);
        order2.setId(2L);
        order2.setStatus(OrderStatus.PROCESSING);
        
        List<Order> orders = Arrays.asList(testOrder, order2);
        Page<Order> ordersPage = new PageImpl<>(orders);
        
        when(orderRepository.findOrdersByDateRange(eq("test-tenant"), eq(startDate), eq(endDate), any(Pageable.class)))
            .thenReturn(ordersPage);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order2));

        // Act
        CompletableFuture<Void> result = replayService.replayOrderEventsByDateRange(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.join());
        
        // Verify repository was called with correct parameters
        verify(orderRepository).findOrdersByDateRange(eq("test-tenant"), eq(startDate), eq(endDate), any(Pageable.class));
        
        // Verify events were published for both orders
        verify(eventPublisher, times(2)).publishOrderCreated(any(Order.class));
    }

    @Test
    void replayOrderEventsByStatus_ShouldProcessAllOrdersWithStatus() {
        // Arrange
        OrderStatus status = OrderStatus.CONFIRMED;
        Order order2 = new Order("test-tenant", "ORD-124", 2L);
        order2.setId(2L);
        order2.setStatus(OrderStatus.CONFIRMED);
        
        List<Order> orders = Arrays.asList(testOrder, order2);
        
        when(orderRepository.findByStatus("test-tenant", status)).thenReturn(orders);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order2));

        // Act
        CompletableFuture<Void> result = replayService.replayOrderEventsByStatus(status);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.join());
        
        // Verify repository was called with correct parameters
        verify(orderRepository).findByStatus("test-tenant", status);
        
        // Verify events were published for both orders
        verify(eventPublisher, times(2)).publishOrderCreated(any(Order.class));
        verify(eventPublisher, times(2)).publishOrderConfirmed(any(Order.class), eq("replay-payment-id"));
    }

    @Test
    void recoverMissingEvents_ShouldReplayAllEvents() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        CompletableFuture<Void> result = replayService.recoverMissingEvents(1L);

        // Assert
        assertNotNull(result);
        assertDoesNotThrow(() -> result.join());
        
        // Verify events were published (same as replay)
        verify(eventPublisher).publishOrderCreated(testOrder);
        verify(eventPublisher).publishOrderConfirmed(testOrder, "replay-payment-id");
    }

    @Test
    void validateEventConsistency_WithValidOrder_ShouldReturnTrue() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        boolean result = replayService.validateEventConsistency(1L);

        // Assert
        assertTrue(result);
    }

    @Test
    void validateEventConsistency_WithInvalidOrder_ShouldReturnFalse() {
        // Arrange
        testOrder.setStatus(null); // Invalid state
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        boolean result = replayService.validateEventConsistency(1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateEventConsistency_WithNonExistentOrder_ShouldReturnFalse() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = replayService.validateEventConsistency(1L);

        // Assert
        assertFalse(result);
    }
}
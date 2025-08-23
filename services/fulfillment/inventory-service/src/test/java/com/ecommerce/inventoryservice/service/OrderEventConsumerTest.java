package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.ReservationRequest;
import com.ecommerce.inventoryservice.dto.ReservationResponse;
import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.ecommerce.shared.models.events.OrderCancelledEvent;
import com.ecommerce.shared.models.events.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private InventoryReservationService reservationService;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    private final String tenantId = "tenant123";
    private final String orderId = "order123";
    private final String userId = "user123";

    @BeforeEach
    void setUp() {
        // Setup is handled by @Mock annotations
    }

    @Test
    void handleOrderCreated_WithValidEvent_ShouldReserveInventory() {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();
        ReservationResponse successResponse = ReservationResponse.success("reservation123", Arrays.asList());
        
        when(reservationService.reserveInventory(eq(tenantId), any(ReservationRequest.class)))
            .thenReturn(successResponse);

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCreated(
            event, "order-events", 0, 0L, acknowledgment));

        // Then
        verify(reservationService).reserveInventory(eq(tenantId), any(ReservationRequest.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderCreated_WithReservationFailure_ShouldStillAcknowledge() {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();
        ReservationResponse failureResponse = ReservationResponse.failed("reservation123", "Insufficient stock");
        
        when(reservationService.reserveInventory(eq(tenantId), any(ReservationRequest.class)))
            .thenReturn(failureResponse);

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCreated(
            event, "order-events", 0, 0L, acknowledgment));

        // Then
        verify(reservationService).reserveInventory(eq(tenantId), any(ReservationRequest.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderCreated_WithServiceException_ShouldThrowException() {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();
        
        when(reservationService.reserveInventory(eq(tenantId), any(ReservationRequest.class)))
            .thenThrow(new RuntimeException("Service error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> orderEventConsumer.handleOrderCreated(
            event, "order-events", 0, 0L, acknowledgment));

        verify(reservationService).reserveInventory(eq(tenantId), any(ReservationRequest.class));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void handleOrderCancelled_WithValidEvent_ShouldReleaseReservation() {
        // Given
        OrderCancelledEvent event = createOrderCancelledEvent();
        InventoryReservation mockReservation = new InventoryReservation();
        mockReservation.setReservationId("reservation123");
        mockReservation.setOrderId(orderId);
        mockReservation.setTenantId(tenantId);
        
        when(reservationRepository.findActiveReservationsByOrder(tenantId, orderId))
            .thenReturn(Arrays.asList(mockReservation));

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCancelled(
            event, "order-events", 0, 0L, acknowledgment));

        // Then
        verify(reservationService).releaseReservation(eq(tenantId), eq("reservation123"), anyString());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderCancelled_WithReleaseException_ShouldStillAcknowledge() {
        // Given
        OrderCancelledEvent event = createOrderCancelledEvent();
        InventoryReservation mockReservation = new InventoryReservation();
        mockReservation.setReservationId("reservation123");
        mockReservation.setOrderId(orderId);
        mockReservation.setTenantId(tenantId);
        
        when(reservationRepository.findActiveReservationsByOrder(tenantId, orderId))
            .thenReturn(Arrays.asList(mockReservation));
        
        doThrow(new RuntimeException("Release error"))
            .when(reservationService).releaseReservation(anyString(), anyString(), anyString());

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCancelled(
            event, "order-events", 0, 0L, acknowledgment));

        // Then
        verify(reservationService).releaseReservation(eq(tenantId), eq("reservation123"), anyString());
        verify(acknowledgment).acknowledge(); // Should still acknowledge even if release fails
    }

    @Test
    void handleOrderCreated_ShouldMapOrderItemsCorrectly() {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();
        ReservationResponse successResponse = ReservationResponse.success("reservation123", Arrays.asList());
        
        when(reservationService.reserveInventory(eq(tenantId), any(ReservationRequest.class)))
            .thenReturn(successResponse);

        // When
        orderEventConsumer.handleOrderCreated(event, "order-events", 0, 0L, acknowledgment);

        // Then
        verify(reservationService).reserveInventory(eq(tenantId), argThat(request -> {
            assertEquals(orderId, request.getOrderId());
            assertEquals(2, request.getItems().size());
            
            ReservationRequest.ReservationItemRequest item1 = request.getItems().get(0);
            assertEquals("product1", item1.getProductId());
            assertEquals("SKU1", item1.getSku());
            assertEquals(2, item1.getQuantity());
            
            ReservationRequest.ReservationItemRequest item2 = request.getItems().get(1);
            assertEquals("product2", item2.getProductId());
            assertEquals("SKU2", item2.getSku());
            assertEquals(1, item2.getQuantity());
            
            return true;
        }));
    }

    // Helper methods
    private OrderCreatedEvent createOrderCreatedEvent() {
        List<OrderCreatedEvent.OrderItemData> items = Arrays.asList(
            new OrderCreatedEvent.OrderItemData("product1", "SKU1", 2, new BigDecimal("10.00")),
            new OrderCreatedEvent.OrderItemData("product2", "SKU2", 1, new BigDecimal("20.00"))
        );
        
        return new OrderCreatedEvent(tenantId, orderId, userId, items, new BigDecimal("40.00"), "PENDING");
    }

    private OrderCancelledEvent createOrderCancelledEvent() {
        List<OrderCancelledEvent.OrderItemData> items = Arrays.asList(
            new OrderCancelledEvent.OrderItemData("product1", "SKU1", 2),
            new OrderCancelledEvent.OrderItemData("product2", "SKU2", 1)
        );
        
        return new OrderCancelledEvent(tenantId, orderId, userId, items, "Customer requested cancellation");
    }
}
package com.ecommerce.shippingservice.service;

import com.ecommerce.shared.models.events.OrderConfirmedEvent;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shippingservice.dto.CreateShipmentRequest;
import com.ecommerce.shippingservice.dto.ShipmentResponse;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.kafka.ShipmentEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentOrchestrationServiceTest {

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private ShipmentEventPublisher eventPublisher;

    @InjectMocks
    private ShipmentOrchestrationService orchestrationService;

    private OrderConfirmedEvent orderConfirmedEvent;
    private ShipmentResponse mockShipmentResponse;

    @BeforeEach
    void setUp() {
        // Set configuration properties
        ReflectionTestUtils.setField(orchestrationService, "defaultCarrier", "fedex");
        ReflectionTestUtils.setField(orchestrationService, "defaultService", "GROUND");
        ReflectionTestUtils.setField(orchestrationService, "autoShipOnCreation", true);

        // Set up tenant context
        TenantContext.setTenantId("tenant-123");
        TenantContext.setCorrelationId("corr-456");

        // Create test order confirmed event
        List<OrderConfirmedEvent.OrderItemData> items = List.of(
            new OrderConfirmedEvent.OrderItemData("product-1", "SKU-001", 2, BigDecimal.valueOf(29.99)),
            new OrderConfirmedEvent.OrderItemData("product-2", "SKU-002", 1, BigDecimal.valueOf(49.99))
        );

        orderConfirmedEvent = new OrderConfirmedEvent(
            "tenant-123",
            "100",
            "user-789",
            items,
            BigDecimal.valueOf(109.97),
            "payment-abc"
        );
        orderConfirmedEvent.setCorrelationId("corr-456");

        // Create mock shipment response
        mockShipmentResponse = new ShipmentResponse();
        mockShipmentResponse.setId(1L);
        mockShipmentResponse.setOrderId(100L);
        mockShipmentResponse.setShipmentNumber("SH123456");
        mockShipmentResponse.setCarrierName("fedex");
        mockShipmentResponse.setTrackingNumber("1234567890");
        mockShipmentResponse.setStatus(ShipmentStatus.CREATED);
        mockShipmentResponse.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void processOrderConfirmed_Success() {
        // Given
        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
            .thenReturn(mockShipmentResponse);

        ShipmentResponse updatedShipment = new ShipmentResponse();
        updatedShipment.setId(1L);
        updatedShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        updatedShipment.setTrackingNumber("1234567890");
        when(shipmentService.updateShipmentStatus(eq(1L), eq(ShipmentStatus.IN_TRANSIT), anyString()))
            .thenReturn(updatedShipment);

        // When
        orchestrationService.processOrderConfirmed(orderConfirmedEvent);

        // Then
        ArgumentCaptor<CreateShipmentRequest> requestCaptor = ArgumentCaptor.forClass(CreateShipmentRequest.class);
        verify(shipmentService).createShipment(requestCaptor.capture());

        CreateShipmentRequest request = requestCaptor.getValue();
        assertEquals(100L, request.getOrderId());
        assertEquals("fedex", request.getCarrierName());
        assertEquals("GROUND", request.getServiceType());
        assertEquals(2, request.getItems().size());
        assertNotNull(request.getShippingAddress());

        // Verify status update and event publishing
        verify(shipmentService).updateShipmentStatus(1L, ShipmentStatus.IN_TRANSIT, "Shipment picked up by carrier");
        verify(eventPublisher).publishOrderShippedEvent(any());
    }

    @Test
    void processOrderConfirmed_AutoShipDisabled_ShouldNotUpdateStatus() {
        // Given
        ReflectionTestUtils.setField(orchestrationService, "autoShipOnCreation", false);
        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
            .thenReturn(mockShipmentResponse);

        // When
        orchestrationService.processOrderConfirmed(orderConfirmedEvent);

        // Then
        verify(shipmentService).createShipment(any(CreateShipmentRequest.class));
        verify(shipmentService, never()).updateShipmentStatus(anyLong(), any(), anyString());
        verify(eventPublisher, never()).publishOrderShippedEvent(any());
    }

    @Test
    void processOrderConfirmed_NoTrackingNumber_ShouldNotUpdateStatus() {
        // Given
        mockShipmentResponse.setTrackingNumber(null);
        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
            .thenReturn(mockShipmentResponse);

        // When
        orchestrationService.processOrderConfirmed(orderConfirmedEvent);

        // Then
        verify(shipmentService).createShipment(any(CreateShipmentRequest.class));
        verify(shipmentService, never()).updateShipmentStatus(anyLong(), any(), anyString());
        verify(eventPublisher, never()).publishOrderShippedEvent(any());
    }

    @Test
    void processOrderConfirmed_ShipmentCreationFails_ShouldThrowException() {
        // Given
        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
            .thenThrow(new RuntimeException("Shipment creation failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orchestrationService.processOrderConfirmed(orderConfirmedEvent);
        });

        assertTrue(exception.getMessage().contains("Failed to create shipment for order: 100"));
        verify(eventPublisher, never()).publishOrderShippedEvent(any());
    }

    @Test
    void processShipmentStatusUpdate_InTransitStatus_ShouldPublishOrderShippedEvent() {
        // Given
        when(shipmentService.getShipment(1L)).thenReturn(mockShipmentResponse);

        // When
        orchestrationService.processShipmentStatusUpdate(1L, ShipmentStatus.CREATED, ShipmentStatus.IN_TRANSIT);

        // Then
        verify(eventPublisher).publishShipmentStatusUpdateEvent(any(), eq("CREATED"), eq("IN_TRANSIT"));
        verify(eventPublisher).publishOrderShippedEvent(any());
        verify(eventPublisher, never()).publishOrderDeliveredEvent(any());
    }

    @Test
    void processShipmentStatusUpdate_DeliveredStatus_ShouldPublishOrderDeliveredEvent() {
        // Given
        mockShipmentResponse.setStatus(ShipmentStatus.DELIVERED);
        mockShipmentResponse.setDeliveredAt(LocalDateTime.now());
        when(shipmentService.getShipment(1L)).thenReturn(mockShipmentResponse);

        // When
        orchestrationService.processShipmentStatusUpdate(1L, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED);

        // Then
        verify(eventPublisher).publishShipmentStatusUpdateEvent(any(), eq("IN_TRANSIT"), eq("DELIVERED"));
        verify(eventPublisher).publishOrderDeliveredEvent(any());
        verify(eventPublisher, never()).publishOrderShippedEvent(any());
    }

    @Test
    void processShipmentStatusUpdate_ExceptionStatus_ShouldOnlyPublishStatusUpdate() {
        // Given
        mockShipmentResponse.setStatus(ShipmentStatus.EXCEPTION);
        when(shipmentService.getShipment(1L)).thenReturn(mockShipmentResponse);

        // When
        orchestrationService.processShipmentStatusUpdate(1L, ShipmentStatus.IN_TRANSIT, ShipmentStatus.EXCEPTION);

        // Then
        verify(eventPublisher).publishShipmentStatusUpdateEvent(any(), eq("IN_TRANSIT"), eq("EXCEPTION"));
        verify(eventPublisher, never()).publishOrderShippedEvent(any());
        verify(eventPublisher, never()).publishOrderDeliveredEvent(any());
    }

    @Test
    void processTrackingUpdate_StatusChanged_ShouldProcessStatusUpdate() {
        // Given
        ShipmentResponse beforeUpdate = new ShipmentResponse();
        beforeUpdate.setId(1L);
        beforeUpdate.setStatus(ShipmentStatus.IN_TRANSIT);

        ShipmentResponse afterUpdate = new ShipmentResponse();
        afterUpdate.setId(1L);
        afterUpdate.setStatus(ShipmentStatus.DELIVERED);

        when(shipmentService.getShipment(1L))
            .thenReturn(beforeUpdate)
            .thenReturn(afterUpdate);

        // When
        orchestrationService.processTrackingUpdate(1L);

        // Then
        verify(shipmentService).updateTrackingFromCarrier(1L);
        verify(eventPublisher).publishShipmentStatusUpdateEvent(any(), eq("IN_TRANSIT"), eq("DELIVERED"));
        verify(eventPublisher).publishOrderDeliveredEvent(any());
    }

    @Test
    void processTrackingUpdate_StatusUnchanged_ShouldNotPublishEvents() {
        // Given
        ShipmentResponse shipmentResponse = new ShipmentResponse();
        shipmentResponse.setId(1L);
        shipmentResponse.setStatus(ShipmentStatus.IN_TRANSIT);

        when(shipmentService.getShipment(1L)).thenReturn(shipmentResponse);

        // When
        orchestrationService.processTrackingUpdate(1L);

        // Then
        verify(shipmentService).updateTrackingFromCarrier(1L);
        verify(eventPublisher, never()).publishShipmentStatusUpdateEvent(any(), anyString(), anyString());
        verify(eventPublisher, never()).publishOrderShippedEvent(any());
        verify(eventPublisher, never()).publishOrderDeliveredEvent(any());
    }

    @Test
    void processTrackingUpdate_TrackingUpdateFails_ShouldHandleGracefully() {
        // Given
        when(shipmentService.getShipment(1L)).thenReturn(mockShipmentResponse);
        doThrow(new RuntimeException("Tracking update failed"))
            .when(shipmentService).updateTrackingFromCarrier(1L);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> orchestrationService.processTrackingUpdate(1L));

        verify(shipmentService).updateTrackingFromCarrier(1L);
        verify(eventPublisher, never()).publishShipmentStatusUpdateEvent(any(), anyString(), anyString());
    }
}
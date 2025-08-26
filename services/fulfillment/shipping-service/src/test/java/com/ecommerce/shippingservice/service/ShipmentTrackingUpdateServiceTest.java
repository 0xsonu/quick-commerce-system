package com.ecommerce.shippingservice.service;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentTrackingUpdateServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentOrchestrationService orchestrationService;

    @InjectMocks
    private ShipmentTrackingUpdateService trackingUpdateService;

    private List<Shipment> testShipments;

    @BeforeEach
    void setUp() {
        // Set configuration properties
        ReflectionTestUtils.setField(trackingUpdateService, "batchSize", 2);
        ReflectionTestUtils.setField(trackingUpdateService, "trackingUpdateEnabled", true);

        // Create test shipments
        Shipment shipment1 = new Shipment();
        shipment1.setId(1L);
        shipment1.setTenantId("tenant-123");
        shipment1.setOrderId(100L);
        shipment1.setTrackingNumber("TRACK001");
        shipment1.setStatus(ShipmentStatus.IN_TRANSIT);

        Shipment shipment2 = new Shipment();
        shipment2.setId(2L);
        shipment2.setTenantId("tenant-123");
        shipment2.setOrderId(101L);
        shipment2.setTrackingNumber("TRACK002");
        shipment2.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);

        testShipments = List.of(shipment1, shipment2);
    }

    @Test
    void updateActiveShipmentTracking_Success() {
        // Given - create a page that has no next page (hasNext() returns false)
        Page<Shipment> shipmentsPage = new PageImpl<>(testShipments, PageRequest.of(0, 2), 2);
        when(shipmentRepository.findByStatusInAndTrackingNumberIsNotNull(anyList(), any(Pageable.class)))
            .thenReturn(shipmentsPage);

        // When
        trackingUpdateService.updateActiveShipmentTracking();

        // Then
        verify(shipmentRepository, times(1)).findByStatusInAndTrackingNumberIsNotNull(anyList(), any(Pageable.class));
        verify(orchestrationService, times(2)).processTrackingUpdate(anyLong());
    }

    @Test
    void updateActiveShipmentTracking_TrackingDisabled_ShouldSkip() {
        // Given
        ReflectionTestUtils.setField(trackingUpdateService, "trackingUpdateEnabled", false);

        // When
        trackingUpdateService.updateActiveShipmentTracking();

        // Then
        verify(shipmentRepository, never()).findByStatusInAndTrackingNumberIsNotNull(anyList(), any(Pageable.class));
        verify(orchestrationService, never()).processTrackingUpdate(anyLong());
    }

    @Test
    void updateActiveShipmentTracking_NoActiveShipments_ShouldComplete() {
        // Given
        when(shipmentRepository.findByStatusInAndTrackingNumberIsNotNull(anyList(), any(Pageable.class)))
            .thenReturn(Page.empty());

        // When
        trackingUpdateService.updateActiveShipmentTracking();

        // Then
        verify(shipmentRepository).findByStatusInAndTrackingNumberIsNotNull(anyList(), any(Pageable.class));
        verify(orchestrationService, never()).processTrackingUpdate(anyLong());
    }

    @Test
    void updateActiveShipmentTracking_ProcessingError_ShouldContinue() {
        // Given
        Page<Shipment> shipmentsPage = new PageImpl<>(testShipments, PageRequest.of(0, 2), 2);
        when(shipmentRepository.findByStatusInAndTrackingNumberIsNotNull(anyList(), any(Pageable.class)))
            .thenReturn(shipmentsPage)
            .thenReturn(Page.empty());

        doThrow(new RuntimeException("Processing failed"))
            .when(orchestrationService).processTrackingUpdate(1L);

        // When
        trackingUpdateService.updateActiveShipmentTracking();

        // Then
        verify(orchestrationService).processTrackingUpdate(1L);
        verify(orchestrationService).processTrackingUpdate(2L); // Should continue processing
    }

    @Test
    void processTrackingUpdateBatch_Success() throws Exception {
        // When
        CompletableFuture<Integer> result = trackingUpdateService.processTrackingUpdateBatch(testShipments);

        // Then
        assertEquals(2, result.get());
        verify(orchestrationService).processTrackingUpdate(1L);
        verify(orchestrationService).processTrackingUpdate(2L);
    }

    @Test
    void processTrackingUpdateBatch_PartialFailure() throws Exception {
        // Given
        doThrow(new RuntimeException("Processing failed"))
            .when(orchestrationService).processTrackingUpdate(1L);

        // When
        CompletableFuture<Integer> result = trackingUpdateService.processTrackingUpdateBatch(testShipments);

        // Then
        assertEquals(1, result.get()); // Only one successful
        verify(orchestrationService).processTrackingUpdate(1L);
        verify(orchestrationService).processTrackingUpdate(2L);
    }

    @Test
    void processTrackingUpdateBatch_EmptyList() throws Exception {
        // When
        CompletableFuture<Integer> result = trackingUpdateService.processTrackingUpdateBatch(List.of());

        // Then
        assertEquals(0, result.get());
        verify(orchestrationService, never()).processTrackingUpdate(anyLong());
    }

    @Test
    void updateShipmentTracking_Success() {
        // When
        trackingUpdateService.updateShipmentTracking(1L);

        // Then
        verify(orchestrationService).processTrackingUpdate(1L);
    }

    @Test
    void updateShipmentTracking_ProcessingError_ShouldThrowException() {
        // Given
        doThrow(new RuntimeException("Processing failed"))
            .when(orchestrationService).processTrackingUpdate(1L);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            trackingUpdateService.updateShipmentTracking(1L);
        });

        assertTrue(exception.getMessage().contains("Failed to update shipment tracking"));
        verify(orchestrationService).processTrackingUpdate(1L);
    }

    @Test
    void updateOrderShipmentTracking_Success() {
        // Given
        when(shipmentRepository.findByOrderId(100L)).thenReturn(testShipments);

        // When
        trackingUpdateService.updateOrderShipmentTracking(100L);

        // Then
        verify(shipmentRepository).findByOrderId(100L);
        verify(orchestrationService).processTrackingUpdate(1L);
        verify(orchestrationService).processTrackingUpdate(2L);
    }

    @Test
    void updateOrderShipmentTracking_NoShipments_ShouldComplete() {
        // Given
        when(shipmentRepository.findByOrderId(100L)).thenReturn(List.of());

        // When
        trackingUpdateService.updateOrderShipmentTracking(100L);

        // Then
        verify(shipmentRepository).findByOrderId(100L);
        verify(orchestrationService, never()).processTrackingUpdate(anyLong());
    }

    @Test
    void updateOrderShipmentTracking_ShipmentWithoutTracking_ShouldSkip() {
        // Given
        Shipment shipmentWithoutTracking = new Shipment();
        shipmentWithoutTracking.setId(3L);
        shipmentWithoutTracking.setTrackingNumber(null);
        shipmentWithoutTracking.setStatus(ShipmentStatus.IN_TRANSIT);

        when(shipmentRepository.findByOrderId(100L))
            .thenReturn(List.of(testShipments.get(0), shipmentWithoutTracking));

        // When
        trackingUpdateService.updateOrderShipmentTracking(100L);

        // Then
        verify(orchestrationService).processTrackingUpdate(1L);
        verify(orchestrationService, never()).processTrackingUpdate(3L);
    }

    @Test
    void updateOrderShipmentTracking_TerminalStatus_ShouldSkip() {
        // Given
        Shipment deliveredShipment = new Shipment();
        deliveredShipment.setId(3L);
        deliveredShipment.setTrackingNumber("TRACK003");
        deliveredShipment.setStatus(ShipmentStatus.DELIVERED);

        when(shipmentRepository.findByOrderId(100L))
            .thenReturn(List.of(testShipments.get(0), deliveredShipment));

        // When
        trackingUpdateService.updateOrderShipmentTracking(100L);

        // Then
        verify(orchestrationService).processTrackingUpdate(1L);
        verify(orchestrationService, never()).processTrackingUpdate(3L);
    }

    @Test
    void updateOrderShipmentTracking_ProcessingError_ShouldThrowException() {
        // Given
        when(shipmentRepository.findByOrderId(100L)).thenReturn(testShipments);
        doThrow(new RuntimeException("Processing failed"))
            .when(orchestrationService).processTrackingUpdate(anyLong());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            trackingUpdateService.updateOrderShipmentTracking(100L);
        });

        assertTrue(exception.getMessage().contains("Failed to update order shipment tracking"));
    }
}
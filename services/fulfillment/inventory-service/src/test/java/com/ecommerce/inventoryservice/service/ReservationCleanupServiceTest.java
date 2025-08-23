package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationCleanupServiceTest {

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private InventoryReservationService reservationService;

    @InjectMocks
    private ReservationCleanupService cleanupService;

    private final String tenantId = "tenant123";

    @BeforeEach
    void setUp() {
        // Setup is handled by @Mock annotations
    }

    @Test
    void cleanupExpiredReservations_WithExpiredReservations_ShouldCleanupAll() {
        // Given
        InventoryReservation expiredReservation1 = createExpiredReservation("reservation1", "order1");
        InventoryReservation expiredReservation2 = createExpiredReservation("reservation2", "order2");
        List<InventoryReservation> expiredReservations = Arrays.asList(expiredReservation1, expiredReservation2);

        when(reservationRepository.findAllExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(expiredReservations);

        // When
        cleanupService.cleanupExpiredReservations();

        // Then
        verify(reservationRepository).findAllExpiredReservations(any(LocalDateTime.class));
        verify(reservationService).releaseReservation(eq(tenantId), eq("reservation1"), eq("Expired - automatic cleanup"));
        verify(reservationService).releaseReservation(eq(tenantId), eq("reservation2"), eq("Expired - automatic cleanup"));
    }

    @Test
    void cleanupExpiredReservations_WithNoExpiredReservations_ShouldDoNothing() {
        // Given
        when(reservationRepository.findAllExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        cleanupService.cleanupExpiredReservations();

        // Then
        verify(reservationRepository).findAllExpiredReservations(any(LocalDateTime.class));
        verify(reservationService, never()).releaseReservation(anyString(), anyString(), anyString());
    }

    @Test
    void cleanupExpiredReservations_WithReleaseFailure_ShouldContinueWithOthers() {
        // Given
        InventoryReservation expiredReservation1 = createExpiredReservation("reservation1", "order1");
        InventoryReservation expiredReservation2 = createExpiredReservation("reservation2", "order2");
        List<InventoryReservation> expiredReservations = Arrays.asList(expiredReservation1, expiredReservation2);

        when(reservationRepository.findAllExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(expiredReservations);

        // First release fails, second should still be attempted
        doThrow(new RuntimeException("Release failed"))
            .when(reservationService).releaseReservation(eq(tenantId), eq("reservation1"), anyString());
        doNothing()
            .when(reservationService).releaseReservation(eq(tenantId), eq("reservation2"), anyString());

        // When
        cleanupService.cleanupExpiredReservations();

        // Then
        verify(reservationService).releaseReservation(eq(tenantId), eq("reservation1"), anyString());
        verify(reservationService).releaseReservation(eq(tenantId), eq("reservation2"), anyString());
    }

    @Test
    void bulkUpdateExpiredReservations_WithExpiredReservations_ShouldUpdateStatus() {
        // Given
        when(reservationRepository.updateExpiredReservations(
            isNull(), 
            eq(InventoryReservation.ReservationStatus.ACTIVE),
            eq(InventoryReservation.ReservationStatus.EXPIRED),
            any(LocalDateTime.class)))
            .thenReturn(5);

        // When
        cleanupService.bulkUpdateExpiredReservations();

        // Then
        verify(reservationRepository).updateExpiredReservations(
            isNull(),
            eq(InventoryReservation.ReservationStatus.ACTIVE),
            eq(InventoryReservation.ReservationStatus.EXPIRED),
            any(LocalDateTime.class));
    }

    @Test
    void bulkUpdateExpiredReservations_WithNoExpiredReservations_ShouldDoNothing() {
        // Given
        when(reservationRepository.updateExpiredReservations(
            isNull(),
            eq(InventoryReservation.ReservationStatus.ACTIVE),
            eq(InventoryReservation.ReservationStatus.EXPIRED),
            any(LocalDateTime.class)))
            .thenReturn(0);

        // When
        cleanupService.bulkUpdateExpiredReservations();

        // Then
        verify(reservationRepository).updateExpiredReservations(
            isNull(),
            eq(InventoryReservation.ReservationStatus.ACTIVE),
            eq(InventoryReservation.ReservationStatus.EXPIRED),
            any(LocalDateTime.class));
    }

    @Test
    void bulkUpdateExpiredReservations_WithRepositoryException_ShouldHandleGracefully() {
        // Given
        when(reservationRepository.updateExpiredReservations(
            isNull(),
            eq(InventoryReservation.ReservationStatus.ACTIVE),
            eq(InventoryReservation.ReservationStatus.EXPIRED),
            any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw exception
        cleanupService.bulkUpdateExpiredReservations();

        verify(reservationRepository).updateExpiredReservations(
            isNull(),
            eq(InventoryReservation.ReservationStatus.ACTIVE),
            eq(InventoryReservation.ReservationStatus.EXPIRED),
            any(LocalDateTime.class));
    }

    @Test
    void logReservationStatistics_ShouldExecuteWithoutErrors() {
        // When & Then - Should not throw exception
        cleanupService.logReservationStatistics();

        // This method currently just logs, so we verify it completes without error
        // In a real implementation, you might verify specific logging calls or metrics
    }

    // Helper methods
    private InventoryReservation createExpiredReservation(String reservationId, String orderId) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setTenantId(tenantId);
        reservation.setReservationId(reservationId);
        reservation.setOrderId(orderId);
        reservation.setInventoryItemId(1L);
        reservation.setProductId("product123");
        reservation.setSku("SKU123");
        reservation.setReservedQuantity(10);
        reservation.setStatus(InventoryReservation.ReservationStatus.ACTIVE);
        reservation.setCreatedAt(LocalDateTime.now().minusHours(1));
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(10)); // Expired
        return reservation;
    }
}
package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.ReservationRequest;
import com.ecommerce.inventoryservice.dto.ReservationResponse;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockTransactionService stockTransactionService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private InventoryReservationService reservationService;

    private final String tenantId = "tenant123";
    private final String orderId = "order123";
    private final String productId = "product123";
    private final String sku = "SKU123";
    private InventoryItem testInventoryItem;
    private ReservationRequest testReservationRequest;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Set the private field using reflection to avoid @Value annotation issues in tests
        try {
            var field = InventoryReservationService.class.getDeclaredField("inventoryEventsTopic");
            field.setAccessible(true);
            field.set(reservationService, "inventory-events");
        } catch (Exception e) {
            // Ignore reflection errors in tests
        }
        
        testInventoryItem = createTestInventoryItem();
        testReservationRequest = createTestReservationRequest();
    }

    @Test
    void reserveInventory_WithSufficientStock_ShouldSucceed() {
        // Given
        when(reservationRepository.existsByTenantIdAndOrderId(tenantId, orderId)).thenReturn(false);
        when(inventoryItemRepository.findByTenantIdAndProductId(tenantId, productId))
            .thenReturn(Optional.of(testInventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(testInventoryItem);
        when(reservationRepository.save(any(InventoryReservation.class)))
            .thenReturn(createTestReservation());

        // When
        ReservationResponse result = reservationService.reserveInventory(tenantId, testReservationRequest);

        // Then
        assertNotNull(result);
        assertEquals(ReservationResponse.ReservationStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getReservationId());
        assertEquals(1, result.getReservedItems().size());
        
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(reservationRepository).save(any(InventoryReservation.class));
        verify(stockTransactionService).logTransaction(anyString(), anyLong(), any(), anyInt(), 
            anyString(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString());
        verify(kafkaTemplate).send(eq("inventory-events"), any());
    }

    @Test
    void reserveInventory_WithInsufficientStock_ShouldFail() {
        // Given
        testInventoryItem.setAvailableQuantity(5); // Less than requested 10
        when(reservationRepository.existsByTenantIdAndOrderId(tenantId, orderId)).thenReturn(false);
        when(inventoryItemRepository.findByTenantIdAndProductId(tenantId, productId))
            .thenReturn(Optional.of(testInventoryItem));

        // When
        ReservationResponse result = reservationService.reserveInventory(tenantId, testReservationRequest);

        // Then
        assertNotNull(result);
        assertEquals(ReservationResponse.ReservationStatus.FAILED, result.getStatus());
        assertEquals(1, result.getFailedItems().size());
        assertEquals("Insufficient stock for product product123. Requested: 10, Available: 5", 
                    result.getFailedItems().get(0).getFailureReason());
        
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
        verify(reservationRepository, never()).save(any(InventoryReservation.class));
        verify(kafkaTemplate).send(eq("inventory-events"), any());
    }

    @Test
    void reserveInventory_WhenProductNotFound_ShouldFail() {
        // Given
        when(reservationRepository.existsByTenantIdAndOrderId(tenantId, orderId)).thenReturn(false);
        when(inventoryItemRepository.findByTenantIdAndProductId(tenantId, productId))
            .thenReturn(Optional.empty());

        // When
        ReservationResponse result = reservationService.reserveInventory(tenantId, testReservationRequest);

        // Then
        assertNotNull(result);
        assertEquals(ReservationResponse.ReservationStatus.FAILED, result.getStatus());
        assertEquals(1, result.getFailedItems().size());
        assertTrue(result.getFailedItems().get(0).getFailureReason().contains("not found"));
        
        verify(kafkaTemplate).send(eq("inventory-events"), any());
    }

    @Test
    void reserveInventory_WhenReservationAlreadyExists_ShouldThrowException() {
        // Given
        when(reservationRepository.existsByTenantIdAndOrderId(tenantId, orderId)).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> reservationService.reserveInventory(tenantId, testReservationRequest));
        
        verify(inventoryItemRepository, never()).findByTenantIdAndProductId(anyString(), anyString());
    }

    @Test
    void confirmReservation_WithValidReservation_ShouldSucceed() {
        // Given
        String reservationId = "reservation123";
        InventoryReservation reservation = createTestReservation();
        reservation.setStatus(InventoryReservation.ReservationStatus.ACTIVE);
        
        when(reservationRepository.findActiveReservationsByOrder(tenantId, orderId))
            .thenReturn(Arrays.asList(reservation));
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, reservation.getInventoryItemId()))
            .thenReturn(Optional.of(testInventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(testInventoryItem);
        when(reservationRepository.save(any(InventoryReservation.class))).thenReturn(reservation);

        // Mock cache lookup to return null, then mock database lookup
        when(valueOperations.get(anyString())).thenReturn(null);
        when(reservationRepository.findByTenantIdAndReservationId(tenantId, reservationId))
            .thenReturn(Optional.of(reservation));

        // When
        reservationService.confirmReservation(tenantId, reservationId);

        // Then
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(reservationRepository).save(any(InventoryReservation.class));
        verify(stockTransactionService).logTransaction(anyString(), anyLong(), any(), anyInt(), 
            anyString(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void confirmReservation_WhenReservationNotFound_ShouldThrowException() {
        // Given
        String reservationId = "reservation123";
        lenient().when(reservationRepository.findActiveReservationsByOrder(eq(tenantId), anyString()))
            .thenReturn(Arrays.asList());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> reservationService.confirmReservation(tenantId, reservationId));
    }

    @Test
    void releaseReservation_WithValidReservation_ShouldSucceed() {
        // Given
        String reservationId = "reservation123";
        String reason = "Order cancelled";
        InventoryReservation reservation = createTestReservation();
        reservation.setStatus(InventoryReservation.ReservationStatus.ACTIVE);
        
        when(reservationRepository.findActiveReservationsByOrder(tenantId, orderId))
            .thenReturn(Arrays.asList(reservation));
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, reservation.getInventoryItemId()))
            .thenReturn(Optional.of(testInventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(testInventoryItem);
        when(reservationRepository.save(any(InventoryReservation.class))).thenReturn(reservation);

        // Mock cache lookup to return null, then mock database lookup
        when(valueOperations.get(anyString())).thenReturn(null);
        when(reservationRepository.findByTenantIdAndReservationId(tenantId, reservationId))
            .thenReturn(Optional.of(reservation));

        // When
        reservationService.releaseReservation(tenantId, reservationId, reason);

        // Then
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(reservationRepository).save(any(InventoryReservation.class));
        verify(stockTransactionService).logTransaction(anyString(), anyLong(), any(), anyInt(), 
            anyString(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString());
        verify(kafkaTemplate).send(anyString(), any());
    }

    @Test
    void getReservation_FromCache_ShouldReturnCachedData() {
        // Given
        String reservationId = "reservation123";
        ReservationResponse cachedResponse = ReservationResponse.success(reservationId, Arrays.asList());
        when(valueOperations.get(anyString())).thenReturn(cachedResponse);

        // When
        ReservationResponse result = reservationService.getReservation(tenantId, reservationId);

        // Then
        assertNotNull(result);
        assertEquals(reservationId, result.getReservationId());
        verify(reservationRepository, never()).findActiveReservationsByOrder(anyString(), anyString());
    }

    @Test
    void cleanupExpiredReservations_ShouldProcessExpiredReservations() {
        // Given
        InventoryReservation expiredReservation = createTestReservation();
        expiredReservation.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        
        when(reservationRepository.findExpiredReservations(eq(tenantId), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(expiredReservation));
        when(reservationRepository.findActiveReservationsByOrder(tenantId, orderId))
            .thenReturn(Arrays.asList(expiredReservation));
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, expiredReservation.getInventoryItemId()))
            .thenReturn(Optional.of(testInventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(testInventoryItem);
        when(reservationRepository.save(any(InventoryReservation.class))).thenReturn(expiredReservation);
        
        // Mock cache and database lookup for reservation ID
        when(valueOperations.get(anyString())).thenReturn(null);
        when(reservationRepository.findByTenantIdAndReservationId(tenantId, expiredReservation.getReservationId()))
            .thenReturn(Optional.of(expiredReservation));

        // When
        reservationService.cleanupExpiredReservations(tenantId);

        // Then
        verify(reservationRepository).findExpiredReservations(eq(tenantId), any(LocalDateTime.class));
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(reservationRepository).save(any(InventoryReservation.class));
    }

    // Helper methods
    private InventoryItem createTestInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setId(1L);
        item.setTenantId(tenantId);
        item.setProductId(productId);
        item.setSku(sku);
        item.setAvailableQuantity(100);
        item.setReservedQuantity(10); // Set some reserved quantity for release/confirm tests
        item.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        return item;
    }

    private ReservationRequest createTestReservationRequest() {
        ReservationRequest.ReservationItemRequest itemRequest = 
            new ReservationRequest.ReservationItemRequest(productId, sku, 10);
        return new ReservationRequest(orderId, Arrays.asList(itemRequest));
    }

    private InventoryReservation createTestReservation() {
        return new InventoryReservation(
            tenantId, "reservation123", orderId, 1L, productId, sku, 10, 
            LocalDateTime.now().plusMinutes(30));
    }
}
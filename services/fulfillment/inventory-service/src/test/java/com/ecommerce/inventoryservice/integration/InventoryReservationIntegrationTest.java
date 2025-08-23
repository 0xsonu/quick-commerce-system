package com.ecommerce.inventoryservice.integration;

import com.ecommerce.inventoryservice.dto.ReservationRequest;
import com.ecommerce.inventoryservice.dto.ReservationResponse;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.ecommerce.inventoryservice.service.InventoryReservationService;
import com.ecommerce.shared.models.events.InventoryReservedEvent;
import com.ecommerce.shared.models.events.InventoryReservationFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class InventoryReservationIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("inventory_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private InventoryReservationService reservationService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final String tenantId = "test-tenant";
    private final String orderId = "test-order-123";
    private final String productId1 = "product-1";
    private final String productId2 = "product-2";
    private final String sku1 = "SKU-001";
    private final String sku2 = "SKU-002";

    private InventoryItem inventoryItem1;
    private InventoryItem inventoryItem2;

    @BeforeEach
    void setUp() {
        // Clean up data
        reservationRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test inventory items
        inventoryItem1 = createInventoryItem(productId1, sku1, 100, 0);
        inventoryItem2 = createInventoryItem(productId2, sku2, 50, 0);

        inventoryItem1 = inventoryItemRepository.save(inventoryItem1);
        inventoryItem2 = inventoryItemRepository.save(inventoryItem2);
    }

    @Test
    void reserveInventory_WithSufficientStock_ShouldSucceed() {
        // Given
        ReservationRequest request = createReservationRequest(
            Arrays.asList(
                new ReservationRequest.ReservationItemRequest(productId1, sku1, 10),
                new ReservationRequest.ReservationItemRequest(productId2, sku2, 5)
            )
        );

        // When
        ReservationResponse response = reservationService.reserveInventory(tenantId, request);

        // Then
        assertNotNull(response);
        assertEquals(ReservationResponse.ReservationStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getReservationId());
        assertEquals(2, response.getReservedItems().size());

        // Verify database state
        InventoryItem updatedItem1 = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(90, updatedItem1.getAvailableQuantity());
        assertEquals(10, updatedItem1.getReservedQuantity());

        InventoryItem updatedItem2 = inventoryItemRepository.findById(inventoryItem2.getId()).orElseThrow();
        assertEquals(45, updatedItem2.getAvailableQuantity());
        assertEquals(5, updatedItem2.getReservedQuantity());

        // Verify reservations created
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(2, reservations.size());

        // Verify Kafka event published
        verify(kafkaTemplate).send(anyString(), any(InventoryReservedEvent.class));
    }

    @Test
    void reserveInventory_WithInsufficientStock_ShouldFail() {
        // Given
        ReservationRequest request = createReservationRequest(
            Arrays.asList(
                new ReservationRequest.ReservationItemRequest(productId1, sku1, 10),
                new ReservationRequest.ReservationItemRequest(productId2, sku2, 100) // Exceeds available
            )
        );

        // When
        ReservationResponse response = reservationService.reserveInventory(tenantId, request);

        // Then
        assertNotNull(response);
        assertEquals(ReservationResponse.ReservationStatus.FAILED, response.getStatus());
        assertEquals(1, response.getFailedItems().size());
        assertTrue(response.getFailedItems().get(0).getFailureReason().contains("Insufficient stock"));

        // Verify no changes to inventory
        InventoryItem unchangedItem1 = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(100, unchangedItem1.getAvailableQuantity());
        assertEquals(0, unchangedItem1.getReservedQuantity());

        InventoryItem unchangedItem2 = inventoryItemRepository.findById(inventoryItem2.getId()).orElseThrow();
        assertEquals(50, unchangedItem2.getAvailableQuantity());
        assertEquals(0, unchangedItem2.getReservedQuantity());

        // Verify no reservations created
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(0, reservations.size());

        // Verify failure event published
        verify(kafkaTemplate).send(anyString(), any(InventoryReservationFailedEvent.class));
    }

    @Test
    void confirmReservation_WithValidReservation_ShouldSucceed() {
        // Given - First create a reservation
        ReservationRequest request = createReservationRequest(
            Arrays.asList(new ReservationRequest.ReservationItemRequest(productId1, sku1, 10))
        );
        ReservationResponse reservationResponse = reservationService.reserveInventory(tenantId, request);
        String reservationId = reservationResponse.getReservationId();

        // When
        reservationService.confirmReservation(tenantId, reservationId);

        // Then
        // Verify inventory state - reserved quantity should be reduced, available stays same
        InventoryItem updatedItem = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(90, updatedItem.getAvailableQuantity()); // Still reduced from reservation
        assertEquals(0, updatedItem.getReservedQuantity()); // Reserved quantity cleared

        // Verify reservation status
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(0, reservations.size()); // No active reservations

        Optional<InventoryReservation> confirmedReservation = reservationRepository
            .findByTenantIdAndReservationId(tenantId, reservationId);
        assertTrue(confirmedReservation.isPresent());
        assertEquals(InventoryReservation.ReservationStatus.CONFIRMED, confirmedReservation.get().getStatus());
    }

    @Test
    void releaseReservation_WithValidReservation_ShouldSucceed() {
        // Given - First create a reservation
        ReservationRequest request = createReservationRequest(
            Arrays.asList(new ReservationRequest.ReservationItemRequest(productId1, sku1, 10))
        );
        ReservationResponse reservationResponse = reservationService.reserveInventory(tenantId, request);
        String reservationId = reservationResponse.getReservationId();

        // When
        reservationService.releaseReservation(tenantId, reservationId, "Test release");

        // Then
        // Verify inventory state - stock should be returned
        InventoryItem updatedItem = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(100, updatedItem.getAvailableQuantity()); // Stock returned
        assertEquals(0, updatedItem.getReservedQuantity()); // Reserved quantity cleared

        // Verify reservation status
        List<InventoryReservation> activeReservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(0, activeReservations.size()); // No active reservations

        Optional<InventoryReservation> releasedReservation = reservationRepository
            .findByTenantIdAndReservationId(tenantId, reservationId);
        assertTrue(releasedReservation.isPresent());
        assertEquals(InventoryReservation.ReservationStatus.RELEASED, releasedReservation.get().getStatus());
    }

    @Test
    void getReservation_WithValidId_ShouldReturnReservation() {
        // Given - First create a reservation
        ReservationRequest request = createReservationRequest(
            Arrays.asList(new ReservationRequest.ReservationItemRequest(productId1, sku1, 10))
        );
        ReservationResponse createdReservation = reservationService.reserveInventory(tenantId, request);
        String reservationId = createdReservation.getReservationId();

        // When
        ReservationResponse retrievedReservation = reservationService.getReservation(tenantId, reservationId);

        // Then
        assertNotNull(retrievedReservation);
        assertEquals(reservationId, retrievedReservation.getReservationId());
        assertEquals(ReservationResponse.ReservationStatus.SUCCESS, retrievedReservation.getStatus());
        assertEquals(1, retrievedReservation.getReservedItems().size());
    }

    @Test
    void cleanupExpiredReservations_ShouldReleaseExpiredReservations() {
        // Given - Create a reservation and manually expire it
        ReservationRequest request = createReservationRequest(
            Arrays.asList(new ReservationRequest.ReservationItemRequest(productId1, sku1, 10))
        );
        ReservationResponse reservationResponse = reservationService.reserveInventory(tenantId, request);

        // Manually expire the reservation
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        InventoryReservation reservation = reservations.get(0);
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        reservationRepository.save(reservation);

        // When
        reservationService.cleanupExpiredReservations(tenantId);

        // Then
        // Verify inventory state - stock should be returned
        InventoryItem updatedItem = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(100, updatedItem.getAvailableQuantity()); // Stock returned
        assertEquals(0, updatedItem.getReservedQuantity()); // Reserved quantity cleared

        // Verify no active reservations
        List<InventoryReservation> activeReservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(0, activeReservations.size());
    }

    @Test
    void reserveInventory_WithDuplicateOrder_ShouldThrowException() {
        // Given - First create a reservation
        ReservationRequest request = createReservationRequest(
            Arrays.asList(new ReservationRequest.ReservationItemRequest(productId1, sku1, 10))
        );
        reservationService.reserveInventory(tenantId, request);

        // When & Then - Try to create another reservation for same order
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.reserveInventory(tenantId, request);
        });
    }

    @Test
    void reserveInventory_WithRedisCache_ShouldCacheReservationData() {
        // Given
        ReservationRequest request = createReservationRequest(
            Arrays.asList(new ReservationRequest.ReservationItemRequest(productId1, sku1, 10))
        );

        // When
        ReservationResponse response = reservationService.reserveInventory(tenantId, request);

        // Then
        // Verify data is cached in Redis
        String cacheKey = "reservation:" + tenantId + ":" + response.getReservationId();
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cachedData);
    }

    // Helper methods
    private InventoryItem createInventoryItem(String productId, String sku, int available, int reserved) {
        InventoryItem item = new InventoryItem();
        item.setTenantId(tenantId);
        item.setProductId(productId);
        item.setSku(sku);
        item.setAvailableQuantity(available);
        item.setReservedQuantity(reserved);
        item.setReorderLevel(10);
        item.setMaxStockLevel(1000);
        item.setLocationCode("WAREHOUSE-A");
        item.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        return item;
    }

    private ReservationRequest createReservationRequest(List<ReservationRequest.ReservationItemRequest> items) {
        return new ReservationRequest(orderId, items);
    }
}
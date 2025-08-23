package com.ecommerce.inventoryservice.integration;

import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.ecommerce.inventoryservice.service.OrderEventConsumer;
import com.ecommerce.shared.models.events.OrderCancelledEvent;
import com.ecommerce.shared.models.events.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class OrderEventIntegrationTest {

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
    private OrderEventConsumer orderEventConsumer;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private Acknowledgment acknowledgment;

    private final String tenantId = "test-tenant";
    private final String orderId = "test-order-123";
    private final String userId = "test-user-456";
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

        // Create test inventory items
        inventoryItem1 = createInventoryItem(productId1, sku1, 100, 0);
        inventoryItem2 = createInventoryItem(productId2, sku2, 50, 0);

        inventoryItem1 = inventoryItemRepository.save(inventoryItem1);
        inventoryItem2 = inventoryItemRepository.save(inventoryItem2);
    }

    @Test
    void handleOrderCreated_WithSufficientStock_ShouldReserveInventory() {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCreated(
            event, "order-events", 0, 0L, acknowledgment));

        // Then
        // Verify inventory was reserved
        InventoryItem updatedItem1 = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(98, updatedItem1.getAvailableQuantity()); // 100 - 2
        assertEquals(2, updatedItem1.getReservedQuantity());

        InventoryItem updatedItem2 = inventoryItemRepository.findById(inventoryItem2.getId()).orElseThrow();
        assertEquals(49, updatedItem2.getAvailableQuantity()); // 50 - 1
        assertEquals(1, updatedItem2.getReservedQuantity());

        // Verify reservations were created
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(2, reservations.size());

        // Verify acknowledgment was called
        verify(acknowledgment).acknowledge();

        // Verify Kafka event was published
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), any());
    }

    @Test
    void handleOrderCreated_WithInsufficientStock_ShouldFailGracefully() {
        // Given - Create an order that exceeds available stock
        List<OrderCreatedEvent.OrderItemData> items = Arrays.asList(
            new OrderCreatedEvent.OrderItemData(productId1, sku1, 150, new BigDecimal("10.00")), // Exceeds available
            new OrderCreatedEvent.OrderItemData(productId2, sku2, 1, new BigDecimal("20.00"))
        );
        OrderCreatedEvent event = new OrderCreatedEvent(tenantId, orderId, userId, items, 
                                                       new BigDecimal("170.00"), "PENDING");

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCreated(
            event, "order-events", 0, 0L, acknowledgment));

        // Then
        // Verify no inventory was reserved (rollback occurred)
        InventoryItem unchangedItem1 = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(100, unchangedItem1.getAvailableQuantity());
        assertEquals(0, unchangedItem1.getReservedQuantity());

        InventoryItem unchangedItem2 = inventoryItemRepository.findById(inventoryItem2.getId()).orElseThrow();
        assertEquals(50, unchangedItem2.getAvailableQuantity());
        assertEquals(0, unchangedItem2.getReservedQuantity());

        // Verify no reservations were created
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(0, reservations.size());

        // Verify acknowledgment was still called (event processed)
        verify(acknowledgment).acknowledge();

        // Verify failure event was published
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), any());
    }

    @Test
    void handleOrderCreated_WithNonExistentProduct_ShouldFailGracefully() {
        // Given - Create an order with non-existent product
        List<OrderCreatedEvent.OrderItemData> items = Arrays.asList(
            new OrderCreatedEvent.OrderItemData("non-existent-product", "NON-EXISTENT", 1, new BigDecimal("10.00"))
        );
        OrderCreatedEvent event = new OrderCreatedEvent(tenantId, orderId, userId, items, 
                                                       new BigDecimal("10.00"), "PENDING");

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCreated(
            event, "order-events", 0, 0L, acknowledgment));

        // Then
        // Verify no reservations were created
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(0, reservations.size());

        // Verify acknowledgment was called
        verify(acknowledgment).acknowledge();

        // Verify failure event was published
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), any());
    }

    @Test
    void handleOrderCancelled_WithExistingReservations_ShouldReleaseInventory() {
        // Given - First create reservations by processing an order created event
        OrderCreatedEvent createdEvent = createOrderCreatedEvent();
        orderEventConsumer.handleOrderCreated(createdEvent, "order-events", 0, 0L, acknowledgment);

        // Verify reservations exist
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(2, reservations.size());

        // Reset mock to clear previous interactions
        reset(acknowledgment, kafkaTemplate);

        // Create cancellation event
        OrderCancelledEvent cancelledEvent = createOrderCancelledEvent();

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCancelled(
            cancelledEvent, "order-events", 0, 0L, acknowledgment));

        // Then
        // Verify inventory was released
        InventoryItem updatedItem1 = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(100, updatedItem1.getAvailableQuantity()); // Stock returned
        assertEquals(0, updatedItem1.getReservedQuantity());

        InventoryItem updatedItem2 = inventoryItemRepository.findById(inventoryItem2.getId()).orElseThrow();
        assertEquals(50, updatedItem2.getAvailableQuantity()); // Stock returned
        assertEquals(0, updatedItem2.getReservedQuantity());

        // Verify no active reservations remain
        List<InventoryReservation> activeReservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(0, activeReservations.size());

        // Verify acknowledgment was called
        verify(acknowledgment).acknowledge();

        // Verify release event was published
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), any());
    }

    @Test
    void handleOrderCancelled_WithNoReservations_ShouldHandleGracefully() {
        // Given - Order cancellation event without existing reservations
        OrderCancelledEvent event = createOrderCancelledEvent();

        // When
        assertDoesNotThrow(() -> orderEventConsumer.handleOrderCancelled(
            event, "order-events", 0, 0L, acknowledgment));

        // Then
        // Verify inventory remains unchanged
        InventoryItem unchangedItem1 = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(100, unchangedItem1.getAvailableQuantity());
        assertEquals(0, unchangedItem1.getReservedQuantity());

        // Verify acknowledgment was called
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleOrderCreated_WithDuplicateEvent_ShouldFailGracefully() {
        // Given - Process the same order twice
        OrderCreatedEvent event = createOrderCreatedEvent();

        // First processing
        orderEventConsumer.handleOrderCreated(event, "order-events", 0, 0L, acknowledgment);

        // Reset mocks
        reset(acknowledgment);

        // When - Process the same event again
        assertThrows(Exception.class, () -> orderEventConsumer.handleOrderCreated(
            event, "order-events", 0, 0L, acknowledgment));

        // Then - Acknowledgment should not be called for failed processing
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void handleOrderCreated_WithPartialFailure_ShouldRollbackAll() {
        // Given - Create an order where one item succeeds and one fails
        List<OrderCreatedEvent.OrderItemData> items = Arrays.asList(
            new OrderCreatedEvent.OrderItemData(productId1, sku1, 10, new BigDecimal("10.00")), // Should succeed
            new OrderCreatedEvent.OrderItemData(productId2, sku2, 100, new BigDecimal("20.00")) // Should fail (exceeds stock)
        );
        OrderCreatedEvent event = new OrderCreatedEvent(tenantId, orderId, userId, items, 
                                                       new BigDecimal("1010.00"), "PENDING");

        // When
        orderEventConsumer.handleOrderCreated(event, "order-events", 0, 0L, acknowledgment);

        // Then
        // Verify all reservations were rolled back (no partial reservations)
        InventoryItem unchangedItem1 = inventoryItemRepository.findById(inventoryItem1.getId()).orElseThrow();
        assertEquals(100, unchangedItem1.getAvailableQuantity());
        assertEquals(0, unchangedItem1.getReservedQuantity());

        InventoryItem unchangedItem2 = inventoryItemRepository.findById(inventoryItem2.getId()).orElseThrow();
        assertEquals(50, unchangedItem2.getAvailableQuantity());
        assertEquals(0, unchangedItem2.getReservedQuantity());

        // Verify no reservations exist
        List<InventoryReservation> reservations = reservationRepository
            .findActiveReservationsByOrder(tenantId, orderId);
        assertEquals(0, reservations.size());

        // Verify acknowledgment was called (event was processed, even though it failed)
        verify(acknowledgment).acknowledge();
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

    private OrderCreatedEvent createOrderCreatedEvent() {
        List<OrderCreatedEvent.OrderItemData> items = Arrays.asList(
            new OrderCreatedEvent.OrderItemData(productId1, sku1, 2, new BigDecimal("10.00")),
            new OrderCreatedEvent.OrderItemData(productId2, sku2, 1, new BigDecimal("20.00"))
        );
        return new OrderCreatedEvent(tenantId, orderId, userId, items, new BigDecimal("40.00"), "PENDING");
    }

    private OrderCancelledEvent createOrderCancelledEvent() {
        List<OrderCancelledEvent.OrderItemData> items = Arrays.asList(
            new OrderCancelledEvent.OrderItemData(productId1, sku1, 2),
            new OrderCancelledEvent.OrderItemData(productId2, sku2, 1)
        );
        return new OrderCancelledEvent(tenantId, orderId, userId, items, "Customer requested cancellation");
    }
}
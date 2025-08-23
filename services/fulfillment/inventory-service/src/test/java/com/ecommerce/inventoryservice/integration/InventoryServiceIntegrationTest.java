package com.ecommerce.inventoryservice.integration;

import com.ecommerce.inventoryservice.dto.CreateInventoryItemRequest;
import com.ecommerce.inventoryservice.dto.InventoryItemResponse;
import com.ecommerce.inventoryservice.dto.StockAdjustmentRequest;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.StockTransactionRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InventoryServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("inventory_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    private final String tenantId = "test-tenant";

    @BeforeEach
    void setUp() {
        // Clean up data before each test
        stockTransactionRepository.deleteAll();
        inventoryItemRepository.deleteAll();
    }

    @Test
    @Transactional
    void createInventoryItem_ShouldPersistCorrectly() {
        // Given
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setProductId("product-001");
        request.setSku("SKU-001");
        request.setInitialQuantity(100);
        request.setReorderLevel(10);
        request.setMaxStockLevel(1000);
        request.setLocationCode("WH-001");
        request.setStatus(InventoryItem.InventoryStatus.ACTIVE);

        // When
        InventoryItemResponse response = inventoryService.createInventoryItem(tenantId, request);

        // Then
        assertNotNull(response);
        assertEquals(request.getProductId(), response.getProductId());
        assertEquals(request.getSku(), response.getSku());
        assertEquals(request.getInitialQuantity(), response.getAvailableQuantity());
        assertEquals(0, response.getReservedQuantity());
        assertEquals(request.getReorderLevel(), response.getReorderLevel());
        assertEquals(request.getMaxStockLevel(), response.getMaxStockLevel());
        assertEquals(request.getLocationCode(), response.getLocationCode());
        assertEquals(request.getStatus(), response.getStatus());

        // Verify persistence
        InventoryItem savedItem = inventoryItemRepository.findByTenantIdAndProductId(tenantId, request.getProductId())
                .orElseThrow();
        assertEquals(request.getProductId(), savedItem.getProductId());
        assertEquals(request.getInitialQuantity(), savedItem.getAvailableQuantity());

        // Verify transaction log
        long transactionCount = stockTransactionRepository.countByTenantIdAndInventoryItemId(tenantId, savedItem.getId());
        assertEquals(1, transactionCount); // Initial stock transaction
    }

    @Test
    void concurrentStockUpdates_ShouldHandleOptimisticLocking() throws InterruptedException, ExecutionException {
        // Given - Create an inventory item first
        CreateInventoryItemRequest createRequest = new CreateInventoryItemRequest();
        createRequest.setProductId("product-concurrent");
        createRequest.setSku("SKU-concurrent");
        createRequest.setInitialQuantity(1000);
        createRequest.setReorderLevel(10);
        createRequest.setMaxStockLevel(2000);
        createRequest.setLocationCode("WH-001");

        InventoryItemResponse createdItem = inventoryService.createInventoryItem(tenantId, createRequest);
        Long itemId = createdItem.getId();

        // Prepare concurrent stock adjustments
        int numberOfThreads = 10;
        int stockToAdd = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - Execute concurrent stock additions
        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfThreads];
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    StockAdjustmentRequest request = new StockAdjustmentRequest();
                    request.setQuantity(stockToAdd);
                    request.setReason("Concurrent test " + threadIndex);
                    request.setReferenceId("REF-" + threadIndex);
                    request.setReferenceType("TEST");

                    inventoryService.addStock(tenantId, itemId, request);
                    successCount.incrementAndGet();
                } catch (OptimisticLockingFailureException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Unexpected error in thread " + threadIndex + ": " + e.getMessage());
                    failureCount.incrementAndGet();
                }
            }, executor);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures).get();
        executor.shutdown();

        // Then - Verify results
        InventoryItemResponse finalItem = inventoryService.getInventoryItem(tenantId, itemId);
        
        // The final quantity should be initial + (successful operations * stockToAdd)
        int expectedQuantity = createRequest.getInitialQuantity() + (successCount.get() * stockToAdd);
        assertEquals(expectedQuantity, finalItem.getAvailableQuantity());

        // Verify that we had some concurrent operations (success + retries should equal total threads)
        assertTrue(successCount.get() > 0, "At least some operations should succeed");
        
        // Verify transaction count matches successful operations + initial stock transaction
        long transactionCount = stockTransactionRepository.countByTenantIdAndInventoryItemId(tenantId, itemId);
        assertEquals(successCount.get() + 1, transactionCount); // +1 for initial stock transaction

        System.out.println("Concurrent test results:");
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + failureCount.get());
        System.out.println("Final quantity: " + finalItem.getAvailableQuantity());
        System.out.println("Transaction count: " + transactionCount);
    }

    @Test
    @Transactional
    void stockReservationAndRelease_ShouldMaintainConsistency() {
        // Given - Create inventory item with sufficient stock
        CreateInventoryItemRequest createRequest = new CreateInventoryItemRequest();
        createRequest.setProductId("product-reservation");
        createRequest.setSku("SKU-reservation");
        createRequest.setInitialQuantity(100);
        createRequest.setReorderLevel(10);

        InventoryItemResponse createdItem = inventoryService.createInventoryItem(tenantId, createRequest);
        Long itemId = createdItem.getId();

        // When - Test reservation operations
        InventoryItem item = inventoryItemRepository.findByTenantIdAndId(tenantId, itemId).orElseThrow();
        
        // Reserve stock
        int reserveQuantity = 30;
        int initialAvailable = item.getAvailableQuantity();
        int initialReserved = item.getReservedQuantity();
        
        item.reserveStock(reserveQuantity);
        inventoryItemRepository.save(item);

        // Then - Verify reservation
        InventoryItem reservedItem = inventoryItemRepository.findByTenantIdAndId(tenantId, itemId).orElseThrow();
        assertEquals(initialAvailable - reserveQuantity, reservedItem.getAvailableQuantity());
        assertEquals(initialReserved + reserveQuantity, reservedItem.getReservedQuantity());

        // When - Release reservation
        int releaseQuantity = 10;
        reservedItem.releaseReservation(releaseQuantity);
        inventoryItemRepository.save(reservedItem);

        // Then - Verify release
        InventoryItem releasedItem = inventoryItemRepository.findByTenantIdAndId(tenantId, itemId).orElseThrow();
        assertEquals(initialAvailable - reserveQuantity + releaseQuantity, releasedItem.getAvailableQuantity());
        assertEquals(initialReserved + reserveQuantity - releaseQuantity, releasedItem.getReservedQuantity());

        // When - Confirm remaining reservation
        int confirmQuantity = reserveQuantity - releaseQuantity;
        releasedItem.confirmReservation(confirmQuantity);
        inventoryItemRepository.save(releasedItem);

        // Then - Verify confirmation
        InventoryItem confirmedItem = inventoryItemRepository.findByTenantIdAndId(tenantId, itemId).orElseThrow();
        assertEquals(initialAvailable - reserveQuantity + releaseQuantity, confirmedItem.getAvailableQuantity());
        assertEquals(0, confirmedItem.getReservedQuantity());
    }

    @Test
    @Transactional
    void tenantIsolation_ShouldPreventCrossTenantAccess() {
        // Given - Create items for different tenants
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        CreateInventoryItemRequest request1 = new CreateInventoryItemRequest();
        request1.setProductId("product-tenant1");
        request1.setSku("SKU-tenant1");
        request1.setInitialQuantity(100);

        CreateInventoryItemRequest request2 = new CreateInventoryItemRequest();
        request2.setProductId("product-tenant2");
        request2.setSku("SKU-tenant2");
        request2.setInitialQuantity(200);

        InventoryItemResponse item1 = inventoryService.createInventoryItem(tenant1, request1);
        InventoryItemResponse item2 = inventoryService.createInventoryItem(tenant2, request2);

        // When & Then - Verify tenant isolation
        // Tenant 1 should only see their item
        InventoryItemResponse retrievedItem1 = inventoryService.getInventoryItem(tenant1, item1.getId());
        assertEquals(item1.getId(), retrievedItem1.getId());
        assertEquals(request1.getProductId(), retrievedItem1.getProductId());

        // Tenant 2 should only see their item
        InventoryItemResponse retrievedItem2 = inventoryService.getInventoryItem(tenant2, item2.getId());
        assertEquals(item2.getId(), retrievedItem2.getId());
        assertEquals(request2.getProductId(), retrievedItem2.getProductId());

        // Cross-tenant access should fail
        assertThrows(Exception.class, () -> inventoryService.getInventoryItem(tenant1, item2.getId()));
        assertThrows(Exception.class, () -> inventoryService.getInventoryItem(tenant2, item1.getId()));

        // Verify repository-level isolation
        assertTrue(inventoryItemRepository.findByTenantIdAndId(tenant1, item1.getId()).isPresent());
        assertFalse(inventoryItemRepository.findByTenantIdAndId(tenant1, item2.getId()).isPresent());
        
        assertTrue(inventoryItemRepository.findByTenantIdAndId(tenant2, item2.getId()).isPresent());
        assertFalse(inventoryItemRepository.findByTenantIdAndId(tenant2, item1.getId()).isPresent());
    }

    @Test
    @Transactional
    void lowStockDetection_ShouldWorkCorrectly() {
        // Given - Create item with reorder level
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setProductId("product-lowstock");
        request.setSku("SKU-lowstock");
        request.setInitialQuantity(50);
        request.setReorderLevel(20);

        InventoryItemResponse createdItem = inventoryService.createInventoryItem(tenantId, request);

        // When - Reduce stock below reorder level
        StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
        adjustment.setQuantity(35); // This will leave 15, which is below reorder level of 20
        adjustment.setReason("Sale");
        adjustment.setReferenceType("ORDER");

        inventoryService.removeStock(tenantId, createdItem.getId(), adjustment);

        // Then - Verify low stock detection
        var lowStockItems = inventoryService.getLowStockItems(tenantId);
        assertEquals(1, lowStockItems.size());
        assertEquals(createdItem.getId(), lowStockItems.get(0).getId());
        assertTrue(lowStockItems.get(0).isLowStock());
    }
}
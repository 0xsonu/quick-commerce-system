package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class InventoryItemRepositoryTest {

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
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    private final String tenantId1 = "tenant-1";
    private final String tenantId2 = "tenant-2";

    @BeforeEach
    void setUp() {
        // Clean up before each test
        inventoryItemRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByTenantIdAndProductId_ShouldReturnCorrectItem() {
        // Given
        InventoryItem item1 = createInventoryItem(tenantId1, "product-1", "SKU-1", 100, 10);
        InventoryItem item2 = createInventoryItem(tenantId1, "product-2", "SKU-2", 200, 20);
        InventoryItem item3 = createInventoryItem(tenantId2, "product-1", "SKU-3", 300, 30);

        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);
        entityManager.persistAndFlush(item3);

        // When
        Optional<InventoryItem> result = inventoryItemRepository.findByTenantIdAndProductId(tenantId1, "product-1");

        // Then
        assertTrue(result.isPresent());
        assertEquals(item1.getId(), result.get().getId());
        assertEquals("product-1", result.get().getProductId());
        assertEquals(tenantId1, result.get().getTenantId());
    }

    @Test
    void findByTenantIdAndSku_ShouldReturnCorrectItem() {
        // Given
        InventoryItem item = createInventoryItem(tenantId1, "product-1", "SKU-unique", 100, 10);
        entityManager.persistAndFlush(item);

        // When
        Optional<InventoryItem> result = inventoryItemRepository.findByTenantIdAndSku(tenantId1, "SKU-unique");

        // Then
        assertTrue(result.isPresent());
        assertEquals(item.getId(), result.get().getId());
        assertEquals("SKU-unique", result.get().getSku());
    }

    @Test
    void findByTenantIdAndStatus_ShouldReturnFilteredItems() {
        // Given
        InventoryItem activeItem1 = createInventoryItem(tenantId1, "product-1", "SKU-1", 100, 10);
        activeItem1.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        
        InventoryItem activeItem2 = createInventoryItem(tenantId1, "product-2", "SKU-2", 200, 20);
        activeItem2.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        
        InventoryItem inactiveItem = createInventoryItem(tenantId1, "product-3", "SKU-3", 300, 30);
        inactiveItem.setStatus(InventoryItem.InventoryStatus.INACTIVE);

        entityManager.persistAndFlush(activeItem1);
        entityManager.persistAndFlush(activeItem2);
        entityManager.persistAndFlush(inactiveItem);

        // When
        Page<InventoryItem> result = inventoryItemRepository.findByTenantIdAndStatus(
            tenantId1, InventoryItem.InventoryStatus.ACTIVE, PageRequest.of(0, 10));

        // Then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .allMatch(item -> item.getStatus() == InventoryItem.InventoryStatus.ACTIVE));
    }

    @Test
    void findByTenantIdAndLocationCode_ShouldReturnFilteredItems() {
        // Given
        InventoryItem item1 = createInventoryItem(tenantId1, "product-1", "SKU-1", 100, 10);
        item1.setLocationCode("WH-001");
        
        InventoryItem item2 = createInventoryItem(tenantId1, "product-2", "SKU-2", 200, 20);
        item2.setLocationCode("WH-001");
        
        InventoryItem item3 = createInventoryItem(tenantId1, "product-3", "SKU-3", 300, 30);
        item3.setLocationCode("WH-002");

        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);
        entityManager.persistAndFlush(item3);

        // When
        Page<InventoryItem> result = inventoryItemRepository.findByTenantIdAndLocationCode(
            tenantId1, "WH-001", PageRequest.of(0, 10));

        // Then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .allMatch(item -> "WH-001".equals(item.getLocationCode())));
    }

    @Test
    void findLowStockItems_ShouldReturnItemsBelowReorderLevel() {
        // Given
        InventoryItem lowStockItem1 = createInventoryItem(tenantId1, "product-1", "SKU-1", 5, 10); // Below reorder level
        InventoryItem lowStockItem2 = createInventoryItem(tenantId1, "product-2", "SKU-2", 8, 10); // Below reorder level
        InventoryItem normalStockItem = createInventoryItem(tenantId1, "product-3", "SKU-3", 15, 10); // Above reorder level
        InventoryItem noReorderLevelItem = createInventoryItem(tenantId1, "product-4", "SKU-4", 5, null); // No reorder level

        entityManager.persistAndFlush(lowStockItem1);
        entityManager.persistAndFlush(lowStockItem2);
        entityManager.persistAndFlush(normalStockItem);
        entityManager.persistAndFlush(noReorderLevelItem);

        // When
        List<InventoryItem> result = inventoryItemRepository.findLowStockItems(tenantId1);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(item -> item.getId().equals(lowStockItem1.getId())));
        assertTrue(result.stream().anyMatch(item -> item.getId().equals(lowStockItem2.getId())));
    }

    @Test
    void findItemsWithAvailableStock_ShouldReturnItemsWithSufficientStock() {
        // Given
        InventoryItem item1 = createInventoryItem(tenantId1, "product-1", "SKU-1", 100, 10);
        InventoryItem item2 = createInventoryItem(tenantId1, "product-2", "SKU-2", 50, 20);
        InventoryItem item3 = createInventoryItem(tenantId1, "product-3", "SKU-3", 20, 30);

        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);
        entityManager.persistAndFlush(item3);

        // When
        List<InventoryItem> result = inventoryItemRepository.findItemsWithAvailableStock(tenantId1, 30);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(item -> item.getId().equals(item1.getId())));
        assertTrue(result.stream().anyMatch(item -> item.getId().equals(item2.getId())));
        assertFalse(result.stream().anyMatch(item -> item.getId().equals(item3.getId())));
    }

    @Test
    void existsByTenantIdAndProductId_ShouldReturnCorrectResult() {
        // Given
        InventoryItem item = createInventoryItem(tenantId1, "product-exists", "SKU-exists", 100, 10);
        entityManager.persistAndFlush(item);

        // When & Then
        assertTrue(inventoryItemRepository.existsByTenantIdAndProductId(tenantId1, "product-exists"));
        assertFalse(inventoryItemRepository.existsByTenantIdAndProductId(tenantId1, "product-not-exists"));
        assertFalse(inventoryItemRepository.existsByTenantIdAndProductId(tenantId2, "product-exists"));
    }

    @Test
    void existsByTenantIdAndSku_ShouldReturnCorrectResult() {
        // Given
        InventoryItem item = createInventoryItem(tenantId1, "product-1", "SKU-exists", 100, 10);
        entityManager.persistAndFlush(item);

        // When & Then
        assertTrue(inventoryItemRepository.existsByTenantIdAndSku(tenantId1, "SKU-exists"));
        assertFalse(inventoryItemRepository.existsByTenantIdAndSku(tenantId1, "SKU-not-exists"));
        assertFalse(inventoryItemRepository.existsByTenantIdAndSku(tenantId2, "SKU-exists"));
    }

    @Test
    void getTotalAvailableQuantityByProduct_ShouldReturnCorrectSum() {
        // Given
        InventoryItem item1 = createInventoryItem(tenantId1, "product-multi", "SKU-1", 100, 10);
        item1.setLocationCode("WH-001");
        
        InventoryItem item2 = createInventoryItem(tenantId1, "product-multi", "SKU-2", 200, 20);
        item2.setLocationCode("WH-002");
        
        InventoryItem item3 = createInventoryItem(tenantId1, "product-other", "SKU-3", 300, 30);

        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);
        entityManager.persistAndFlush(item3);

        // When
        Integer totalQuantity = inventoryItemRepository.getTotalAvailableQuantityByProduct(tenantId1, "product-multi");

        // Then
        assertEquals(300, totalQuantity); // 100 + 200
    }

    @Test
    void getTotalReservedQuantityByProduct_ShouldReturnCorrectSum() {
        // Given
        InventoryItem item1 = createInventoryItem(tenantId1, "product-reserved", "SKU-1", 100, 10);
        item1.setReservedQuantity(25);
        
        InventoryItem item2 = createInventoryItem(tenantId1, "product-reserved", "SKU-2", 200, 20);
        item2.setReservedQuantity(35);

        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);

        // When
        Integer totalReserved = inventoryItemRepository.getTotalReservedQuantityByProduct(tenantId1, "product-reserved");

        // Then
        assertEquals(60, totalReserved); // 25 + 35
    }

    @Test
    void findByTenantIdAndProductIdIn_ShouldReturnMatchingItems() {
        // Given
        InventoryItem item1 = createInventoryItem(tenantId1, "product-1", "SKU-1", 100, 10);
        InventoryItem item2 = createInventoryItem(tenantId1, "product-2", "SKU-2", 200, 20);
        InventoryItem item3 = createInventoryItem(tenantId1, "product-3", "SKU-3", 300, 30);

        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);
        entityManager.persistAndFlush(item3);

        // When
        List<String> productIds = Arrays.asList("product-1", "product-3");
        List<InventoryItem> result = inventoryItemRepository.findByTenantIdAndProductIdIn(tenantId1, productIds);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(item -> "product-1".equals(item.getProductId())));
        assertTrue(result.stream().anyMatch(item -> "product-3".equals(item.getProductId())));
        assertFalse(result.stream().anyMatch(item -> "product-2".equals(item.getProductId())));
    }

    @Test
    void tenantIsolation_ShouldPreventCrossTenantAccess() {
        // Given
        InventoryItem tenant1Item = createInventoryItem(tenantId1, "product-1", "SKU-1", 100, 10);
        InventoryItem tenant2Item = createInventoryItem(tenantId2, "product-1", "SKU-2", 200, 20);

        entityManager.persistAndFlush(tenant1Item);
        entityManager.persistAndFlush(tenant2Item);

        // When & Then
        List<InventoryItem> tenant1Items = inventoryItemRepository.findByTenantId(tenantId1);
        List<InventoryItem> tenant2Items = inventoryItemRepository.findByTenantId(tenantId2);

        assertEquals(1, tenant1Items.size());
        assertEquals(1, tenant2Items.size());
        assertEquals(tenantId1, tenant1Items.get(0).getTenantId());
        assertEquals(tenantId2, tenant2Items.get(0).getTenantId());

        // Verify counts
        assertEquals(1, inventoryItemRepository.countByTenantId(tenantId1));
        assertEquals(1, inventoryItemRepository.countByTenantId(tenantId2));
    }

    // Helper method
    private InventoryItem createInventoryItem(String tenantId, String productId, String sku, 
                                            Integer availableQuantity, Integer reorderLevel) {
        InventoryItem item = new InventoryItem();
        item.setTenantId(tenantId);
        item.setProductId(productId);
        item.setSku(sku);
        item.setAvailableQuantity(availableQuantity);
        item.setReservedQuantity(0);
        item.setReorderLevel(reorderLevel);
        item.setMaxStockLevel(1000);
        item.setLocationCode("WH-DEFAULT");
        item.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        return item;
    }
}
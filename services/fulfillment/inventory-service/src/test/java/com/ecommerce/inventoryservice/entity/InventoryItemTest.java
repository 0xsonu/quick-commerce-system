package com.ecommerce.inventoryservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryItemTest {

    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        inventoryItem = new InventoryItem();
        inventoryItem.setTenantId("tenant123");
        inventoryItem.setProductId("product123");
        inventoryItem.setSku("SKU123");
        inventoryItem.setAvailableQuantity(100);
        inventoryItem.setReservedQuantity(10);
        inventoryItem.setReorderLevel(20);
    }

    @Test
    void getTotalQuantity_ShouldReturnSumOfAvailableAndReserved() {
        // When
        Integer totalQuantity = inventoryItem.getTotalQuantity();

        // Then
        assertEquals(110, totalQuantity);
    }

    @Test
    void isLowStock_WhenQuantityBelowReorderLevel_ShouldReturnTrue() {
        // Given
        inventoryItem.setAvailableQuantity(15);

        // When
        boolean isLowStock = inventoryItem.isLowStock();

        // Then
        assertTrue(isLowStock);
    }

    @Test
    void isLowStock_WhenQuantityAboveReorderLevel_ShouldReturnFalse() {
        // Given
        inventoryItem.setAvailableQuantity(25);

        // When
        boolean isLowStock = inventoryItem.isLowStock();

        // Then
        assertFalse(isLowStock);
    }

    @Test
    void isLowStock_WhenReorderLevelIsNull_ShouldReturnFalse() {
        // Given
        inventoryItem.setReorderLevel(null);

        // When
        boolean isLowStock = inventoryItem.isLowStock();

        // Then
        assertFalse(isLowStock);
    }

    @Test
    void canReserve_WhenSufficientStock_ShouldReturnTrue() {
        // When
        boolean canReserve = inventoryItem.canReserve(50);

        // Then
        assertTrue(canReserve);
    }

    @Test
    void canReserve_WhenInsufficientStock_ShouldReturnFalse() {
        // When
        boolean canReserve = inventoryItem.canReserve(150);

        // Then
        assertFalse(canReserve);
    }

    @Test
    void canReserve_WhenQuantityIsNull_ShouldReturnFalse() {
        // When
        boolean canReserve = inventoryItem.canReserve(null);

        // Then
        assertFalse(canReserve);
    }

    @Test
    void canReserve_WhenQuantityIsZeroOrNegative_ShouldReturnFalse() {
        // When & Then
        assertFalse(inventoryItem.canReserve(0));
        assertFalse(inventoryItem.canReserve(-5));
    }

    @Test
    void reserveStock_WhenSufficientStock_ShouldUpdateQuantities() {
        // Given
        int quantityToReserve = 30;
        int initialAvailable = inventoryItem.getAvailableQuantity();
        int initialReserved = inventoryItem.getReservedQuantity();

        // When
        inventoryItem.reserveStock(quantityToReserve);

        // Then
        assertEquals(initialAvailable - quantityToReserve, inventoryItem.getAvailableQuantity());
        assertEquals(initialReserved + quantityToReserve, inventoryItem.getReservedQuantity());
    }

    @Test
    void reserveStock_WhenInsufficientStock_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryItem.reserveStock(150));
    }

    @Test
    void releaseReservation_WhenValidQuantity_ShouldUpdateQuantities() {
        // Given
        int quantityToRelease = 5;
        int initialAvailable = inventoryItem.getAvailableQuantity();
        int initialReserved = inventoryItem.getReservedQuantity();

        // When
        inventoryItem.releaseReservation(quantityToRelease);

        // Then
        assertEquals(initialAvailable + quantityToRelease, inventoryItem.getAvailableQuantity());
        assertEquals(initialReserved - quantityToRelease, inventoryItem.getReservedQuantity());
    }

    @Test
    void releaseReservation_WhenQuantityExceedsReserved_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryItem.releaseReservation(20));
    }

    @Test
    void releaseReservation_WhenQuantityIsNullOrNegative_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryItem.releaseReservation(null));
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryItem.releaseReservation(-5));
    }

    @Test
    void confirmReservation_WhenValidQuantity_ShouldReduceReservedQuantity() {
        // Given
        int quantityToConfirm = 5;
        int initialReserved = inventoryItem.getReservedQuantity();
        int initialAvailable = inventoryItem.getAvailableQuantity();

        // When
        inventoryItem.confirmReservation(quantityToConfirm);

        // Then
        assertEquals(initialReserved - quantityToConfirm, inventoryItem.getReservedQuantity());
        assertEquals(initialAvailable, inventoryItem.getAvailableQuantity()); // Available should not change
    }

    @Test
    void confirmReservation_WhenQuantityExceedsReserved_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryItem.confirmReservation(20));
    }

    @Test
    void addStock_WhenValidQuantity_ShouldIncreaseAvailableQuantity() {
        // Given
        int quantityToAdd = 50;
        int initialAvailable = inventoryItem.getAvailableQuantity();

        // When
        inventoryItem.addStock(quantityToAdd);

        // Then
        assertEquals(initialAvailable + quantityToAdd, inventoryItem.getAvailableQuantity());
        assertNotNull(inventoryItem.getLastRestockedAt());
    }

    @Test
    void addStock_WhenQuantityIsNullOrNegative_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryItem.addStock(null));
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryItem.addStock(-10));
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryItem.addStock(0));
    }

    @Test
    void constructor_WithParameters_ShouldSetFieldsCorrectly() {
        // Given
        String tenantId = "tenant456";
        String productId = "product456";
        String sku = "SKU456";
        Integer availableQuantity = 200;

        // When
        InventoryItem item = new InventoryItem(tenantId, productId, sku, availableQuantity);

        // Then
        assertEquals(tenantId, item.getTenantId());
        assertEquals(productId, item.getProductId());
        assertEquals(sku, item.getSku());
        assertEquals(availableQuantity, item.getAvailableQuantity());
        assertEquals(0, item.getReservedQuantity());
        assertEquals(InventoryItem.InventoryStatus.ACTIVE, item.getStatus());
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    void defaultConstructor_ShouldSetDefaultValues() {
        // When
        InventoryItem item = new InventoryItem();

        // Then
        assertEquals(0, item.getAvailableQuantity());
        assertEquals(0, item.getReservedQuantity());
        assertEquals(InventoryItem.InventoryStatus.ACTIVE, item.getStatus());
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    void setters_ShouldUpdateTimestamp() {
        // Given
        var initialUpdatedAt = inventoryItem.getUpdatedAt();
        
        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        inventoryItem.setAvailableQuantity(200);

        // Then
        assertTrue(inventoryItem.getUpdatedAt().isAfter(initialUpdatedAt));
    }
}
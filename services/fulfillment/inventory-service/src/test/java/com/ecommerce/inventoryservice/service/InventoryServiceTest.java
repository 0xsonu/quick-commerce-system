package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.StockTransaction;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockTransactionService stockTransactionService;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem testInventoryItem;
    private CreateInventoryItemRequest createRequest;
    private UpdateInventoryItemRequest updateRequest;
    private StockAdjustmentRequest stockAdjustmentRequest;
    private final String tenantId = "tenant123";
    private final Long itemId = 1L;

    @BeforeEach
    void setUp() {
        testInventoryItem = createTestInventoryItem();
        createRequest = createTestCreateRequest();
        updateRequest = createTestUpdateRequest();
        stockAdjustmentRequest = createTestStockAdjustmentRequest();
    }

    @Test
    void getInventoryItem_WhenItemExists_ShouldReturnItem() {
        // Given
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, itemId))
            .thenReturn(Optional.of(testInventoryItem));

        // When
        InventoryItemResponse result = inventoryService.getInventoryItem(tenantId, itemId);

        // Then
        assertNotNull(result);
        assertEquals(itemId, result.getId());
        assertEquals(testInventoryItem.getProductId(), result.getProductId());
        assertEquals(testInventoryItem.getSku(), result.getSku());
        verify(inventoryItemRepository).findByTenantIdAndId(tenantId, itemId);
    }

    @Test
    void getInventoryItem_WhenItemNotExists_ShouldThrowException() {
        // Given
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, itemId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> inventoryService.getInventoryItem(tenantId, itemId));
        verify(inventoryItemRepository).findByTenantIdAndId(tenantId, itemId);
    }

    @Test
    void getInventoryItemByProductId_WhenItemExists_ShouldReturnItem() {
        // Given
        String productId = "product123";
        when(inventoryItemRepository.findByTenantIdAndProductId(tenantId, productId))
            .thenReturn(Optional.of(testInventoryItem));

        // When
        InventoryItemResponse result = inventoryService.getInventoryItemByProductId(tenantId, productId);

        // Then
        assertNotNull(result);
        assertEquals(productId, result.getProductId());
        verify(inventoryItemRepository).findByTenantIdAndProductId(tenantId, productId);
    }

    @Test
    void getInventoryItemBySku_WhenItemExists_ShouldReturnItem() {
        // Given
        String sku = "SKU123";
        when(inventoryItemRepository.findByTenantIdAndSku(tenantId, sku))
            .thenReturn(Optional.of(testInventoryItem));

        // When
        InventoryItemResponse result = inventoryService.getInventoryItemBySku(tenantId, sku);

        // Then
        assertNotNull(result);
        assertEquals(sku, result.getSku());
        verify(inventoryItemRepository).findByTenantIdAndSku(tenantId, sku);
    }

    @Test
    void getInventoryItems_WithNoFilters_ShouldReturnPagedItems() {
        // Given
        List<InventoryItem> items = Arrays.asList(testInventoryItem);
        Page<InventoryItem> page = new PageImpl<>(items, PageRequest.of(0, 20), 1);
        when(inventoryItemRepository.findByTenantId(eq(tenantId), any(Pageable.class)))
            .thenReturn(page);

        // When
        Page<InventoryItemResponse> result = inventoryService.getInventoryItems(
            tenantId, 0, 20, null, null, "updatedAt", "desc");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(itemId, result.getContent().get(0).getId());
        verify(inventoryItemRepository).findByTenantId(eq(tenantId), any(Pageable.class));
    }

    @Test
    void getInventoryItems_WithStatusFilter_ShouldReturnFilteredItems() {
        // Given
        InventoryItem.InventoryStatus status = InventoryItem.InventoryStatus.ACTIVE;
        List<InventoryItem> items = Arrays.asList(testInventoryItem);
        Page<InventoryItem> page = new PageImpl<>(items, PageRequest.of(0, 20), 1);
        when(inventoryItemRepository.findByTenantIdAndStatus(eq(tenantId), eq(status), any(Pageable.class)))
            .thenReturn(page);

        // When
        Page<InventoryItemResponse> result = inventoryService.getInventoryItems(
            tenantId, 0, 20, status, null, "updatedAt", "desc");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(inventoryItemRepository).findByTenantIdAndStatus(eq(tenantId), eq(status), any(Pageable.class));
    }

    @Test
    void createInventoryItem_WithValidRequest_ShouldCreateItem() {
        // Given
        when(inventoryItemRepository.existsByTenantIdAndProductId(tenantId, createRequest.getProductId()))
            .thenReturn(false);
        when(inventoryItemRepository.existsByTenantIdAndSku(tenantId, createRequest.getSku()))
            .thenReturn(false);
        when(inventoryItemRepository.save(any(InventoryItem.class)))
            .thenReturn(testInventoryItem);
        when(stockTransactionService.logTransaction(anyString(), anyLong(), any(), anyInt(), 
            any(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(new StockTransactionResponse());

        // When
        InventoryItemResponse result = inventoryService.createInventoryItem(tenantId, createRequest);

        // Then
        assertNotNull(result);
        assertEquals(testInventoryItem.getProductId(), result.getProductId());
        verify(inventoryItemRepository).existsByTenantIdAndProductId(tenantId, createRequest.getProductId());
        verify(inventoryItemRepository).existsByTenantIdAndSku(tenantId, createRequest.getSku());
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(stockTransactionService).logTransaction(anyString(), anyLong(), any(), anyInt(), 
            any(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void createInventoryItem_WhenProductAlreadyExists_ShouldThrowException() {
        // Given
        when(inventoryItemRepository.existsByTenantIdAndProductId(tenantId, createRequest.getProductId()))
            .thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryService.createInventoryItem(tenantId, createRequest));
        verify(inventoryItemRepository).existsByTenantIdAndProductId(tenantId, createRequest.getProductId());
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void createInventoryItem_WhenSkuAlreadyExists_ShouldThrowException() {
        // Given
        when(inventoryItemRepository.existsByTenantIdAndProductId(tenantId, createRequest.getProductId()))
            .thenReturn(false);
        when(inventoryItemRepository.existsByTenantIdAndSku(tenantId, createRequest.getSku()))
            .thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryService.createInventoryItem(tenantId, createRequest));
        verify(inventoryItemRepository).existsByTenantIdAndSku(tenantId, createRequest.getSku());
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void updateInventoryItem_WithValidRequest_ShouldUpdateItem() {
        // Given
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, itemId))
            .thenReturn(Optional.of(testInventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class)))
            .thenReturn(testInventoryItem);
        when(stockTransactionService.logTransaction(anyString(), anyLong(), any(), anyInt(), 
            any(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(new StockTransactionResponse());

        // When
        InventoryItemResponse result = inventoryService.updateInventoryItem(tenantId, itemId, updateRequest);

        // Then
        assertNotNull(result);
        verify(inventoryItemRepository).findByTenantIdAndId(tenantId, itemId);
        verify(inventoryItemRepository).save(any(InventoryItem.class));
    }

    @Test
    void updateInventoryItem_WhenItemNotExists_ShouldThrowException() {
        // Given
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, itemId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> inventoryService.updateInventoryItem(tenantId, itemId, updateRequest));
        verify(inventoryItemRepository).findByTenantIdAndId(tenantId, itemId);
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void deleteInventoryItem_WhenItemExists_ShouldDeleteItem() {
        // Given
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, itemId))
            .thenReturn(Optional.of(testInventoryItem));

        // When
        inventoryService.deleteInventoryItem(tenantId, itemId);

        // Then
        verify(inventoryItemRepository).findByTenantIdAndId(tenantId, itemId);
        verify(inventoryItemRepository).delete(testInventoryItem);
    }

    @Test
    void addStock_WithValidRequest_ShouldAddStock() {
        // Given
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, itemId))
            .thenReturn(Optional.of(testInventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class)))
            .thenReturn(testInventoryItem);
        when(stockTransactionService.logTransaction(anyString(), anyLong(), any(), anyInt(), 
            any(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(new StockTransactionResponse());

        // When
        InventoryItemResponse result = inventoryService.addStock(tenantId, itemId, stockAdjustmentRequest);

        // Then
        assertNotNull(result);
        verify(inventoryItemRepository).findByTenantIdAndId(tenantId, itemId);
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(stockTransactionService).logTransaction(eq(tenantId), eq(itemId), 
            eq(StockTransaction.TransactionType.STOCK_IN), eq(stockAdjustmentRequest.getQuantity()),
            eq(stockAdjustmentRequest.getReferenceId()), anyInt(), anyInt(), anyInt(), anyInt(),
            eq(stockAdjustmentRequest.getReferenceType()), eq(stockAdjustmentRequest.getReason()));
    }

    @Test
    void removeStock_WithSufficientStock_ShouldRemoveStock() {
        // Given
        testInventoryItem.setAvailableQuantity(100); // Ensure sufficient stock
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, itemId))
            .thenReturn(Optional.of(testInventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class)))
            .thenReturn(testInventoryItem);
        when(stockTransactionService.logTransaction(anyString(), anyLong(), any(), anyInt(), 
            any(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(new StockTransactionResponse());

        // When
        InventoryItemResponse result = inventoryService.removeStock(tenantId, itemId, stockAdjustmentRequest);

        // Then
        assertNotNull(result);
        verify(inventoryItemRepository).findByTenantIdAndId(tenantId, itemId);
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(stockTransactionService).logTransaction(eq(tenantId), eq(itemId), 
            eq(StockTransaction.TransactionType.STOCK_OUT), eq(stockAdjustmentRequest.getQuantity()),
            eq(stockAdjustmentRequest.getReferenceId()), anyInt(), anyInt(), anyInt(), anyInt(),
            eq(stockAdjustmentRequest.getReferenceType()), eq(stockAdjustmentRequest.getReason()));
    }

    @Test
    void removeStock_WithInsufficientStock_ShouldThrowException() {
        // Given
        testInventoryItem.setAvailableQuantity(10); // Less than requested quantity
        stockAdjustmentRequest.setQuantity(50); // More than available
        when(inventoryItemRepository.findByTenantIdAndId(tenantId, itemId))
            .thenReturn(Optional.of(testInventoryItem));

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> inventoryService.removeStock(tenantId, itemId, stockAdjustmentRequest));
        verify(inventoryItemRepository).findByTenantIdAndId(tenantId, itemId);
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void getLowStockItems_ShouldReturnLowStockItems() {
        // Given
        List<InventoryItem> lowStockItems = Arrays.asList(testInventoryItem);
        when(inventoryItemRepository.findLowStockItems(tenantId))
            .thenReturn(lowStockItems);

        // When
        List<InventoryItemResponse> result = inventoryService.getLowStockItems(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getId());
        verify(inventoryItemRepository).findLowStockItems(tenantId);
    }

    @Test
    void checkStockAvailability_ShouldReturnAvailableItems() {
        // Given
        List<String> productIds = Arrays.asList("product1", "product2");
        List<InventoryItem> availableItems = Arrays.asList(testInventoryItem);
        when(inventoryItemRepository.findByTenantIdAndProductIdIn(tenantId, productIds))
            .thenReturn(availableItems);

        // When
        List<InventoryItemResponse> result = inventoryService.checkStockAvailability(tenantId, productIds);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getId());
        verify(inventoryItemRepository).findByTenantIdAndProductIdIn(tenantId, productIds);
    }

    @Test
    void getInventoryCountByStatus_ShouldReturnCount() {
        // Given
        InventoryItem.InventoryStatus status = InventoryItem.InventoryStatus.ACTIVE;
        when(inventoryItemRepository.countByTenantIdAndStatus(tenantId, status))
            .thenReturn(5L);

        // When
        long result = inventoryService.getInventoryCountByStatus(tenantId, status);

        // Then
        assertEquals(5L, result);
        verify(inventoryItemRepository).countByTenantIdAndStatus(tenantId, status);
    }

    // Helper methods
    private InventoryItem createTestInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setId(itemId);
        item.setTenantId(tenantId);
        item.setProductId("product123");
        item.setSku("SKU123");
        item.setAvailableQuantity(100);
        item.setReservedQuantity(0);
        item.setReorderLevel(10);
        item.setMaxStockLevel(1000);
        item.setLocationCode("WH001");
        item.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        item.setVersion(1L);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        return item;
    }

    private CreateInventoryItemRequest createTestCreateRequest() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setProductId("product123");
        request.setSku("SKU123");
        request.setInitialQuantity(100);
        request.setReorderLevel(10);
        request.setMaxStockLevel(1000);
        request.setLocationCode("WH001");
        request.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        return request;
    }

    private UpdateInventoryItemRequest createTestUpdateRequest() {
        UpdateInventoryItemRequest request = new UpdateInventoryItemRequest();
        request.setAvailableQuantity(150);
        request.setReorderLevel(15);
        request.setMaxStockLevel(1500);
        request.setLocationCode("WH002");
        request.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        return request;
    }

    private StockAdjustmentRequest createTestStockAdjustmentRequest() {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setQuantity(50);
        request.setReason("Restock");
        request.setReferenceId("REF123");
        request.setReferenceType("PURCHASE_ORDER");
        return request;
    }
}
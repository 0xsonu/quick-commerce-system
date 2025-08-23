package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.inventoryservice.service.StockTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private StockTransactionService stockTransactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private InventoryItemResponse testInventoryItemResponse;
    private CreateInventoryItemRequest createRequest;
    private UpdateInventoryItemRequest updateRequest;
    private StockAdjustmentRequest stockAdjustmentRequest;
    private final String tenantId = "tenant123";
    private final Long itemId = 1L;

    @BeforeEach
    void setUp() {
        testInventoryItemResponse = createTestInventoryItemResponse();
        createRequest = createTestCreateRequest();
        updateRequest = createTestUpdateRequest();
        stockAdjustmentRequest = createTestStockAdjustmentRequest();
    }

    @Test
    void getInventoryItem_ShouldReturnInventoryItem() throws Exception {
        // Given
        when(inventoryService.getInventoryItem(tenantId, itemId))
            .thenReturn(testInventoryItemResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/{id}", itemId)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(itemId))
                .andExpect(jsonPath("$.data.productId").value("product123"))
                .andExpect(jsonPath("$.data.sku").value("SKU123"));
    }

    @Test
    void getInventoryItemByProductId_ShouldReturnInventoryItem() throws Exception {
        // Given
        String productId = "product123";
        when(inventoryService.getInventoryItemByProductId(tenantId, productId))
            .thenReturn(testInventoryItemResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/product/{productId}", productId)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(productId));
    }

    @Test
    void getInventoryItemBySku_ShouldReturnInventoryItem() throws Exception {
        // Given
        String sku = "SKU123";
        when(inventoryService.getInventoryItemBySku(tenantId, sku))
            .thenReturn(testInventoryItemResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/sku/{sku}", sku)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value(sku));
    }

    @Test
    void getInventoryItems_ShouldReturnPagedInventoryItems() throws Exception {
        // Given
        List<InventoryItemResponse> items = Arrays.asList(testInventoryItemResponse);
        Page<InventoryItemResponse> page = new PageImpl<>(items, PageRequest.of(0, 20), 1);
        
        when(inventoryService.getInventoryItems(eq(tenantId), eq(0), eq(20), 
            any(), any(), eq("updatedAt"), eq("desc")))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory")
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(itemId))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void createInventoryItem_WithValidRequest_ShouldReturnCreatedItem() throws Exception {
        // Given
        when(inventoryService.createInventoryItem(eq(tenantId), any(CreateInventoryItemRequest.class)))
            .thenReturn(testInventoryItemResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(itemId))
                .andExpect(jsonPath("$.data.productId").value(createRequest.getProductId()));
    }

    @Test
    void createInventoryItem_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        CreateInventoryItemRequest invalidRequest = new CreateInventoryItemRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/inventory")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateInventoryItem_WithValidRequest_ShouldReturnUpdatedItem() throws Exception {
        // Given
        when(inventoryService.updateInventoryItem(eq(tenantId), eq(itemId), any(UpdateInventoryItemRequest.class)))
            .thenReturn(testInventoryItemResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/inventory/{id}", itemId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(itemId));
    }

    @Test
    void deleteInventoryItem_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/inventory/{id}", itemId)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void addStock_WithValidRequest_ShouldReturnUpdatedItem() throws Exception {
        // Given
        when(inventoryService.addStock(eq(tenantId), eq(itemId), any(StockAdjustmentRequest.class)))
            .thenReturn(testInventoryItemResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/{id}/add-stock", itemId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockAdjustmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(itemId));
    }

    @Test
    void removeStock_WithValidRequest_ShouldReturnUpdatedItem() throws Exception {
        // Given
        when(inventoryService.removeStock(eq(tenantId), eq(itemId), any(StockAdjustmentRequest.class)))
            .thenReturn(testInventoryItemResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/{id}/remove-stock", itemId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockAdjustmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(itemId));
    }

    @Test
    void getLowStockItems_ShouldReturnLowStockItems() throws Exception {
        // Given
        List<InventoryItemResponse> lowStockItems = Arrays.asList(testInventoryItemResponse);
        when(inventoryService.getLowStockItems(tenantId))
            .thenReturn(lowStockItems);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/low-stock")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(itemId));
    }

    @Test
    void checkStockAvailability_ShouldReturnAvailableItems() throws Exception {
        // Given
        List<String> productIds = Arrays.asList("product1", "product2");
        List<InventoryItemResponse> availableItems = Arrays.asList(testInventoryItemResponse);
        when(inventoryService.checkStockAvailability(tenantId, productIds))
            .thenReturn(availableItems);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/check-availability")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(itemId));
    }

    @Test
    void getInventoryStats_ShouldReturnStatistics() throws Exception {
        // Given
        when(inventoryService.getInventoryCountByStatus(tenantId, null)).thenReturn(100L);
        when(inventoryService.getInventoryCountByStatus(tenantId, InventoryItem.InventoryStatus.ACTIVE)).thenReturn(80L);
        when(inventoryService.getInventoryCountByStatus(tenantId, InventoryItem.InventoryStatus.INACTIVE)).thenReturn(15L);
        when(inventoryService.getInventoryCountByStatus(tenantId, InventoryItem.InventoryStatus.OUT_OF_STOCK)).thenReturn(5L);
        when(inventoryService.getLowStockItems(tenantId)).thenReturn(Arrays.asList(testInventoryItemResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/stats")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(100))
                .andExpect(jsonPath("$.data.activeItems").value(80))
                .andExpect(jsonPath("$.data.inactiveItems").value(15))
                .andExpect(jsonPath("$.data.outOfStockItems").value(5))
                .andExpect(jsonPath("$.data.lowStockItems").value(1));
    }

    @Test
    void getInventoryItemTransactions_ShouldReturnTransactions() throws Exception {
        // Given
        List<StockTransactionResponse> transactions = Arrays.asList(new StockTransactionResponse());
        Page<StockTransactionResponse> page = new PageImpl<>(transactions, PageRequest.of(0, 20), 1);
        
        when(stockTransactionService.getTransactionsByInventoryItem(tenantId, itemId, 0, 20))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/{id}/transactions", itemId)
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // Helper methods
    private InventoryItemResponse createTestInventoryItemResponse() {
        InventoryItemResponse response = new InventoryItemResponse();
        response.setId(itemId);
        response.setProductId("product123");
        response.setSku("SKU123");
        response.setAvailableQuantity(100);
        response.setReservedQuantity(0);
        response.setTotalQuantity(100);
        response.setReorderLevel(10);
        response.setMaxStockLevel(1000);
        response.setLocationCode("WH001");
        response.setStatus(InventoryItem.InventoryStatus.ACTIVE);
        response.setLowStock(false);
        response.setVersion(1L);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
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
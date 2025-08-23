package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.inventoryservice.service.InventoryReservationService;
import com.ecommerce.inventoryservice.service.StockTransactionService;
import com.ecommerce.shared.utils.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryReservationService reservationService;
    private final StockTransactionService stockTransactionService;

    @Autowired
    public InventoryController(InventoryService inventoryService,
                              InventoryReservationService reservationService,
                              StockTransactionService stockTransactionService) {
        this.inventoryService = inventoryService;
        this.reservationService = reservationService;
        this.stockTransactionService = stockTransactionService;
    }

    /**
     * Get inventory item by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getInventoryItem(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Long id) {
        
        InventoryItemResponse response = inventoryService.getInventoryItem(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get inventory item by product ID
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getInventoryItemByProductId(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId) {
        
        InventoryItemResponse response = inventoryService.getInventoryItemByProductId(tenantId, productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get inventory item by SKU
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getInventoryItemBySku(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String sku) {
        
        InventoryItemResponse response = inventoryService.getInventoryItemBySku(tenantId, sku);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all inventory items with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InventoryItemResponse>>> getInventoryItems(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) InventoryItem.InventoryStatus status,
            @RequestParam(required = false) String locationCode,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Page<InventoryItemResponse> response = inventoryService.getInventoryItems(
            tenantId, page, size, status, locationCode, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Create new inventory item
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createInventoryItem(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateInventoryItemRequest request) {
        
        InventoryItemResponse response = inventoryService.createInventoryItem(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Update inventory item
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateInventoryItem(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
        
        InventoryItemResponse response = inventoryService.updateInventoryItem(tenantId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete inventory item
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInventoryItem(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Long id) {
        
        inventoryService.deleteInventoryItem(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Add stock to inventory item
     */
    @PostMapping("/{id}/add-stock")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> addStock(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        
        InventoryItemResponse response = inventoryService.addStock(tenantId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Remove stock from inventory item
     */
    @PostMapping("/{id}/remove-stock")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> removeStock(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        
        InventoryItemResponse response = inventoryService.removeStock(tenantId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get low stock items
     */
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getLowStockItems(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<InventoryItemResponse> response = inventoryService.getLowStockItems(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Check stock availability for multiple products
     */
    @PostMapping("/check-availability")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> checkStockAvailability(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody List<String> productIds) {
        
        List<InventoryItemResponse> response = inventoryService.checkStockAvailability(tenantId, productIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get inventory statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<InventoryStats>> getInventoryStats(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        long totalItems = inventoryService.getInventoryCountByStatus(tenantId, null);
        long activeItems = inventoryService.getInventoryCountByStatus(tenantId, InventoryItem.InventoryStatus.ACTIVE);
        long inactiveItems = inventoryService.getInventoryCountByStatus(tenantId, InventoryItem.InventoryStatus.INACTIVE);
        long outOfStockItems = inventoryService.getInventoryCountByStatus(tenantId, InventoryItem.InventoryStatus.OUT_OF_STOCK);
        List<InventoryItemResponse> lowStockItems = inventoryService.getLowStockItems(tenantId);
        
        InventoryStats stats = new InventoryStats(totalItems, activeItems, inactiveItems, 
                                                 outOfStockItems, lowStockItems.size());
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get transactions for inventory item
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<Page<StockTransactionResponse>>> getInventoryItemTransactions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<StockTransactionResponse> response = stockTransactionService
            .getTransactionsByInventoryItem(tenantId, id, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get transaction history for inventory item within date range
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getInventoryItemHistory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<StockTransactionResponse> response = stockTransactionService
            .getInventoryItemHistory(tenantId, id, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Reserve inventory for an order
     */
    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveInventory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody ReservationRequest request) {
        
        ReservationResponse response = reservationService.reserveInventory(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Get reservation by ID
     */
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String reservationId) {
        
        ReservationResponse response = reservationService.getReservation(tenantId, reservationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Confirm reservation (convert to actual stock deduction)
     */
    @PostMapping("/reservations/{reservationId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmReservation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String reservationId) {
        
        reservationService.confirmReservation(tenantId, reservationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Release reservation (return stock to available)
     */
    @PostMapping("/reservations/{reservationId}/release")
    public ResponseEntity<ApiResponse<Void>> releaseReservation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String reservationId,
            @RequestParam(defaultValue = "Manual release") String reason) {
        
        reservationService.releaseReservation(tenantId, reservationId, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Inventory statistics DTO
     */
    public static class InventoryStats {
        private final long totalItems;
        private final long activeItems;
        private final long inactiveItems;
        private final long outOfStockItems;
        private final long lowStockItems;

        public InventoryStats(long totalItems, long activeItems, long inactiveItems, 
                            long outOfStockItems, long lowStockItems) {
            this.totalItems = totalItems;
            this.activeItems = activeItems;
            this.inactiveItems = inactiveItems;
            this.outOfStockItems = outOfStockItems;
            this.lowStockItems = lowStockItems;
        }

        public long getTotalItems() { return totalItems; }
        public long getActiveItems() { return activeItems; }
        public long getInactiveItems() { return inactiveItems; }
        public long getOutOfStockItems() { return outOfStockItems; }
        public long getLowStockItems() { return lowStockItems; }
    }
}
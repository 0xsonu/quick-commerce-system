package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.StockTransaction;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository inventoryItemRepository;
    private final StockTransactionService stockTransactionService;

    @Autowired
    public InventoryService(InventoryItemRepository inventoryItemRepository,
                           StockTransactionService stockTransactionService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockTransactionService = stockTransactionService;
    }

    /**
     * Get inventory item by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "inventory-items", key = "#tenantId + ':' + #id")
    public InventoryItemResponse getInventoryItem(String tenantId, Long id) {
        InventoryItem item = inventoryItemRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with ID: " + id));
        
        return new InventoryItemResponse(item);
    }

    /**
     * Get inventory item by product ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "inventory-by-product", key = "#tenantId + ':' + #productId")
    public InventoryItemResponse getInventoryItemByProductId(String tenantId, String productId) {
        InventoryItem item = inventoryItemRepository.findByTenantIdAndProductId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found for product ID: " + productId));
        
        return new InventoryItemResponse(item);
    }

    /**
     * Get inventory item by SKU
     */
    @Transactional(readOnly = true)
    public InventoryItemResponse getInventoryItemBySku(String tenantId, String sku) {
        InventoryItem item = inventoryItemRepository.findByTenantIdAndSku(tenantId, sku)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found for SKU: " + sku));
        
        return new InventoryItemResponse(item);
    }

    /**
     * Get all inventory items with pagination and filtering
     */
    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> getInventoryItems(String tenantId, int page, int size, 
                                                        InventoryItem.InventoryStatus status,
                                                        String locationCode, String sortBy, String sortDirection) {
        
        // Validate and set default sorting
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "updatedAt";
        }
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<InventoryItem> itemPage;
        
        if (status != null && locationCode != null && !locationCode.trim().isEmpty()) {
            // Filter by both status and location - need to implement this method or use separate queries
            itemPage = inventoryItemRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (status != null) {
            // Filter by status only
            itemPage = inventoryItemRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (locationCode != null && !locationCode.trim().isEmpty()) {
            // Filter by location only
            itemPage = inventoryItemRepository.findByTenantIdAndLocationCode(tenantId, locationCode, pageable);
        } else {
            // No filters
            itemPage = inventoryItemRepository.findByTenantId(tenantId, pageable);
        }
        
        return itemPage.map(InventoryItemResponse::new);
    }

    /**
     * Create new inventory item
     */
    public InventoryItemResponse createInventoryItem(String tenantId, CreateInventoryItemRequest request) {
        // Check if product already exists in inventory
        if (inventoryItemRepository.existsByTenantIdAndProductId(tenantId, request.getProductId())) {
            throw new IllegalArgumentException("Inventory item already exists for product ID: " + request.getProductId());
        }
        
        // Check if SKU already exists
        if (inventoryItemRepository.existsByTenantIdAndSku(tenantId, request.getSku())) {
            throw new IllegalArgumentException("Inventory item already exists for SKU: " + request.getSku());
        }
        
        InventoryItem item = mapToInventoryItem(tenantId, request);
        InventoryItem savedItem = inventoryItemRepository.save(item);
        
        // Log initial stock transaction
        if (request.getInitialQuantity() > 0) {
            stockTransactionService.logTransaction(
                tenantId, savedItem.getId(), StockTransaction.TransactionType.STOCK_IN,
                request.getInitialQuantity(), null, 0, savedItem.getAvailableQuantity(),
                0, 0, "INITIAL_STOCK", "Initial stock creation"
            );
        }
        
        logger.info("Created inventory item for tenant: {}, product: {}, SKU: {}", 
                   tenantId, request.getProductId(), request.getSku());
        
        return new InventoryItemResponse(savedItem);
    }

    /**
     * Update inventory item
     */
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @CacheEvict(value = {"inventory-items", "inventory-by-product"}, key = "#tenantId + ':' + #id")
    public InventoryItemResponse updateInventoryItem(String tenantId, Long id, UpdateInventoryItemRequest request) {
        InventoryItem existingItem = inventoryItemRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with ID: " + id));
        
        Integer previousAvailable = existingItem.getAvailableQuantity();
        
        updateInventoryItemFields(existingItem, request);
        InventoryItem savedItem = inventoryItemRepository.save(existingItem);
        
        // Log adjustment transaction if quantity changed
        if (request.getAvailableQuantity() != null && !request.getAvailableQuantity().equals(previousAvailable)) {
            Integer quantityDiff = request.getAvailableQuantity() - previousAvailable;
            StockTransaction.TransactionType transactionType = quantityDiff > 0 ? 
                StockTransaction.TransactionType.ADJUSTMENT_IN : StockTransaction.TransactionType.ADJUSTMENT_OUT;
            
            stockTransactionService.logTransaction(
                tenantId, savedItem.getId(), transactionType, Math.abs(quantityDiff),
                null, previousAvailable, savedItem.getAvailableQuantity(),
                savedItem.getReservedQuantity(), savedItem.getReservedQuantity(),
                "MANUAL_ADJUSTMENT", "Manual inventory adjustment"
            );
        }
        
        logger.info("Updated inventory item for tenant: {}, ID: {}", tenantId, id);
        
        return new InventoryItemResponse(savedItem);
    }

    /**
     * Delete inventory item
     */
    public void deleteInventoryItem(String tenantId, Long id) {
        InventoryItem item = inventoryItemRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with ID: " + id));
        
        inventoryItemRepository.delete(item);
        
        logger.info("Deleted inventory item for tenant: {}, ID: {}", tenantId, id);
    }

    /**
     * Add stock to inventory item
     */
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @CacheEvict(value = {"inventory-items", "inventory-by-product"}, key = "#tenantId + ':' + #id")
    public InventoryItemResponse addStock(String tenantId, Long id, StockAdjustmentRequest request) {
        InventoryItem item = inventoryItemRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with ID: " + id));
        
        Integer previousAvailable = item.getAvailableQuantity();
        item.addStock(request.getQuantity());
        InventoryItem savedItem = inventoryItemRepository.save(item);
        
        // Log stock in transaction
        stockTransactionService.logTransaction(
            tenantId, savedItem.getId(), StockTransaction.TransactionType.STOCK_IN,
            request.getQuantity(), request.getReferenceId(), previousAvailable, savedItem.getAvailableQuantity(),
            savedItem.getReservedQuantity(), savedItem.getReservedQuantity(),
            request.getReferenceType(), request.getReason()
        );
        
        logger.info("Added {} stock to inventory item for tenant: {}, ID: {}", 
                   request.getQuantity(), tenantId, id);
        
        return new InventoryItemResponse(savedItem);
    }

    /**
     * Remove stock from inventory item
     */
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @CacheEvict(value = {"inventory-items", "inventory-by-product"}, key = "#tenantId + ':' + #id")
    public InventoryItemResponse removeStock(String tenantId, Long id, StockAdjustmentRequest request) {
        InventoryItem item = inventoryItemRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with ID: " + id));
        
        if (!item.canReserve(request.getQuantity())) {
            throw new IllegalArgumentException("Insufficient stock to remove " + request.getQuantity() + " units");
        }
        
        Integer previousAvailable = item.getAvailableQuantity();
        item.setAvailableQuantity(item.getAvailableQuantity() - request.getQuantity());
        InventoryItem savedItem = inventoryItemRepository.save(item);
        
        // Log stock out transaction
        stockTransactionService.logTransaction(
            tenantId, savedItem.getId(), StockTransaction.TransactionType.STOCK_OUT,
            request.getQuantity(), request.getReferenceId(), previousAvailable, savedItem.getAvailableQuantity(),
            savedItem.getReservedQuantity(), savedItem.getReservedQuantity(),
            request.getReferenceType(), request.getReason()
        );
        
        logger.info("Removed {} stock from inventory item for tenant: {}, ID: {}", 
                   request.getQuantity(), tenantId, id);
        
        return new InventoryItemResponse(savedItem);
    }

    /**
     * Get low stock items
     */
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getLowStockItems(String tenantId) {
        List<InventoryItem> lowStockItems = inventoryItemRepository.findLowStockItems(tenantId);
        return lowStockItems.stream()
            .map(InventoryItemResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * Check stock availability for multiple products
     */
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> checkStockAvailability(String tenantId, List<String> productIds) {
        List<InventoryItem> items = inventoryItemRepository.findByTenantIdAndProductIdIn(tenantId, productIds);
        return items.stream()
            .map(InventoryItemResponse::new)
            .collect(Collectors.toList());
    }

    /**
     * Get inventory count by status
     */
    @Transactional(readOnly = true)
    public long getInventoryCountByStatus(String tenantId, InventoryItem.InventoryStatus status) {
        return inventoryItemRepository.countByTenantIdAndStatus(tenantId, status);
    }

    // Private helper methods
    private InventoryItem mapToInventoryItem(String tenantId, CreateInventoryItemRequest request) {
        InventoryItem item = new InventoryItem();
        item.setTenantId(tenantId);
        item.setProductId(request.getProductId());
        item.setSku(request.getSku());
        item.setAvailableQuantity(request.getInitialQuantity());
        item.setReorderLevel(request.getReorderLevel());
        item.setMaxStockLevel(request.getMaxStockLevel());
        item.setLocationCode(request.getLocationCode());
        
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }
        
        return item;
    }

    private void updateInventoryItemFields(InventoryItem item, UpdateInventoryItemRequest request) {
        if (request.getAvailableQuantity() != null) {
            item.setAvailableQuantity(request.getAvailableQuantity());
        }
        if (request.getReorderLevel() != null) {
            item.setReorderLevel(request.getReorderLevel());
        }
        if (request.getMaxStockLevel() != null) {
            item.setMaxStockLevel(request.getMaxStockLevel());
        }
        if (request.getLocationCode() != null) {
            item.setLocationCode(request.getLocationCode());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }
    }
}
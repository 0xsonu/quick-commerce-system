package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.shared.utils.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends TenantAwareRepository<InventoryItem, Long> {

    /**
     * Find inventory item by tenant ID and product ID with optimistic locking
     */
    @Lock(LockModeType.OPTIMISTIC)
    Optional<InventoryItem> findByTenantIdAndProductId(String tenantId, String productId);

    /**
     * Find inventory item by tenant ID and SKU with optimistic locking
     */
    @Lock(LockModeType.OPTIMISTIC)
    Optional<InventoryItem> findByTenantIdAndSku(String tenantId, String sku);

    /**
     * Find all inventory items by tenant ID and status
     */
    Page<InventoryItem> findByTenantIdAndStatus(String tenantId, InventoryItem.InventoryStatus status, Pageable pageable);

    /**
     * Find all inventory items by tenant ID and location
     */
    Page<InventoryItem> findByTenantIdAndLocationCode(String tenantId, String locationCode, Pageable pageable);

    /**
     * Find low stock items (available quantity <= reorder level)
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.tenantId = :tenantId " +
           "AND i.status = 'ACTIVE' " +
           "AND i.reorderLevel IS NOT NULL " +
           "AND i.availableQuantity <= i.reorderLevel")
    List<InventoryItem> findLowStockItems(@Param("tenantId") String tenantId);

    /**
     * Find items with available stock greater than specified quantity
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.tenantId = :tenantId " +
           "AND i.status = 'ACTIVE' " +
           "AND i.availableQuantity >= :quantity")
    List<InventoryItem> findItemsWithAvailableStock(@Param("tenantId") String tenantId, 
                                                   @Param("quantity") Integer quantity);

    /**
     * Check if product exists in inventory
     */
    boolean existsByTenantIdAndProductId(String tenantId, String productId);

    /**
     * Check if SKU exists in inventory
     */
    boolean existsByTenantIdAndSku(String tenantId, String sku);

    /**
     * Get total available quantity for a product across all locations
     */
    @Query("SELECT COALESCE(SUM(i.availableQuantity), 0) FROM InventoryItem i " +
           "WHERE i.tenantId = :tenantId AND i.productId = :productId AND i.status = 'ACTIVE'")
    Integer getTotalAvailableQuantityByProduct(@Param("tenantId") String tenantId, 
                                             @Param("productId") String productId);

    /**
     * Get total reserved quantity for a product across all locations
     */
    @Query("SELECT COALESCE(SUM(i.reservedQuantity), 0) FROM InventoryItem i " +
           "WHERE i.tenantId = :tenantId AND i.productId = :productId AND i.status = 'ACTIVE'")
    Integer getTotalReservedQuantityByProduct(@Param("tenantId") String tenantId, 
                                            @Param("productId") String productId);

    /**
     * Find items by multiple product IDs
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.tenantId = :tenantId " +
           "AND i.productId IN :productIds AND i.status = 'ACTIVE'")
    List<InventoryItem> findByTenantIdAndProductIdIn(@Param("tenantId") String tenantId, 
                                                    @Param("productIds") List<String> productIds);

    /**
     * Find items by multiple SKUs
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.tenantId = :tenantId " +
           "AND i.sku IN :skus AND i.status = 'ACTIVE'")
    List<InventoryItem> findByTenantIdAndSkuIn(@Param("tenantId") String tenantId, 
                                             @Param("skus") List<String> skus);

    /**
     * Count total inventory items by tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Count inventory items by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, InventoryItem.InventoryStatus status);
}
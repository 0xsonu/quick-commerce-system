package com.ecommerce.inventoryservice.entity;

import com.ecommerce.shared.models.TenantAware;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items", 
       indexes = {
           @Index(name = "idx_tenant_product", columnList = "tenant_id, product_id", unique = true),
           @Index(name = "idx_tenant_sku", columnList = "tenant_id, sku"),
           @Index(name = "idx_tenant_status", columnList = "tenant_id, status")
       })
public class InventoryItem implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @NotBlank(message = "Product ID is required")
    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @NotBlank(message = "SKU is required")
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @NotNull(message = "Reserved quantity is required")
    @Min(value = 0, message = "Reserved quantity cannot be negative")
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Min(value = 0, message = "Reorder level cannot be negative")
    @Column(name = "reorder_level")
    private Integer reorderLevel;

    @Min(value = 0, message = "Maximum stock level cannot be negative")
    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    @Column(name = "location_code", length = 50)
    private String locationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InventoryStatus status;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    // Constructors
    public InventoryItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = InventoryStatus.ACTIVE;
        this.availableQuantity = 0;
        this.reservedQuantity = 0;
    }

    public InventoryItem(String tenantId, String productId, String sku, Integer availableQuantity) {
        this();
        this.tenantId = tenantId;
        this.productId = productId;
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business methods
    public Integer getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }

    public boolean isLowStock() {
        return reorderLevel != null && availableQuantity <= reorderLevel;
    }

    public boolean canReserve(Integer quantity) {
        return quantity != null && quantity > 0 && availableQuantity >= quantity;
    }

    public void reserveStock(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalArgumentException("Insufficient stock to reserve " + quantity + " units");
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void releaseReservation(Integer quantity) {
        if (quantity == null || quantity <= 0 || reservedQuantity < quantity) {
            throw new IllegalArgumentException("Invalid quantity to release: " + quantity);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmReservation(Integer quantity) {
        if (quantity == null || quantity <= 0 || reservedQuantity < quantity) {
            throw new IllegalArgumentException("Invalid quantity to confirm: " + quantity);
        }
        this.reservedQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void addStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }
        this.availableQuantity += quantity;
        this.lastRestockedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Integer getMaxStockLevel() {
        return maxStockLevel;
    }

    public void setMaxStockLevel(Integer maxStockLevel) {
        this.maxStockLevel = maxStockLevel;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastRestockedAt() {
        return lastRestockedAt;
    }

    public void setLastRestockedAt(LocalDateTime lastRestockedAt) {
        this.lastRestockedAt = lastRestockedAt;
    }

    public enum InventoryStatus {
        ACTIVE, INACTIVE, DISCONTINUED, OUT_OF_STOCK
    }
}
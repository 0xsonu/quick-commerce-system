package com.ecommerce.inventoryservice.dto;

import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class InventoryItemResponse {

    private Long id;
    private String productId;
    private String sku;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private String locationCode;
    private InventoryItem.InventoryStatus status;
    private boolean lowStock;
    private Long version;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastRestockedAt;

    // Constructors
    public InventoryItemResponse() {}

    public InventoryItemResponse(InventoryItem inventoryItem) {
        this.id = inventoryItem.getId();
        this.productId = inventoryItem.getProductId();
        this.sku = inventoryItem.getSku();
        this.availableQuantity = inventoryItem.getAvailableQuantity();
        this.reservedQuantity = inventoryItem.getReservedQuantity();
        this.totalQuantity = inventoryItem.getTotalQuantity();
        this.reorderLevel = inventoryItem.getReorderLevel();
        this.maxStockLevel = inventoryItem.getMaxStockLevel();
        this.locationCode = inventoryItem.getLocationCode();
        this.status = inventoryItem.getStatus();
        this.lowStock = inventoryItem.isLowStock();
        this.version = inventoryItem.getVersion();
        this.createdAt = inventoryItem.getCreatedAt();
        this.updatedAt = inventoryItem.getUpdatedAt();
        this.lastRestockedAt = inventoryItem.getLastRestockedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
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

    public InventoryItem.InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryItem.InventoryStatus status) {
        this.status = status;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public void setLowStock(boolean lowStock) {
        this.lowStock = lowStock;
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
}
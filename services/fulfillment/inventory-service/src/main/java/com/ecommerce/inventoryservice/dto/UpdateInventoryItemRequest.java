package com.ecommerce.inventoryservice.dto;

import com.ecommerce.inventoryservice.entity.InventoryItem;
import jakarta.validation.constraints.Min;

public class UpdateInventoryItemRequest {

    @Min(value = 0, message = "Available quantity cannot be negative")
    private Integer availableQuantity;

    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    @Min(value = 0, message = "Maximum stock level cannot be negative")
    private Integer maxStockLevel;

    private String locationCode;

    private InventoryItem.InventoryStatus status;

    // Constructors
    public UpdateInventoryItemRequest() {}

    // Getters and Setters
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
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
}
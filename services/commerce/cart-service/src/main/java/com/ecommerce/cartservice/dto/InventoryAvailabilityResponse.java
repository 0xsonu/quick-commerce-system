package com.ecommerce.cartservice.dto;

/**
 * Response DTO for inventory availability from Inventory Service
 */
public class InventoryAvailabilityResponse {

    private String productId;
    private String sku;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private boolean available;
    private Integer requestedQuantity;
    private String message;

    public InventoryAvailabilityResponse() {}

    public InventoryAvailabilityResponse(String productId, String sku, Integer availableQuantity, 
                                       Integer reservedQuantity, boolean available, Integer requestedQuantity) {
        this.productId = productId;
        this.sku = sku;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.available = available;
        this.requestedQuantity = requestedQuantity;
    }

    // Getters and Setters
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Check if the requested quantity is available
     */
    public boolean canFulfillRequest() {
        return available && availableQuantity != null && requestedQuantity != null 
               && availableQuantity >= requestedQuantity;
    }
}
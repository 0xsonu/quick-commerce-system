package com.ecommerce.cartservice.dto;

/**
 * Response DTO for inventory check operations
 */
public class InventoryCheckResponse {

    private String productId;
    private boolean available;
    private int availableQuantity;
    private int requestedQuantity;

    public InventoryCheckResponse() {}

    public InventoryCheckResponse(String productId, boolean available, int availableQuantity, int requestedQuantity) {
        this.productId = productId;
        this.available = available;
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }
}
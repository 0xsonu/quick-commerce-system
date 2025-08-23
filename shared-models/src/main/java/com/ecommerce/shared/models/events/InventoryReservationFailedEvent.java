package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Event published when inventory reservation fails
 */
public class InventoryReservationFailedEvent extends DomainEvent {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotEmpty(message = "Failed items are required")
    private List<FailedItemData> failedItems;

    @NotBlank(message = "Failure reason is required")
    private String reason;

    public InventoryReservationFailedEvent() {
        super();
        setEventType("INVENTORY_RESERVATION_FAILED");
    }

    public InventoryReservationFailedEvent(String tenantId, String orderId, 
                                         List<FailedItemData> failedItems, String reason) {
        super(tenantId, "INVENTORY_RESERVATION_FAILED");
        this.orderId = orderId;
        this.failedItems = failedItems;
        this.reason = reason;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<FailedItemData> getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(List<FailedItemData> failedItems) {
        this.failedItems = failedItems;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Failed item data for events
     */
    public static class FailedItemData {
        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotBlank(message = "SKU is required")
        private String sku;

        @NotBlank(message = "Requested quantity is required")
        private Integer requestedQuantity;

        @NotBlank(message = "Available quantity is required")
        private Integer availableQuantity;

        @NotBlank(message = "Failure reason is required")
        private String failureReason;

        public FailedItemData() {}

        public FailedItemData(String productId, String sku, Integer requestedQuantity, 
                            Integer availableQuantity, String failureReason) {
            this.productId = productId;
            this.sku = sku;
            this.requestedQuantity = requestedQuantity;
            this.availableQuantity = availableQuantity;
            this.failureReason = failureReason;
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

        public Integer getRequestedQuantity() {
            return requestedQuantity;
        }

        public void setRequestedQuantity(Integer requestedQuantity) {
            this.requestedQuantity = requestedQuantity;
        }

        public Integer getAvailableQuantity() {
            return availableQuantity;
        }

        public void setAvailableQuantity(Integer availableQuantity) {
            this.availableQuantity = availableQuantity;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }
    }
}
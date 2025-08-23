package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Event published when inventory reservation is released
 */
public class InventoryReleasedEvent extends DomainEvent {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Reservation ID is required")
    private String reservationId;

    @NotEmpty(message = "Released items are required")
    private List<ReleasedItemData> releasedItems;

    @NotBlank(message = "Release reason is required")
    private String reason;

    public InventoryReleasedEvent() {
        super();
        setEventType("INVENTORY_RELEASED");
    }

    public InventoryReleasedEvent(String tenantId, String orderId, String reservationId, 
                                List<ReleasedItemData> releasedItems, String reason) {
        super(tenantId, "INVENTORY_RELEASED");
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.releasedItems = releasedItems;
        this.reason = reason;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public List<ReleasedItemData> getReleasedItems() {
        return releasedItems;
    }

    public void setReleasedItems(List<ReleasedItemData> releasedItems) {
        this.releasedItems = releasedItems;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Released item data for events
     */
    public static class ReleasedItemData {
        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotBlank(message = "SKU is required")
        private String sku;

        @NotNull(message = "Released quantity is required")
        @Positive(message = "Released quantity must be positive")
        private Integer releasedQuantity;

        @NotNull(message = "Inventory item ID is required")
        private Long inventoryItemId;

        public ReleasedItemData() {}

        public ReleasedItemData(String productId, String sku, Integer releasedQuantity, Long inventoryItemId) {
            this.productId = productId;
            this.sku = sku;
            this.releasedQuantity = releasedQuantity;
            this.inventoryItemId = inventoryItemId;
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

        public Integer getReleasedQuantity() {
            return releasedQuantity;
        }

        public void setReleasedQuantity(Integer releasedQuantity) {
            this.releasedQuantity = releasedQuantity;
        }

        public Long getInventoryItemId() {
            return inventoryItemId;
        }

        public void setInventoryItemId(Long inventoryItemId) {
            this.inventoryItemId = inventoryItemId;
        }
    }
}
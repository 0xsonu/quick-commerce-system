package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Event published when inventory is successfully reserved
 */
public class InventoryReservedEvent extends DomainEvent {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Reservation ID is required")
    private String reservationId;

    @NotEmpty(message = "Reserved items are required")
    private List<ReservedItemData> reservedItems;

    public InventoryReservedEvent() {
        super();
        setEventType("INVENTORY_RESERVED");
    }

    public InventoryReservedEvent(String tenantId, String orderId, String reservationId, 
                                List<ReservedItemData> reservedItems) {
        super(tenantId, "INVENTORY_RESERVED");
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.reservedItems = reservedItems;
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

    public List<ReservedItemData> getReservedItems() {
        return reservedItems;
    }

    public void setReservedItems(List<ReservedItemData> reservedItems) {
        this.reservedItems = reservedItems;
    }

    /**
     * Reserved item data for events
     */
    public static class ReservedItemData {
        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotBlank(message = "SKU is required")
        private String sku;

        @NotNull(message = "Reserved quantity is required")
        @Positive(message = "Reserved quantity must be positive")
        private Integer reservedQuantity;

        @NotNull(message = "Inventory item ID is required")
        private Long inventoryItemId;

        public ReservedItemData() {}

        public ReservedItemData(String productId, String sku, Integer reservedQuantity, Long inventoryItemId) {
            this.productId = productId;
            this.sku = sku;
            this.reservedQuantity = reservedQuantity;
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

        public Integer getReservedQuantity() {
            return reservedQuantity;
        }

        public void setReservedQuantity(Integer reservedQuantity) {
            this.reservedQuantity = reservedQuantity;
        }

        public Long getInventoryItemId() {
            return inventoryItemId;
        }

        public void setInventoryItemId(Long inventoryItemId) {
            this.inventoryItemId = inventoryItemId;
        }
    }
}
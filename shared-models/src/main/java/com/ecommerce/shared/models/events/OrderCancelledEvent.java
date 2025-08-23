package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Event published when an order is cancelled
 */
public class OrderCancelledEvent extends DomainEvent {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotEmpty(message = "Order items are required")
    private List<OrderItemData> items;

    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    public OrderCancelledEvent() {
        super();
        setEventType("ORDER_CANCELLED");
    }

    public OrderCancelledEvent(String tenantId, String orderId, String userId, 
                             List<OrderItemData> items, String reason) {
        super(tenantId, "ORDER_CANCELLED");
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
        this.reason = reason;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<OrderItemData> getItems() {
        return items;
    }

    public void setItems(List<OrderItemData> items) {
        this.items = items;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Order item data for events
     */
    public static class OrderItemData {
        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotBlank(message = "SKU is required")
        private String sku;

        @NotBlank(message = "Quantity is required")
        private Integer quantity;

        public OrderItemData() {}

        public OrderItemData(String productId, String sku, Integer quantity) {
            this.productId = productId;
            this.sku = sku;
            this.quantity = quantity;
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

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
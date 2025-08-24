package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Event published when an order starts processing
 */
public class OrderProcessingEvent extends DomainEvent {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotEmpty(message = "Order items are required")
    private List<OrderItemData> items;

    public OrderProcessingEvent() {
        super();
        setEventType("ORDER_PROCESSING");
    }

    public OrderProcessingEvent(String tenantId, String orderId, String userId, List<OrderItemData> items) {
        super(tenantId, "ORDER_PROCESSING");
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
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
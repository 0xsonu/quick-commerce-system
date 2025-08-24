package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when an order is delivered
 */
public class OrderDeliveredEvent extends DomainEvent {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotEmpty(message = "Order items are required")
    private List<OrderItemData> items;

    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    @NotBlank(message = "Carrier is required")
    private String carrier;

    private LocalDateTime deliveredAt;

    private String deliverySignature;

    public OrderDeliveredEvent() {
        super();
        setEventType("ORDER_DELIVERED");
    }

    public OrderDeliveredEvent(String tenantId, String orderId, String userId, 
                             List<OrderItemData> items, String trackingNumber, 
                             String carrier, LocalDateTime deliveredAt, String deliverySignature) {
        super(tenantId, "ORDER_DELIVERED");
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.deliveredAt = deliveredAt;
        this.deliverySignature = deliverySignature;
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

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getDeliverySignature() {
        return deliverySignature;
    }

    public void setDeliverySignature(String deliverySignature) {
        this.deliverySignature = deliverySignature;
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
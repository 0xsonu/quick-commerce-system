package com.ecommerce.inventoryservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class ReservationRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotEmpty(message = "Items are required")
    @Valid
    private List<ReservationItemRequest> items;

    public ReservationRequest() {}

    public ReservationRequest(String orderId, List<ReservationItemRequest> items) {
        this.orderId = orderId;
        this.items = items;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<ReservationItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ReservationItemRequest> items) {
        this.items = items;
    }

    public static class ReservationItemRequest {
        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotBlank(message = "SKU is required")
        private String sku;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;

        public ReservationItemRequest() {}

        public ReservationItemRequest(String productId, String sku, Integer quantity) {
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
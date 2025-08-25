package com.ecommerce.shippingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ShipmentItemRequest {

    @NotNull
    private Long orderItemId;

    @NotBlank
    private String productId;

    @NotBlank
    private String sku;

    @NotBlank
    private String productName;

    @NotNull
    @Positive
    private Integer quantity;

    private BigDecimal weightKg;

    // Constructors
    public ShipmentItemRequest() {}

    public ShipmentItemRequest(Long orderItemId, String productId, String sku, 
                              String productName, Integer quantity) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }
}
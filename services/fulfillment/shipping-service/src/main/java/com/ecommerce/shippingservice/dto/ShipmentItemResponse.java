package com.ecommerce.shippingservice.dto;

import java.math.BigDecimal;

public class ShipmentItemResponse {

    private Long id;
    private Long orderItemId;
    private String productId;
    private String sku;
    private String productName;
    private Integer quantity;
    private BigDecimal weightKg;

    // Constructors
    public ShipmentItemResponse() {}

    public ShipmentItemResponse(Long id, Long orderItemId, String productId, String sku,
                               String productName, Integer quantity, BigDecimal weightKg) {
        this.id = id;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
        this.weightKg = weightKg;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
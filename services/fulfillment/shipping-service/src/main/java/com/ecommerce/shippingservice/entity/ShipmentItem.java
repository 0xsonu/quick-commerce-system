package com.ecommerce.shippingservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Entity
@Table(name = "shipment_items", indexes = {
    @Index(name = "idx_shipment_id", columnList = "shipment_id"),
    @Index(name = "idx_order_item_id", columnList = "order_item_id"),
    @Index(name = "idx_product_id", columnList = "product_id")
})
public class ShipmentItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    @JsonBackReference
    private Shipment shipment;

    @NotNull
    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @NotBlank
    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @NotBlank
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @NotBlank
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Positive
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;

    // Constructors
    public ShipmentItem() {}

    public ShipmentItem(Long orderItemId, String productId, String sku, String productName, Integer quantity) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
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
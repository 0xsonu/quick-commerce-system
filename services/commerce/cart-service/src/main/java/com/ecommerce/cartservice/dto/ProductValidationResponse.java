package com.ecommerce.cartservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Response DTO for product validation from Product Service
 */
public class ProductValidationResponse {

    @JsonProperty("id")
    private String productId;

    private String name;
    private String sku;
    private BigDecimal price;
    private String status;
    private boolean active;
    private String category;
    private String brand;

    public ProductValidationResponse() {}

    public ProductValidationResponse(String productId, String name, String sku, BigDecimal price, String status, boolean active) {
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.status = status;
        this.active = active;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    // Additional fields for gRPC compatibility
    private boolean valid;
    private String currency;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Check if product is available for purchase
     */
    public boolean isAvailableForPurchase() {
        return active && "ACTIVE".equalsIgnoreCase(status);
    }
}
package com.ecommerce.shared.models.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Event published when a product is deleted
 */
public class ProductDeletedEvent extends DomainEvent {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    private String category;

    private String subcategory;

    private String brand;

    @NotBlank(message = "SKU is required")
    private String sku;

    private BigDecimal price;

    private String currency;

    private Map<String, Object> attributes;

    private String status;

    @JsonProperty("correlation_id")
    private String correlationId;

    // Reason for deletion (optional)
    private String deletionReason;

    public ProductDeletedEvent() {
        super();
        setEventType("ProductDeleted");
    }

    public ProductDeletedEvent(String tenantId, String productId, String name, String description,
                              String category, String subcategory, String brand, String sku,
                              BigDecimal price, String currency, Map<String, Object> attributes,
                              String status, String correlationId, String deletionReason) {
        super(tenantId, "ProductDeleted");
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.subcategory = subcategory;
        this.brand = brand;
        this.sku = sku;
        this.price = price;
        this.currency = currency;
        this.attributes = attributes;
        this.status = status;
        this.correlationId = correlationId;
        this.deletionReason = deletionReason;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public void setDeletionReason(String deletionReason) {
        this.deletionReason = deletionReason;
    }
}
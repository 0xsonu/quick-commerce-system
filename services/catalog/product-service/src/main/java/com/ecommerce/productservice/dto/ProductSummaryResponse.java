package com.ecommerce.productservice.dto;

import com.ecommerce.productservice.entity.Product;

import java.math.BigDecimal;

public class ProductSummaryResponse {

    private String id;
    private String name;
    private String brand;
    private String category;
    private String subcategory;
    private String sku;
    private PriceDto price;
    private String primaryImageUrl;
    private Product.ProductStatus status;

    // Constructors
    public ProductSummaryResponse() {}

    public ProductSummaryResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.brand = product.getBrand();
        this.category = product.getCategory();
        this.subcategory = product.getSubcategory();
        this.sku = product.getSku();
        
        if (product.getPrice() != null) {
            this.price = new PriceDto(product.getPrice().getAmount(), product.getPrice().getCurrency());
        }
        
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            this.primaryImageUrl = product.getImages().stream()
                .filter(Product.ProductImage::isPrimary)
                .findFirst()
                .map(Product.ProductImage::getUrl)
                .orElse(product.getImages().get(0).getUrl());
        }
        
        this.status = product.getStatus();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
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

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public PriceDto getPrice() {
        return price;
    }

    public void setPrice(PriceDto price) {
        this.price = price;
    }

    public String getPrimaryImageUrl() {
        return primaryImageUrl;
    }

    public void setPrimaryImageUrl(String primaryImageUrl) {
        this.primaryImageUrl = primaryImageUrl;
    }

    public Product.ProductStatus getStatus() {
        return status;
    }

    public void setStatus(Product.ProductStatus status) {
        this.status = status;
    }

    // Nested DTO
    public static class PriceDto {
        private BigDecimal amount;
        private String currency;

        public PriceDto() {}

        public PriceDto(BigDecimal amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
package com.ecommerce.productservice.dto;

import com.ecommerce.productservice.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProductResponse {

    private String id;
    private String name;
    private String description;
    private String category;
    private String subcategory;
    private String brand;
    private String sku;
    private PriceDto price;
    private List<ProductImageDto> images;
    private Map<String, Object> attributes;
    private SEODto seo;
    private Product.ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ProductResponse() {}

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.category = product.getCategory();
        this.subcategory = product.getSubcategory();
        this.brand = product.getBrand();
        this.sku = product.getSku();
        
        if (product.getPrice() != null) {
            this.price = new PriceDto(product.getPrice().getAmount(), product.getPrice().getCurrency());
        }
        
        if (product.getImages() != null) {
            this.images = product.getImages().stream()
                .map(img -> new ProductImageDto(img.getUrl(), img.getAltText(), img.isPrimary()))
                .toList();
        }
        
        this.attributes = product.getAttributes();
        
        if (product.getSeo() != null) {
            this.seo = new SEODto(product.getSeo().getMetaTitle(), 
                                 product.getSeo().getMetaDescription(), 
                                 product.getSeo().getSlug());
        }
        
        this.status = product.getStatus();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
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

    public PriceDto getPrice() {
        return price;
    }

    public void setPrice(PriceDto price) {
        this.price = price;
    }

    public List<ProductImageDto> getImages() {
        return images;
    }

    public void setImages(List<ProductImageDto> images) {
        this.images = images;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public SEODto getSeo() {
        return seo;
    }

    public void setSeo(SEODto seo) {
        this.seo = seo;
    }

    public Product.ProductStatus getStatus() {
        return status;
    }

    public void setStatus(Product.ProductStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Nested DTOs
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

    public static class ProductImageDto {
        private String url;
        private String altText;
        private boolean isPrimary;

        public ProductImageDto() {}

        public ProductImageDto(String url, String altText, boolean isPrimary) {
            this.url = url;
            this.altText = altText;
            this.isPrimary = isPrimary;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getAltText() {
            return altText;
        }

        public void setAltText(String altText) {
            this.altText = altText;
        }

        public boolean isPrimary() {
            return isPrimary;
        }

        public void setPrimary(boolean primary) {
            isPrimary = primary;
        }
    }

    public static class SEODto {
        private String metaTitle;
        private String metaDescription;
        private String slug;

        public SEODto() {}

        public SEODto(String metaTitle, String metaDescription, String slug) {
            this.metaTitle = metaTitle;
            this.metaDescription = metaDescription;
            this.slug = slug;
        }

        public String getMetaTitle() {
            return metaTitle;
        }

        public void setMetaTitle(String metaTitle) {
            this.metaTitle = metaTitle;
        }

        public String getMetaDescription() {
            return metaDescription;
        }

        public void setMetaDescription(String metaDescription) {
            this.metaDescription = metaDescription;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }
}
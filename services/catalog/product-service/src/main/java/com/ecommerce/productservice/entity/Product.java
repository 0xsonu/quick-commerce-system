package com.ecommerce.productservice.entity;

import com.ecommerce.shared.models.TenantAware;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
@CompoundIndex(def = "{'tenantId': 1, 'sku': 1}", unique = true)
@CompoundIndex(def = "{'tenantId': 1, 'status': 1}")
@CompoundIndex(def = "{'tenantId': 1, 'category': 1, 'subcategory': 1}")
public class Product implements TenantAware {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @TextIndexed(weight = 3)
    private String name;

    @TextIndexed(weight = 2)
    private String description;

    @Indexed
    private String category;

    @Indexed
    private String subcategory;

    @TextIndexed(weight = 2)
    private String brand;

    @Indexed
    private String sku;

    private Price price;

    private List<ProductImage> images;

    private Map<String, Object> attributes;

    private SEO seo;

    @Indexed
    private ProductStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructors
    public Product() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ProductStatus.ACTIVE;
    }

    public Product(String tenantId, String name, String description, String category, 
                   String brand, String sku, Price price) {
        this();
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.sku = sku;
        this.price = price;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
        this.updatedAt = LocalDateTime.now();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
        this.updatedAt = LocalDateTime.now();
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.updatedAt = LocalDateTime.now();
    }

    public SEO getSeo() {
        return seo;
    }

    public void setSeo(SEO seo) {
        this.seo = seo;
        this.updatedAt = LocalDateTime.now();
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
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

    // Nested classes for embedded documents
    public static class Price {
        private BigDecimal amount;
        private String currency;

        public Price() {}

        public Price(BigDecimal amount, String currency) {
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

    public static class ProductImage {
        private String url;
        private String altText;
        private boolean isPrimary;

        public ProductImage() {}

        public ProductImage(String url, String altText, boolean isPrimary) {
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

    public static class SEO {
        private String metaTitle;
        private String metaDescription;
        private String slug;

        public SEO() {}

        public SEO(String metaTitle, String metaDescription, String slug) {
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

    public enum ProductStatus {
        ACTIVE, INACTIVE, DISCONTINUED, OUT_OF_STOCK
    }
}
package com.ecommerce.productservice.dto;

import com.ecommerce.productservice.entity.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class UpdateProductRequest {

    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Product description must not exceed 2000 characters")
    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Size(max = 100, message = "Subcategory must not exceed 100 characters")
    private String subcategory;

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    @Valid
    private CreateProductRequest.PriceDto price;

    @Valid
    private List<CreateProductRequest.ProductImageDto> images;

    private Map<String, Object> attributes;

    @Valid
    private CreateProductRequest.SEODto seo;

    private Product.ProductStatus status;

    // Constructors
    public UpdateProductRequest() {}

    // Getters and Setters
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

    public CreateProductRequest.PriceDto getPrice() {
        return price;
    }

    public void setPrice(CreateProductRequest.PriceDto price) {
        this.price = price;
    }

    public List<CreateProductRequest.ProductImageDto> getImages() {
        return images;
    }

    public void setImages(List<CreateProductRequest.ProductImageDto> images) {
        this.images = images;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public CreateProductRequest.SEODto getSeo() {
        return seo;
    }

    public void setSeo(CreateProductRequest.SEODto seo) {
        this.seo = seo;
    }

    public Product.ProductStatus getStatus() {
        return status;
    }

    public void setStatus(Product.ProductStatus status) {
        this.status = status;
    }
}
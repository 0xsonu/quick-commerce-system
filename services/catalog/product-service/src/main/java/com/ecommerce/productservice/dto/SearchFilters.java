package com.ecommerce.productservice.dto;

import com.ecommerce.productservice.entity.Product;

import java.math.BigDecimal;
import java.util.Objects;

public class SearchFilters {

    private String searchText;
    private String category;
    private String subcategory;
    private String brand;
    private Product.ProductStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
    private String sortDirection;

    // Constructors
    public SearchFilters() {}

    public SearchFilters(String searchText, String category, String subcategory, String brand, 
                        Product.ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                        String sortBy, String sortDirection) {
        this.searchText = searchText;
        this.category = category;
        this.subcategory = subcategory;
        this.brand = brand;
        this.status = status;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    // Getters and Setters
    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
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

    public Product.ProductStatus getStatus() {
        return status;
    }

    public void setStatus(Product.ProductStatus status) {
        this.status = status;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    // Override equals and hashCode for caching
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchFilters that = (SearchFilters) o;
        return Objects.equals(searchText, that.searchText) &&
               Objects.equals(category, that.category) &&
               Objects.equals(subcategory, that.subcategory) &&
               Objects.equals(brand, that.brand) &&
               status == that.status &&
               Objects.equals(minPrice, that.minPrice) &&
               Objects.equals(maxPrice, that.maxPrice) &&
               Objects.equals(sortBy, that.sortBy) &&
               Objects.equals(sortDirection, that.sortDirection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchText, category, subcategory, brand, status, 
                          minPrice, maxPrice, sortBy, sortDirection);
    }

    @Override
    public String toString() {
        return "SearchFilters{" +
               "searchText='" + searchText + '\'' +
               ", category='" + category + '\'' +
               ", subcategory='" + subcategory + '\'' +
               ", brand='" + brand + '\'' +
               ", status=" + status +
               ", minPrice=" + minPrice +
               ", maxPrice=" + maxPrice +
               ", sortBy='" + sortBy + '\'' +
               ", sortDirection='" + sortDirection + '\'' +
               '}';
    }
}
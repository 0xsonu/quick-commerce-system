package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.PagedResponse;
import com.ecommerce.productservice.dto.ProductSummaryResponse;
import com.ecommerce.productservice.dto.SearchFilters;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductSearchService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductSearchService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "product-search", key = "#tenantId + ':' + #filters.hashCode() + ':' + #page + ':' + #size")
    public PagedResponse<ProductSummaryResponse> searchProducts(String tenantId, SearchFilters filters, 
                                                              int page, int size) {
        
        Sort sort = createSort(filters.getSortBy(), filters.getSortDirection());
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> productPage;
        
        if (StringUtils.hasText(filters.getSearchText())) {
            // Text search with filters
            if (hasAdvancedFilters(filters)) {
                productPage = productRepository.findByTenantIdAndTextSearchWithFilters(
                    tenantId, filters.getSearchText(), filters.getCategory(), 
                    filters.getBrand(), filters.getStatus(), pageable);
            } else {
                productPage = productRepository.findByTenantIdAndTextSearch(
                    tenantId, filters.getSearchText(), pageable);
            }
        } else if (filters.getMinPrice() != null || filters.getMaxPrice() != null) {
            // Price range search
            double minPrice = filters.getMinPrice() != null ? filters.getMinPrice().doubleValue() : 0.0;
            double maxPrice = filters.getMaxPrice() != null ? filters.getMaxPrice().doubleValue() : Double.MAX_VALUE;
            productPage = productRepository.findByTenantIdAndPriceRange(tenantId, minPrice, maxPrice, pageable);
        } else if (hasBasicFilters(filters)) {
            // Filter-based search
            if (StringUtils.hasText(filters.getCategory()) && StringUtils.hasText(filters.getSubcategory())) {
                productPage = productRepository.findByTenantIdAndCategoryAndSubcategory(
                    tenantId, filters.getCategory(), filters.getSubcategory(), pageable);
            } else if (StringUtils.hasText(filters.getCategory())) {
                productPage = productRepository.findByTenantIdAndCategory(tenantId, filters.getCategory(), pageable);
            } else if (StringUtils.hasText(filters.getBrand())) {
                productPage = productRepository.findByTenantIdAndBrand(tenantId, filters.getBrand(), pageable);
            } else if (filters.getStatus() != null) {
                productPage = productRepository.findByTenantIdAndStatus(tenantId, filters.getStatus(), pageable);
            } else {
                productPage = productRepository.findByTenantId(tenantId, pageable);
            }
        } else {
            // Get all products
            productPage = productRepository.findByTenantId(tenantId, pageable);
        }
        
        List<ProductSummaryResponse> content = productPage.getContent().stream()
            .map(ProductSummaryResponse::new)
            .collect(Collectors.toList());
        
        return new PagedResponse<>(content, productPage.getNumber(), productPage.getSize(),
                                 productPage.getTotalElements(), productPage.getTotalPages());
    }

    @Cacheable(value = "product-search", key = "#tenantId + ':similar:' + #productId")
    public List<ProductSummaryResponse> findSimilarProducts(String tenantId, String productId, int limit) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId).orElse(null);
        if (product == null) {
            return List.of();
        }
        
        // Find products in same category and subcategory, excluding the current product
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Product> similarProducts;
        
        if (StringUtils.hasText(product.getSubcategory())) {
            similarProducts = productRepository.findByTenantIdAndCategoryAndSubcategory(
                tenantId, product.getCategory(), product.getSubcategory(), pageable);
        } else {
            similarProducts = productRepository.findByTenantIdAndCategory(
                tenantId, product.getCategory(), pageable);
        }
        
        return similarProducts.getContent().stream()
            .filter(p -> !p.getId().equals(productId)) // Exclude the current product
            .map(ProductSummaryResponse::new)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "product-search", key = "#tenantId + ':popular:' + #category + ':' + #limit")
    public List<ProductSummaryResponse> findPopularProducts(String tenantId, String category, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Product> products;
        
        if (StringUtils.hasText(category)) {
            products = productRepository.findByTenantIdAndCategory(tenantId, category, pageable);
        } else {
            products = productRepository.findByTenantIdAndStatus(tenantId, Product.ProductStatus.ACTIVE, pageable);
        }
        
        return products.getContent().stream()
            .map(ProductSummaryResponse::new)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "product-search", key = "#tenantId + ':recent:' + #limit")
    public List<ProductSummaryResponse> findRecentProducts(String tenantId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.findByTenantIdAndStatus(
            tenantId, Product.ProductStatus.ACTIVE, pageable);
        
        return products.getContent().stream()
            .map(ProductSummaryResponse::new)
            .collect(Collectors.toList());
    }

    public List<String> getSearchSuggestions(String tenantId, String query, int limit) {
        if (!StringUtils.hasText(query) || query.length() < 2) {
            return List.of();
        }
        
        // Simple implementation - in production, you might use Elasticsearch or similar
        Pageable pageable = PageRequest.of(0, limit);
        Page<Product> products = productRepository.findByTenantIdAndTextSearch(tenantId, query, pageable);
        
        return products.getContent().stream()
            .map(Product::getName)
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }

    // Private helper methods
    private Sort createSort(String sortBy, String sortDirection) {
        if (!StringUtils.hasText(sortBy)) {
            sortBy = "updatedAt";
        }
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        return Sort.by(direction, sortBy);
    }

    private boolean hasAdvancedFilters(SearchFilters filters) {
        return StringUtils.hasText(filters.getCategory()) || 
               StringUtils.hasText(filters.getBrand()) || 
               filters.getStatus() != null ||
               filters.getMinPrice() != null ||
               filters.getMaxPrice() != null;
    }

    private boolean hasBasicFilters(SearchFilters filters) {
        return StringUtils.hasText(filters.getCategory()) || 
               StringUtils.hasText(filters.getSubcategory()) ||
               StringUtils.hasText(filters.getBrand()) || 
               filters.getStatus() != null;
    }
}
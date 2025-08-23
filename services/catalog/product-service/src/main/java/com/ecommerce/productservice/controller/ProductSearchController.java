package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.service.ProductRecommendationService;
import com.ecommerce.productservice.service.ProductSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final ProductRecommendationService recommendationService;

    @Autowired
    public ProductSearchController(ProductSearchService productSearchService,
                                 ProductRecommendationService recommendationService) {
        this.productSearchService = productSearchService;
        this.recommendationService = recommendationService;
    }

    @PostMapping("/advanced-search")
    public ResponseEntity<PagedResponse<ProductSummaryResponse>> advancedSearch(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody SearchFilters filters,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        PagedResponse<ProductSummaryResponse> response = productSearchService.searchProducts(
            tenantId, filters, page, size);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<PagedResponse<ProductSummaryResponse>> filterProducts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Product.ProductStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        SearchFilters filters = new SearchFilters(searchText, category, subcategory, brand, 
                                                status, minPrice, maxPrice, sortBy, sortDirection);
        
        PagedResponse<ProductSummaryResponse> response = productSearchService.searchProducts(
            tenantId, filters, page, size);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {
        
        List<String> suggestions = productSearchService.getSearchSuggestions(tenantId, query, limit);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/{productId}/similar")
    public ResponseEntity<List<ProductSummaryResponse>> getSimilarProducts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        
        List<ProductSummaryResponse> similar = recommendationService.getSimilarProducts(
            tenantId, productId, limit);
        
        return ResponseEntity.ok(similar);
    }

    @GetMapping("/{productId}/frequently-bought-together")
    public ResponseEntity<List<ProductSummaryResponse>> getFrequentlyBoughtTogether(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        
        List<ProductSummaryResponse> recommendations = recommendationService.getFrequentlyBoughtTogether(
            tenantId, productId, limit);
        
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<ProductSummaryResponse>> getTrendingProducts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        
        List<ProductSummaryResponse> trending = recommendationService.getTrendingProducts(
            tenantId, category, limit);
        
        return ResponseEntity.ok(trending);
    }

    @PostMapping("/personalized-recommendations")
    public ResponseEntity<List<ProductSummaryResponse>> getPersonalizedRecommendations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody ProductRecommendationService.UserPreferences userPreferences,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        
        List<ProductSummaryResponse> recommendations = recommendationService.getPersonalizedRecommendations(
            tenantId, userPreferences, limit);
        
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<List<ProductSummaryResponse>> getNewArrivals(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        
        List<ProductSummaryResponse> newArrivals = recommendationService.getNewArrivals(tenantId, limit);
        return ResponseEntity.ok(newArrivals);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductSummaryResponse>> getFeaturedProducts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        
        List<ProductSummaryResponse> featured = recommendationService.getFeaturedProducts(tenantId, limit);
        return ResponseEntity.ok(featured);
    }

    @PostMapping("/cross-sell")
    public ResponseEntity<List<ProductSummaryResponse>> getCrossSellRecommendations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody List<String> cartItems,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        
        List<ProductSummaryResponse> crossSell = recommendationService.getCrossSellRecommendations(
            tenantId, cartItems, limit);
        
        return ResponseEntity.ok(crossSell);
    }
}
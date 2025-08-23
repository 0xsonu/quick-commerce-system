package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ProductSummaryResponse>> getProducts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Product.ProductStatus status,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        PagedResponse<ProductSummaryResponse> response = productService.getProducts(
            tenantId, page, size, category, subcategory, brand, search, status, sortBy, sortDirection);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId) {
        
        ProductResponse response = productService.getProduct(tenantId, productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String sku) {
        
        ProductResponse response = productService.getProductBySku(tenantId, sku);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateProductRequest request) {
        
        ProductResponse response = productService.createProduct(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductRequest request) {
        
        ProductResponse response = productService.updateProduct(tenantId, productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId) {
        
        productService.deleteProduct(tenantId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductSummaryResponse>> searchProducts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        PagedResponse<ProductSummaryResponse> response = productService.searchProducts(tenantId, q, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<String> categories = productService.getCategories(tenantId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getBrands(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<String> brands = productService.getBrands(tenantId);
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getProductCount(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) Product.ProductStatus status) {
        
        long count = status != null ? 
            productService.getProductCountByStatus(tenantId, status) :
            productService.getProductCount(tenantId);
        
        return ResponseEntity.ok(count);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service is healthy");
    }
}
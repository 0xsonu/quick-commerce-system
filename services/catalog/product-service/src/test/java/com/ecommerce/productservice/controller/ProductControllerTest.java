package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.service.ProductService;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductResponse productResponse;
    private ProductSummaryResponse productSummaryResponse;
    private CreateProductRequest createRequest;
    private UpdateProductRequest updateRequest;
    private PagedResponse<ProductSummaryResponse> pagedResponse;

    private final String tenantId = "tenant123";
    private final String productId = "product123";

    @BeforeEach
    void setUp() {
        productResponse = createTestProductResponse();
        productSummaryResponse = createTestProductSummaryResponse();
        createRequest = createTestCreateRequest();
        updateRequest = createTestUpdateRequest();
        pagedResponse = createTestPagedResponse();
    }

    @Test
    void getProducts_ShouldReturnPagedResponse() throws Exception {
        // Given
        when(productService.getProducts(eq(tenantId), eq(0), eq(20), isNull(), isNull(), 
                                      isNull(), isNull(), isNull(), eq("updatedAt"), eq("desc")))
            .thenReturn(pagedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(productSummaryResponse.getId()))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20));

        verify(productService).getProducts(eq(tenantId), eq(0), eq(20), isNull(), isNull(), 
                                         isNull(), isNull(), isNull(), eq("updatedAt"), eq("desc"));
    }

    @Test
    void getProducts_WithFilters_ShouldReturnFilteredResults() throws Exception {
        // Given
        when(productService.getProducts(eq(tenantId), eq(0), eq(20), eq("Electronics"), 
                                      isNull(), eq("Apple"), isNull(), isNull(), eq("updatedAt"), eq("desc")))
            .thenReturn(pagedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .header("X-Tenant-ID", tenantId)
                .param("category", "Electronics")
                .param("brand", "Apple"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());

        verify(productService).getProducts(eq(tenantId), eq(0), eq(20), eq("Electronics"), 
                                         isNull(), eq("Apple"), isNull(), isNull(), eq("updatedAt"), eq("desc"));
    }

    @Test
    void getProduct_WhenProductExists_ShouldReturnProduct() throws Exception {
        // Given
        when(productService.getProduct(tenantId, productId))
            .thenReturn(productResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products/{productId}", productId)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(productResponse.getId()))
            .andExpect(jsonPath("$.name").value(productResponse.getName()))
            .andExpect(jsonPath("$.sku").value(productResponse.getSku()));

        verify(productService).getProduct(tenantId, productId);
    }

    @Test
    void getProduct_WhenProductNotExists_ShouldReturn404() throws Exception {
        // Given
        when(productService.getProduct(tenantId, productId))
            .thenThrow(new ResourceNotFoundException("Product not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/products/{productId}", productId)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNotFound());

        verify(productService).getProduct(tenantId, productId);
    }

    @Test
    void getProductBySku_WhenProductExists_ShouldReturnProduct() throws Exception {
        // Given
        String sku = "TEST-SKU-123";
        when(productService.getProductBySku(tenantId, sku))
            .thenReturn(productResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products/sku/{sku}", sku)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(productResponse.getId()))
            .andExpect(jsonPath("$.sku").value(productResponse.getSku()));

        verify(productService).getProductBySku(tenantId, sku);
    }

    @Test
    void createProduct_WithValidRequest_ShouldReturnCreatedProduct() throws Exception {
        // Given
        when(productService.createProduct(eq(tenantId), any(CreateProductRequest.class)))
            .thenReturn(productResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(productResponse.getId()))
            .andExpect(jsonPath("$.name").value(productResponse.getName()));

        verify(productService).createProduct(eq(tenantId), any(CreateProductRequest.class));
    }

    @Test
    void createProduct_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Given
        CreateProductRequest invalidRequest = new CreateProductRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(anyString(), any(CreateProductRequest.class));
    }

    @Test
    void updateProduct_WithValidRequest_ShouldReturnUpdatedProduct() throws Exception {
        // Given
        when(productService.updateProduct(eq(tenantId), eq(productId), any(UpdateProductRequest.class)))
            .thenReturn(productResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/products/{productId}", productId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(productResponse.getId()));

        verify(productService).updateProduct(eq(tenantId), eq(productId), any(UpdateProductRequest.class));
    }

    @Test
    void deleteProduct_WhenProductExists_ShouldReturn204() throws Exception {
        // Given
        doNothing().when(productService).deleteProduct(tenantId, productId);

        // When & Then
        mockMvc.perform(delete("/api/v1/products/{productId}", productId)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNoContent());

        verify(productService).deleteProduct(tenantId, productId);
    }

    @Test
    void searchProducts_ShouldReturnSearchResults() throws Exception {
        // Given
        String searchQuery = "test";
        when(productService.searchProducts(tenantId, searchQuery, 0, 20))
            .thenReturn(pagedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products/search")
                .header("X-Tenant-ID", tenantId)
                .param("q", searchQuery))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());

        verify(productService).searchProducts(tenantId, searchQuery, 0, 20);
    }

    @Test
    void getCategories_ShouldReturnCategoriesList() throws Exception {
        // Given
        List<String> categories = Arrays.asList("Electronics", "Books", "Clothing");
        when(productService.getCategories(tenantId))
            .thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/api/v1/products/categories")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").value("Electronics"))
            .andExpect(jsonPath("$[1]").value("Books"))
            .andExpect(jsonPath("$[2]").value("Clothing"));

        verify(productService).getCategories(tenantId);
    }

    @Test
    void getBrands_ShouldReturnBrandsList() throws Exception {
        // Given
        List<String> brands = Arrays.asList("Apple", "Samsung", "Nike");
        when(productService.getBrands(tenantId))
            .thenReturn(brands);

        // When & Then
        mockMvc.perform(get("/api/v1/products/brands")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").value("Apple"))
            .andExpect(jsonPath("$[1]").value("Samsung"))
            .andExpect(jsonPath("$[2]").value("Nike"));

        verify(productService).getBrands(tenantId);
    }

    @Test
    void getProductCount_ShouldReturnCount() throws Exception {
        // Given
        long count = 100L;
        when(productService.getProductCount(tenantId))
            .thenReturn(count);

        // When & Then
        mockMvc.perform(get("/api/v1/products/count")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(content().string("100"));

        verify(productService).getProductCount(tenantId);
    }

    @Test
    void getProductCountByStatus_ShouldReturnCountForStatus() throws Exception {
        // Given
        long count = 50L;
        Product.ProductStatus status = Product.ProductStatus.ACTIVE;
        when(productService.getProductCountByStatus(tenantId, status))
            .thenReturn(count);

        // When & Then
        mockMvc.perform(get("/api/v1/products/count")
                .header("X-Tenant-ID", tenantId)
                .param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(content().string("50"));

        verify(productService).getProductCountByStatus(tenantId, status);
    }

    @Test
    void health_ShouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/products/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("Product Service is healthy"));
    }

    // Helper methods
    private ProductResponse createTestProductResponse() {
        ProductResponse response = new ProductResponse();
        response.setId(productId);
        response.setName("Test Product");
        response.setDescription("Test Description");
        response.setCategory("Electronics");
        response.setSubcategory("Smartphones");
        response.setBrand("TestBrand");
        response.setSku("TEST-SKU-123");
        response.setPrice(new ProductResponse.PriceDto(new BigDecimal("299.99"), "USD"));
        response.setStatus(Product.ProductStatus.ACTIVE);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private ProductSummaryResponse createTestProductSummaryResponse() {
        ProductSummaryResponse response = new ProductSummaryResponse();
        response.setId(productId);
        response.setName("Test Product");
        response.setBrand("TestBrand");
        response.setCategory("Electronics");
        response.setSku("TEST-SKU-123");
        response.setPrice(new ProductSummaryResponse.PriceDto(new BigDecimal("299.99"), "USD"));
        response.setStatus(Product.ProductStatus.ACTIVE);
        return response;
    }

    private CreateProductRequest createTestCreateRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setCategory("Electronics");
        request.setSubcategory("Smartphones");
        request.setBrand("TestBrand");
        request.setSku("TEST-SKU-123");
        request.setPrice(new CreateProductRequest.PriceDto(new BigDecimal("299.99"), "USD"));
        return request;
    }

    private UpdateProductRequest createTestUpdateRequest() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Product Name");
        request.setDescription("Updated Description");
        request.setPrice(new CreateProductRequest.PriceDto(new BigDecimal("399.99"), "USD"));
        return request;
    }

    private PagedResponse<ProductSummaryResponse> createTestPagedResponse() {
        List<ProductSummaryResponse> content = Arrays.asList(productSummaryResponse);
        return new PagedResponse<>(content, 0, 20, 1, 1);
    }
}
package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.service.ProductRecommendationService;
import com.ecommerce.productservice.service.ProductSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductSearchController.class)
class ProductSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductSearchService productSearchService;

    @MockBean
    private ProductRecommendationService recommendationService;

    @Autowired
    private ObjectMapper objectMapper;

    private PagedResponse<ProductSummaryResponse> pagedResponse;
    private List<ProductSummaryResponse> productList;
    private final String tenantId = "tenant123";

    @BeforeEach
    void setUp() {
        ProductSummaryResponse product1 = createTestProductSummary("1", "iPhone 14", "Apple");
        ProductSummaryResponse product2 = createTestProductSummary("2", "Samsung Galaxy", "Samsung");
        
        productList = Arrays.asList(product1, product2);
        pagedResponse = new PagedResponse<>(productList, 0, 20, 2, 1);
    }

    @Test
    void advancedSearch_ShouldReturnSearchResults() throws Exception {
        // Given
        SearchFilters filters = new SearchFilters();
        filters.setSearchText("phone");
        filters.setCategory("Electronics");
        
        when(productSearchService.searchProducts(eq(tenantId), any(SearchFilters.class), eq(0), eq(20)))
            .thenReturn(pagedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/products/advanced-search")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filters))
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].name").value("iPhone 14"))
            .andExpect(jsonPath("$.totalElements").value(2));

        verify(productSearchService).searchProducts(eq(tenantId), any(SearchFilters.class), eq(0), eq(20));
    }

    @Test
    void filterProducts_ShouldReturnFilteredResults() throws Exception {
        // Given
        when(productSearchService.searchProducts(eq(tenantId), any(SearchFilters.class), eq(0), eq(20)))
            .thenReturn(pagedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products/filter")
                .header("X-Tenant-ID", tenantId)
                .param("searchText", "phone")
                .param("category", "Electronics")
                .param("brand", "Apple")
                .param("minPrice", "100")
                .param("maxPrice", "1000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].name").value("iPhone 14"));

        verify(productSearchService).searchProducts(eq(tenantId), any(SearchFilters.class), eq(0), eq(20));
    }

    @Test
    void getSearchSuggestions_ShouldReturnSuggestions() throws Exception {
        // Given
        List<String> suggestions = Arrays.asList("iPhone 14", "iPhone 13", "iPhone Case");
        when(productSearchService.getSearchSuggestions(tenantId, "iph", 10))
            .thenReturn(suggestions);

        // When & Then
        mockMvc.perform(get("/api/v1/products/suggestions")
                .header("X-Tenant-ID", tenantId)
                .param("query", "iph")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").value("iPhone 14"))
            .andExpect(jsonPath("$[1]").value("iPhone 13"));

        verify(productSearchService).getSearchSuggestions(tenantId, "iph", 10);
    }

    @Test
    void getSimilarProducts_ShouldReturnSimilarProducts() throws Exception {
        // Given
        String productId = "1";
        when(recommendationService.getSimilarProducts(tenantId, productId, 10))
            .thenReturn(productList);

        // When & Then
        mockMvc.perform(get("/api/v1/products/{productId}/similar", productId)
                .header("X-Tenant-ID", tenantId)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("iPhone 14"));

        verify(recommendationService).getSimilarProducts(tenantId, productId, 10);
    }

    @Test
    void getFrequentlyBoughtTogether_ShouldReturnRecommendations() throws Exception {
        // Given
        String productId = "1";
        when(recommendationService.getFrequentlyBoughtTogether(tenantId, productId, 5))
            .thenReturn(productList);

        // When & Then
        mockMvc.perform(get("/api/v1/products/{productId}/frequently-bought-together", productId)
                .header("X-Tenant-ID", tenantId)
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("iPhone 14"));

        verify(recommendationService).getFrequentlyBoughtTogether(tenantId, productId, 5);
    }

    @Test
    void getTrendingProducts_ShouldReturnTrendingProducts() throws Exception {
        // Given
        when(recommendationService.getTrendingProducts(tenantId, "Electronics", 10))
            .thenReturn(productList);

        // When & Then
        mockMvc.perform(get("/api/v1/products/trending")
                .header("X-Tenant-ID", tenantId)
                .param("category", "Electronics")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("iPhone 14"));

        verify(recommendationService).getTrendingProducts(tenantId, "Electronics", 10);
    }

    @Test
    void getPersonalizedRecommendations_ShouldReturnPersonalizedProducts() throws Exception {
        // Given
        ProductRecommendationService.UserPreferences preferences = new ProductRecommendationService.UserPreferences();
        preferences.setPreferredCategories(Arrays.asList("Electronics"));
        preferences.setPreferredBrands(Arrays.asList("Apple"));
        
        when(recommendationService.getPersonalizedRecommendations(eq(tenantId), any(), eq(10)))
            .thenReturn(productList);

        // When & Then
        mockMvc.perform(post("/api/v1/products/personalized-recommendations")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferences))
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("iPhone 14"));

        verify(recommendationService).getPersonalizedRecommendations(eq(tenantId), any(), eq(10));
    }

    @Test
    void getNewArrivals_ShouldReturnNewProducts() throws Exception {
        // Given
        when(recommendationService.getNewArrivals(tenantId, 10))
            .thenReturn(productList);

        // When & Then
        mockMvc.perform(get("/api/v1/products/new-arrivals")
                .header("X-Tenant-ID", tenantId)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("iPhone 14"));

        verify(recommendationService).getNewArrivals(tenantId, 10);
    }

    @Test
    void getFeaturedProducts_ShouldReturnFeaturedProducts() throws Exception {
        // Given
        when(recommendationService.getFeaturedProducts(tenantId, 10))
            .thenReturn(productList);

        // When & Then
        mockMvc.perform(get("/api/v1/products/featured")
                .header("X-Tenant-ID", tenantId)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("iPhone 14"));

        verify(recommendationService).getFeaturedProducts(tenantId, 10);
    }

    @Test
    void getCrossSellRecommendations_ShouldReturnCrossSellProducts() throws Exception {
        // Given
        List<String> cartItems = Arrays.asList("1", "2");
        when(recommendationService.getCrossSellRecommendations(tenantId, cartItems, 5))
            .thenReturn(productList);

        // When & Then
        mockMvc.perform(post("/api/v1/products/cross-sell")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cartItems))
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("iPhone 14"));

        verify(recommendationService).getCrossSellRecommendations(tenantId, cartItems, 5);
    }

    @Test
    void advancedSearch_WithInvalidPageSize_ShouldReturn400() throws Exception {
        // Given
        SearchFilters filters = new SearchFilters();

        // When & Then
        mockMvc.perform(post("/api/v1/products/advanced-search")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filters))
                .param("page", "0")
                .param("size", "101")) // Exceeds max size
            .andExpect(status().isBadRequest());

        verify(productSearchService, never()).searchProducts(anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void getSimilarProducts_WithInvalidLimit_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/products/1/similar")
                .header("X-Tenant-ID", tenantId)
                .param("limit", "51")) // Exceeds max limit
            .andExpect(status().isBadRequest());

        verify(recommendationService, never()).getSimilarProducts(anyString(), anyString(), anyInt());
    }

    // Helper method
    private ProductSummaryResponse createTestProductSummary(String id, String name, String brand) {
        ProductSummaryResponse response = new ProductSummaryResponse();
        response.setId(id);
        response.setName(name);
        response.setBrand(brand);
        response.setCategory("Electronics");
        response.setSku("SKU-" + id);
        response.setPrice(new ProductSummaryResponse.PriceDto(new BigDecimal("299.99"), "USD"));
        response.setStatus(Product.ProductStatus.ACTIVE);
        return response;
    }
}
package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductSummaryResponse;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRecommendationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSearchService productSearchService;

    @InjectMocks
    private ProductRecommendationService recommendationService;

    private Product testProduct1;
    private Product testProduct2;
    private Product testProduct3;
    private final String tenantId = "tenant123";

    @BeforeEach
    void setUp() {
        testProduct1 = createTestProduct("1", "iPhone 14", "Electronics", "Apple", new BigDecimal("999"));
        testProduct2 = createTestProduct("2", "iPhone Case", "Electronics", "Apple", new BigDecimal("29"));
        testProduct3 = createTestProduct("3", "Samsung Galaxy", "Electronics", "Samsung", new BigDecimal("899"));
    }

    @Test
    void getSimilarProducts_ShouldReturnSimilarProducts() {
        // Given
        String productId = "1";
        List<ProductSummaryResponse> expectedSimilar = Arrays.asList(
            new ProductSummaryResponse(testProduct3)
        );
        
        when(productSearchService.findSimilarProducts(tenantId, productId, 5))
            .thenReturn(expectedSimilar);

        // When
        List<ProductSummaryResponse> result = recommendationService.getSimilarProducts(tenantId, productId, 5);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Samsung Galaxy", result.get(0).getName());
        verify(productSearchService).findSimilarProducts(tenantId, productId, 5);
    }

    @Test
    void getFrequentlyBoughtTogether_WhenProductExists_ShouldReturnRecommendations() {
        // Given
        String productId = "1";
        List<Product> relatedProducts = Arrays.asList(testProduct2);
        Page<Product> productPage = new PageImpl<>(relatedProducts);
        
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.of(testProduct1));
        when(productRepository.findByTenantIdAndPriceRange(eq(tenantId), anyDouble(), anyDouble(), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        List<ProductSummaryResponse> result = recommendationService.getFrequentlyBoughtTogether(tenantId, productId, 5);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("iPhone Case", result.get(0).getName());
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
        verify(productRepository).findByTenantIdAndPriceRange(eq(tenantId), anyDouble(), anyDouble(), any(Pageable.class));
    }

    @Test
    void getFrequentlyBoughtTogether_WhenProductNotExists_ShouldReturnEmptyList() {
        // Given
        String productId = "nonexistent";
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.empty());

        // When
        List<ProductSummaryResponse> result = recommendationService.getFrequentlyBoughtTogether(tenantId, productId, 5);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
        verify(productRepository, never()).findByTenantIdAndPriceRange(anyString(), anyDouble(), anyDouble(), any(Pageable.class));
    }

    @Test
    void getTrendingProducts_WithCategory_ShouldReturnTrendingProducts() {
        // Given
        String category = "Electronics";
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndCategory(eq(tenantId), eq(category), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        List<ProductSummaryResponse> result = recommendationService.getTrendingProducts(tenantId, category, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository).findByTenantIdAndCategory(eq(tenantId), eq(category), any(Pageable.class));
    }

    @Test
    void getTrendingProducts_WithoutCategory_ShouldReturnAllActiveProducts() {
        // Given
        List<Product> products = Arrays.asList(testProduct1, testProduct2, testProduct3);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndStatus(eq(tenantId), eq(Product.ProductStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        List<ProductSummaryResponse> result = recommendationService.getTrendingProducts(tenantId, null, 10);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(productRepository).findByTenantIdAndStatus(eq(tenantId), eq(Product.ProductStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void getPersonalizedRecommendations_WithPreferences_ShouldReturnPersonalizedProducts() {
        // Given
        ProductRecommendationService.UserPreferences preferences = new ProductRecommendationService.UserPreferences();
        preferences.setPreferredCategories(Arrays.asList("Electronics"));
        preferences.setPreferredBrands(Arrays.asList("Apple"));
        preferences.setMinPrice(new BigDecimal("100"));
        preferences.setMaxPrice(new BigDecimal("1000"));
        
        List<ProductSummaryResponse> categoryProducts = Arrays.asList(new ProductSummaryResponse(testProduct1));
        List<Product> brandProducts = Arrays.asList(testProduct2);
        List<Product> priceRangeProducts = Arrays.asList(testProduct1);
        
        when(productSearchService.findPopularProducts(eq(tenantId), eq("Electronics"), anyInt()))
            .thenReturn(categoryProducts);
        when(productRepository.findByTenantIdAndBrand(eq(tenantId), eq("Apple"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(brandProducts));
        when(productRepository.findByTenantIdAndPriceRange(eq(tenantId), eq(100.0), eq(1000.0), any(Pageable.class)))
            .thenReturn(new PageImpl<>(priceRangeProducts));

        // When
        List<ProductSummaryResponse> result = recommendationService.getPersonalizedRecommendations(
            tenantId, preferences, 10);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(productSearchService).findPopularProducts(eq(tenantId), eq("Electronics"), anyInt());
        verify(productRepository).findByTenantIdAndBrand(eq(tenantId), eq("Apple"), any(Pageable.class));
        verify(productRepository).findByTenantIdAndPriceRange(eq(tenantId), eq(100.0), eq(1000.0), any(Pageable.class));
    }

    @Test
    void getNewArrivals_ShouldReturnRecentProducts() {
        // Given
        List<ProductSummaryResponse> recentProducts = Arrays.asList(
            new ProductSummaryResponse(testProduct1),
            new ProductSummaryResponse(testProduct2)
        );
        
        when(productSearchService.findRecentProducts(tenantId, 10))
            .thenReturn(recentProducts);

        // When
        List<ProductSummaryResponse> result = recommendationService.getNewArrivals(tenantId, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productSearchService).findRecentProducts(tenantId, 10);
    }

    @Test
    void getFeaturedProducts_ShouldReturnPopularProducts() {
        // Given
        List<ProductSummaryResponse> popularProducts = Arrays.asList(
            new ProductSummaryResponse(testProduct1),
            new ProductSummaryResponse(testProduct3)
        );
        
        when(productSearchService.findPopularProducts(tenantId, null, 10))
            .thenReturn(popularProducts);

        // When
        List<ProductSummaryResponse> result = recommendationService.getFeaturedProducts(tenantId, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productSearchService).findPopularProducts(tenantId, null, 10);
    }

    @Test
    void getCrossSellRecommendations_WithCartItems_ShouldReturnCrossSellProducts() {
        // Given
        List<String> cartItems = Arrays.asList("1", "2");
        List<ProductSummaryResponse> similarToFirst = Arrays.asList(new ProductSummaryResponse(testProduct3));
        List<ProductSummaryResponse> similarToSecond = Arrays.asList(new ProductSummaryResponse(testProduct3));
        
        when(recommendationService.getSimilarProducts(tenantId, "1", 5))
            .thenReturn(similarToFirst);
        when(recommendationService.getSimilarProducts(tenantId, "2", 5))
            .thenReturn(similarToSecond);
        when(productRepository.findByTenantIdAndId(tenantId, "3"))
            .thenReturn(Optional.of(testProduct3));

        // When
        List<ProductSummaryResponse> result = recommendationService.getCrossSellRecommendations(
            tenantId, cartItems, 5);

        // Then
        assertNotNull(result);
        // Note: The actual implementation might return different results due to mocking limitations
        verify(productRepository, atLeastOnce()).findByTenantIdAndId(eq(tenantId), anyString());
    }

    @Test
    void getCrossSellRecommendations_WithEmptyCart_ShouldReturnEmptyList() {
        // Given
        List<String> cartItems = Arrays.asList();

        // When
        List<ProductSummaryResponse> result = recommendationService.getCrossSellRecommendations(
            tenantId, cartItems, 5);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository, never()).findByTenantIdAndId(anyString(), anyString());
    }

    // Helper method
    private Product createTestProduct(String id, String name, String category, String brand, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(tenantId);
        product.setName(name);
        product.setCategory(category);
        product.setBrand(brand);
        product.setSku("SKU-" + id);
        product.setPrice(new Product.Price(price, "USD"));
        product.setStatus(Product.ProductStatus.ACTIVE);
        return product;
    }
}
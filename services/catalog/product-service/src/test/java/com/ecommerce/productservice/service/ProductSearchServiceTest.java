package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.PagedResponse;
import com.ecommerce.productservice.dto.ProductSummaryResponse;
import com.ecommerce.productservice.dto.SearchFilters;
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
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductSearchService productSearchService;

    private Product testProduct1;
    private Product testProduct2;
    private final String tenantId = "tenant123";

    @BeforeEach
    void setUp() {
        testProduct1 = createTestProduct("1", "iPhone 14", "Electronics", "Apple");
        testProduct2 = createTestProduct("2", "Samsung Galaxy", "Electronics", "Samsung");
    }

    @Test
    void searchProducts_WithTextSearch_ShouldReturnResults() {
        // Given
        SearchFilters filters = new SearchFilters();
        filters.setSearchText("iPhone");
        
        List<Product> products = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndTextSearch(eq(tenantId), eq("iPhone"), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        PagedResponse<ProductSummaryResponse> result = productSearchService.searchProducts(
            tenantId, filters, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("iPhone 14", result.getContent().get(0).getName());
        verify(productRepository).findByTenantIdAndTextSearch(eq(tenantId), eq("iPhone"), any(Pageable.class));
    }

    @Test
    void searchProducts_WithCategoryFilter_ShouldReturnResults() {
        // Given
        SearchFilters filters = new SearchFilters();
        filters.setCategory("Electronics");
        
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndCategory(eq(tenantId), eq("Electronics"), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        PagedResponse<ProductSummaryResponse> result = productSearchService.searchProducts(
            tenantId, filters, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(productRepository).findByTenantIdAndCategory(eq(tenantId), eq("Electronics"), any(Pageable.class));
    }

    @Test
    void searchProducts_WithPriceRange_ShouldReturnResults() {
        // Given
        SearchFilters filters = new SearchFilters();
        filters.setMinPrice(new BigDecimal("100"));
        filters.setMaxPrice(new BigDecimal("1000"));
        
        List<Product> products = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndPriceRange(eq(tenantId), eq(100.0), eq(1000.0), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        PagedResponse<ProductSummaryResponse> result = productSearchService.searchProducts(
            tenantId, filters, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(productRepository).findByTenantIdAndPriceRange(eq(tenantId), eq(100.0), eq(1000.0), any(Pageable.class));
    }

    @Test
    void searchProducts_WithTextAndFilters_ShouldReturnResults() {
        // Given
        SearchFilters filters = new SearchFilters();
        filters.setSearchText("phone");
        filters.setCategory("Electronics");
        filters.setBrand("Apple");
        filters.setStatus(Product.ProductStatus.ACTIVE);
        
        List<Product> products = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndTextSearchWithFilters(
            eq(tenantId), eq("phone"), eq("Electronics"), eq("Apple"), 
            eq(Product.ProductStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        PagedResponse<ProductSummaryResponse> result = productSearchService.searchProducts(
            tenantId, filters, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(productRepository).findByTenantIdAndTextSearchWithFilters(
            eq(tenantId), eq("phone"), eq("Electronics"), eq("Apple"), 
            eq(Product.ProductStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void findSimilarProducts_WhenProductExists_ShouldReturnSimilarProducts() {
        // Given
        String productId = "1";
        List<Product> similarProducts = Arrays.asList(testProduct2);
        Page<Product> productPage = new PageImpl<>(similarProducts);
        
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.of(testProduct1));
        when(productRepository.findByTenantIdAndCategory(eq(tenantId), eq("Electronics"), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        List<ProductSummaryResponse> result = productSearchService.findSimilarProducts(tenantId, productId, 5);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Samsung Galaxy", result.get(0).getName());
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
        verify(productRepository).findByTenantIdAndCategory(eq(tenantId), eq("Electronics"), any(Pageable.class));
    }

    @Test
    void findSimilarProducts_WhenProductNotExists_ShouldReturnEmptyList() {
        // Given
        String productId = "nonexistent";
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.empty());

        // When
        List<ProductSummaryResponse> result = productSearchService.findSimilarProducts(tenantId, productId, 5);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
        verify(productRepository, never()).findByTenantIdAndCategory(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void findPopularProducts_WithCategory_ShouldReturnPopularProducts() {
        // Given
        String category = "Electronics";
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndCategory(eq(tenantId), eq(category), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        List<ProductSummaryResponse> result = productSearchService.findPopularProducts(tenantId, category, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository).findByTenantIdAndCategory(eq(tenantId), eq(category), any(Pageable.class));
    }

    @Test
    void findPopularProducts_WithoutCategory_ShouldReturnAllActiveProducts() {
        // Given
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndStatus(eq(tenantId), eq(Product.ProductStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        List<ProductSummaryResponse> result = productSearchService.findPopularProducts(tenantId, null, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository).findByTenantIdAndStatus(eq(tenantId), eq(Product.ProductStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void findRecentProducts_ShouldReturnRecentProducts() {
        // Given
        List<Product> products = Arrays.asList(testProduct1, testProduct2);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndStatus(eq(tenantId), eq(Product.ProductStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        List<ProductSummaryResponse> result = productSearchService.findRecentProducts(tenantId, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository).findByTenantIdAndStatus(eq(tenantId), eq(Product.ProductStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void getSearchSuggestions_WithValidQuery_ShouldReturnSuggestions() {
        // Given
        String query = "iph";
        List<Product> products = Arrays.asList(testProduct1);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findByTenantIdAndTextSearch(eq(tenantId), eq(query), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        List<String> result = productSearchService.getSearchSuggestions(tenantId, query, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("iPhone 14", result.get(0));
        verify(productRepository).findByTenantIdAndTextSearch(eq(tenantId), eq(query), any(Pageable.class));
    }

    @Test
    void getSearchSuggestions_WithShortQuery_ShouldReturnEmptyList() {
        // Given
        String query = "i";

        // When
        List<String> result = productSearchService.getSearchSuggestions(tenantId, query, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository, never()).findByTenantIdAndTextSearch(anyString(), anyString(), any(Pageable.class));
    }

    // Helper method
    private Product createTestProduct(String id, String name, String category, String brand) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(tenantId);
        product.setName(name);
        product.setCategory(category);
        product.setBrand(brand);
        product.setSku("SKU-" + id);
        product.setPrice(new Product.Price(new BigDecimal("299.99"), "USD"));
        product.setStatus(Product.ProductStatus.ACTIVE);
        return product;
    }
}
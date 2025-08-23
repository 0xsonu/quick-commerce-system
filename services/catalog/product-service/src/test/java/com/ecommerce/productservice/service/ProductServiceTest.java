package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private CreateProductRequest createRequest;
    private UpdateProductRequest updateRequest;
    private final String tenantId = "tenant123";
    private final String productId = "product123";

    @BeforeEach
    void setUp() {
        testProduct = createTestProduct();
        createRequest = createTestCreateRequest();
        updateRequest = createTestUpdateRequest();
    }

    @Test
    void getProduct_WhenProductExists_ShouldReturnProductResponse() {
        // Given
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.of(testProduct));

        // When
        ProductResponse result = productService.getProduct(tenantId, productId);

        // Then
        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        assertEquals(testProduct.getName(), result.getName());
        assertEquals(testProduct.getSku(), result.getSku());
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
    }

    @Test
    void getProduct_WhenProductNotExists_ShouldThrowResourceNotFoundException() {
        // Given
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> productService.getProduct(tenantId, productId));
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
    }

    @Test
    void getProductBySku_WhenProductExists_ShouldReturnProductResponse() {
        // Given
        String sku = "TEST-SKU-123";
        when(productRepository.findByTenantIdAndSku(tenantId, sku))
            .thenReturn(Optional.of(testProduct));

        // When
        ProductResponse result = productService.getProductBySku(tenantId, sku);

        // Then
        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        assertEquals(testProduct.getSku(), result.getSku());
        verify(productRepository).findByTenantIdAndSku(tenantId, sku);
    }

    @Test
    void getProducts_WithoutFilters_ShouldReturnPagedResponse() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productRepository.findByTenantId(eq(tenantId), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        PagedResponse<ProductSummaryResponse> result = productService.getProducts(
            tenantId, 0, 20, null, null, null, null, null, "updatedAt", "desc");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1, result.getTotalElements());
        verify(productRepository).findByTenantId(eq(tenantId), any(Pageable.class));
    }

    @Test
    void getProducts_WithSearch_ShouldReturnSearchResults() {
        // Given
        String searchText = "test product";
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productRepository.findByTenantIdAndTextSearch(eq(tenantId), eq(searchText), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        PagedResponse<ProductSummaryResponse> result = productService.getProducts(
            tenantId, 0, 20, null, null, null, searchText, null, "updatedAt", "desc");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(productRepository).findByTenantIdAndTextSearch(eq(tenantId), eq(searchText), any(Pageable.class));
    }

    @Test
    void createProduct_WithValidRequest_ShouldReturnCreatedProduct() {
        // Given
        when(productRepository.existsByTenantIdAndSku(tenantId, createRequest.getSku()))
            .thenReturn(false);
        when(productRepository.save(any(Product.class)))
            .thenReturn(testProduct);

        // When
        ProductResponse result = productService.createProduct(tenantId, createRequest);

        // Then
        assertNotNull(result);
        assertEquals(testProduct.getName(), result.getName());
        assertEquals(testProduct.getSku(), result.getSku());
        verify(productRepository).existsByTenantIdAndSku(tenantId, createRequest.getSku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_WithDuplicateSku_ShouldThrowIllegalArgumentException() {
        // Given
        when(productRepository.existsByTenantIdAndSku(tenantId, createRequest.getSku()))
            .thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> productService.createProduct(tenantId, createRequest));
        verify(productRepository).existsByTenantIdAndSku(tenantId, createRequest.getSku());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_WhenProductExists_ShouldReturnUpdatedProduct() {
        // Given
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class)))
            .thenReturn(testProduct);

        // When
        ProductResponse result = productService.updateProduct(tenantId, productId, updateRequest);

        // Then
        assertNotNull(result);
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_WhenProductNotExists_ShouldThrowResourceNotFoundException() {
        // Given
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> productService.updateProduct(tenantId, productId, updateRequest));
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_WhenProductExists_ShouldDeleteProduct() {
        // Given
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.of(testProduct));

        // When
        productService.deleteProduct(tenantId, productId);

        // Then
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
        verify(productRepository).deleteByTenantIdAndId(tenantId, productId);
    }

    @Test
    void deleteProduct_WhenProductNotExists_ShouldThrowResourceNotFoundException() {
        // Given
        when(productRepository.findByTenantIdAndId(tenantId, productId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
            () -> productService.deleteProduct(tenantId, productId));
        verify(productRepository).findByTenantIdAndId(tenantId, productId);
        verify(productRepository, never()).deleteByTenantIdAndId(anyString(), anyString());
    }

    @Test
    void searchProducts_ShouldReturnSearchResults() {
        // Given
        String searchText = "test";
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 1);
        
        when(productRepository.findByTenantIdAndTextSearch(eq(tenantId), eq(searchText), any(Pageable.class)))
            .thenReturn(productPage);

        // When
        PagedResponse<ProductSummaryResponse> result = productService.searchProducts(tenantId, searchText, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(productRepository).findByTenantIdAndTextSearch(eq(tenantId), eq(searchText), any(Pageable.class));
    }

    @Test
    void getCategories_ShouldReturnDistinctCategories() {
        // Given
        Product product1 = createTestProduct();
        product1.setCategory("Electronics");
        Product product2 = createTestProduct();
        product2.setCategory("Books");
        
        when(productRepository.findDistinctCategoriesByTenantId(tenantId))
            .thenReturn(Arrays.asList(product1, product2));

        // When
        List<String> result = productService.getCategories(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Electronics"));
        assertTrue(result.contains("Books"));
        verify(productRepository).findDistinctCategoriesByTenantId(tenantId);
    }

    @Test
    void getBrands_ShouldReturnDistinctBrands() {
        // Given
        Product product1 = createTestProduct();
        product1.setBrand("Apple");
        Product product2 = createTestProduct();
        product2.setBrand("Samsung");
        
        when(productRepository.findDistinctBrandsByTenantId(tenantId))
            .thenReturn(Arrays.asList(product1, product2));

        // When
        List<String> result = productService.getBrands(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Apple"));
        assertTrue(result.contains("Samsung"));
        verify(productRepository).findDistinctBrandsByTenantId(tenantId);
    }

    @Test
    void getProductCount_ShouldReturnCount() {
        // Given
        long expectedCount = 10L;
        when(productRepository.countByTenantId(tenantId))
            .thenReturn(expectedCount);

        // When
        long result = productService.getProductCount(tenantId);

        // Then
        assertEquals(expectedCount, result);
        verify(productRepository).countByTenantId(tenantId);
    }

    @Test
    void getProductCountByStatus_ShouldReturnCountForStatus() {
        // Given
        long expectedCount = 5L;
        Product.ProductStatus status = Product.ProductStatus.ACTIVE;
        when(productRepository.countByTenantIdAndStatus(tenantId, status))
            .thenReturn(expectedCount);

        // When
        long result = productService.getProductCountByStatus(tenantId, status);

        // Then
        assertEquals(expectedCount, result);
        verify(productRepository).countByTenantIdAndStatus(tenantId, status);
    }

    // Helper methods
    private Product createTestProduct() {
        Product product = new Product();
        product.setId(productId);
        product.setTenantId(tenantId);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setCategory("Electronics");
        product.setSubcategory("Smartphones");
        product.setBrand("TestBrand");
        product.setSku("TEST-SKU-123");
        product.setPrice(new Product.Price(new BigDecimal("299.99"), "USD"));
        product.setStatus(Product.ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
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
}
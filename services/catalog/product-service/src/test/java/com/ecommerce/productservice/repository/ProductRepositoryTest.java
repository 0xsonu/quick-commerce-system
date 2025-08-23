package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class ProductRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct1;
    private Product testProduct2;
    private final String tenantId1 = "tenant1";
    private final String tenantId2 = "tenant2";

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        
        testProduct1 = createTestProduct(tenantId1, "Product 1", "Electronics", "Apple", "APPLE-001");
        testProduct2 = createTestProduct(tenantId1, "Product 2", "Books", "Penguin", "BOOK-001");
        
        productRepository.save(testProduct1);
        productRepository.save(testProduct2);
        
        // Product for different tenant
        Product productTenant2 = createTestProduct(tenantId2, "Product 3", "Electronics", "Samsung", "SAMSUNG-001");
        productRepository.save(productTenant2);
    }

    @Test
    void findByTenantIdAndId_WhenProductExists_ShouldReturnProduct() {
        // When
        Optional<Product> result = productRepository.findByTenantIdAndId(tenantId1, testProduct1.getId());

        // Then
        assertTrue(result.isPresent());
        assertEquals(testProduct1.getId(), result.get().getId());
        assertEquals(testProduct1.getName(), result.get().getName());
    }

    @Test
    void findByTenantIdAndId_WhenProductNotExists_ShouldReturnEmpty() {
        // When
        Optional<Product> result = productRepository.findByTenantIdAndId(tenantId1, "nonexistent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findByTenantIdAndId_WhenDifferentTenant_ShouldReturnEmpty() {
        // When
        Optional<Product> result = productRepository.findByTenantIdAndId(tenantId2, testProduct1.getId());

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findByTenantIdAndSku_WhenSkuExists_ShouldReturnProduct() {
        // When
        Optional<Product> result = productRepository.findByTenantIdAndSku(tenantId1, "APPLE-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testProduct1.getSku(), result.get().getSku());
    }

    @Test
    void findByTenantId_ShouldReturnOnlyTenantProducts() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findByTenantId(tenantId1, pageable);

        // Then
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(p -> tenantId1.equals(p.getTenantId())));
    }

    @Test
    void findByTenantIdAndCategory_ShouldReturnProductsInCategory() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findByTenantIdAndCategory(tenantId1, "Electronics", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("Electronics", result.getContent().get(0).getCategory());
    }

    @Test
    void findByTenantIdAndBrand_ShouldReturnProductsByBrand() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findByTenantIdAndBrand(tenantId1, "Apple", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("Apple", result.getContent().get(0).getBrand());
    }

    @Test
    void findByTenantIdAndStatus_ShouldReturnProductsByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> result = productRepository.findByTenantIdAndStatus(tenantId1, Product.ProductStatus.ACTIVE, pageable);

        // Then
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(p -> p.getStatus() == Product.ProductStatus.ACTIVE));
    }

    @Test
    void countByTenantId_ShouldReturnCorrectCount() {
        // When
        long count = productRepository.countByTenantId(tenantId1);

        // Then
        assertEquals(2, count);
    }

    @Test
    void countByTenantIdAndStatus_ShouldReturnCorrectCount() {
        // When
        long count = productRepository.countByTenantIdAndStatus(tenantId1, Product.ProductStatus.ACTIVE);

        // Then
        assertEquals(2, count);
    }

    @Test
    void existsByTenantIdAndSku_WhenSkuExists_ShouldReturnTrue() {
        // When
        boolean exists = productRepository.existsByTenantIdAndSku(tenantId1, "APPLE-001");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByTenantIdAndSku_WhenSkuNotExists_ShouldReturnFalse() {
        // When
        boolean exists = productRepository.existsByTenantIdAndSku(tenantId1, "NONEXISTENT");

        // Then
        assertFalse(exists);
    }

    @Test
    void existsByTenantIdAndSkuAndIdNot_WhenSkuExistsForDifferentProduct_ShouldReturnTrue() {
        // When
        boolean exists = productRepository.existsByTenantIdAndSkuAndIdNot(tenantId1, "APPLE-001", "different-id");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByTenantIdAndSkuAndIdNot_WhenSkuExistsForSameProduct_ShouldReturnFalse() {
        // When
        boolean exists = productRepository.existsByTenantIdAndSkuAndIdNot(tenantId1, "APPLE-001", testProduct1.getId());

        // Then
        assertFalse(exists);
    }

    @Test
    void findDistinctCategoriesByTenantId_ShouldReturnUniqueCategories() {
        // When
        List<Product> result = productRepository.findDistinctCategoriesByTenantId(tenantId1);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void findDistinctBrandsByTenantId_ShouldReturnUniqueBrands() {
        // When
        List<Product> result = productRepository.findDistinctBrandsByTenantId(tenantId1);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void deleteByTenantIdAndId_ShouldDeleteProduct() {
        // Given
        String productId = testProduct1.getId();
        assertTrue(productRepository.findByTenantIdAndId(tenantId1, productId).isPresent());

        // When
        productRepository.deleteByTenantIdAndId(tenantId1, productId);

        // Then
        assertFalse(productRepository.findByTenantIdAndId(tenantId1, productId).isPresent());
    }

    // Helper method
    private Product createTestProduct(String tenantId, String name, String category, String brand, String sku) {
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(name);
        product.setDescription("Test description for " + name);
        product.setCategory(category);
        product.setBrand(brand);
        product.setSku(sku);
        product.setPrice(new Product.Price(new BigDecimal("99.99"), "USD"));
        product.setStatus(Product.ProductStatus.ACTIVE);
        return product;
    }
}
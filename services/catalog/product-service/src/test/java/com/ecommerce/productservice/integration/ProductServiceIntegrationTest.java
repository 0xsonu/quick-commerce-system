package com.ecommerce.productservice.integration;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
class ProductServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.cache.type", () -> "none"); // Disable cache for integration tests
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private final String tenantId = "integration-test-tenant";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        productRepository.deleteAll();
    }

    @Test
    void createProduct_ShouldPersistProductInDatabase() throws Exception {
        // Given
        CreateProductRequest request = createValidProductRequest();

        // When
        String responseContent = mockMvc.perform(post("/api/v1/products")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(request.getName()))
            .andExpect(jsonPath("$.sku").value(request.getSku()))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Then
        ProductResponse response = objectMapper.readValue(responseContent, ProductResponse.class);
        assertNotNull(response.getId());

        // Verify in database
        Product savedProduct = productRepository.findByTenantIdAndId(tenantId, response.getId()).orElse(null);
        assertNotNull(savedProduct);
        assertEquals(request.getName(), savedProduct.getName());
        assertEquals(request.getSku(), savedProduct.getSku());
        assertEquals(tenantId, savedProduct.getTenantId());
    }

    @Test
    void getProduct_ShouldReturnProductFromDatabase() throws Exception {
        // Given
        Product product = createAndSaveTestProduct();

        // When & Then
        mockMvc.perform(get("/api/v1/products/{productId}", product.getId())
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(product.getId()))
            .andExpect(jsonPath("$.name").value(product.getName()))
            .andExpect(jsonPath("$.sku").value(product.getSku()));
    }

    @Test
    void getProducts_ShouldReturnPaginatedResults() throws Exception {
        // Given
        createAndSaveTestProduct();
        createAndSaveTestProduct("Product 2", "SKU-002");

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void searchProducts_ShouldReturnMatchingProducts() throws Exception {
        // Given
        Product product1 = createAndSaveTestProduct("iPhone 14", "IPHONE-14");
        createAndSaveTestProduct("Samsung Galaxy", "SAMSUNG-S23");

        // When & Then
        mockMvc.perform(get("/api/v1/products/search")
                .header("X-Tenant-ID", tenantId)
                .param("q", "iPhone"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("iPhone 14"));
    }

    @Test
    void updateProduct_ShouldModifyProductInDatabase() throws Exception {
        // Given
        Product product = createAndSaveTestProduct();
        String updatedName = "Updated Product Name";
        
        String updateJson = """
            {
                "name": "%s",
                "description": "Updated description"
            }
            """.formatted(updatedName);

        // When
        mockMvc.perform(put("/api/v1/products/{productId}", product.getId())
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(updatedName));

        // Then
        Product updatedProduct = productRepository.findByTenantIdAndId(tenantId, product.getId()).orElse(null);
        assertNotNull(updatedProduct);
        assertEquals(updatedName, updatedProduct.getName());
    }

    @Test
    void deleteProduct_ShouldRemoveProductFromDatabase() throws Exception {
        // Given
        Product product = createAndSaveTestProduct();
        assertTrue(productRepository.findByTenantIdAndId(tenantId, product.getId()).isPresent());

        // When
        mockMvc.perform(delete("/api/v1/products/{productId}", product.getId())
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNoContent());

        // Then
        assertFalse(productRepository.findByTenantIdAndId(tenantId, product.getId()).isPresent());
    }

    @Test
    void getProductBySku_ShouldReturnCorrectProduct() throws Exception {
        // Given
        Product product = createAndSaveTestProduct();

        // When & Then
        mockMvc.perform(get("/api/v1/products/sku/{sku}", product.getSku())
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(product.getId()))
            .andExpect(jsonPath("$.sku").value(product.getSku()));
    }

    @Test
    void getCategories_ShouldReturnDistinctCategories() throws Exception {
        // Given
        createAndSaveTestProduct("Product 1", "SKU-001", "Electronics");
        createAndSaveTestProduct("Product 2", "SKU-002", "Books");
        createAndSaveTestProduct("Product 3", "SKU-003", "Electronics");

        // When & Then
        mockMvc.perform(get("/api/v1/products/categories")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getBrands_ShouldReturnDistinctBrands() throws Exception {
        // Given
        createAndSaveTestProduct("Product 1", "SKU-001", "Electronics", "Apple");
        createAndSaveTestProduct("Product 2", "SKU-002", "Electronics", "Samsung");
        createAndSaveTestProduct("Product 3", "SKU-003", "Electronics", "Apple");

        // When & Then
        mockMvc.perform(get("/api/v1/products/brands")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getProductCount_ShouldReturnCorrectCount() throws Exception {
        // Given
        createAndSaveTestProduct();
        createAndSaveTestProduct("Product 2", "SKU-002");

        // When & Then
        mockMvc.perform(get("/api/v1/products/count")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(content().string("2"));
    }

    @Test
    void tenantIsolation_ShouldOnlyReturnTenantSpecificProducts() throws Exception {
        // Given
        String tenant1 = "tenant1";
        String tenant2 = "tenant2";
        
        Product product1 = createTestProduct("Product 1", "SKU-001");
        product1.setTenantId(tenant1);
        productRepository.save(product1);
        
        Product product2 = createTestProduct("Product 2", "SKU-002");
        product2.setTenantId(tenant2);
        productRepository.save(product2);

        // When & Then - Tenant 1 should only see their product
        mockMvc.perform(get("/api/v1/products")
                .header("X-Tenant-ID", tenant1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Product 1"));

        // When & Then - Tenant 2 should only see their product
        mockMvc.perform(get("/api/v1/products")
                .header("X-Tenant-ID", tenant2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Product 2"));
    }

    // Helper methods
    private CreateProductRequest createValidProductRequest() {
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

    private Product createAndSaveTestProduct() {
        return createAndSaveTestProduct("Test Product", "TEST-SKU-123");
    }

    private Product createAndSaveTestProduct(String name, String sku) {
        return createAndSaveTestProduct(name, sku, "Electronics");
    }

    private Product createAndSaveTestProduct(String name, String sku, String category) {
        return createAndSaveTestProduct(name, sku, category, "TestBrand");
    }

    private Product createAndSaveTestProduct(String name, String sku, String category, String brand) {
        Product product = createTestProduct(name, sku);
        product.setCategory(category);
        product.setBrand(brand);
        product.setTenantId(tenantId);
        return productRepository.save(product);
    }

    private Product createTestProduct(String name, String sku) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Test Description");
        product.setCategory("Electronics");
        product.setBrand("TestBrand");
        product.setSku(sku);
        product.setPrice(new Product.Price(new BigDecimal("299.99"), "USD"));
        product.setStatus(Product.ProductStatus.ACTIVE);
        return product;
    }
}
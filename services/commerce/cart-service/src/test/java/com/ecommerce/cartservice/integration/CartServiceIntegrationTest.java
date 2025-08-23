package com.ecommerce.cartservice.integration;

import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.entity.ShoppingCartBackup;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.redis.CartRedisRepository;
import com.ecommerce.cartservice.repository.ShoppingCartBackupRepository;
import com.ecommerce.cartservice.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class CartServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("cart_service_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRedisRepository cartRedisRepository;

    @Autowired
    private ShoppingCartBackupRepository cartBackupRepository;

    private static final String TENANT_ID = "tenant1";
    private static final String USER_ID = "user1";

    @BeforeEach
    void setUp() {
        // Clean up before each test
        cartRedisRepository.deleteAll();
        cartBackupRepository.deleteAll();
    }

    @Test
    void testFullCartWorkflow() {
        // 1. Get empty cart (should create new one)
        Cart cart = cartService.getCart(TENANT_ID, USER_ID);
        assertNotNull(cart);
        assertEquals(TENANT_ID, cart.getTenantId());
        assertEquals(USER_ID, cart.getUserId());
        assertTrue(cart.getItems().isEmpty());

        // 2. Add item to cart
        AddToCartRequest addRequest = new AddToCartRequest(
            "product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        Cart updatedCart = cartService.addToCart(TENANT_ID, USER_ID, addRequest);
        
        assertEquals(1, updatedCart.getItems().size());
        assertEquals("product1", updatedCart.getItems().get(0).getProductId());
        assertEquals(Integer.valueOf(2), updatedCart.getItems().get(0).getQuantity());

        // 3. Verify cart exists in Redis
        Optional<Cart> redisCart = cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID);
        assertTrue(redisCart.isPresent());
        assertEquals(1, redisCart.get().getItems().size());

        // 4. Verify backup exists in MySQL
        Optional<ShoppingCartBackup> backup = cartBackupRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID);
        assertTrue(backup.isPresent());
        Cart backupCart = backup.get().getCartObject();
        assertEquals(1, backupCart.getItems().size());
        assertEquals("product1", backupCart.getItems().get(0).getProductId());

        // 5. Add another item
        AddToCartRequest addRequest2 = new AddToCartRequest(
            "product2", "sku2", "Product 2", 1, new BigDecimal("15.00"));
        Cart cartWith2Items = cartService.addToCart(TENANT_ID, USER_ID, addRequest2);
        
        assertEquals(2, cartWith2Items.getItems().size());

        // 6. Remove first item
        Cart cartAfterRemoval = cartService.removeFromCart(TENANT_ID, USER_ID, "product1", "sku1");
        assertEquals(1, cartAfterRemoval.getItems().size());
        assertEquals("product2", cartAfterRemoval.getItems().get(0).getProductId());

        // 7. Clear cart
        cartService.clearCart(TENANT_ID, USER_ID);
        Cart clearedCart = cartService.getCart(TENANT_ID, USER_ID);
        assertTrue(clearedCart.getItems().isEmpty());

        // 8. Delete cart completely
        cartService.deleteCart(TENANT_ID, USER_ID);
        assertFalse(cartService.cartExists(TENANT_ID, USER_ID));
    }

    @Test
    void testCartRecoveryFromBackup() {
        // 1. Create cart with items
        AddToCartRequest addRequest = new AddToCartRequest(
            "product1", "sku1", "Product 1", 3, new BigDecimal("12.50"));
        Cart originalCart = cartService.addToCart(TENANT_ID, USER_ID, addRequest);
        
        assertEquals(1, originalCart.getItems().size());

        // 2. Simulate Redis failure by deleting from Redis only
        cartRedisRepository.deleteByTenantIdAndUserId(TENANT_ID, USER_ID);
        assertFalse(cartRedisRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID));

        // 3. Verify backup still exists
        assertTrue(cartBackupRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID));

        // 4. Get cart should recover from backup
        Cart recoveredCart = cartService.getCart(TENANT_ID, USER_ID);
        assertNotNull(recoveredCart);
        assertEquals(1, recoveredCart.getItems().size());
        assertEquals("product1", recoveredCart.getItems().get(0).getProductId());
        assertEquals(Integer.valueOf(3), recoveredCart.getItems().get(0).getQuantity());

        // 5. Verify cart is back in Redis
        assertTrue(cartRedisRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID));
    }

    @Test
    void testCartCalculations() {
        // Add items with different prices and quantities
        AddToCartRequest item1 = new AddToCartRequest(
            "product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        AddToCartRequest item2 = new AddToCartRequest(
            "product2", "sku2", "Product 2", 1, new BigDecimal("15.50"));

        cartService.addToCart(TENANT_ID, USER_ID, item1);
        Cart cart = cartService.addToCart(TENANT_ID, USER_ID, item2);

        // Verify calculations
        assertEquals(new BigDecimal("35.50"), cart.getSubtotal()); // 20.00 + 15.50
        assertEquals(new BigDecimal("2.84"), cart.getTax()); // 8% of 35.50
        assertEquals(new BigDecimal("38.34"), cart.getTotal()); // 35.50 + 2.84
    }

    @Test
    void testTenantIsolation() {
        String tenant1 = "tenant1";
        String tenant2 = "tenant2";
        String user1 = "user1";

        // Add item to tenant1 cart
        AddToCartRequest request1 = new AddToCartRequest(
            "product1", "sku1", "Product 1", 1, new BigDecimal("10.00"));
        cartService.addToCart(tenant1, user1, request1);

        // Add different item to tenant2 cart
        AddToCartRequest request2 = new AddToCartRequest(
            "product2", "sku2", "Product 2", 2, new BigDecimal("20.00"));
        cartService.addToCart(tenant2, user1, request2);

        // Verify tenant1 cart
        Cart tenant1Cart = cartService.getCart(tenant1, user1);
        assertEquals(1, tenant1Cart.getItems().size());
        assertEquals("product1", tenant1Cart.getItems().get(0).getProductId());

        // Verify tenant2 cart
        Cart tenant2Cart = cartService.getCart(tenant2, user1);
        assertEquals(1, tenant2Cart.getItems().size());
        assertEquals("product2", tenant2Cart.getItems().get(0).getProductId());

        // Verify carts are completely separate
        assertNotEquals(tenant1Cart.getId(), tenant2Cart.getId());
    }

    @Test
    void testConcurrentCartOperations() throws InterruptedException {
        // This test simulates concurrent operations on the same cart
        // In a real scenario, this would test optimistic locking or other concurrency controls
        
        AddToCartRequest request = new AddToCartRequest(
            "product1", "sku1", "Product 1", 1, new BigDecimal("10.00"));
        
        // Add item multiple times concurrently (simulated)
        cartService.addToCart(TENANT_ID, USER_ID, request);
        cartService.addToCart(TENANT_ID, USER_ID, request);
        cartService.addToCart(TENANT_ID, USER_ID, request);

        Cart finalCart = cartService.getCart(TENANT_ID, USER_ID);
        
        // Should have accumulated quantity
        assertEquals(1, finalCart.getItems().size());
        assertEquals(Integer.valueOf(3), finalCart.getItems().get(0).getQuantity());
    }
}
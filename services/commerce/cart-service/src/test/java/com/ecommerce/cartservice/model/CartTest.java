package com.ecommerce.cartservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    private Cart cart;
    private static final String TENANT_ID = "tenant1";
    private static final String USER_ID = "user1";

    @BeforeEach
    void setUp() {
        cart = new Cart(TENANT_ID, USER_ID);
    }

    @Test
    void testCartCreation() {
        assertNotNull(cart);
        assertEquals(TENANT_ID, cart.getTenantId());
        assertEquals(USER_ID, cart.getUserId());
        assertEquals(Cart.generateCartId(TENANT_ID, USER_ID), cart.getId());
        assertEquals(BigDecimal.ZERO, cart.getSubtotal());
        assertEquals(BigDecimal.ZERO, cart.getTax());
        assertEquals(BigDecimal.ZERO, cart.getTotal());
        assertEquals("USD", cart.getCurrency());
        assertTrue(cart.getItems().isEmpty());
        assertNotNull(cart.getUpdatedAt());
        assertNotNull(cart.getExpiresAt());
        assertEquals(Long.valueOf(7L), cart.getTtl());
    }

    @Test
    void testGenerateCartId() {
        String cartId = Cart.generateCartId(TENANT_ID, USER_ID);
        assertEquals("tenant1:user1", cartId);
    }

    @Test
    void testAddItem_NewItem() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        LocalDateTime beforeAdd = cart.getUpdatedAt();
        
        cart.addItem(item);
        
        assertEquals(1, cart.getItems().size());
        assertEquals(item, cart.getItems().get(0));
        assertTrue(cart.getUpdatedAt().isAfter(beforeAdd));
    }

    @Test
    void testAddItem_ExistingItem() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product1", "sku1", "Product 1", 3, new BigDecimal("10.00"));
        
        cart.addItem(item1);
        cart.addItem(item2);
        
        assertEquals(1, cart.getItems().size());
        assertEquals(Integer.valueOf(5), cart.getItems().get(0).getQuantity()); // 2 + 3
    }

    @Test
    void testRemoveItem() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product2", "sku2", "Product 2", 1, new BigDecimal("15.00"));
        
        cart.addItem(item1);
        cart.addItem(item2);
        assertEquals(2, cart.getItems().size());
        
        cart.removeItem("product1", "sku1");
        
        assertEquals(1, cart.getItems().size());
        assertEquals("product2", cart.getItems().get(0).getProductId());
    }

    @Test
    void testUpdateItemQuantity_ValidQuantity() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        cart.addItem(item);
        
        cart.updateItemQuantity("product1", "sku1", 5);
        
        assertEquals(1, cart.getItems().size());
        assertEquals(Integer.valueOf(5), cart.getItems().get(0).getQuantity());
    }

    @Test
    void testUpdateItemQuantity_ZeroQuantity() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        cart.addItem(item);
        
        cart.updateItemQuantity("product1", "sku1", 0);
        
        assertEquals(0, cart.getItems().size());
    }

    @Test
    void testUpdateItemQuantity_NegativeQuantity() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        cart.addItem(item);
        
        cart.updateItemQuantity("product1", "sku1", -1);
        
        assertEquals(0, cart.getItems().size());
    }

    @Test
    void testUpdateItemQuantity_NonExistentItem() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        cart.addItem(item);
        
        cart.updateItemQuantity("product2", "sku2", 5);
        
        assertEquals(1, cart.getItems().size());
        assertEquals(Integer.valueOf(2), cart.getItems().get(0).getQuantity()); // Unchanged
    }

    @Test
    void testClearItems() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product2", "sku2", "Product 2", 1, new BigDecimal("15.00"));
        
        cart.addItem(item1);
        cart.addItem(item2);
        assertEquals(2, cart.getItems().size());
        
        cart.clearItems();
        
        assertEquals(0, cart.getItems().size());
    }

    @Test
    void testGetTotalItemCount() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product2", "sku2", "Product 2", 3, new BigDecimal("15.00"));
        
        cart.addItem(item1);
        cart.addItem(item2);
        
        assertEquals(5, cart.getTotalItemCount()); // 2 + 3
    }

    @Test
    void testGetTotalItemCount_EmptyCart() {
        assertEquals(0, cart.getTotalItemCount());
    }

    @Test
    void testTimestampUpdates() {
        LocalDateTime initialUpdatedAt = cart.getUpdatedAt();
        LocalDateTime initialExpiresAt = cart.getExpiresAt();
        
        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        cart.addItem(item);
        
        assertTrue(cart.getUpdatedAt().isAfter(initialUpdatedAt));
        assertTrue(cart.getExpiresAt().isAfter(initialExpiresAt));
    }

    @Test
    void testSetItems_NullList() {
        cart.setItems(null);
        assertNotNull(cart.getItems());
        assertTrue(cart.getItems().isEmpty());
    }
}
package com.ecommerce.cartservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        cartItem = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
    }

    @Test
    void testCartItemCreation() {
        assertNotNull(cartItem);
        assertEquals("product1", cartItem.getProductId());
        assertEquals("sku1", cartItem.getSku());
        assertEquals("Product 1", cartItem.getProductName());
        assertEquals(Integer.valueOf(2), cartItem.getQuantity());
        assertEquals(new BigDecimal("10.00"), cartItem.getUnitPrice());
        assertNotNull(cartItem.getAddedAt());
        assertNotNull(cartItem.getUpdatedAt());
    }

    @Test
    void testGetTotalPrice() {
        BigDecimal totalPrice = cartItem.getTotalPrice();
        assertEquals(new BigDecimal("20.00"), totalPrice); // 2 * 10.00
    }

    @Test
    void testGetTotalPrice_NullQuantity() {
        cartItem.setQuantity(null);
        BigDecimal totalPrice = cartItem.getTotalPrice();
        assertEquals(BigDecimal.ZERO, totalPrice);
    }

    @Test
    void testGetTotalPrice_NullUnitPrice() {
        cartItem.setUnitPrice(null);
        BigDecimal totalPrice = cartItem.getTotalPrice();
        assertEquals(BigDecimal.ZERO, totalPrice);
    }

    @Test
    void testGetTotalPrice_ZeroQuantity() {
        cartItem.setQuantity(0);
        BigDecimal totalPrice = cartItem.getTotalPrice();
        assertEquals(new BigDecimal("0.00"), totalPrice);
    }

    @Test
    void testGetTotalPrice_DecimalCalculation() {
        cartItem.setQuantity(3);
        cartItem.setUnitPrice(new BigDecimal("9.99"));
        BigDecimal totalPrice = cartItem.getTotalPrice();
        assertEquals(new BigDecimal("29.97"), totalPrice); // 3 * 9.99
    }

    @Test
    void testSetQuantity_UpdatesTimestamp() {
        LocalDateTime initialUpdatedAt = cartItem.getUpdatedAt();
        
        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        cartItem.setQuantity(5);
        
        assertEquals(Integer.valueOf(5), cartItem.getQuantity());
        assertTrue(cartItem.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    @Test
    void testEquals_SameProductAndSku() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product1", "sku1", "Product 1 Different Name", 5, new BigDecimal("15.00"));
        
        assertEquals(item1, item2);
    }

    @Test
    void testEquals_DifferentProduct() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product2", "sku1", "Product 2", 2, new BigDecimal("10.00"));
        
        assertNotEquals(item1, item2);
    }

    @Test
    void testEquals_DifferentSku() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product1", "sku2", "Product 1", 2, new BigDecimal("10.00"));
        
        assertNotEquals(item1, item2);
    }

    @Test
    void testEquals_SameObject() {
        assertEquals(cartItem, cartItem);
    }

    @Test
    void testEquals_NullObject() {
        assertNotEquals(cartItem, null);
    }

    @Test
    void testEquals_DifferentClass() {
        assertNotEquals(cartItem, "not a cart item");
    }

    @Test
    void testHashCode_SameProductAndSku() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product1", "sku1", "Product 1 Different Name", 5, new BigDecimal("15.00"));
        
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    void testHashCode_DifferentProductOrSku() {
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product2", "sku1", "Product 2", 2, new BigDecimal("10.00"));
        CartItem item3 = new CartItem("product1", "sku2", "Product 1", 2, new BigDecimal("10.00"));
        
        assertNotEquals(item1.hashCode(), item2.hashCode());
        assertNotEquals(item1.hashCode(), item3.hashCode());
    }

    @Test
    void testOptionalFields() {
        cartItem.setImageUrl("https://example.com/image.jpg");
        cartItem.setAttributes("{\"color\": \"red\", \"size\": \"M\"}");
        
        assertEquals("https://example.com/image.jpg", cartItem.getImageUrl());
        assertEquals("{\"color\": \"red\", \"size\": \"M\"}", cartItem.getAttributes());
    }
}
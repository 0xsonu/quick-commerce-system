package com.ecommerce.orderservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        orderItem = new OrderItem("prod1", "SKU1", "Product 1", 2, new BigDecimal("10.50"));
    }

    @Test
    void testOrderItemCreation() {
        assertNotNull(orderItem);
        assertEquals("prod1", orderItem.getProductId());
        assertEquals("SKU1", orderItem.getSku());
        assertEquals("Product 1", orderItem.getProductName());
        assertEquals(2, orderItem.getQuantity());
        assertEquals(new BigDecimal("10.50"), orderItem.getUnitPrice());
        assertEquals(new BigDecimal("21.00"), orderItem.getTotalPrice());
    }

    @Test
    void testCalculateTotalPrice() {
        orderItem.setQuantity(3);
        orderItem.calculateTotalPrice();
        
        assertEquals(new BigDecimal("31.50"), orderItem.getTotalPrice());
    }

    @Test
    void testUpdateQuantity() {
        orderItem.updateQuantity(5);
        
        assertEquals(5, orderItem.getQuantity());
        assertEquals(new BigDecimal("52.50"), orderItem.getTotalPrice());
    }

    @Test
    void testUpdateQuantityWithZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            orderItem.updateQuantity(0);
        });
    }

    @Test
    void testUpdateQuantityWithNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            orderItem.updateQuantity(-1);
        });
    }

    @Test
    void testDecimalPrecision() {
        OrderItem precisionItem = new OrderItem("prod2", "SKU2", "Product 2", 3, new BigDecimal("10.333"));
        
        // Total should be 3 * 10.333 = 30.999
        assertEquals(new BigDecimal("30.999"), precisionItem.getTotalPrice());
    }
}
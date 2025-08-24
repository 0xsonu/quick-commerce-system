package com.ecommerce.orderservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order("tenant1", "ORD-123", 1L);
    }

    @Test
    void testOrderCreation() {
        assertNotNull(order);
        assertEquals("tenant1", order.getTenantId());
        assertEquals("ORD-123", order.getOrderNumber());
        assertEquals(1L, order.getUserId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void testAddItem() {
        OrderItem item = new OrderItem("prod1", "SKU1", "Product 1", 2, new BigDecimal("10.00"));
        
        order.addItem(item);
        
        assertEquals(1, order.getItems().size());
        assertEquals(order, item.getOrder());
        assertTrue(order.getItems().contains(item));
    }

    @Test
    void testRemoveItem() {
        OrderItem item = new OrderItem("prod1", "SKU1", "Product 1", 2, new BigDecimal("10.00"));
        order.addItem(item);
        
        order.removeItem(item);
        
        assertTrue(order.getItems().isEmpty());
        assertNull(item.getOrder());
    }

    @Test
    void testCalculateTotals() {
        OrderItem item1 = new OrderItem("prod1", "SKU1", "Product 1", 2, new BigDecimal("10.00"));
        OrderItem item2 = new OrderItem("prod2", "SKU2", "Product 2", 1, new BigDecimal("30.00"));
        
        order.addItem(item1);
        order.addItem(item2);
        order.calculateTotals();
        
        // Subtotal: (2 * 10.00) + (1 * 30.00) = 50.00
        assertEquals(0, new BigDecimal("50.00").compareTo(order.getSubtotal()));
        
        // Tax: 50.00 * 0.08 = 4.00
        assertEquals(0, new BigDecimal("4.00").compareTo(order.getTaxAmount()));
        
        // Shipping: 50.00 < 100.00, so 9.99
        assertEquals(0, new BigDecimal("9.99").compareTo(order.getShippingAmount()));
        
        // Total: 50.00 + 4.00 + 9.99 = 63.99
        assertEquals(0, new BigDecimal("63.99").compareTo(order.getTotalAmount()));
    }

    @Test
    void testCalculateTotalsWithFreeShipping() {
        OrderItem item = new OrderItem("prod1", "SKU1", "Product 1", 10, new BigDecimal("15.00"));
        
        order.addItem(item);
        order.calculateTotals();
        
        // Subtotal: 10 * 15.00 = 150.00
        assertEquals(0, new BigDecimal("150.00").compareTo(order.getSubtotal()));
        
        // Shipping: 150.00 >= 100.00, so free shipping
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getShippingAmount()));
    }

    @Test
    void testStatusTransitions() {
        // Test valid transitions
        assertTrue(order.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(order.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(order.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(order.canTransitionTo(OrderStatus.DELIVERED));

        // Test status update
        order.updateStatus(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        // Test invalid transition
        assertThrows(IllegalStateException.class, () -> {
            order.updateStatus(OrderStatus.DELIVERED);
        });
    }

    @Test
    void testStatusTransitionFromConfirmed() {
        order.updateStatus(OrderStatus.CONFIRMED);
        
        assertTrue(order.canTransitionTo(OrderStatus.PROCESSING));
        assertTrue(order.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(order.canTransitionTo(OrderStatus.PENDING));
        assertFalse(order.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void testFinalStates() {
        order.updateStatus(OrderStatus.CANCELLED);
        assertFalse(order.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(order.canTransitionTo(OrderStatus.PROCESSING));
        
        Order deliveredOrder = new Order("tenant1", "ORD-456", 2L);
        deliveredOrder.updateStatus(OrderStatus.CONFIRMED);
        deliveredOrder.updateStatus(OrderStatus.PROCESSING);
        deliveredOrder.updateStatus(OrderStatus.SHIPPED);
        deliveredOrder.updateStatus(OrderStatus.DELIVERED);
        
        assertFalse(deliveredOrder.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(deliveredOrder.canTransitionTo(OrderStatus.SHIPPED));
    }
}
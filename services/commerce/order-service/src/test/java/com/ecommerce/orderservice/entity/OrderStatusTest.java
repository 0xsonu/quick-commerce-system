package com.ecommerce.orderservice.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void testPendingTransitions() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.PROCESSING));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void testConfirmedTransitions() {
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PROCESSING));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void testProcessingTransitions() {
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void testShippedTransitions() {
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PROCESSING));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void testFinalStates() {
        assertTrue(OrderStatus.DELIVERED.isFinalState());
        assertTrue(OrderStatus.CANCELLED.isFinalState());
        assertFalse(OrderStatus.PENDING.isFinalState());
        assertFalse(OrderStatus.CONFIRMED.isFinalState());
        assertFalse(OrderStatus.PROCESSING.isFinalState());
        assertFalse(OrderStatus.SHIPPED.isFinalState());
    }

    @Test
    void testActiveStates() {
        assertTrue(OrderStatus.PENDING.isActive());
        assertTrue(OrderStatus.CONFIRMED.isActive());
        assertTrue(OrderStatus.PROCESSING.isActive());
        assertTrue(OrderStatus.SHIPPED.isActive());
        assertFalse(OrderStatus.DELIVERED.isActive());
        assertFalse(OrderStatus.CANCELLED.isActive());
    }

    @Test
    void testAllowedTransitions() {
        assertEquals(2, OrderStatus.PENDING.getAllowedTransitions().size());
        assertEquals(2, OrderStatus.CONFIRMED.getAllowedTransitions().size());
        assertEquals(2, OrderStatus.PROCESSING.getAllowedTransitions().size());
        assertEquals(1, OrderStatus.SHIPPED.getAllowedTransitions().size());
        assertEquals(0, OrderStatus.DELIVERED.getAllowedTransitions().size());
        assertEquals(0, OrderStatus.CANCELLED.getAllowedTransitions().size());
    }
}
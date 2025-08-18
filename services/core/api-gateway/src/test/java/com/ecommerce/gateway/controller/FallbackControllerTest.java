package com.ecommerce.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackControllerTest {

    private final FallbackController fallbackController = new FallbackController();

    @Test
    void shouldReturnAuthServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.authFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTH_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Authentication service is temporarily unavailable", response.getBody().get("message"));
        assertEquals(503, response.getBody().get("status"));
        assertNotNull(response.getBody().get("timestamp"));
        assertNotNull(response.getBody().get("suggestion"));
    }

    @Test
    void shouldReturnUserServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.userFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("USER_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("User service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldReturnProductServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.productFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PRODUCT_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Product service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldReturnInventoryServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.inventoryFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVENTORY_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Inventory service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldReturnCartServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.cartFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CART_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Shopping cart service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldReturnOrderServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.orderFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ORDER_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Order service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldReturnPaymentServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.paymentFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PAYMENT_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Payment service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldReturnShippingServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.shippingFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SHIPPING_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Shipping service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldReturnNotificationServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.notificationFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOTIFICATION_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Notification service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldReturnReviewServiceFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.reviewFallback();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("REVIEW_SERVICE_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("Review service is temporarily unavailable", response.getBody().get("message"));
    }

    @Test
    void shouldIncludeAllRequiredFieldsInFallbackResponse() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.authFallback();

        // Then
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        // Verify all required fields are present
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("suggestion"));
        
        // Verify field types
        assertTrue(body.get("error") instanceof String);
        assertTrue(body.get("message") instanceof String);
        assertTrue(body.get("timestamp") instanceof String);
        assertTrue(body.get("status") instanceof Integer);
        assertTrue(body.get("suggestion") instanceof String);
    }
}
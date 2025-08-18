package com.ecommerce.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller for circuit breaker responses
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        logger.warn("Auth service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Authentication service is temporarily unavailable", 
            "AUTH_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        logger.warn("User service circuit breaker activated - returning fallback response");
        return createFallbackResponse("User service is temporarily unavailable", 
            "USER_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/product")
    public ResponseEntity<Map<String, Object>> productFallback() {
        logger.warn("Product service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Product service is temporarily unavailable", 
            "PRODUCT_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryFallback() {
        logger.warn("Inventory service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Inventory service is temporarily unavailable", 
            "INVENTORY_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/cart")
    public ResponseEntity<Map<String, Object>> cartFallback() {
        logger.warn("Cart service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Shopping cart service is temporarily unavailable", 
            "CART_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/order")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        logger.warn("Order service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Order service is temporarily unavailable", 
            "ORDER_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        logger.warn("Payment service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Payment service is temporarily unavailable", 
            "PAYMENT_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/shipping")
    public ResponseEntity<Map<String, Object>> shippingFallback() {
        logger.warn("Shipping service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Shipping service is temporarily unavailable", 
            "SHIPPING_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/notification")
    public ResponseEntity<Map<String, Object>> notificationFallback() {
        logger.warn("Notification service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Notification service is temporarily unavailable", 
            "NOTIFICATION_SERVICE_UNAVAILABLE");
    }

    @RequestMapping("/review")
    public ResponseEntity<Map<String, Object>> reviewFallback() {
        logger.warn("Review service circuit breaker activated - returning fallback response");
        return createFallbackResponse("Review service is temporarily unavailable", 
            "REVIEW_SERVICE_UNAVAILABLE");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message, String errorCode) {
        Map<String, Object> response = Map.of(
            "error", errorCode,
            "message", message,
            "timestamp", LocalDateTime.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
            "suggestion", "Please try again in a few moments. If the problem persists, contact support."
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
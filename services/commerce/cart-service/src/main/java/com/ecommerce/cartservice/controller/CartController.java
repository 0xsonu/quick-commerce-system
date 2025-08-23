package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.UpdateCartItemRequest;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.service.CartService;
import com.ecommerce.shared.models.dto.ErrorResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for shopping cart operations
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Get user's shopping cart
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.debug("GET /api/v1/cart - tenant: {}, user: {}", tenantId, userId);
        
        Cart cart = cartService.getCart(tenantId, userId);
        CartResponse response = new CartResponse(cart);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Add item to cart
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody AddToCartRequest request) {
        
        logger.debug("POST /api/v1/cart/items - tenant: {}, user: {}, product: {}", 
                    tenantId, userId, request.getProductId());
        
        Cart cart = cartService.addToCart(tenantId, userId, request);
        CartResponse response = new CartResponse(cart);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update cart item quantity
     */
    @PutMapping("/items")
    public ResponseEntity<CartResponse> updateCartItem(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        
        logger.debug("PUT /api/v1/cart/items - tenant: {}, user: {}, product: {}, quantity: {}", 
                    tenantId, userId, request.getProductId(), request.getQuantity());
        
        Cart cart = cartService.updateCartItem(tenantId, userId, request);
        CartResponse response = new CartResponse(cart);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/items")
    public ResponseEntity<CartResponse> removeFromCart(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestParam("productId") String productId,
            @RequestParam("sku") String sku) {
        
        logger.debug("DELETE /api/v1/cart/items - tenant: {}, user: {}, product: {}, sku: {}", 
                    tenantId, userId, productId, sku);
        
        Cart cart = cartService.removeFromCart(tenantId, userId, productId, sku);
        CartResponse response = new CartResponse(cart);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Clear entire cart
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.debug("DELETE /api/v1/cart - tenant: {}, user: {}", tenantId, userId);
        
        cartService.clearCart(tenantId, userId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete cart completely (for cleanup)
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteCart(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.debug("DELETE /api/v1/cart/delete - tenant: {}, user: {}", tenantId, userId);
        
        cartService.deleteCart(tenantId, userId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if cart exists
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> cartExists(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.debug("GET /api/v1/cart/exists - tenant: {}, user: {}", tenantId, userId);
        
        boolean exists = cartService.cartExists(tenantId, userId);
        
        return ResponseEntity.ok(exists);
    }

    /**
     * Validate cart for checkout
     */
    @PostMapping("/validate")
    public ResponseEntity<Void> validateCart(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.debug("POST /api/v1/cart/validate - tenant: {}, user: {}", tenantId, userId);
        
        cartService.validateCartForCheckout(tenantId, userId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Cart Service is healthy");
    }
}
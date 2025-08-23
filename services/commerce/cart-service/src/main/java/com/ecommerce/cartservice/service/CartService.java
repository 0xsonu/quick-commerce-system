package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.UpdateCartItemRequest;
import com.ecommerce.cartservice.entity.ShoppingCartBackup;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import com.ecommerce.cartservice.redis.CartRedisRepository;
import com.ecommerce.cartservice.repository.ShoppingCartBackupRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for shopping cart operations with Redis primary storage and MySQL backup
 */
@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRedisRepository cartRedisRepository;
    private final ShoppingCartBackupRepository cartBackupRepository;
    private final CartCalculationService calculationService;

    @Autowired
    public CartService(CartRedisRepository cartRedisRepository,
                      ShoppingCartBackupRepository cartBackupRepository,
                      CartCalculationService calculationService) {
        this.cartRedisRepository = cartRedisRepository;
        this.cartBackupRepository = cartBackupRepository;
        this.calculationService = calculationService;
    }

    /**
     * Get cart for user, create if not exists
     */
    public Cart getCart(String tenantId, String userId) {
        logger.debug("Getting cart for tenant: {} and user: {}", tenantId, userId);

        Optional<Cart> cartOpt = cartRedisRepository.findByTenantIdAndUserId(tenantId, userId);
        
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            calculationService.calculateCartTotals(cart);
            return cart;
        }

        // Try to recover from MySQL backup
        Optional<ShoppingCartBackup> backupOpt = cartBackupRepository.findByTenantIdAndUserId(tenantId, userId);
        if (backupOpt.isPresent()) {
            logger.info("Recovering cart from backup for tenant: {} and user: {}", tenantId, userId);
            Cart recoveredCart = backupOpt.get().getCartObject();
            calculationService.calculateCartTotals(recoveredCart);
            
            // Save back to Redis
            cartRedisRepository.save(recoveredCart);
            return recoveredCart;
        }

        // Create new cart
        logger.info("Creating new cart for tenant: {} and user: {}", tenantId, userId);
        Cart newCart = new Cart(tenantId, userId);
        calculationService.calculateCartTotals(newCart);
        return cartRedisRepository.save(newCart);
    }

    /**
     * Add item to cart
     */
    @Transactional
    public Cart addToCart(String tenantId, String userId, AddToCartRequest request) {
        logger.debug("Adding item to cart for tenant: {} and user: {}", tenantId, userId);

        Cart cart = getCart(tenantId, userId);
        
        CartItem newItem = new CartItem(
            request.getProductId(),
            request.getSku(),
            request.getProductName(),
            request.getQuantity(),
            request.getUnitPrice()
        );
        newItem.setImageUrl(request.getImageUrl());
        newItem.setAttributes(request.getAttributes());

        if (!calculationService.validateCartItem(newItem)) {
            throw new IllegalArgumentException("Invalid cart item data");
        }

        cart.addItem(newItem);
        calculationService.calculateCartTotals(cart);

        // Save to Redis and backup to MySQL
        Cart savedCart = cartRedisRepository.save(cart);
        saveToBackup(savedCart);

        logger.info("Added item {} to cart for tenant: {} and user: {}", 
                   request.getProductId(), tenantId, userId);
        return savedCart;
    }

    /**
     * Update cart item quantity
     */
    @Transactional
    public Cart updateCartItem(String tenantId, String userId, UpdateCartItemRequest request) {
        logger.debug("Updating cart item for tenant: {} and user: {}", tenantId, userId);

        Cart cart = getCart(tenantId, userId);
        cart.updateItemQuantity(request.getProductId(), request.getSku(), request.getQuantity());
        calculationService.calculateCartTotals(cart);

        // Save to Redis and backup to MySQL
        Cart savedCart = cartRedisRepository.save(cart);
        saveToBackup(savedCart);

        logger.info("Updated item {} quantity to {} for tenant: {} and user: {}", 
                   request.getProductId(), request.getQuantity(), tenantId, userId);
        return savedCart;
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public Cart removeFromCart(String tenantId, String userId, String productId, String sku) {
        logger.debug("Removing item from cart for tenant: {} and user: {}", tenantId, userId);

        Cart cart = getCart(tenantId, userId);
        cart.removeItem(productId, sku);
        calculationService.calculateCartTotals(cart);

        // Save to Redis and backup to MySQL
        Cart savedCart = cartRedisRepository.save(cart);
        saveToBackup(savedCart);

        logger.info("Removed item {} from cart for tenant: {} and user: {}", 
                   productId, tenantId, userId);
        return savedCart;
    }

    /**
     * Clear entire cart
     */
    @Transactional
    public void clearCart(String tenantId, String userId) {
        logger.debug("Clearing cart for tenant: {} and user: {}", tenantId, userId);

        Cart cart = getCart(tenantId, userId);
        cart.clearItems();
        calculationService.calculateCartTotals(cart);

        // Save to Redis and backup to MySQL
        cartRedisRepository.save(cart);
        saveToBackup(cart);

        logger.info("Cleared cart for tenant: {} and user: {}", tenantId, userId);
    }

    /**
     * Delete cart completely
     */
    @Transactional
    public void deleteCart(String tenantId, String userId) {
        logger.debug("Deleting cart for tenant: {} and user: {}", tenantId, userId);

        // Delete from Redis
        cartRedisRepository.deleteByTenantIdAndUserId(tenantId, userId);
        
        // Delete from MySQL backup
        cartBackupRepository.deleteByTenantIdAndUserId(tenantId, userId);

        logger.info("Deleted cart for tenant: {} and user: {}", tenantId, userId);
    }

    /**
     * Check if cart exists
     */
    public boolean cartExists(String tenantId, String userId) {
        return cartRedisRepository.existsByTenantIdAndUserId(tenantId, userId) ||
               cartBackupRepository.existsByTenantIdAndUserId(tenantId, userId);
    }

    /**
     * Save cart to MySQL backup
     */
    private void saveToBackup(Cart cart) {
        try {
            Optional<ShoppingCartBackup> existingBackup = 
                cartBackupRepository.findByTenantIdAndUserId(cart.getTenantId(), cart.getUserId());

            if (existingBackup.isPresent()) {
                ShoppingCartBackup backup = existingBackup.get();
                backup.setCartData(cart);
                cartBackupRepository.save(backup);
            } else {
                ShoppingCartBackup newBackup = new ShoppingCartBackup(
                    cart.getTenantId(), cart.getUserId(), cart);
                cartBackupRepository.save(newBackup);
            }
            
            logger.debug("Saved cart backup for tenant: {} and user: {}", 
                        cart.getTenantId(), cart.getUserId());
        } catch (Exception e) {
            logger.error("Failed to save cart backup for tenant: {} and user: {}", 
                        cart.getTenantId(), cart.getUserId(), e);
            // Don't fail the main operation if backup fails
        }
    }
}
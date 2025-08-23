package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.client.InventoryServiceClient;
import com.ecommerce.cartservice.client.ProductServiceClient;
import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.InventoryAvailabilityResponse;
import com.ecommerce.cartservice.dto.ProductValidationResponse;
import com.ecommerce.cartservice.exception.CartValidationException;
import com.ecommerce.cartservice.exception.InsufficientInventoryException;
import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service for validating cart operations against external services
 */
@Service
public class CartValidationService {

    private static final Logger logger = LoggerFactory.getLogger(CartValidationService.class);

    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;

    @Autowired
    public CartValidationService(ProductServiceClient productServiceClient,
                                InventoryServiceClient inventoryServiceClient) {
        this.productServiceClient = productServiceClient;
        this.inventoryServiceClient = inventoryServiceClient;
    }

    /**
     * Validate add to cart request
     */
    public void validateAddToCartRequest(String tenantId, AddToCartRequest request) {
        logger.debug("Validating add to cart request for product {} in tenant {}", request.getProductId(), tenantId);

        // Validate product exists and is active
        ProductValidationResponse product = validateProduct(tenantId, request.getProductId(), request.getSku());
        
        // Validate price hasn't changed significantly (allow 5% variance)
        validatePrice(request.getUnitPrice(), product.getPrice(), request.getProductId());
        
        // Validate inventory availability
        validateInventoryAvailability(tenantId, request.getProductId(), request.getQuantity());

        logger.debug("Add to cart request validation passed for product {}", request.getProductId());
    }

    /**
     * Validate cart item update
     */
    public void validateCartItemUpdate(String tenantId, String productId, String sku, Integer newQuantity) {
        logger.debug("Validating cart item update for product {} quantity {} in tenant {}", productId, newQuantity, tenantId);

        // Validate product still exists and is active
        validateProduct(tenantId, productId, sku);
        
        // Validate inventory availability for new quantity
        validateInventoryAvailability(tenantId, productId, newQuantity);

        logger.debug("Cart item update validation passed for product {}", productId);
    }

    /**
     * Validate entire cart before checkout
     */
    public void validateCartForCheckout(String tenantId, Cart cart) {
        logger.debug("Validating cart for checkout in tenant {}", tenantId);

        if (cart.getItems().isEmpty()) {
            throw new CartValidationException("Cart is empty");
        }

        for (CartItem item : cart.getItems()) {
            // Validate each product
            ProductValidationResponse product = validateProduct(tenantId, item.getProductId(), item.getSku());
            
            // Validate price hasn't changed significantly
            validatePrice(item.getUnitPrice(), product.getPrice(), item.getProductId());
            
            // Validate inventory availability
            validateInventoryAvailability(tenantId, item.getProductId(), item.getQuantity());
        }

        logger.debug("Cart validation for checkout passed");
    }

    /**
     * Validate product exists and is available for purchase
     */
    private ProductValidationResponse validateProduct(String tenantId, String productId, String sku) {
        Optional<ProductValidationResponse> productOpt = productServiceClient.validateProductWithSku(tenantId, productId, sku);
        
        if (productOpt.isEmpty()) {
            logger.warn("Product {} with SKU {} not found for tenant {}", productId, sku, tenantId);
            throw new ProductNotAvailableException("Product not found: " + productId);
        }

        ProductValidationResponse product = productOpt.get();
        if (!product.isAvailableForPurchase()) {
            logger.warn("Product {} is not available for purchase: status={}, active={}", 
                       productId, product.getStatus(), product.isActive());
            throw new ProductNotAvailableException("Product is not available for purchase: " + productId);
        }

        return product;
    }

    /**
     * Validate price hasn't changed significantly
     */
    private void validatePrice(BigDecimal requestedPrice, BigDecimal currentPrice, String productId) {
        if (currentPrice == null) {
            logger.warn("Current price is null for product {}", productId);
            return; // Skip validation if price is not available
        }

        // Allow 5% variance in price
        BigDecimal variance = currentPrice.multiply(BigDecimal.valueOf(0.05));
        BigDecimal minPrice = currentPrice.subtract(variance);
        BigDecimal maxPrice = currentPrice.add(variance);

        if (requestedPrice.compareTo(minPrice) < 0 || requestedPrice.compareTo(maxPrice) > 0) {
            logger.warn("Price mismatch for product {}: requested={}, current={}", 
                       productId, requestedPrice, currentPrice);
            throw new CartValidationException(
                String.format("Price has changed for product %s. Current price: %s", productId, currentPrice));
        }
    }

    /**
     * Validate inventory availability
     */
    private void validateInventoryAvailability(String tenantId, String productId, Integer requestedQuantity) {
        Optional<InventoryAvailabilityResponse> inventoryOpt = 
            inventoryServiceClient.checkAvailability(tenantId, productId, requestedQuantity);
        
        if (inventoryOpt.isEmpty()) {
            logger.warn("Could not check inventory for product {} in tenant {}", productId, tenantId);
            throw new CartValidationException("Unable to verify inventory availability for product: " + productId);
        }

        InventoryAvailabilityResponse inventory = inventoryOpt.get();
        if (!inventory.canFulfillRequest()) {
            logger.warn("Insufficient inventory for product {}: requested={}, available={}", 
                       productId, requestedQuantity, inventory.getAvailableQuantity());
            throw new InsufficientInventoryException(
                String.format("Insufficient inventory for product %s. Available: %d, Requested: %d", 
                             productId, inventory.getAvailableQuantity(), requestedQuantity));
        }
    }
}
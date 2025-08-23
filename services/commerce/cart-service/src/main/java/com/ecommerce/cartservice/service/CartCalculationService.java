package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for cart calculation logic with proper decimal handling
 */
@Service
public class CartCalculationService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8% tax rate
    private static final int DECIMAL_SCALE = 2;

    /**
     * Calculate and update cart totals
     */
    public void calculateCartTotals(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return;
        }

        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal tax = calculateTax(subtotal);
        BigDecimal total = subtotal.add(tax);

        cart.setSubtotal(subtotal);
        cart.setTax(tax);
        cart.setTotal(total);
    }

    /**
     * Calculate subtotal from all cart items
     */
    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(this::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total for a single cart item
     */
    private BigDecimal calculateItemTotal(CartItem item) {
        if (item.getQuantity() == null || item.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        return item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate tax based on subtotal
     */
    private BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(TAX_RATE)
                .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Validate cart item prices and quantities
     */
    public boolean validateCartItem(CartItem item) {
        if (item == null) {
            return false;
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            return false;
        }

        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return true;
    }

    /**
     * Get tax rate for display purposes
     */
    public BigDecimal getTaxRate() {
        return TAX_RATE;
    }
}
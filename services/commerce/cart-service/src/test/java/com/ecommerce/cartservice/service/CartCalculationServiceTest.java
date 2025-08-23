package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class CartCalculationServiceTest {

    private CartCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new CartCalculationService();
    }

    @Test
    void testCalculateCartTotals_EmptyCart() {
        Cart cart = new Cart("tenant1", "user1");
        
        calculationService.calculateCartTotals(cart);
        
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), cart.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), cart.getTax());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), cart.getTotal());
    }

    @Test
    void testCalculateCartTotals_SingleItem() {
        Cart cart = new Cart("tenant1", "user1");
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        cart.addItem(item);
        
        calculationService.calculateCartTotals(cart);
        
        assertEquals(new BigDecimal("20.00"), cart.getSubtotal());
        assertEquals(new BigDecimal("1.60"), cart.getTax()); // 8% of 20.00
        assertEquals(new BigDecimal("21.60"), cart.getTotal());
    }

    @Test
    void testCalculateCartTotals_MultipleItems() {
        Cart cart = new Cart("tenant1", "user1");
        CartItem item1 = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        CartItem item2 = new CartItem("product2", "sku2", "Product 2", 1, new BigDecimal("15.50"));
        cart.addItem(item1);
        cart.addItem(item2);
        
        calculationService.calculateCartTotals(cart);
        
        assertEquals(new BigDecimal("35.50"), cart.getSubtotal()); // 20.00 + 15.50
        assertEquals(new BigDecimal("2.84"), cart.getTax()); // 8% of 35.50
        assertEquals(new BigDecimal("38.34"), cart.getTotal());
    }

    @Test
    void testCalculateCartTotals_DecimalPrecision() {
        Cart cart = new Cart("tenant1", "user1");
        CartItem item = new CartItem("product1", "sku1", "Product 1", 3, new BigDecimal("9.99"));
        cart.addItem(item);
        
        calculationService.calculateCartTotals(cart);
        
        assertEquals(new BigDecimal("29.97"), cart.getSubtotal());
        assertEquals(new BigDecimal("2.40"), cart.getTax()); // 8% of 29.97 = 2.3976, rounded to 2.40
        assertEquals(new BigDecimal("32.37"), cart.getTotal());
    }

    @Test
    void testValidateCartItem_ValidItem() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        
        assertTrue(calculationService.validateCartItem(item));
    }

    @Test
    void testValidateCartItem_NullItem() {
        assertFalse(calculationService.validateCartItem(null));
    }

    @Test
    void testValidateCartItem_ZeroQuantity() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 0, new BigDecimal("10.00"));
        
        assertFalse(calculationService.validateCartItem(item));
    }

    @Test
    void testValidateCartItem_NegativeQuantity() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", -1, new BigDecimal("10.00"));
        
        assertFalse(calculationService.validateCartItem(item));
    }

    @Test
    void testValidateCartItem_ZeroPrice() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, BigDecimal.ZERO);
        
        assertFalse(calculationService.validateCartItem(item));
    }

    @Test
    void testValidateCartItem_NegativePrice() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("-10.00"));
        
        assertFalse(calculationService.validateCartItem(item));
    }

    @Test
    void testValidateCartItem_NullQuantity() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", null, new BigDecimal("10.00"));
        
        assertFalse(calculationService.validateCartItem(item));
    }

    @Test
    void testValidateCartItem_NullPrice() {
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, null);
        
        assertFalse(calculationService.validateCartItem(item));
    }

    @Test
    void testGetTaxRate() {
        BigDecimal taxRate = calculationService.getTaxRate();
        assertEquals(new BigDecimal("0.08"), taxRate);
    }
}
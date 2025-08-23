package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.UpdateCartItemRequest;
import com.ecommerce.cartservice.entity.ShoppingCartBackup;
import com.ecommerce.cartservice.exception.CartValidationException;
import com.ecommerce.cartservice.exception.DuplicateOperationException;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import com.ecommerce.cartservice.redis.CartRedisRepository;
import com.ecommerce.cartservice.repository.ShoppingCartBackupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRedisRepository cartRedisRepository;

    @Mock
    private ShoppingCartBackupRepository cartBackupRepository;

    @Mock
    private CartCalculationService calculationService;

    @Mock
    private CartValidationService validationService;

    @Mock
    private IdempotencyService idempotencyService;

    private CartService cartService;

    private static final String TENANT_ID = "tenant1";
    private static final String USER_ID = "user1";

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRedisRepository, cartBackupRepository, calculationService, 
                                     validationService, idempotencyService);
    }

    @Test
    void testGetCart_ExistingCartInRedis() {
        // Given
        Cart existingCart = new Cart(TENANT_ID, USER_ID);
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(existingCart));

        // When
        Cart result = cartService.getCart(TENANT_ID, USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(USER_ID, result.getUserId());
        verify(calculationService).calculateCartTotals(existingCart);
        verify(cartBackupRepository, never()).findByTenantIdAndUserId(any(), any());
    }

    @Test
    void testGetCart_RecoverFromBackup() {
        // Given
        Cart backupCart = new Cart(TENANT_ID, USER_ID);
        ShoppingCartBackup backup = new ShoppingCartBackup(TENANT_ID, USER_ID, backupCart);
        
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.empty());
        when(cartBackupRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(backup));
        when(cartRedisRepository.save(any(Cart.class))).thenReturn(backupCart);

        // When
        Cart result = cartService.getCart(TENANT_ID, USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(USER_ID, result.getUserId());
        verify(calculationService, atLeastOnce()).calculateCartTotals(any(Cart.class));
        verify(cartRedisRepository).save(any(Cart.class));
    }

    @Test
    void testGetCart_CreateNewCart() {
        // Given
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.empty());
        when(cartBackupRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.empty());
        
        Cart newCart = new Cart(TENANT_ID, USER_ID);
        when(cartRedisRepository.save(any(Cart.class))).thenReturn(newCart);

        // When
        Cart result = cartService.getCart(TENANT_ID, USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(USER_ID, result.getUserId());
        verify(calculationService, atLeastOnce()).calculateCartTotals(any(Cart.class));
        verify(cartRedisRepository).save(any(Cart.class));
    }

    @Test
    void testAddToCart_Success() {
        // Given
        Cart existingCart = new Cart(TENANT_ID, USER_ID);
        AddToCartRequest request = new AddToCartRequest("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(existingCart));
        when(calculationService.validateCartItem(any(CartItem.class))).thenReturn(true);
        when(cartRedisRepository.save(any(Cart.class))).thenReturn(existingCart);

        // When
        Cart result = cartService.addToCart(TENANT_ID, USER_ID, request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        CartItem addedItem = result.getItems().get(0);
        assertEquals("product1", addedItem.getProductId());
        assertEquals("sku1", addedItem.getSku());
        assertEquals(Integer.valueOf(2), addedItem.getQuantity());
        
        verify(validationService).validateAddToCartRequest(TENANT_ID, request);
        verify(calculationService).validateCartItem(any(CartItem.class));
        verify(calculationService, times(2)).calculateCartTotals(any(Cart.class));
        verify(cartRedisRepository).save(any(Cart.class));
        verify(cartBackupRepository).save(any(ShoppingCartBackup.class));
    }

    @Test
    void testAddToCart_InvalidItem() {
        // Given
        Cart existingCart = new Cart(TENANT_ID, USER_ID);
        AddToCartRequest request = new AddToCartRequest("product1", "sku1", "Product 1", 0, new BigDecimal("10.00"));
        
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(existingCart));
        when(calculationService.validateCartItem(any(CartItem.class))).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.addToCart(TENANT_ID, USER_ID, request);
        });
        
        verify(calculationService).validateCartItem(any(CartItem.class));
        verify(cartRedisRepository, never()).save(any(Cart.class));
    }

    @Test
    void testUpdateCartItem_Success() {
        // Given
        Cart existingCart = new Cart(TENANT_ID, USER_ID);
        CartItem existingItem = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        existingCart.addItem(existingItem);
        
        UpdateCartItemRequest request = new UpdateCartItemRequest("product1", "sku1", 5);
        
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(existingCart));
        when(cartRedisRepository.save(any(Cart.class))).thenReturn(existingCart);

        // When
        Cart result = cartService.updateCartItem(TENANT_ID, USER_ID, request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        CartItem updatedItem = result.getItems().get(0);
        assertEquals(Integer.valueOf(5), updatedItem.getQuantity());
        
        verify(validationService).validateCartItemUpdate(TENANT_ID, "product1", "sku1", 5);
        verify(calculationService, times(2)).calculateCartTotals(any(Cart.class));
        verify(cartRedisRepository).save(any(Cart.class));
        verify(cartBackupRepository).save(any(ShoppingCartBackup.class));
    }

    @Test
    void testRemoveFromCart_Success() {
        // Given
        Cart existingCart = new Cart(TENANT_ID, USER_ID);
        CartItem existingItem = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        existingCart.addItem(existingItem);
        
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(existingCart));
        when(cartRedisRepository.save(any(Cart.class))).thenReturn(existingCart);

        // When
        Cart result = cartService.removeFromCart(TENANT_ID, USER_ID, "product1", "sku1");

        // Then
        assertNotNull(result);
        assertEquals(0, result.getItems().size());
        
        verify(calculationService, times(2)).calculateCartTotals(any(Cart.class));
        verify(cartRedisRepository).save(any(Cart.class));
        verify(cartBackupRepository).save(any(ShoppingCartBackup.class));
    }

    @Test
    void testClearCart_Success() {
        // Given
        Cart existingCart = new Cart(TENANT_ID, USER_ID);
        CartItem existingItem = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        existingCart.addItem(existingItem);
        
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(existingCart));
        when(cartRedisRepository.save(any(Cart.class))).thenReturn(existingCart);

        // When
        cartService.clearCart(TENANT_ID, USER_ID);

        // Then
        assertEquals(0, existingCart.getItems().size());
        
        verify(calculationService, times(2)).calculateCartTotals(any(Cart.class));
        verify(cartRedisRepository).save(any(Cart.class));
        verify(cartBackupRepository).save(any(ShoppingCartBackup.class));
    }

    @Test
    void testDeleteCart_Success() {
        // When
        cartService.deleteCart(TENANT_ID, USER_ID);

        // Then
        verify(cartRedisRepository).deleteByTenantIdAndUserId(TENANT_ID, USER_ID);
        verify(cartBackupRepository).deleteByTenantIdAndUserId(TENANT_ID, USER_ID);
    }

    @Test
    void testCartExists_ExistsInRedis() {
        // Given
        when(cartRedisRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(true);

        // When
        boolean exists = cartService.cartExists(TENANT_ID, USER_ID);

        // Then
        assertTrue(exists);
        verify(cartRedisRepository).existsByTenantIdAndUserId(TENANT_ID, USER_ID);
        verify(cartBackupRepository, never()).existsByTenantIdAndUserId(any(), any());
    }

    @Test
    void testCartExists_ExistsInBackup() {
        // Given
        when(cartRedisRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(false);
        when(cartBackupRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(true);

        // When
        boolean exists = cartService.cartExists(TENANT_ID, USER_ID);

        // Then
        assertTrue(exists);
        verify(cartRedisRepository).existsByTenantIdAndUserId(TENANT_ID, USER_ID);
        verify(cartBackupRepository).existsByTenantIdAndUserId(TENANT_ID, USER_ID);
    }

    @Test
    void testCartExists_DoesNotExist() {
        // Given
        when(cartRedisRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(false);
        when(cartBackupRepository.existsByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(false);

        // When
        boolean exists = cartService.cartExists(TENANT_ID, USER_ID);

        // Then
        assertFalse(exists);
        verify(cartRedisRepository).existsByTenantIdAndUserId(TENANT_ID, USER_ID);
        verify(cartBackupRepository).existsByTenantIdAndUserId(TENANT_ID, USER_ID);
    }

    @Test
    void testAddToCart_WithIdempotencyKey_NewOperation() {
        // Given
        Cart existingCart = new Cart(TENANT_ID, USER_ID);
        AddToCartRequest request = new AddToCartRequest("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        request.setIdempotencyKey("idempotency123");
        
        when(idempotencyService.checkIdempotency(TENANT_ID, USER_ID, "idempotency123", CartResponse.class))
            .thenReturn(Optional.empty());
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(existingCart));
        when(calculationService.validateCartItem(any(CartItem.class))).thenReturn(true);
        when(cartRedisRepository.save(any(Cart.class))).thenReturn(existingCart);

        // When
        Cart result = cartService.addToCart(TENANT_ID, USER_ID, request);

        // Then
        assertNotNull(result);
        verify(idempotencyService).checkIdempotency(TENANT_ID, USER_ID, "idempotency123", CartResponse.class);
        verify(idempotencyService).markAsProcessing(TENANT_ID, USER_ID, "idempotency123");
        verify(idempotencyService).storeResult(eq(TENANT_ID), eq(USER_ID), eq("idempotency123"), any(CartResponse.class));
        verify(validationService).validateAddToCartRequest(TENANT_ID, request);
    }

    @Test
    void testAddToCart_WithIdempotencyKey_ExistingOperation() {
        // Given
        AddToCartRequest request = new AddToCartRequest("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        request.setIdempotencyKey("idempotency123");
        
        CartResponse cachedResponse = new CartResponse();
        cachedResponse.setTenantId(TENANT_ID);
        cachedResponse.setUserId(USER_ID);
        
        when(idempotencyService.checkIdempotency(TENANT_ID, USER_ID, "idempotency123", CartResponse.class))
            .thenReturn(Optional.of(cachedResponse));

        // When
        Cart result = cartService.addToCart(TENANT_ID, USER_ID, request);

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(USER_ID, result.getUserId());
        verify(idempotencyService).checkIdempotency(TENANT_ID, USER_ID, "idempotency123", CartResponse.class);
        verifyNoInteractions(validationService);
        verifyNoInteractions(cartRedisRepository);
    }

    @Test
    void testAddToCart_ValidationFails_RemovesIdempotencyKey() {
        // Given
        AddToCartRequest request = new AddToCartRequest("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        request.setIdempotencyKey("idempotency123");
        
        when(idempotencyService.checkIdempotency(TENANT_ID, USER_ID, "idempotency123", CartResponse.class))
            .thenReturn(Optional.empty());
        doThrow(new CartValidationException("Validation failed"))
            .when(validationService).validateAddToCartRequest(TENANT_ID, request);

        // When & Then
        assertThrows(CartValidationException.class, () -> {
            cartService.addToCart(TENANT_ID, USER_ID, request);
        });

        verify(idempotencyService).markAsProcessing(TENANT_ID, USER_ID, "idempotency123");
        verify(idempotencyService).removeIdempotencyKey(TENANT_ID, USER_ID, "idempotency123");
        verify(validationService).validateAddToCartRequest(TENANT_ID, request);
    }

    @Test
    void testUpdateCartItem_WithIdempotencyKey_Success() {
        // Given
        Cart existingCart = new Cart(TENANT_ID, USER_ID);
        CartItem existingItem = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        existingCart.addItem(existingItem);
        
        UpdateCartItemRequest request = new UpdateCartItemRequest("product1", "sku1", 5);
        request.setIdempotencyKey("idempotency456");
        
        when(idempotencyService.checkIdempotency(TENANT_ID, USER_ID, "idempotency456", CartResponse.class))
            .thenReturn(Optional.empty());
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(existingCart));
        when(cartRedisRepository.save(any(Cart.class))).thenReturn(existingCart);

        // When
        Cart result = cartService.updateCartItem(TENANT_ID, USER_ID, request);

        // Then
        assertNotNull(result);
        verify(idempotencyService).checkIdempotency(TENANT_ID, USER_ID, "idempotency456", CartResponse.class);
        verify(idempotencyService).markAsProcessing(TENANT_ID, USER_ID, "idempotency456");
        verify(idempotencyService).storeResult(eq(TENANT_ID), eq(USER_ID), eq("idempotency456"), any(CartResponse.class));
        verify(validationService).validateCartItemUpdate(TENANT_ID, "product1", "sku1", 5);
    }

    @Test
    void testValidateCartForCheckout_Success() {
        // Given
        Cart cart = new Cart(TENANT_ID, USER_ID);
        CartItem item = new CartItem("product1", "sku1", "Product 1", 2, new BigDecimal("10.00"));
        cart.addItem(item);
        
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(cart));

        // When
        cartService.validateCartForCheckout(TENANT_ID, USER_ID);

        // Then
        verify(validationService).validateCartForCheckout(TENANT_ID, cart);
    }

    @Test
    void testValidateCartForCheckout_ValidationFails() {
        // Given
        Cart cart = new Cart(TENANT_ID, USER_ID);
        
        when(cartRedisRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID))
            .thenReturn(Optional.of(cart));
        doThrow(new CartValidationException("Cart is empty"))
            .when(validationService).validateCartForCheckout(TENANT_ID, cart);

        // When & Then
        assertThrows(CartValidationException.class, () -> {
            cartService.validateCartForCheckout(TENANT_ID, USER_ID);
        });

        verify(validationService).validateCartForCheckout(TENANT_ID, cart);
    }
}
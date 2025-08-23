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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartValidationServiceTest {

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @InjectMocks
    private CartValidationService validationService;

    private String tenantId;
    private AddToCartRequest validRequest;
    private ProductValidationResponse validProduct;
    private InventoryAvailabilityResponse availableInventory;

    @BeforeEach
    void setUp() {
        tenantId = "tenant123";
        
        validRequest = new AddToCartRequest();
        validRequest.setProductId("product123");
        validRequest.setSku("SKU123");
        validRequest.setProductName("Test Product");
        validRequest.setQuantity(2);
        validRequest.setUnitPrice(new BigDecimal("29.99"));

        validProduct = new ProductValidationResponse();
        validProduct.setProductId("product123");
        validProduct.setSku("SKU123");
        validProduct.setName("Test Product");
        validProduct.setPrice(new BigDecimal("29.99"));
        validProduct.setStatus("ACTIVE");
        validProduct.setActive(true);

        availableInventory = new InventoryAvailabilityResponse();
        availableInventory.setProductId("product123");
        availableInventory.setAvailableQuantity(10);
        availableInventory.setAvailable(true);
        availableInventory.setRequestedQuantity(2);
    }

    @Test
    void validateAddToCartRequest_Success() {
        // Arrange
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.of(availableInventory));

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateAddToCartRequest(tenantId, validRequest));

        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 2);
    }

    @Test
    void validateAddToCartRequest_ProductNotFound() {
        // Arrange
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.empty());

        // Act & Assert
        ProductNotAvailableException exception = assertThrows(
            ProductNotAvailableException.class,
            () -> validationService.validateAddToCartRequest(tenantId, validRequest)
        );

        assertEquals("Product not found: product123", exception.getMessage());
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void validateAddToCartRequest_ProductNotActive() {
        // Arrange
        validProduct.setActive(false);
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));

        // Act & Assert
        ProductNotAvailableException exception = assertThrows(
            ProductNotAvailableException.class,
            () -> validationService.validateAddToCartRequest(tenantId, validRequest)
        );

        assertEquals("Product is not available for purchase: product123", exception.getMessage());
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void validateAddToCartRequest_PriceMismatch() {
        // Arrange
        validProduct.setPrice(new BigDecimal("39.99")); // Significant price difference
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));

        // Act & Assert
        CartValidationException exception = assertThrows(
            CartValidationException.class,
            () -> validationService.validateAddToCartRequest(tenantId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Price has changed for product product123"));
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void validateAddToCartRequest_InsufficientInventory() {
        // Arrange
        availableInventory.setAvailableQuantity(1);
        availableInventory.setAvailable(false);
        
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.of(availableInventory));

        // Act & Assert
        InsufficientInventoryException exception = assertThrows(
            InsufficientInventoryException.class,
            () -> validationService.validateAddToCartRequest(tenantId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Insufficient inventory for product product123"));
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 2);
    }

    @Test
    void validateCartItemUpdate_Success() {
        // Arrange
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 3))
            .thenReturn(Optional.of(availableInventory));

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateCartItemUpdate(tenantId, "product123", "SKU123", 3));

        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 3);
    }

    @Test
    void validateCartForCheckout_EmptyCart() {
        // Arrange
        Cart emptyCart = new Cart(tenantId, "user123");

        // Act & Assert
        CartValidationException exception = assertThrows(
            CartValidationException.class,
            () -> validationService.validateCartForCheckout(tenantId, emptyCart)
        );

        assertEquals("Cart is empty", exception.getMessage());
    }

    @Test
    void validateCartForCheckout_Success() {
        // Arrange
        Cart cart = new Cart(tenantId, "user123");
        CartItem item = new CartItem("product123", "SKU123", "Test Product", 2, new BigDecimal("29.99"));
        cart.addItem(item);

        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.of(availableInventory));

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateCartForCheckout(tenantId, cart));

        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 2);
    }

    @Test
    void validateAddToCartRequest_InventoryCheckFails() {
        // Arrange
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.empty());

        // Act & Assert
        CartValidationException exception = assertThrows(
            CartValidationException.class,
            () -> validationService.validateAddToCartRequest(tenantId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Unable to verify inventory availability"));
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 2);
    }

    @Test
    void validateAddToCartRequest_PriceWithinTolerance() {
        // Arrange
        validProduct.setPrice(new BigDecimal("30.50")); // Within 5% tolerance
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.of(availableInventory));

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateAddToCartRequest(tenantId, validRequest));

        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 2);
    }
}
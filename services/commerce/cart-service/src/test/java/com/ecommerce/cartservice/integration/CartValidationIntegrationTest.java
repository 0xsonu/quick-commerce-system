package com.ecommerce.cartservice.integration;

import com.ecommerce.cartservice.client.InventoryServiceClient;
import com.ecommerce.cartservice.client.ProductServiceClient;
import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.InventoryAvailabilityResponse;
import com.ecommerce.cartservice.dto.ProductValidationResponse;
import com.ecommerce.cartservice.exception.CartValidationException;
import com.ecommerce.cartservice.exception.InsufficientInventoryException;
import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartValidationIntegrationTest {

    @Autowired
    private CartService cartService;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String tenantId;
    private String userId;
    private AddToCartRequest validRequest;
    private ProductValidationResponse validProduct;
    private InventoryAvailabilityResponse availableInventory;

    @BeforeEach
    void setUp() {
        tenantId = "tenant123";
        userId = "user123";

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

        // Clear Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void addToCart_WithValidation_Success() {
        // Arrange
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.of(availableInventory));

        // Act
        var result = cartService.addToCart(tenantId, userId, validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals("product123", result.getItems().get(0).getProductId());
        assertEquals(Integer.valueOf(2), result.getItems().get(0).getQuantity());

        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 2);
    }

    @Test
    void addToCart_ProductNotFound_ThrowsException() {
        // Arrange
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.empty());

        // Act & Assert
        ProductNotAvailableException exception = assertThrows(
            ProductNotAvailableException.class,
            () -> cartService.addToCart(tenantId, userId, validRequest)
        );

        assertEquals("Product not found: product123", exception.getMessage());
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void addToCart_InsufficientInventory_ThrowsException() {
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
            () -> cartService.addToCart(tenantId, userId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Insufficient inventory"));
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 2);
    }

    @Test
    void addToCart_WithIdempotencyKey_PreventsDuplicates() throws Exception {
        // Arrange
        validRequest.setIdempotencyKey("idempotency123");

        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.of(availableInventory));

        // Act - First call
        var result1 = cartService.addToCart(tenantId, userId, validRequest);

        // Act - Second call with same idempotency key
        var result2 = cartService.addToCart(tenantId, userId, validRequest);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getItems().size(), result2.getItems().size());
        assertEquals(result1.getTotal(), result2.getTotal());

        // Verify external services called only once
        verify(productServiceClient, times(1)).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient, times(1)).checkAvailability(tenantId, "product123", 2);
    }

    @Test
    void addToCart_PriceMismatch_ThrowsException() {
        // Arrange
        validProduct.setPrice(new BigDecimal("39.99")); // Significant price difference

        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));

        // Act & Assert
        CartValidationException exception = assertThrows(
            CartValidationException.class,
            () -> cartService.addToCart(tenantId, userId, validRequest)
        );

        assertTrue(exception.getMessage().contains("Price has changed"));
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void validateCartForCheckout_EmptyCart_ThrowsException() {
        // Act & Assert
        CartValidationException exception = assertThrows(
            CartValidationException.class,
            () -> cartService.validateCartForCheckout(tenantId, userId)
        );

        assertEquals("Cart is empty", exception.getMessage());
    }

    @Test
    void validateCartForCheckout_ValidCart_Success() {
        // Arrange - First add item to cart
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.of(availableInventory));

        cartService.addToCart(tenantId, userId, validRequest);

        // Reset mocks for validation call
        reset(productServiceClient, inventoryServiceClient);
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenReturn(Optional.of(validProduct));
        when(inventoryServiceClient.checkAvailability(tenantId, "product123", 2))
            .thenReturn(Optional.of(availableInventory));

        // Act & Assert
        assertDoesNotThrow(() -> cartService.validateCartForCheckout(tenantId, userId));

        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
        verify(inventoryServiceClient).checkAvailability(tenantId, "product123", 2);
    }

    @Test
    void addToCart_ExternalServiceFailure_HandlesGracefully() {
        // Arrange
        when(productServiceClient.validateProductWithSku(tenantId, "product123", "SKU123"))
            .thenThrow(new RuntimeException("External service unavailable"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> cartService.addToCart(tenantId, userId, validRequest)
        );

        assertEquals("External service unavailable", exception.getMessage());
        verify(productServiceClient).validateProductWithSku(tenantId, "product123", "SKU123");
    }
}
package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.AddressDto;
import com.ecommerce.orderservice.dto.CreateOrderItemRequest;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.entity.IdempotencyStatus;
import com.ecommerce.orderservice.entity.IdempotencyToken;
import com.ecommerce.orderservice.exception.IdempotencyException;
import com.ecommerce.orderservice.repository.IdempotencyTokenRepository;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceSimpleTest {

    @Mock
    private IdempotencyTokenRepository tokenRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private CreateOrderRequest createOrderRequest;
    private String tenantId = "tenant123";
    private Long userId = 1L;
    private String token = "idempotency-token-123";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
        
        // Create test order request
        AddressDto address = new AddressDto();
        address.setStreetAddress("123 Test St");
        address.setCity("Test City");
        address.setState("TS");
        address.setPostalCode("12345");
        address.setCountry("US");

        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId("product123");
        item.setSku("SKU123");
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("29.99"));

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(userId);
        createOrderRequest.setItems(List.of(item));
        createOrderRequest.setBillingAddress(address);
        createOrderRequest.setShippingAddress(address);
    }

    @Test
    void validateIdempotencyToken_NewToken_ShouldCreateNewToken() throws Exception {
        // Given
        when(tokenRepository.findByTenantIdAndToken(tenantId, token)).thenReturn(Optional.empty());
        when(tokenRepository.findByTenantIdAndUserIdAndRequestHashAndStatus(
            eq(tenantId), eq(userId), anyString(), eq(IdempotencyStatus.COMPLETED)))
            .thenReturn(Optional.empty());
        when(tokenRepository.countActiveTokensForUserSince(eq(tenantId), eq(userId), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(objectMapper.writeValueAsString(createOrderRequest)).thenReturn("{\"test\":\"data\"}");
        
        IdempotencyToken savedToken = new IdempotencyToken(tenantId, token, userId, "hash123", LocalDateTime.now().plusHours(24));
        when(tokenRepository.save(any(IdempotencyToken.class))).thenReturn(savedToken);

        // When
        IdempotencyService.IdempotencyValidationResult result = 
            idempotencyService.validateIdempotencyToken(token, userId, createOrderRequest);

        // Then
        assertNotNull(result);
        assertFalse(result.isReturnCachedResult());
        assertNull(result.getCachedResponse());
        verify(tokenRepository).save(any(IdempotencyToken.class));
    }

    @Test
    void validateIdempotencyToken_ProcessingToken_ShouldThrowException() throws Exception {
        // Given
        IdempotencyToken existingToken = new IdempotencyToken(tenantId, token, userId, "hash123", LocalDateTime.now().plusHours(24));
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, token)).thenReturn(Optional.of(existingToken));
        when(objectMapper.writeValueAsString(createOrderRequest)).thenReturn("{\"test\":\"data\"}");

        // When & Then
        assertThrows(IdempotencyException.class, () -> 
            idempotencyService.validateIdempotencyToken(token, userId, createOrderRequest));
    }

    @Test
    void validateIdempotencyToken_ExpiredToken_ShouldThrowException() throws Exception {
        // Given
        IdempotencyToken expiredToken = new IdempotencyToken(tenantId, token, userId, "hash123", LocalDateTime.now().minusHours(1));
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, token)).thenReturn(Optional.of(expiredToken));
        when(objectMapper.writeValueAsString(createOrderRequest)).thenReturn("{\"test\":\"data\"}");

        // When & Then
        assertThrows(IdempotencyException.class, () -> 
            idempotencyService.validateIdempotencyToken(token, userId, createOrderRequest));
    }

    @Test
    void validateIdempotencyToken_WrongUser_ShouldThrowException() throws Exception {
        // Given
        IdempotencyToken tokenForDifferentUser = new IdempotencyToken(tenantId, token, 999L, "hash123", LocalDateTime.now().plusHours(24));
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, token)).thenReturn(Optional.of(tokenForDifferentUser));
        when(objectMapper.writeValueAsString(createOrderRequest)).thenReturn("{\"test\":\"data\"}");

        // When & Then
        assertThrows(IdempotencyException.class, () -> 
            idempotencyService.validateIdempotencyToken(token, userId, createOrderRequest));
    }

    @Test
    void validateIdempotencyToken_RateLimitExceeded_ShouldThrowException() {
        // Given
        when(tokenRepository.countActiveTokensForUserSince(eq(tenantId), eq(userId), any(LocalDateTime.class)))
            .thenReturn(15L); // Exceeds limit of 10

        // When & Then
        IdempotencyException exception = assertThrows(IdempotencyException.class, () -> 
            idempotencyService.validateIdempotencyToken(token, userId, createOrderRequest));
        
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    void markTokenCompleted_ExistingToken_ShouldUpdateToken() throws Exception {
        // Given
        IdempotencyToken existingToken = new IdempotencyToken(tenantId, token, userId, "hash123", LocalDateTime.now().plusHours(24));
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, token)).thenReturn(Optional.of(existingToken));
        when(tokenRepository.save(existingToken)).thenReturn(existingToken);

        // When
        idempotencyService.markTokenCompleted(token, 123L, null);

        // Then
        assertEquals(IdempotencyStatus.COMPLETED, existingToken.getStatus());
        assertEquals(123L, existingToken.getOrderId());
        verify(tokenRepository).save(existingToken);
    }

    @Test
    void markTokenFailed_ExistingToken_ShouldUpdateToken() {
        // Given
        IdempotencyToken existingToken = new IdempotencyToken(tenantId, token, userId, "hash123", LocalDateTime.now().plusHours(24));
        String errorMessage = "Order creation failed";
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, token)).thenReturn(Optional.of(existingToken));
        when(tokenRepository.save(existingToken)).thenReturn(existingToken);

        // When
        idempotencyService.markTokenFailed(token, errorMessage);

        // Then
        assertEquals(IdempotencyStatus.FAILED, existingToken.getStatus());
        assertEquals(errorMessage, existingToken.getResponseData());
        verify(tokenRepository).save(existingToken);
    }

    @Test
    void cleanupExpiredTokens_ShouldDeleteExpiredTokens() {
        // Given
        when(tokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(5);

        // When
        idempotencyService.cleanupExpiredTokens();

        // Then
        verify(tokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }
}
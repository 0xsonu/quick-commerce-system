package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.exception.DuplicateOperationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private String tenantId;
    private String userId;
    private String idempotencyKey;
    private String redisKey;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        tenantId = "tenant123";
        userId = "user123";
        idempotencyKey = "idempotency123";
        redisKey = "idempotency:tenant123:user123:idempotency123";

        cartResponse = new CartResponse();
        cartResponse.setTenantId(tenantId);
        cartResponse.setUserId(userId);
        cartResponse.setTotal(new BigDecimal("59.98"));
    }

    @Test
    void checkIdempotency_NoKey_ReturnsEmpty() {
        // Act
        Optional<CartResponse> result = idempotencyService.checkIdempotency(tenantId, userId, null, CartResponse.class);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void checkIdempotency_EmptyKey_ReturnsEmpty() {
        // Act
        Optional<CartResponse> result = idempotencyService.checkIdempotency(tenantId, userId, "", CartResponse.class);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void checkIdempotency_NoExistingOperation_ReturnsEmpty() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // Act
        Optional<CartResponse> result = idempotencyService.checkIdempotency(tenantId, userId, idempotencyKey, CartResponse.class);

        // Assert
        assertTrue(result.isEmpty());
        verify(valueOperations).get(redisKey);
    }

    @Test
    void checkIdempotency_OperationInProgress_ThrowsException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn("PROCESSING");

        // Act & Assert
        DuplicateOperationException exception = assertThrows(
            DuplicateOperationException.class,
            () -> idempotencyService.checkIdempotency(tenantId, userId, idempotencyKey, CartResponse.class)
        );

        assertEquals("Operation with this idempotency key is already in progress", exception.getMessage());
        verify(valueOperations).get(redisKey);
    }

    @Test
    void checkIdempotency_ExistingResult_ReturnsResult() throws Exception {
        // Arrange
        String serializedResult = "{\"tenantId\":\"tenant123\",\"userId\":\"user123\",\"total\":59.98}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(serializedResult);
        when(objectMapper.readValue(serializedResult, CartResponse.class)).thenReturn(cartResponse);

        // Act
        Optional<CartResponse> result = idempotencyService.checkIdempotency(tenantId, userId, idempotencyKey, CartResponse.class);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(cartResponse, result.get());
        verify(valueOperations).get(redisKey);
        verify(objectMapper).readValue(serializedResult, CartResponse.class);
    }

    @Test
    void checkIdempotency_CorruptedData_ReturnsEmpty() throws Exception {
        // Arrange
        String corruptedData = "invalid json";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(corruptedData);
        when(objectMapper.readValue(eq(corruptedData), eq(CartResponse.class)))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("JSON parsing error") {});

        // Act
        Optional<CartResponse> result = idempotencyService.checkIdempotency(tenantId, userId, idempotencyKey, CartResponse.class);

        // Assert
        assertTrue(result.isEmpty());
        verify(valueOperations).get(redisKey);
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    void markAsProcessing_ValidKey_SetsProcessingState() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        idempotencyService.markAsProcessing(tenantId, userId, idempotencyKey);

        // Assert
        verify(valueOperations).set(redisKey, "PROCESSING", Duration.ofMinutes(5));
    }

    @Test
    void markAsProcessing_NullKey_DoesNothing() {
        // Act
        idempotencyService.markAsProcessing(tenantId, userId, null);

        // Assert
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void storeResult_ValidKey_StoresResult() throws Exception {
        // Arrange
        String serializedResult = "{\"tenantId\":\"tenant123\",\"userId\":\"user123\",\"total\":59.98}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(cartResponse)).thenReturn(serializedResult);

        // Act
        idempotencyService.storeResult(tenantId, userId, idempotencyKey, cartResponse);

        // Assert
        verify(objectMapper).writeValueAsString(cartResponse);
        verify(valueOperations).set(redisKey, serializedResult, Duration.ofHours(24));
    }

    @Test
    void storeResult_SerializationFails_DeletesKey() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(cartResponse))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Serialization error") {});

        // Act
        idempotencyService.storeResult(tenantId, userId, idempotencyKey, cartResponse);

        // Assert
        verify(objectMapper).writeValueAsString(cartResponse);
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    void removeIdempotencyKey_ValidKey_DeletesKey() {
        // Act
        idempotencyService.removeIdempotencyKey(tenantId, userId, idempotencyKey);

        // Assert
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    void removeIdempotencyKey_NullKey_DoesNothing() {
        // Act
        idempotencyService.removeIdempotencyKey(tenantId, userId, null);

        // Assert
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void exists_ValidKey_ReturnsTrue() {
        // Arrange
        when(redisTemplate.hasKey(redisKey)).thenReturn(true);

        // Act
        boolean result = idempotencyService.exists(tenantId, userId, idempotencyKey);

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey(redisKey);
    }

    @Test
    void exists_KeyNotFound_ReturnsFalse() {
        // Arrange
        when(redisTemplate.hasKey(redisKey)).thenReturn(false);

        // Act
        boolean result = idempotencyService.exists(tenantId, userId, idempotencyKey);

        // Assert
        assertFalse(result);
        verify(redisTemplate).hasKey(redisKey);
    }

    @Test
    void exists_NullKey_ReturnsFalse() {
        // Act
        boolean result = idempotencyService.exists(tenantId, userId, null);

        // Assert
        assertFalse(result);
        verifyNoInteractions(redisTemplate);
    }
}
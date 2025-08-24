package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.AddressDto;
import com.ecommerce.orderservice.dto.CreateOrderItemRequest;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.IdempotencyStatus;
import com.ecommerce.orderservice.entity.IdempotencyToken;
import com.ecommerce.orderservice.exception.DuplicateOrderException;
import com.ecommerce.orderservice.exception.IdempotencyException;
import com.ecommerce.orderservice.repository.IdempotencyTokenRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(MockitoExtension.class)
class OrderServiceIdempotencyTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private IdempotencyTokenRepository tokenRepository;

    @MockBean
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    private CreateOrderRequest createOrderRequest;
    private String tenantId = "tenant123";
    private Long userId = 1L;
    private String idempotencyToken = "test-token-123";

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

        // Mock event publisher to return completed future
        when(eventPublisher.publishOrderCreated(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    @Test
    void createOrderWithIdempotency_NewToken_ShouldCreateOrderAndToken() {
        // Given
        when(tokenRepository.findByTenantIdAndToken(tenantId, idempotencyToken)).thenReturn(Optional.empty());
        when(tokenRepository.findByTenantIdAndUserIdAndRequestHashAndStatus(
            eq(tenantId), eq(userId), anyString(), eq(IdempotencyStatus.COMPLETED)))
            .thenReturn(Optional.empty());
        when(tokenRepository.countActiveTokensForUserSince(eq(tenantId), eq(userId), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        
        IdempotencyToken savedToken = new IdempotencyToken(tenantId, idempotencyToken, userId, "hash123", LocalDateTime.now().plusHours(24));
        when(tokenRepository.save(any(IdempotencyToken.class))).thenReturn(savedToken);

        // Mock order save
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            var order = invocation.getArgument(0);
            // Set ID to simulate database save
            try {
                var idField = order.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(order, 123L);
            } catch (Exception e) {
                // Ignore reflection errors in test
            }
            return order;
        });

        // When
        OrderResponse result = orderService.createOrderWithIdempotency(createOrderRequest, idempotencyToken);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(tokenRepository).save(any(IdempotencyToken.class));
        verify(orderRepository).save(any());
        verify(tokenRepository).save(argThat(token -> 
            token.getStatus() == IdempotencyStatus.COMPLETED && token.getOrderId() != null));
    }

    @Test
    void createOrderWithIdempotency_ExistingCompletedToken_ShouldReturnCachedResult() throws Exception {
        // Given
        OrderResponse cachedResponse = new OrderResponse();
        cachedResponse.setId(456L);
        cachedResponse.setUserId(userId);
        
        IdempotencyToken existingToken = new IdempotencyToken(tenantId, idempotencyToken, userId, "hash123", LocalDateTime.now().plusHours(24));
        existingToken.markCompleted(456L, "{\"id\":456,\"userId\":1}");
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, idempotencyToken)).thenReturn(Optional.of(existingToken));

        // When
        OrderResponse result = orderService.createOrderWithIdempotency(createOrderRequest, idempotencyToken);

        // Then
        assertNotNull(result);
        assertEquals(456L, result.getId());
        assertEquals(userId, result.getUserId());
        verify(orderRepository, never()).save(any());
        verify(tokenRepository, never()).save(any(IdempotencyToken.class));
    }

    @Test
    void createOrderWithIdempotency_ProcessingToken_ShouldThrowException() {
        // Given
        IdempotencyToken processingToken = new IdempotencyToken(tenantId, idempotencyToken, userId, "hash123", LocalDateTime.now().plusHours(24));
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, idempotencyToken)).thenReturn(Optional.of(processingToken));

        // When & Then
        assertThrows(IdempotencyException.class, () -> 
            orderService.createOrderWithIdempotency(createOrderRequest, idempotencyToken));
        
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderWithIdempotency_DuplicateRequest_ShouldThrowDuplicateException() {
        // Given
        IdempotencyToken duplicateToken = new IdempotencyToken(tenantId, "different-token", userId, "hash123", LocalDateTime.now().plusHours(24));
        duplicateToken.markCompleted(789L, "{\"id\":789}");
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, idempotencyToken)).thenReturn(Optional.empty());
        when(tokenRepository.findByTenantIdAndUserIdAndRequestHashAndStatus(
            eq(tenantId), eq(userId), anyString(), eq(IdempotencyStatus.COMPLETED)))
            .thenReturn(Optional.of(duplicateToken));
        when(tokenRepository.countActiveTokensForUserSince(eq(tenantId), eq(userId), any(LocalDateTime.class)))
            .thenReturn(0L);

        // When & Then
        DuplicateOrderException exception = assertThrows(DuplicateOrderException.class, () -> 
            orderService.createOrderWithIdempotency(createOrderRequest, idempotencyToken));
        
        assertEquals(789L, exception.getExistingOrderId());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderWithIdempotency_OrderCreationFails_ShouldMarkTokenAsFailed() {
        // Given
        when(tokenRepository.findByTenantIdAndToken(tenantId, idempotencyToken)).thenReturn(Optional.empty());
        when(tokenRepository.findByTenantIdAndUserIdAndRequestHashAndStatus(
            eq(tenantId), eq(userId), anyString(), eq(IdempotencyStatus.COMPLETED)))
            .thenReturn(Optional.empty());
        when(tokenRepository.countActiveTokensForUserSince(eq(tenantId), eq(userId), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        
        IdempotencyToken savedToken = new IdempotencyToken(tenantId, idempotencyToken, userId, "hash123", LocalDateTime.now().plusHours(24));
        when(tokenRepository.save(any(IdempotencyToken.class))).thenReturn(savedToken);
        
        // Mock order save to throw exception
        when(orderRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            orderService.createOrderWithIdempotency(createOrderRequest, idempotencyToken));
        
        verify(tokenRepository).save(argThat(token -> 
            token.getStatus() == IdempotencyStatus.FAILED && 
            token.getResponseData().contains("Database error")));
    }

    @Test
    void createOrder_WithoutIdempotency_ShouldCreateOrderNormally() {
        // Given
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        
        // Mock order save
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            var order = invocation.getArgument(0);
            // Set ID to simulate database save
            try {
                var idField = order.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(order, 123L);
            } catch (Exception e) {
                // Ignore reflection errors in test
            }
            return order;
        });

        // When
        OrderResponse result = orderService.createOrder(createOrderRequest);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(orderRepository).save(any());
        verify(tokenRepository, never()).save(any(IdempotencyToken.class));
    }

    @Test
    void createOrderWithIdempotency_RateLimitExceeded_ShouldThrowException() {
        // Given
        when(tokenRepository.findByTenantIdAndToken(tenantId, idempotencyToken)).thenReturn(Optional.empty());
        when(tokenRepository.countActiveTokensForUserSince(eq(tenantId), eq(userId), any(LocalDateTime.class)))
            .thenReturn(15L); // Exceeds limit

        // When & Then
        assertThrows(IdempotencyException.class, () -> 
            orderService.createOrderWithIdempotency(createOrderRequest, idempotencyToken));
        
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderWithIdempotency_ExpiredToken_ShouldThrowException() {
        // Given
        IdempotencyToken expiredToken = new IdempotencyToken(tenantId, idempotencyToken, userId, "hash123", LocalDateTime.now().minusHours(1));
        
        when(tokenRepository.findByTenantIdAndToken(tenantId, idempotencyToken)).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThrows(IdempotencyException.class, () -> 
            orderService.createOrderWithIdempotency(createOrderRequest, idempotencyToken));
        
        verify(orderRepository, never()).save(any());
    }
}
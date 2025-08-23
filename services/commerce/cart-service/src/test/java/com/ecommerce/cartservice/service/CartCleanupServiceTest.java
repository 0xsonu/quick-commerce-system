package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.entity.ShoppingCartBackup;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.redis.CartRedisRepository;
import com.ecommerce.cartservice.repository.ShoppingCartBackupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartCleanupServiceTest {

    @Mock
    private CartRedisRepository cartRedisRepository;

    @Mock
    private ShoppingCartBackupRepository cartBackupRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private CartCleanupService cleanupService;

    private String tenantId;
    private String userId;
    private ShoppingCartBackup expiredBackup;
    private Cart cart;

    @BeforeEach
    void setUp() {
        tenantId = "tenant123";
        userId = "user123";

        // Set test properties
        ReflectionTestUtils.setField(cleanupService, "expiredDays", 7);
        ReflectionTestUtils.setField(cleanupService, "batchSize", 100);

        expiredBackup = new ShoppingCartBackup();
        expiredBackup.setTenantId(tenantId);
        expiredBackup.setUserId(userId);
        expiredBackup.setUpdatedAt(LocalDateTime.now().minusDays(10));

        cart = new Cart(tenantId, userId);
        cart.setUpdatedAt(LocalDateTime.now().minusDays(2));
    }

    @Test
    void cleanupExpiredCarts_Success() {
        // Arrange
        List<ShoppingCartBackup> expiredBackups = Arrays.asList(expiredBackup);
        Set<String> cartKeys = new HashSet<>(Arrays.asList("cart:tenant123:user123"));
        Set<String> idempotencyKeys = new HashSet<>(Arrays.asList("idempotency:tenant123:user123:key1"));

        when(cartBackupRepository.findByUpdatedAtBefore(any(LocalDateTime.class)))
            .thenReturn(expiredBackups);
        when(redisTemplate.keys("cart:*")).thenReturn(cartKeys);
        when(redisTemplate.keys("idempotency:*")).thenReturn(idempotencyKeys);
        when(cartBackupRepository.existsByTenantIdAndUserId(tenantId, userId)).thenReturn(true);
        when(redisTemplate.getExpire("idempotency:tenant123:user123:key1")).thenReturn(-1L);

        // Act
        cleanupService.cleanupExpiredCarts();

        // Assert
        verify(cartRedisRepository).deleteByTenantIdAndUserId(tenantId, userId);
        verify(cartBackupRepository).delete(expiredBackup);
        verify(redisTemplate).delete("idempotency:tenant123:user123:key1");
    }

    @Test
    void cleanupCartsForTenant_Success() {
        // Arrange
        List<ShoppingCartBackup> expiredBackups = Arrays.asList(expiredBackup);
        when(cartBackupRepository.findByTenantIdAndUpdatedAtBefore(eq(tenantId), any(LocalDateTime.class)))
            .thenReturn(expiredBackups);

        // Act
        cleanupService.cleanupCartsForTenant(tenantId);

        // Assert
        verify(cartRedisRepository).deleteByTenantIdAndUserId(tenantId, userId);
        verify(cartBackupRepository).delete(expiredBackup);
    }

    @Test
    void syncRedisCartsToBackup_Success() {
        // Arrange
        Set<String> cartKeys = new HashSet<>(Arrays.asList("cart:tenant123:user123"));
        when(redisTemplate.keys("cart:*")).thenReturn(cartKeys);
        when(cartRedisRepository.findByTenantIdAndUserId(tenantId, userId))
            .thenReturn(Optional.of(cart));
        when(cartBackupRepository.findByTenantIdAndUserId(tenantId, userId))
            .thenReturn(Optional.empty());

        // Act
        cleanupService.syncRedisCartsToBackup();

        // Assert
        verify(cartRedisRepository).findByTenantIdAndUserId(tenantId, userId);
        verify(cartBackupRepository).save(any(ShoppingCartBackup.class));
    }

    @Test
    void syncRedisCartsToBackup_EmptyCart_SkipsSync() {
        // Arrange
        Set<String> cartKeys = new HashSet<>(Arrays.asList("cart:tenant123:user123"));
        Cart emptyCart = new Cart(tenantId, userId);
        
        when(redisTemplate.keys("cart:*")).thenReturn(cartKeys);
        when(cartRedisRepository.findByTenantIdAndUserId(tenantId, userId))
            .thenReturn(Optional.of(emptyCart));

        // Act
        cleanupService.syncRedisCartsToBackup();

        // Assert
        verify(cartRedisRepository).findByTenantIdAndUserId(tenantId, userId);
        verifyNoInteractions(cartBackupRepository);
    }

    @Test
    void syncRedisCartsToBackup_NoRedisKeys_DoesNothing() {
        // Arrange
        when(redisTemplate.keys("cart:*")).thenReturn(null);

        // Act
        cleanupService.syncRedisCartsToBackup();

        // Assert
        verify(redisTemplate).keys("cart:*");
        verifyNoInteractions(cartRedisRepository);
        verifyNoInteractions(cartBackupRepository);
    }

    @Test
    void syncRedisCartsToBackup_UpdateExistingBackup() {
        // Arrange
        Set<String> cartKeys = new HashSet<>(Arrays.asList("cart:tenant123:user123"));
        ShoppingCartBackup existingBackup = new ShoppingCartBackup();
        existingBackup.setTenantId(tenantId);
        existingBackup.setUserId(userId);

        when(redisTemplate.keys("cart:*")).thenReturn(cartKeys);
        when(cartRedisRepository.findByTenantIdAndUserId(tenantId, userId))
            .thenReturn(Optional.of(cart));
        when(cartBackupRepository.findByTenantIdAndUserId(tenantId, userId))
            .thenReturn(Optional.of(existingBackup));

        // Act
        cleanupService.syncRedisCartsToBackup();

        // Assert
        verify(cartRedisRepository).findByTenantIdAndUserId(tenantId, userId);
        verify(cartBackupRepository).save(existingBackup);
    }

    @Test
    void cleanupExpiredCarts_HandlesException() {
        // Arrange
        when(cartBackupRepository.findByUpdatedAtBefore(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act - should not throw exception
        cleanupService.cleanupExpiredCarts();

        // Assert
        verify(cartBackupRepository).findByUpdatedAtBefore(any(LocalDateTime.class));
        // Verify that the method completes despite the exception
    }

    @Test
    void cleanupCartsForTenant_HandlesException() {
        // Arrange
        when(cartBackupRepository.findByTenantIdAndUpdatedAtBefore(eq(tenantId), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act - should not throw exception
        cleanupService.cleanupCartsForTenant(tenantId);

        // Assert
        verify(cartBackupRepository).findByTenantIdAndUpdatedAtBefore(eq(tenantId), any(LocalDateTime.class));
        // Verify that the method completes despite the exception
    }

    @Test
    void syncRedisCartsToBackup_HandlesInvalidKey() {
        // Arrange
        Set<String> cartKeys = new HashSet<>(Arrays.asList("invalid:key", "cart:tenant123:user123"));
        
        when(redisTemplate.keys("cart:*")).thenReturn(cartKeys);
        when(cartRedisRepository.findByTenantIdAndUserId(tenantId, userId))
            .thenReturn(Optional.of(cart));
        when(cartBackupRepository.findByTenantIdAndUserId(tenantId, userId))
            .thenReturn(Optional.empty());

        // Act
        cleanupService.syncRedisCartsToBackup();

        // Assert
        verify(cartRedisRepository).findByTenantIdAndUserId(tenantId, userId);
        verify(cartBackupRepository).save(any(ShoppingCartBackup.class));
        // Invalid key should be skipped without causing issues
    }
}
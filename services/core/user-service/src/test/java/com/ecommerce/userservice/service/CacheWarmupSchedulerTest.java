package com.ecommerce.userservice.service;

import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheWarmupSchedulerTest {

    @Mock
    private UserCacheService userCacheService;

    @Mock
    private UserRepository userRepository;

    private CacheWarmupScheduler cacheWarmupScheduler;

    @BeforeEach
    void setUp() {
        cacheWarmupScheduler = new CacheWarmupScheduler(userCacheService, userRepository);
    }

    @Test
    void testWarmUpCacheOnStartup() throws Exception {
        // Arrange
        List<String> tenantIds = Arrays.asList("tenant1", "tenant2", "tenant3");
        when(userRepository.findDistinctTenantIds()).thenReturn(tenantIds);
        when(userCacheService.warmUpUserCache(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = cacheWarmupScheduler.warmUpCacheOnStartup();
        result.get(); // Wait for completion

        // Assert
        verify(userRepository).findDistinctTenantIds();
        verify(userCacheService, times(3)).warmUpUserCache(anyString());
        verify(userCacheService).warmUpUserCache("tenant1");
        verify(userCacheService).warmUpUserCache("tenant2");
        verify(userCacheService).warmUpUserCache("tenant3");
    }

    @Test
    void testScheduledCacheRefresh() throws Exception {
        // Arrange
        List<String> tenantIds = Arrays.asList("tenant1", "tenant2");
        when(userRepository.findDistinctTenantIds()).thenReturn(tenantIds);
        when(userRepository.countByTenantId("tenant1")).thenReturn(10L);
        when(userRepository.countByTenantId("tenant2")).thenReturn(0L); // No users, should skip
        when(userCacheService.warmUpUserCache(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        cacheWarmupScheduler.scheduledCacheRefresh();
        // Wait a bit for async execution
        Thread.sleep(100);

        // Assert
        verify(userRepository).findDistinctTenantIds();
        verify(userRepository).countByTenantId("tenant1");
        verify(userRepository).countByTenantId("tenant2");
        verify(userCacheService, times(1)).warmUpUserCache("tenant1"); // Only tenant1 has users
        verify(userCacheService, never()).warmUpUserCache("tenant2"); // tenant2 has no users
    }

    @Test
    void testCacheHealthCheck() {
        // Arrange
        when(userCacheService.isCacheHealthy()).thenReturn(true);

        // Act
        cacheWarmupScheduler.cacheHealthCheck();

        // Assert
        verify(userCacheService).isCacheHealthy();
    }

    @Test
    void testCacheHealthCheck_Unhealthy() {
        // Arrange
        when(userCacheService.isCacheHealthy()).thenReturn(false);

        // Act
        cacheWarmupScheduler.cacheHealthCheck();

        // Assert
        verify(userCacheService).isCacheHealthy();
    }

    @Test
    void testLogCacheStatistics() {
        // Arrange
        UserCacheService.CacheStatistics stats = new UserCacheService.CacheStatistics(100L, 80L, 20L);
        when(userCacheService.getCacheStatistics()).thenReturn(stats);

        // Act
        cacheWarmupScheduler.logCacheStatistics();

        // Assert
        verify(userCacheService).getCacheStatistics();
    }

    @Test
    void testWarmUpCacheOnStartup_WithException() throws Exception {
        // Arrange
        List<String> tenantIds = Arrays.asList("tenant1", "tenant2");
        when(userRepository.findDistinctTenantIds()).thenReturn(tenantIds);
        when(userCacheService.warmUpUserCache("tenant1")).thenReturn(CompletableFuture.completedFuture(null));
        when(userCacheService.warmUpUserCache("tenant2")).thenThrow(new RuntimeException("Cache error"));

        // Act & Assert - Should not throw exception
        CompletableFuture<Void> result = cacheWarmupScheduler.warmUpCacheOnStartup();
        result.get(); // Wait for completion

        verify(userRepository).findDistinctTenantIds();
        verify(userCacheService).warmUpUserCache("tenant1");
        verify(userCacheService).warmUpUserCache("tenant2");
    }

    @Test
    void testScheduledCacheRefresh_WithException() throws Exception {
        // Arrange
        when(userRepository.findDistinctTenantIds()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert - Should not throw exception
        cacheWarmupScheduler.scheduledCacheRefresh();
        // Wait a bit for async execution
        Thread.sleep(100);

        verify(userRepository).findDistinctTenantIds();
    }

    @Test
    void testCacheHealthCheck_WithException() {
        // Arrange
        when(userCacheService.isCacheHealthy()).thenThrow(new RuntimeException("Cache error"));

        // Act & Assert - Should not throw exception
        cacheWarmupScheduler.cacheHealthCheck();

        verify(userCacheService).isCacheHealthy();
    }

    @Test
    void testLogCacheStatistics_WithException() {
        // Arrange
        when(userCacheService.getCacheStatistics()).thenThrow(new RuntimeException("Cache error"));

        // Act & Assert - Should not throw exception
        cacheWarmupScheduler.logCacheStatistics();

        verify(userCacheService).getCacheStatistics();
    }
}
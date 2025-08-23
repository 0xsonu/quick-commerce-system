package com.ecommerce.userservice.service;

import com.ecommerce.userservice.config.CacheConfig;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Cache cache;

    @Mock
    private Cache.ValueWrapper valueWrapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private UserCacheService userCacheService;

    private User testUser;
    private UserProfileResponse testUserProfile;
    private final String tenantId = "test-tenant";
    private final Long authUserId = 123L;
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        userCacheService = new UserCacheService(cacheManager, redisTemplate, userRepository);

        // Create test user
        testUser = new User(tenantId, authUserId, email);
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhone("+1234567890");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        testUserProfile = new UserProfileResponse(testUser);

        // Setup will be done in individual tests as needed
    }

    @Test
    void testGetUserProfileFromCache_CacheHit() {
        // Arrange
        String cacheKey = tenantId + ":user:" + authUserId;
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);
        when(cache.get(cacheKey)).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(testUserProfile);

        // Act
        Optional<UserProfileResponse> result = userCacheService.getUserProfileFromCache(tenantId, authUserId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUserProfile.getId(), result.get().getId());
        assertEquals(testUserProfile.getEmail(), result.get().getEmail());
        verify(cache).get(cacheKey);
    }

    @Test
    void testGetUserProfileFromCache_CacheMiss() {
        // Arrange
        String cacheKey = tenantId + ":user:" + authUserId;
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);
        when(cache.get(cacheKey)).thenReturn(null);

        // Act
        Optional<UserProfileResponse> result = userCacheService.getUserProfileFromCache(tenantId, authUserId);

        // Assert
        assertFalse(result.isPresent());
        verify(cache).get(cacheKey);
    }

    @Test
    void testPutUserProfileInCache() {
        // Arrange
        String cacheKey = tenantId + ":user:" + authUserId;
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);

        // Act
        userCacheService.putUserProfileInCache(tenantId, authUserId, testUserProfile);

        // Assert
        verify(cache).put(cacheKey, testUserProfile);
    }

    @Test
    void testGetUserProfileByEmailFromCache_CacheHit() {
        // Arrange
        String cacheKey = tenantId + ":email:" + email;
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);
        when(cache.get(cacheKey)).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(testUserProfile);

        // Act
        Optional<UserProfileResponse> result = userCacheService.getUserProfileByEmailFromCache(tenantId, email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUserProfile.getEmail(), result.get().getEmail());
        verify(cache).get(cacheKey);
    }

    @Test
    void testPutUserProfileByEmailInCache() {
        // Arrange
        String cacheKey = tenantId + ":email:" + email;
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);

        // Act
        userCacheService.putUserProfileByEmailInCache(tenantId, email, testUserProfile);

        // Assert
        verify(cache).put(cacheKey, testUserProfile);
    }

    @Test
    void testInvalidateUserProfileCache() {
        // Arrange
        String cacheKey = tenantId + ":user:" + authUserId;
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);

        // Act
        userCacheService.invalidateUserProfileCache(tenantId, authUserId);

        // Assert
        verify(cache).evict(cacheKey);
    }

    @Test
    void testInvalidateUserProfileCacheByEmail() {
        // Arrange
        String cacheKey = tenantId + ":email:" + email;
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);

        // Act
        userCacheService.invalidateUserProfileCacheByEmail(tenantId, email);

        // Assert
        verify(cache).evict(cacheKey);
    }

    @Test
    void testInvalidateAllUserCaches() {
        // Arrange
        Cache userProfileCache = mock(Cache.class);
        Cache userSearchCache = mock(Cache.class);
        Cache userCountCache = mock(Cache.class);

        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(userProfileCache);
        when(cacheManager.getCache(CacheConfig.USER_SEARCH_CACHE)).thenReturn(userSearchCache);
        when(cacheManager.getCache(CacheConfig.USER_COUNT_CACHE)).thenReturn(userCountCache);

        // Act
        userCacheService.invalidateAllUserCaches(tenantId);

        // Assert
        verify(userProfileCache).clear();
        verify(userSearchCache).clear();
        verify(userCountCache).clear();
    }

    @Test
    void testWarmUpUserCache() throws Exception {
        // Arrange
        User user2 = new User(tenantId, 456L, "user2@example.com");
        user2.setId(2L);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");

        List<User> users = Arrays.asList(testUser, user2);
        when(userRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(users);
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);

        // Act
        CompletableFuture<Void> result = userCacheService.warmUpUserCache(tenantId);
        result.get(); // Wait for completion

        // Assert
        verify(userRepository).findByTenantIdOrderByCreatedAtDesc(tenantId);
        verify(cache, times(4)).put(anyString(), any(UserProfileResponse.class)); // 2 users * 2 cache entries each
    }

    @Test
    void testWarmUpUserCacheForSpecificUser() throws Exception {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(Optional.of(testUser));
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenReturn(cache);

        // Act
        CompletableFuture<Void> result = userCacheService.warmUpUserCache(tenantId, authUserId);
        result.get(); // Wait for completion

        // Assert
        verify(userRepository).findByAuthUserIdAndTenantId(authUserId, tenantId);
        verify(cache, times(2)).put(anyString(), any(UserProfileResponse.class)); // 1 user * 2 cache entries
    }

    @Test
    void testIsCacheHealthy_Healthy() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("test");
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any());
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        boolean result = userCacheService.isCacheHealthy();

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsCacheHealthy_Unhealthy() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        boolean result = userCacheService.isCacheHealthy();

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetCacheStatistics() {
        // Arrange
        when(redisTemplate.keys(anyString())).thenReturn(null);
        when(redisTemplate.countExistingKeys(any())).thenReturn(5L);

        // Act
        UserCacheService.CacheStatistics stats = userCacheService.getCacheStatistics();

        // Assert
        assertNotNull(stats);
        assertEquals(5L, stats.getCacheSize());
    }

    @Test
    void testCacheErrorHandling() {
        // Arrange
        when(cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE)).thenThrow(new RuntimeException("Cache error"));

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            Optional<UserProfileResponse> result = userCacheService.getUserProfileFromCache(tenantId, authUserId);
            assertFalse(result.isPresent());
        });

        assertDoesNotThrow(() -> {
            userCacheService.putUserProfileInCache(tenantId, authUserId, testUserProfile);
        });

        assertDoesNotThrow(() -> {
            userCacheService.invalidateUserProfileCache(tenantId, authUserId);
        });
    }
}
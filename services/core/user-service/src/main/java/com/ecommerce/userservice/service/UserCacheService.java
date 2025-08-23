package com.ecommerce.userservice.service;

import com.ecommerce.userservice.config.CacheConfig;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing user profile caching with cache-aside pattern and warming strategies
 */
@Service
public class UserCacheService {

    private static final Logger logger = LoggerFactory.getLogger(UserCacheService.class);
    
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    @Autowired
    public UserCacheService(CacheManager cacheManager, 
                           RedisTemplate<String, Object> redisTemplate,
                           UserRepository userRepository) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
    }

    /**
     * Get user profile with cache-aside pattern
     */
    public Optional<UserProfileResponse> getUserProfileFromCache(String tenantId, Long authUserId) {
        String cacheKey = buildUserCacheKey(tenantId, authUserId);
        
        try {
            Cache cache = cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null) {
                    logger.debug("Cache hit for user profile: {}", cacheKey);
                    return Optional.of((UserProfileResponse) wrapper.get());
                }
            }
            
            logger.debug("Cache miss for user profile: {}", cacheKey);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error accessing cache for user profile: {}", cacheKey, e);
            return Optional.empty();
        }
    }

    /**
     * Put user profile in cache
     */
    public void putUserProfileInCache(String tenantId, Long authUserId, UserProfileResponse userProfile) {
        String cacheKey = buildUserCacheKey(tenantId, authUserId);
        
        try {
            Cache cache = cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE);
            if (cache != null) {
                cache.put(cacheKey, userProfile);
                logger.debug("Cached user profile: {}", cacheKey);
            }
        } catch (Exception e) {
            logger.warn("Error caching user profile: {}", cacheKey, e);
        }
    }

    /**
     * Get user profile by email with cache-aside pattern
     */
    public Optional<UserProfileResponse> getUserProfileByEmailFromCache(String tenantId, String email) {
        String cacheKey = buildUserEmailCacheKey(tenantId, email);
        
        try {
            Cache cache = cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null) {
                    logger.debug("Cache hit for user profile by email: {}", cacheKey);
                    return Optional.of((UserProfileResponse) wrapper.get());
                }
            }
            
            logger.debug("Cache miss for user profile by email: {}", cacheKey);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error accessing cache for user profile by email: {}", cacheKey, e);
            return Optional.empty();
        }
    }

    /**
     * Put user profile by email in cache
     */
    public void putUserProfileByEmailInCache(String tenantId, String email, UserProfileResponse userProfile) {
        String cacheKey = buildUserEmailCacheKey(tenantId, email);
        
        try {
            Cache cache = cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE);
            if (cache != null) {
                cache.put(cacheKey, userProfile);
                logger.debug("Cached user profile by email: {}", cacheKey);
            }
        } catch (Exception e) {
            logger.warn("Error caching user profile by email: {}", cacheKey, e);
        }
    }

    /**
     * Invalidate user profile cache
     */
    public void invalidateUserProfileCache(String tenantId, Long authUserId) {
        String cacheKey = buildUserCacheKey(tenantId, authUserId);
        
        try {
            Cache cache = cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE);
            if (cache != null) {
                cache.evict(cacheKey);
                logger.debug("Evicted user profile from cache: {}", cacheKey);
            }
        } catch (Exception e) {
            logger.warn("Error evicting user profile from cache: {}", cacheKey, e);
        }
    }

    /**
     * Invalidate user profile cache by email
     */
    public void invalidateUserProfileCacheByEmail(String tenantId, String email) {
        String cacheKey = buildUserEmailCacheKey(tenantId, email);
        
        try {
            Cache cache = cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE);
            if (cache != null) {
                cache.evict(cacheKey);
                logger.debug("Evicted user profile by email from cache: {}", cacheKey);
            }
        } catch (Exception e) {
            logger.warn("Error evicting user profile by email from cache: {}", cacheKey, e);
        }
    }

    /**
     * Invalidate all user-related caches for a tenant
     */
    public void invalidateAllUserCaches(String tenantId) {
        try {
            // Clear user profile cache
            Cache userProfileCache = cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE);
            if (userProfileCache != null) {
                userProfileCache.clear();
            }
            
            // Clear user search cache
            Cache userSearchCache = cacheManager.getCache(CacheConfig.USER_SEARCH_CACHE);
            if (userSearchCache != null) {
                userSearchCache.clear();
            }
            
            // Clear user count cache
            Cache userCountCache = cacheManager.getCache(CacheConfig.USER_COUNT_CACHE);
            if (userCountCache != null) {
                userCountCache.clear();
            }
            
            logger.info("Cleared all user caches for tenant: {}", tenantId);
        } catch (Exception e) {
            logger.warn("Error clearing user caches for tenant: {}", tenantId, e);
        }
    }

    /**
     * Warm up cache for frequently accessed users
     */
    @Async
    public CompletableFuture<Void> warmUpUserCache(String tenantId) {
        logger.info("Starting cache warm-up for tenant: {}", tenantId);
        
        try {
            // Get recently active users (last 100 users by creation date)
            List<User> recentUsers = userRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                    .stream()
                    .limit(100)
                    .toList();
            
            for (User user : recentUsers) {
                try {
                    UserProfileResponse userProfile = new UserProfileResponse(user);
                    putUserProfileInCache(tenantId, user.getAuthUserId(), userProfile);
                    putUserProfileByEmailInCache(tenantId, user.getEmail(), userProfile);
                    
                    // Small delay to avoid overwhelming the cache
                    Thread.sleep(10);
                } catch (Exception e) {
                    logger.warn("Error warming up cache for user: {}", user.getId(), e);
                }
            }
            
            logger.info("Completed cache warm-up for tenant: {} ({} users cached)", tenantId, recentUsers.size());
        } catch (Exception e) {
            logger.error("Error during cache warm-up for tenant: {}", tenantId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Warm up cache for specific user
     */
    @Async
    public CompletableFuture<Void> warmUpUserCache(String tenantId, Long authUserId) {
        logger.debug("Warming up cache for user: {} in tenant: {}", authUserId, tenantId);
        
        try {
            Optional<User> userOpt = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserProfileResponse userProfile = new UserProfileResponse(user);
                putUserProfileInCache(tenantId, authUserId, userProfile);
                putUserProfileByEmailInCache(tenantId, user.getEmail(), userProfile);
                logger.debug("Warmed up cache for user: {}", authUserId);
            }
        } catch (Exception e) {
            logger.warn("Error warming up cache for user: {}", authUserId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        try {
            // Get cache hit/miss statistics from Redis
            String userProfileCacheKey = "user-service:" + CacheConfig.USER_PROFILE_CACHE + ":*";
            Long userProfileCacheSize = redisTemplate.countExistingKeys(
                redisTemplate.keys(userProfileCacheKey)
            );
            
            return new CacheStatistics(
                userProfileCacheSize != null ? userProfileCacheSize : 0L,
                0L, // Hit count would need to be tracked separately
                0L  // Miss count would need to be tracked separately
            );
        } catch (Exception e) {
            logger.warn("Error getting cache statistics", e);
            return new CacheStatistics(0L, 0L, 0L);
        }
    }

    /**
     * Check if cache is healthy
     */
    public boolean isCacheHealthy() {
        try {
            String testKey = "health-check:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "test", 1, TimeUnit.SECONDS);
            String result = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            return "test".equals(result);
        } catch (Exception e) {
            logger.warn("Cache health check failed", e);
            return false;
        }
    }

    private String buildUserCacheKey(String tenantId, Long authUserId) {
        return tenantId + ":user:" + authUserId;
    }

    private String buildUserEmailCacheKey(String tenantId, String email) {
        return tenantId + ":email:" + email;
    }

    /**
     * Cache statistics data class
     */
    public static class CacheStatistics {
        private final Long cacheSize;
        private final Long hitCount;
        private final Long missCount;

        public CacheStatistics(Long cacheSize, Long hitCount, Long missCount) {
            this.cacheSize = cacheSize;
            this.hitCount = hitCount;
            this.missCount = missCount;
        }

        public Long getCacheSize() { return cacheSize; }
        public Long getHitCount() { return hitCount; }
        public Long getMissCount() { return missCount; }
        
        public double getHitRatio() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0.0;
        }
    }
}
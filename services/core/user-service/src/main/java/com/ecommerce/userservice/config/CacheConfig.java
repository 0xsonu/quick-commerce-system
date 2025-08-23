package com.ecommerce.userservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for user service with comprehensive caching strategy
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USER_CACHE = "users";
    public static final String USER_PROFILE_CACHE = "user-profiles";
    public static final String USER_SEARCH_CACHE = "user-search";
    public static final String USER_COUNT_CACHE = "user-count";

    /**
     * Configure Redis cache manager with different TTL for different cache types
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL of 30 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("user-service:");

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // User profile cache - longer TTL for frequently accessed data
        cacheConfigurations.put(USER_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofHours(2)));
        
        // User profile response cache - medium TTL
        cacheConfigurations.put(USER_PROFILE_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofMinutes(45)));
        
        // Search results cache - shorter TTL as data changes frequently
        cacheConfigurations.put(USER_SEARCH_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofMinutes(15)));
        
        // User count cache - very short TTL as it changes frequently
        cacheConfigurations.put(USER_COUNT_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Redis template for manual cache operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
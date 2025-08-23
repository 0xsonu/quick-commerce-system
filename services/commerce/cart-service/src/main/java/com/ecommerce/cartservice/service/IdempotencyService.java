package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.exception.DuplicateOperationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service for handling idempotency keys to prevent duplicate operations
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24); // 24 hours TTL

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public IdempotencyService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Check if operation with idempotency key already exists
     * If exists, return the cached result
     * If not, store the key and return empty
     */
    public <T> Optional<T> checkIdempotency(String tenantId, String userId, String idempotencyKey, Class<T> resultType) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty(); // No idempotency key provided
        }

        String key = buildIdempotencyKey(tenantId, userId, idempotencyKey);
        
        try {
            String cachedResult = redisTemplate.opsForValue().get(key);
            if (cachedResult != null) {
                logger.debug("Found existing operation for idempotency key: {}", idempotencyKey);
                
                if ("PROCESSING".equals(cachedResult)) {
                    // Operation is still in progress
                    throw new DuplicateOperationException("Operation with this idempotency key is already in progress");
                }
                
                // Return cached result
                T result = objectMapper.readValue(cachedResult, resultType);
                return Optional.of(result);
            }
            
            return Optional.empty();
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize cached result for idempotency key: {}", idempotencyKey, e);
            // Remove corrupted cache entry
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    /**
     * Mark operation as processing
     */
    public void markAsProcessing(String tenantId, String userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return; // No idempotency key provided
        }

        String key = buildIdempotencyKey(tenantId, userId, idempotencyKey);
        
        // Set with shorter TTL for processing state
        redisTemplate.opsForValue().set(key, "PROCESSING", Duration.ofMinutes(5));
        logger.debug("Marked operation as processing for idempotency key: {}", idempotencyKey);
    }

    /**
     * Store operation result with idempotency key
     */
    public <T> void storeResult(String tenantId, String userId, String idempotencyKey, T result) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return; // No idempotency key provided
        }

        String key = buildIdempotencyKey(tenantId, userId, idempotencyKey);
        
        try {
            String serializedResult = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, serializedResult, DEFAULT_TTL);
            logger.debug("Stored result for idempotency key: {}", idempotencyKey);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize result for idempotency key: {}", idempotencyKey, e);
            // Remove the processing marker if serialization fails
            redisTemplate.delete(key);
        }
    }

    /**
     * Remove idempotency key (in case of operation failure)
     */
    public void removeIdempotencyKey(String tenantId, String userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return; // No idempotency key provided
        }

        String key = buildIdempotencyKey(tenantId, userId, idempotencyKey);
        redisTemplate.delete(key);
        logger.debug("Removed idempotency key: {}", idempotencyKey);
    }

    /**
     * Check if idempotency key exists
     */
    public boolean exists(String tenantId, String userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false;
        }

        String key = buildIdempotencyKey(tenantId, userId, idempotencyKey);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Build Redis key for idempotency
     */
    private String buildIdempotencyKey(String tenantId, String userId, String idempotencyKey) {
        return IDEMPOTENCY_KEY_PREFIX + tenantId + ":" + userId + ":" + idempotencyKey;
    }
}
package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.IdempotencyToken;
import com.ecommerce.orderservice.entity.IdempotencyStatus;
import com.ecommerce.orderservice.exception.DuplicateOrderException;
import com.ecommerce.orderservice.exception.IdempotencyException;
import com.ecommerce.orderservice.repository.IdempotencyTokenRepository;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final int TOKEN_EXPIRY_HOURS = 24;
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    private final IdempotencyTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public IdempotencyService(IdempotencyTokenRepository tokenRepository, ObjectMapper objectMapper) {
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates idempotency token and checks for duplicate requests
     */
    @Transactional
    public IdempotencyValidationResult validateIdempotencyToken(String token, Long userId, CreateOrderRequest request) {
        String tenantId = TenantContext.getTenantId();
        
        logger.debug("Validating idempotency token: {} for user: {} in tenant: {}", token, userId, tenantId);

        // Check rate limiting
        checkRateLimit(tenantId, userId);

        // Generate request hash for duplicate detection
        String requestHash = generateRequestHash(request);

        // Check for existing token
        Optional<IdempotencyToken> existingToken = tokenRepository.findByTenantIdAndToken(tenantId, token);
        
        if (existingToken.isPresent()) {
            return handleExistingToken(existingToken.get(), userId, requestHash);
        }

        // Check for duplicate request with different token
        Optional<IdempotencyToken> duplicateRequest = tokenRepository.findByTenantIdAndUserIdAndRequestHashAndStatus(
            tenantId, userId, requestHash, IdempotencyStatus.COMPLETED
        );

        if (duplicateRequest.isPresent()) {
            logger.warn("Duplicate order request detected for user: {} with different token", userId);
            throw new DuplicateOrderException("Duplicate order request detected", duplicateRequest.get().getOrderId());
        }

        // Create new token
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);
        IdempotencyToken newToken = new IdempotencyToken(tenantId, token, userId, requestHash, expiresAt);
        IdempotencyToken savedToken = tokenRepository.save(newToken);

        logger.debug("Created new idempotency token: {} for user: {}", token, userId);
        return new IdempotencyValidationResult(savedToken, false, null);
    }

    /**
     * Marks an idempotency token as completed with the order result
     */
    @Transactional
    public void markTokenCompleted(String token, Long orderId, OrderResponse orderResponse) {
        String tenantId = TenantContext.getTenantId();
        
        Optional<IdempotencyToken> tokenOpt = tokenRepository.findByTenantIdAndToken(tenantId, token);
        
        if (tokenOpt.isPresent()) {
            IdempotencyToken idempotencyToken = tokenOpt.get();
            try {
                String responseData = objectMapper.writeValueAsString(orderResponse);
                idempotencyToken.markCompleted(orderId, responseData);
                tokenRepository.save(idempotencyToken);
                
                logger.debug("Marked idempotency token as completed: {} for order: {}", token, orderId);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize order response for token: {}", token, e);
                idempotencyToken.markFailed("Failed to serialize response");
                tokenRepository.save(idempotencyToken);
            }
        } else {
            logger.warn("Idempotency token not found when marking completed: {}", token);
        }
    }

    /**
     * Marks an idempotency token as failed
     */
    @Transactional
    public void markTokenFailed(String token, String errorMessage) {
        String tenantId = TenantContext.getTenantId();
        
        Optional<IdempotencyToken> tokenOpt = tokenRepository.findByTenantIdAndToken(tenantId, token);
        
        if (tokenOpt.isPresent()) {
            IdempotencyToken idempotencyToken = tokenOpt.get();
            idempotencyToken.markFailed(errorMessage);
            tokenRepository.save(idempotencyToken);
            
            logger.debug("Marked idempotency token as failed: {} with error: {}", token, errorMessage);
        } else {
            logger.warn("Idempotency token not found when marking failed: {}", token);
        }
    }

    /**
     * Cleanup expired idempotency tokens (scheduled task)
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredTokens() {
        logger.debug("Starting cleanup of expired idempotency tokens");
        
        int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        
        if (deletedCount > 0) {
            logger.info("Cleaned up {} expired idempotency tokens", deletedCount);
        }
    }

    private IdempotencyValidationResult handleExistingToken(IdempotencyToken token, Long userId, String requestHash) {
        // Validate token ownership
        if (!token.getUserId().equals(userId)) {
            logger.warn("Idempotency token ownership mismatch. Token user: {}, Request user: {}", 
                       token.getUserId(), userId);
            throw new IdempotencyException("Invalid idempotency token");
        }

        // Check if token is expired
        if (token.isExpired()) {
            logger.warn("Expired idempotency token used: {}", token.getToken());
            throw new IdempotencyException("Idempotency token has expired");
        }

        // Validate request hash matches (same request)
        if (!token.getRequestHash().equals(requestHash)) {
            logger.warn("Request hash mismatch for idempotency token: {}", token.getToken());
            throw new IdempotencyException("Request content does not match idempotency token");
        }

        // If token is completed, return the cached result
        if (token.isCompleted()) {
            logger.debug("Returning cached result for idempotency token: {}", token.getToken());
            try {
                OrderResponse cachedResponse = objectMapper.readValue(token.getResponseData(), OrderResponse.class);
                return new IdempotencyValidationResult(token, true, cachedResponse);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize cached response for token: {}", token.getToken(), e);
                throw new IdempotencyException("Failed to retrieve cached response");
            }
        }

        // If token is failed, throw exception
        if (token.isFailed()) {
            logger.warn("Failed idempotency token reused: {}", token.getToken());
            throw new IdempotencyException("Previous request with this token failed: " + token.getResponseData());
        }

        // Token is still processing - this could be a retry or concurrent request
        logger.debug("Idempotency token still processing: {}", token.getToken());
        throw new IdempotencyException("Request is still being processed");
    }

    private void checkRateLimit(String tenantId, Long userId) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long activeRequests = tokenRepository.countActiveTokensForUserSince(tenantId, userId, oneMinuteAgo);
        
        if (activeRequests >= MAX_REQUESTS_PER_MINUTE) {
            logger.warn("Rate limit exceeded for user: {} in tenant: {}. Active requests: {}", 
                       userId, tenantId, activeRequests);
            throw new IdempotencyException("Rate limit exceeded. Too many concurrent requests.");
        }
    }

    private String generateRequestHash(CreateOrderRequest request) {
        try {
            // Create a normalized representation of the request for hashing
            String requestJson = objectMapper.writeValueAsString(request);
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestJson.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            logger.error("Failed to generate request hash", e);
            throw new RuntimeException("Failed to generate request hash", e);
        }
    }

    /**
     * Result of idempotency validation
     */
    public static class IdempotencyValidationResult {
        private final IdempotencyToken token;
        private final boolean isReturnCachedResult;
        private final OrderResponse cachedResponse;

        public IdempotencyValidationResult(IdempotencyToken token, boolean isReturnCachedResult, OrderResponse cachedResponse) {
            this.token = token;
            this.isReturnCachedResult = isReturnCachedResult;
            this.cachedResponse = cachedResponse;
        }

        public IdempotencyToken getToken() {
            return token;
        }

        public boolean isReturnCachedResult() {
            return isReturnCachedResult;
        }

        public OrderResponse getCachedResponse() {
            return cachedResponse;
        }
    }
}
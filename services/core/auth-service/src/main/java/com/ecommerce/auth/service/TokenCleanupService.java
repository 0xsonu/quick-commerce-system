package com.ecommerce.auth.service;

import com.ecommerce.auth.config.TokenCleanupProperties;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service responsible for cleaning up expired and old revoked refresh tokens
 */
@Service
@ConditionalOnProperty(name = "auth.token.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class TokenCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenCleanupProperties cleanupProperties;

    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository, 
                              TokenCleanupProperties cleanupProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.cleanupProperties = cleanupProperties;
    }

    /**
     * Scheduled task to clean up expired and old revoked refresh tokens
     * Runs based on the configured cron expression (default: daily at 2 AM)
     */
    @Scheduled(cron = "${auth.token.cleanup.cron-expression:0 0 2 * * ?}")
    @Transactional
    public void cleanupTokens() {
        logger.info("Starting scheduled refresh token cleanup job");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Clean up expired tokens
            int expiredTokensDeleted = cleanupExpiredTokens(now);
            
            // Clean up old revoked tokens
            int revokedTokensDeleted = cleanupOldRevokedTokens(now);
            
            logger.info("Token cleanup completed successfully. Deleted {} expired tokens and {} old revoked tokens", 
                       expiredTokensDeleted, revokedTokensDeleted);
                       
        } catch (Exception e) {
            logger.error("Error occurred during token cleanup", e);
            throw e; // Re-throw to ensure proper error handling and monitoring
        }
    }

    /**
     * Clean up expired refresh tokens
     * 
     * @param now Current timestamp
     * @return Number of expired tokens deleted
     */
    @Transactional
    public int cleanupExpiredTokens(LocalDateTime now) {
        logger.debug("Cleaning up expired refresh tokens before: {}", now);
        
        try {
            refreshTokenRepository.deleteExpiredTokens(now);
            
            // Since JPA doesn't return count from bulk delete, we'll log without count
            // In a production system, you might want to count first or use native queries
            logger.debug("Successfully deleted expired refresh tokens");
            
            // Return 0 as we can't get the actual count from JPA bulk delete
            // This could be improved by counting first or using native queries
            return 0;
            
        } catch (Exception e) {
            logger.error("Failed to delete expired refresh tokens", e);
            throw e;
        }
    }

    /**
     * Clean up old revoked refresh tokens (older than retention period)
     * 
     * @param now Current timestamp
     * @return Number of old revoked tokens deleted
     */
    @Transactional
    public int cleanupOldRevokedTokens(LocalDateTime now) {
        LocalDateTime cutoffDate = now.minus(cleanupProperties.getRevokedTokenRetentionPeriod());
        logger.debug("Cleaning up revoked refresh tokens older than: {}", cutoffDate);
        
        try {
            refreshTokenRepository.deleteRevokedTokensOlderThan(cutoffDate);
            
            logger.debug("Successfully deleted old revoked refresh tokens");
            
            // Return 0 as we can't get the actual count from JPA bulk delete
            return 0;
            
        } catch (Exception e) {
            logger.error("Failed to delete old revoked refresh tokens", e);
            throw e;
        }
    }

    /**
     * Manual cleanup method for testing or administrative purposes
     */
    @Transactional
    public void performManualCleanup() {
        logger.info("Performing manual token cleanup");
        cleanupTokens();
    }
}
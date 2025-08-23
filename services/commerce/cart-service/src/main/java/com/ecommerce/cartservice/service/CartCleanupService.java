package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.entity.ShoppingCartBackup;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.redis.CartRedisRepository;
import com.ecommerce.cartservice.repository.ShoppingCartBackupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service for cleaning up expired carts and maintaining cart data consistency
 */
@Service
public class CartCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(CartCleanupService.class);
    private static final String CART_KEY_PATTERN = "cart:*";
    private static final String IDEMPOTENCY_KEY_PATTERN = "idempotency:*";

    private final CartRedisRepository cartRedisRepository;
    private final ShoppingCartBackupRepository cartBackupRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${cart.cleanup.expired-days:7}")
    private int expiredDays;
    
    @Value("${cart.cleanup.batch-size:100}")
    private int batchSize;

    @Autowired
    public CartCleanupService(CartRedisRepository cartRedisRepository,
                             ShoppingCartBackupRepository cartBackupRepository,
                             RedisTemplate<String, String> redisTemplate) {
        this.cartRedisRepository = cartRedisRepository;
        this.cartBackupRepository = cartBackupRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Scheduled cleanup of expired carts - runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 ms
    @Transactional
    public void cleanupExpiredCarts() {
        logger.info("Starting cleanup of expired carts older than {} days", expiredDays);
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(expiredDays);
            
            // Clean up MySQL backup carts
            int deletedBackups = cleanupExpiredBackupCarts(cutoffDate);
            
            // Clean up orphaned Redis carts (carts in Redis but not in MySQL)
            int deletedRedisCarts = cleanupOrphanedRedisCarts();
            
            // Clean up expired idempotency keys
            int deletedIdempotencyKeys = cleanupExpiredIdempotencyKeys();
            
            logger.info("Cleanup completed: {} backup carts, {} Redis carts, {} idempotency keys deleted", 
                       deletedBackups, deletedRedisCarts, deletedIdempotencyKeys);
                       
        } catch (Exception e) {
            logger.error("Error during cart cleanup", e);
        }
    }

    /**
     * Manual cleanup trigger for specific tenant
     */
    @Transactional
    public void cleanupCartsForTenant(String tenantId) {
        logger.info("Starting manual cleanup for tenant: {}", tenantId);
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(expiredDays);
            
            // Clean up backup carts for tenant
            List<ShoppingCartBackup> expiredBackups = cartBackupRepository
                .findByTenantIdAndUpdatedAtBefore(tenantId, cutoffDate);
            
            for (ShoppingCartBackup backup : expiredBackups) {
                // Remove from Redis if exists
                cartRedisRepository.deleteByTenantIdAndUserId(backup.getTenantId(), backup.getUserId());
                // Remove from MySQL
                cartBackupRepository.delete(backup);
            }
            
            logger.info("Manual cleanup completed for tenant {}: {} carts deleted", tenantId, expiredBackups.size());
            
        } catch (Exception e) {
            logger.error("Error during manual cleanup for tenant: {}", tenantId, e);
        }
    }

    /**
     * Sync Redis carts to MySQL backup
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes = 1800000 ms
    public void syncRedisCartsToBackup() {
        logger.debug("Starting sync of Redis carts to MySQL backup");
        
        try {
            Set<String> cartKeys = redisTemplate.keys(CART_KEY_PATTERN);
            if (cartKeys == null || cartKeys.isEmpty()) {
                logger.debug("No Redis carts found to sync");
                return;
            }
            
            int syncedCount = 0;
            for (String key : cartKeys) {
                try {
                    // Extract tenant and user from key format: cart:tenantId:userId
                    String[] keyParts = key.split(":");
                    if (keyParts.length >= 3) {
                        String tenantId = keyParts[1];
                        String userId = keyParts[2];
                        
                        // Get cart from Redis
                        Cart cart = cartRedisRepository.findByTenantIdAndUserId(tenantId, userId).orElse(null);
                        if (cart != null && !cart.getItems().isEmpty()) {
                            syncCartToBackup(cart);
                            syncedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to sync cart key: {}", key, e);
                }
            }
            
            logger.debug("Sync completed: {} carts synced to backup", syncedCount);
            
        } catch (Exception e) {
            logger.error("Error during Redis to MySQL sync", e);
        }
    }

    /**
     * Clean up expired backup carts from MySQL
     */
    private int cleanupExpiredBackupCarts(LocalDateTime cutoffDate) {
        List<ShoppingCartBackup> expiredBackups = cartBackupRepository.findByUpdatedAtBefore(cutoffDate);
        
        int deletedCount = 0;
        for (ShoppingCartBackup backup : expiredBackups) {
            try {
                // Remove from Redis if exists
                cartRedisRepository.deleteByTenantIdAndUserId(backup.getTenantId(), backup.getUserId());
                // Remove from MySQL
                cartBackupRepository.delete(backup);
                deletedCount++;
                
                if (deletedCount % batchSize == 0) {
                    logger.debug("Processed {} expired backup carts", deletedCount);
                }
                
            } catch (Exception e) {
                logger.warn("Failed to delete expired backup cart for tenant {} user {}", 
                           backup.getTenantId(), backup.getUserId(), e);
            }
        }
        
        return deletedCount;
    }

    /**
     * Clean up orphaned Redis carts (exist in Redis but not in MySQL)
     */
    private int cleanupOrphanedRedisCarts() {
        Set<String> cartKeys = redisTemplate.keys(CART_KEY_PATTERN);
        if (cartKeys == null || cartKeys.isEmpty()) {
            return 0;
        }
        
        int deletedCount = 0;
        for (String key : cartKeys) {
            try {
                // Extract tenant and user from key
                String[] keyParts = key.split(":");
                if (keyParts.length >= 3) {
                    String tenantId = keyParts[1];
                    String userId = keyParts[2];
                    
                    // Check if backup exists
                    if (!cartBackupRepository.existsByTenantIdAndUserId(tenantId, userId)) {
                        // Get cart to check if it's empty or very old
                        Cart cart = cartRedisRepository.findByTenantIdAndUserId(tenantId, userId).orElse(null);
                        if (cart != null) {
                            LocalDateTime cartAge = cart.getUpdatedAt();
                            LocalDateTime cutoff = LocalDateTime.now().minusDays(1); // 1 day for Redis-only carts
                            
                            if (cart.getItems().isEmpty() || (cartAge != null && cartAge.isBefore(cutoff))) {
                                cartRedisRepository.deleteByTenantIdAndUserId(tenantId, userId);
                                deletedCount++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to process Redis cart key: {}", key, e);
            }
        }
        
        return deletedCount;
    }

    /**
     * Clean up expired idempotency keys
     */
    private int cleanupExpiredIdempotencyKeys() {
        Set<String> idempotencyKeys = redisTemplate.keys(IDEMPOTENCY_KEY_PATTERN);
        if (idempotencyKeys == null || idempotencyKeys.isEmpty()) {
            return 0;
        }
        
        int deletedCount = 0;
        for (String key : idempotencyKeys) {
            try {
                // Check TTL - if no TTL set or very old, delete
                Long ttl = redisTemplate.getExpire(key);
                if (ttl != null && ttl <= 0) {
                    redisTemplate.delete(key);
                    deletedCount++;
                }
            } catch (Exception e) {
                logger.warn("Failed to process idempotency key: {}", key, e);
            }
        }
        
        return deletedCount;
    }

    /**
     * Sync individual cart to backup
     */
    private void syncCartToBackup(Cart cart) {
        try {
            ShoppingCartBackup existingBackup = cartBackupRepository
                .findByTenantIdAndUserId(cart.getTenantId(), cart.getUserId())
                .orElse(null);
            
            if (existingBackup != null) {
                existingBackup.setCartData(cart);
                cartBackupRepository.save(existingBackup);
            } else {
                ShoppingCartBackup newBackup = new ShoppingCartBackup(
                    cart.getTenantId(), cart.getUserId(), cart);
                cartBackupRepository.save(newBackup);
            }
        } catch (Exception e) {
            logger.warn("Failed to sync cart to backup for tenant {} user {}", 
                       cart.getTenantId(), cart.getUserId(), e);
        }
    }
}
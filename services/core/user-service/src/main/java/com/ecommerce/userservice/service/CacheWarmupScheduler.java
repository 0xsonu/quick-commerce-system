package com.ecommerce.userservice.service;

import com.ecommerce.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for scheduling cache warm-up operations
 */
@Service
public class CacheWarmupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupScheduler.class);

    private final UserCacheService userCacheService;
    private final UserRepository userRepository;

    @Autowired
    public CacheWarmupScheduler(UserCacheService userCacheService, UserRepository userRepository) {
        this.userCacheService = userCacheService;
        this.userRepository = userRepository;
    }

    /**
     * Warm up cache on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public CompletableFuture<Void> warmUpCacheOnStartup() {
        logger.info("Starting cache warm-up on application startup");
        
        try {
            // Get all distinct tenant IDs
            List<String> tenantIds = userRepository.findDistinctTenantIds();
            
            for (String tenantId : tenantIds) {
                try {
                    userCacheService.warmUpUserCache(tenantId).get();
                    logger.info("Completed cache warm-up for tenant: {}", tenantId);
                } catch (Exception e) {
                    logger.warn("Failed to warm up cache for tenant: {}", tenantId, e);
                }
            }
            
            logger.info("Completed cache warm-up on application startup for {} tenants", tenantIds.size());
        } catch (Exception e) {
            logger.error("Error during application startup cache warm-up", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Scheduled cache refresh - runs every 2 hours
     */
    @Scheduled(fixedRate = 7200000) // 2 hours in milliseconds
    @Async
    public void scheduledCacheRefresh() {
        logger.info("Starting scheduled cache refresh");
        
        try {
            // Get all distinct tenant IDs
            List<String> tenantIds = userRepository.findDistinctTenantIds();
            
            for (String tenantId : tenantIds) {
                try {
                    // Only warm up cache for active tenants (those with recent activity)
                    long userCount = userRepository.countByTenantId(tenantId);
                    if (userCount > 0) {
                        userCacheService.warmUpUserCache(tenantId).get();
                        logger.debug("Refreshed cache for tenant: {} ({} users)", tenantId, userCount);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to refresh cache for tenant: {}", tenantId, e);
                }
            }
            
            logger.info("Completed scheduled cache refresh for {} tenants", tenantIds.size());
        } catch (Exception e) {
            logger.error("Error during scheduled cache refresh", e);
        }
    }

    /**
     * Cache health check - runs every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void cacheHealthCheck() {
        try {
            boolean isHealthy = userCacheService.isCacheHealthy();
            if (!isHealthy) {
                logger.warn("Cache health check failed - cache may be unavailable");
            } else {
                logger.debug("Cache health check passed");
            }
        } catch (Exception e) {
            logger.error("Error during cache health check", e);
        }
    }

    /**
     * Cache statistics logging - runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void logCacheStatistics() {
        try {
            UserCacheService.CacheStatistics stats = userCacheService.getCacheStatistics();
            logger.info("Cache Statistics - Size: {}, Hit Count: {}, Miss Count: {}, Hit Ratio: {:.2f}%", 
                    stats.getCacheSize(), 
                    stats.getHitCount(), 
                    stats.getMissCount(), 
                    stats.getHitRatio() * 100);
        } catch (Exception e) {
            logger.warn("Error logging cache statistics", e);
        }
    }
}
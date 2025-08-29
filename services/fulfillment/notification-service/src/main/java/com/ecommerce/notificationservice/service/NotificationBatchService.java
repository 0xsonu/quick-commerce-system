package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for batching and throttling notifications to improve performance and prevent spam
 */
@Service
public class NotificationBatchService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationBatchService.class);

    private final NotificationService notificationService;
    
    // Configuration properties
    @Value("${notification.batch.size:50}")
    private int batchSize;
    
    @Value("${notification.batch.timeout.seconds:30}")
    private int batchTimeoutSeconds;
    
    @Value("${notification.throttle.email.per-minute:100}")
    private int emailThrottlePerMinute;
    
    @Value("${notification.throttle.sms.per-minute:20}")
    private int smsThrottlePerMinute;
    
    @Value("${notification.throttle.user.per-hour:10}")
    private int userThrottlePerHour;

    // Batching queues
    private final Queue<NotificationRequest> pendingNotifications = new ConcurrentLinkedQueue<>();
    private final Map<String, Queue<NotificationRequest>> channelQueues = new ConcurrentHashMap<>();
    
    // Throttling counters
    private final Map<String, AtomicInteger> channelCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> userCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> counterResetTimes = new ConcurrentHashMap<>();

    @Autowired
    public NotificationBatchService(NotificationService notificationService) {
        this.notificationService = notificationService;
        initializeChannelQueues();
        initializeThrottleCounters();
    }

    /**
     * Submit notifications for batched processing
     */
    @Async
    public void submitNotifications(List<NotificationRequest> requests) {
        logger.debug("Submitting {} notifications for batched processing", requests.size());
        
        for (NotificationRequest request : requests) {
            if (shouldThrottleNotification(request)) {
                logger.info("Throttling notification: userId={}, type={}, channel={}", 
                           request.getUserId(), request.getNotificationType(), request.getChannel());
                continue;
            }
            
            // Add to appropriate channel queue
            String channelKey = request.getChannel().name();
            channelQueues.computeIfAbsent(channelKey, k -> new ConcurrentLinkedQueue<>()).offer(request);
            
            // Also add to main queue for general processing
            pendingNotifications.offer(request);
        }
        
        logger.debug("Queued {} notifications across channels", requests.size());
    }

    /**
     * Submit single notification for batched processing
     */
    public void submitNotification(NotificationRequest request) {
        submitNotifications(List.of(request));
    }

    /**
     * Process batched notifications - runs every 10 seconds
     */
    @Scheduled(fixedDelay = 10000) // 10 seconds
    public void processBatchedNotifications() {
        if (pendingNotifications.isEmpty()) {
            return;
        }

        logger.debug("Processing batched notifications. Queue size: {}", pendingNotifications.size());
        
        // Process each channel separately
        for (NotificationChannel channel : NotificationChannel.values()) {
            processBatchForChannel(channel);
        }
    }

    /**
     * Process batch for specific channel
     */
    private void processBatchForChannel(NotificationChannel channel) {
        String channelKey = channel.name();
        Queue<NotificationRequest> channelQueue = channelQueues.get(channelKey);
        
        if (channelQueue == null || channelQueue.isEmpty()) {
            return;
        }

        List<NotificationRequest> batch = new ArrayList<>();
        int processed = 0;
        
        // Collect batch up to batch size or until queue is empty
        while (!channelQueue.isEmpty() && processed < batchSize) {
            NotificationRequest request = channelQueue.poll();
            if (request != null) {
                batch.add(request);
                processed++;
            }
        }

        if (!batch.isEmpty()) {
            logger.info("Processing batch of {} notifications for channel: {}", batch.size(), channel);
            processBatch(batch);
        }
    }

    /**
     * Process a batch of notifications
     */
    @Async
    public void processBatch(List<NotificationRequest> batch) {
        for (NotificationRequest request : batch) {
            try {
                // Remove from main queue
                pendingNotifications.remove(request);
                
                // Update throttle counters
                updateThrottleCounters(request);
                
                // Send notification
                notificationService.sendNotificationAsync(request);
                
            } catch (Exception e) {
                logger.error("Failed to process notification in batch: userId={}, type={}, channel={}", 
                           request.getUserId(), request.getNotificationType(), request.getChannel(), e);
            }
        }
    }

    /**
     * Check if notification should be throttled
     */
    private boolean shouldThrottleNotification(NotificationRequest request) {
        // Check channel-level throttling
        if (isChannelThrottled(request.getChannel())) {
            return true;
        }
        
        // Check user-level throttling
        if (isUserThrottled(request.getUserId(), request.getNotificationType())) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if channel is throttled
     */
    private boolean isChannelThrottled(NotificationChannel channel) {
        String channelKey = channel.name() + "_minute";
        AtomicInteger counter = channelCounters.get(channelKey);
        
        if (counter == null) {
            return false;
        }
        
        int limit = getChannelThrottleLimit(channel);
        return counter.get() >= limit;
    }

    /**
     * Check if user is throttled
     */
    private boolean isUserThrottled(Long userId, NotificationType notificationType) {
        String userKey = userId + "_" + notificationType.name() + "_hour";
        AtomicInteger counter = userCounters.get(userKey);
        
        if (counter == null) {
            return false;
        }
        
        return counter.get() >= userThrottlePerHour;
    }

    /**
     * Update throttle counters
     */
    private void updateThrottleCounters(NotificationRequest request) {
        // Update channel counter
        String channelKey = request.getChannel().name() + "_minute";
        channelCounters.computeIfAbsent(channelKey, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Update user counter
        String userKey = request.getUserId() + "_" + request.getNotificationType().name() + "_hour";
        userCounters.computeIfAbsent(userKey, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Get throttle limit for channel
     */
    private int getChannelThrottleLimit(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> emailThrottlePerMinute;
            case SMS -> smsThrottlePerMinute;
            case PUSH -> 200; // Default for push notifications
        };
    }

    /**
     * Reset throttle counters - runs every minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void resetMinuteCounters() {
        logger.debug("Resetting minute-based throttle counters");
        
        // Reset channel counters
        channelCounters.entrySet().removeIf(entry -> entry.getKey().endsWith("_minute"));
        
        // Update reset times
        counterResetTimes.put("minute", LocalDateTime.now());
    }

    /**
     * Reset hourly throttle counters - runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void resetHourlyCounters() {
        logger.debug("Resetting hourly throttle counters");
        
        // Reset user counters
        userCounters.entrySet().removeIf(entry -> entry.getKey().endsWith("_hour"));
        
        // Update reset times
        counterResetTimes.put("hour", LocalDateTime.now());
    }

    /**
     * Initialize channel queues
     */
    private void initializeChannelQueues() {
        for (NotificationChannel channel : NotificationChannel.values()) {
            channelQueues.put(channel.name(), new ConcurrentLinkedQueue<>());
        }
    }

    /**
     * Initialize throttle counters
     */
    private void initializeThrottleCounters() {
        counterResetTimes.put("minute", LocalDateTime.now());
        counterResetTimes.put("hour", LocalDateTime.now());
    }

    /**
     * Get queue statistics for monitoring
     */
    public Map<String, Object> getQueueStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPending", pendingNotifications.size());
        stats.put("batchSize", batchSize);
        stats.put("batchTimeoutSeconds", batchTimeoutSeconds);
        
        Map<String, Integer> channelStats = new HashMap<>();
        for (Map.Entry<String, Queue<NotificationRequest>> entry : channelQueues.entrySet()) {
            channelStats.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("channelQueues", channelStats);
        
        Map<String, Integer> throttleStats = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : channelCounters.entrySet()) {
            throttleStats.put(entry.getKey(), entry.getValue().get());
        }
        stats.put("throttleCounters", throttleStats);
        
        return stats;
    }

    /**
     * Force process all pending notifications (for testing or emergency)
     */
    public void forceProcessAll() {
        logger.warn("Force processing all pending notifications");
        
        List<NotificationRequest> allPending = new ArrayList<>();
        NotificationRequest request;
        while ((request = pendingNotifications.poll()) != null) {
            allPending.add(request);
        }
        
        // Clear channel queues
        channelQueues.values().forEach(Queue::clear);
        
        if (!allPending.isEmpty()) {
            processBatch(allPending);
        }
    }
}
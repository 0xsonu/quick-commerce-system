package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.service.NotificationDeliveryTrackingService;
import com.ecommerce.shared.utils.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications/delivery")
public class NotificationDeliveryController {

    private final NotificationDeliveryTrackingService deliveryTrackingService;

    @Autowired
    public NotificationDeliveryController(NotificationDeliveryTrackingService deliveryTrackingService) {
        this.deliveryTrackingService = deliveryTrackingService;
    }

    /**
     * Get delivery status for a specific notification
     */
    @GetMapping("/{notificationId}/status")
    public ResponseEntity<NotificationDeliveryTrackingService.NotificationDeliveryStatus> getDeliveryStatus(
            @PathVariable Long notificationId) {
        
        Optional<NotificationDeliveryTrackingService.NotificationDeliveryStatus> status = 
            deliveryTrackingService.getDeliveryStatus(notificationId);
        
        return status.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get notification delivery history for current user
     */
    @GetMapping("/history")
    public ResponseEntity<Page<NotificationDeliveryTrackingService.NotificationDeliveryStatus>> getDeliveryHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Long userId = Long.valueOf(TenantContext.getUserId());
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<NotificationDeliveryTrackingService.NotificationDeliveryStatus> history = 
            deliveryTrackingService.getUserNotificationHistory(userId, pageable);
        
        return ResponseEntity.ok(history);
    }

    /**
     * Get delivery statistics for the tenant
     */
    @GetMapping("/stats")
    public ResponseEntity<NotificationDeliveryTrackingService.NotificationDeliveryStats> getDeliveryStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        if (toDate == null) {
            toDate = LocalDateTime.now();
        }
        
        NotificationDeliveryTrackingService.NotificationDeliveryStats stats = 
            deliveryTrackingService.getDeliveryStats(fromDate, toDate);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get failed notifications that need attention
     */
    @GetMapping("/failed")
    public ResponseEntity<List<NotificationDeliveryTrackingService.NotificationDeliveryStatus>> getFailedNotifications(
            @RequestParam(defaultValue = "3") int maxRetries,
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        
        List<NotificationDeliveryTrackingService.NotificationDeliveryStatus> failedNotifications = 
            deliveryTrackingService.getFailedNotifications(maxRetries, since);
        
        return ResponseEntity.ok(failedNotifications);
    }

    /**
     * Update delivery status (webhook endpoint for external providers)
     */
    @PostMapping("/{notificationId}/status")
    public ResponseEntity<Void> updateDeliveryStatus(
            @PathVariable Long notificationId,
            @RequestParam NotificationStatus status,
            @RequestParam(required = false) String message) {
        
        deliveryTrackingService.updateDeliveryStatus(notificationId, status, message);
        
        return ResponseEntity.ok().build();
    }
}
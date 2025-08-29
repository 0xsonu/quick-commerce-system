package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.service.NotificationService;
import com.ecommerce.shared.utils.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Send a notification
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        NotificationResponse response = notificationService.sendNotification(request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Notification processed"));
    }

    /**
     * Send a notification asynchronously
     */
    @PostMapping("/send-async")
    public ResponseEntity<ApiResponse<String>> sendNotificationAsync(
            @Valid @RequestBody NotificationRequest request) {
        
        notificationService.sendNotificationAsync(request);
        
        return ResponseEntity.ok(ApiResponse.success("Notification queued for processing", 
                                                    "Notification queued successfully"));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK", "Notification service is healthy"));
    }
}
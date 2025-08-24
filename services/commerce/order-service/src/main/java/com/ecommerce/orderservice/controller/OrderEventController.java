package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.service.OrderEventReplayService;
import com.ecommerce.shared.utils.response.ApiResponse;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for order event operations (replay, recovery)
 */
@RestController
@RequestMapping("/api/v1/orders/events")

public class OrderEventController {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventController.class);

    private final OrderEventReplayService replayService;

    @Autowired
    public OrderEventController(OrderEventReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/{orderId}/replay")
    public ResponseEntity<ApiResponse<String>> replayOrderEvents(
            @PathVariable @NotNull Long orderId) {
        
        logger.info("Received request to replay events for order: {}", orderId);

        try {
            CompletableFuture<Void> future = replayService.replayOrderEvents(orderId);
            
            // Don't wait for completion, return immediately
            return ResponseEntity.ok(ApiResponse.success(
                "Event replay initiated for order: " + orderId, 
                "Event replay started successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to initiate event replay for order: {}", orderId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to initiate event replay: " + e.getMessage()));
        }
    }

    @PostMapping("/replay/date-range")
    public ResponseEntity<ApiResponse<String>> replayOrderEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("Received request to replay events for orders between {} and {}", startDate, endDate);

        try {
            CompletableFuture<Void> future = replayService.replayOrderEventsByDateRange(startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Batch event replay initiated for date range: " + startDate + " to " + endDate, 
                "Batch event replay started successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to initiate batch event replay for date range {} to {}", 
                        startDate, endDate, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to initiate batch event replay: " + e.getMessage()));
        }
    }

    @PostMapping("/replay/status/{status}")
    public ResponseEntity<ApiResponse<String>> replayOrderEventsByStatus(
            @PathVariable OrderStatus status) {
        
        logger.info("Received request to replay events for orders with status: {}", status);

        try {
            CompletableFuture<Void> future = replayService.replayOrderEventsByStatus(status);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Event replay initiated for orders with status: " + status, 
                "Status-based event replay started successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to initiate event replay for status: {}", status, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to initiate event replay: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/recover")
    public ResponseEntity<ApiResponse<String>> recoverMissingEvents(
            @PathVariable @NotNull Long orderId) {
        
        logger.info("Received request to recover missing events for order: {}", orderId);

        try {
            CompletableFuture<Void> future = replayService.recoverMissingEvents(orderId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Event recovery initiated for order: " + orderId, 
                "Event recovery started successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to initiate event recovery for order: {}", orderId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to initiate event recovery: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateEventConsistency(
            @PathVariable @NotNull Long orderId) {
        
        logger.info("Received request to validate event consistency for order: {}", orderId);

        try {
            boolean isValid = replayService.validateEventConsistency(orderId);
            
            return ResponseEntity.ok(ApiResponse.success(
                isValid, 
                isValid ? "Event consistency validation passed" : "Event consistency validation failed"
            ));
        } catch (Exception e) {
            logger.error("Failed to validate event consistency for order: {}", orderId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to validate event consistency: " + e.getMessage()));
        }
    }
}
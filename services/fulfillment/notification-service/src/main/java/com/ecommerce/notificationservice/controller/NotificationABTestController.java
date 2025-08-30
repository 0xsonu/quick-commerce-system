package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationABTestRequest;
import com.ecommerce.notificationservice.dto.NotificationABTestResponse;
import com.ecommerce.notificationservice.service.NotificationABTestService;
import com.ecommerce.shared.utils.response.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/ab-tests")
public class NotificationABTestController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationABTestController.class);

    private final NotificationABTestService abTestService;

    @Autowired
    public NotificationABTestController(NotificationABTestService abTestService) {
        this.abTestService = abTestService;
    }

    /**
     * Create a new A/B test
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationABTestResponse>> createABTest(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody NotificationABTestRequest request) {
        
        logger.info("Creating A/B test '{}' for tenant: {}", request.getTestName(), tenantId);
        
        NotificationABTestResponse abTest = abTestService.createABTest(tenantId, request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(abTest));
    }

    /**
     * Get A/B test results
     */
    @GetMapping("/{testName}")
    public ResponseEntity<ApiResponse<NotificationABTestResponse>> getABTestResults(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String testName) {
        
        logger.info("Getting A/B test results for tenant: {}, test: {}", tenantId, testName);
        
        NotificationABTestResponse results = abTestService.getABTestResults(tenantId, testName);
        
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Stop an A/B test
     */
    @PostMapping("/{testName}/stop")
    public ResponseEntity<ApiResponse<NotificationABTestResponse>> stopABTest(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable String testName,
            @RequestParam(required = false) String stopReason) {
        
        logger.info("Stopping A/B test '{}' for tenant: {}", testName, tenantId);
        
        String reason = stopReason != null ? stopReason : "Manually stopped";
        NotificationABTestResponse abTest = abTestService.stopABTest(tenantId, testName, userId, reason);
        
        return ResponseEntity.ok(ApiResponse.success(abTest));
    }

    /**
     * Get all A/B tests for a tenant
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationABTestResponse>>> getAllABTests(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        
        logger.info("Getting {} A/B tests for tenant: {}", 
                   activeOnly ? "active" : "all", tenantId);
        
        List<NotificationABTestResponse> tests = activeOnly 
            ? abTestService.getActiveABTests(tenantId)
            : abTestService.getAllABTests(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(tests));
    }

    /**
     * Get active A/B tests for a tenant
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<NotificationABTestResponse>>> getActiveABTests(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        logger.info("Getting active A/B tests for tenant: {}", tenantId);
        
        List<NotificationABTestResponse> activeTests = abTestService.getActiveABTests(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(activeTests));
    }
}
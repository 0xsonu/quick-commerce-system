package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationTemplateVersionRequest;
import com.ecommerce.notificationservice.dto.NotificationTemplateVersionResponse;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.service.NotificationTemplateVersionService;
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
@RequestMapping("/api/v1/notifications/templates/versions")
public class NotificationTemplateVersionController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationTemplateVersionController.class);

    private final NotificationTemplateVersionService templateVersionService;

    @Autowired
    public NotificationTemplateVersionController(NotificationTemplateVersionService templateVersionService) {
        this.templateVersionService = templateVersionService;
    }

    /**
     * Create a new template version
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationTemplateVersionResponse>> createTemplateVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody NotificationTemplateVersionRequest request) {
        
        logger.info("Creating template version for tenant: {}, template: {}, channel: {}", 
                   tenantId, request.getTemplateKey(), request.getChannel());
        
        NotificationTemplateVersionResponse version = templateVersionService
            .createTemplateVersion(tenantId, request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(version));
    }

    /**
     * Get all versions for a template
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationTemplateVersionResponse>>> getTemplateVersions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String templateKey,
            @RequestParam NotificationChannel channel) {
        
        logger.info("Getting template versions for tenant: {}, template: {}, channel: {}", 
                   tenantId, templateKey, channel);
        
        List<NotificationTemplateVersionResponse> versions = templateVersionService
            .getTemplateVersions(tenantId, templateKey, channel);
        
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    /**
     * Get active template version
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<NotificationTemplateVersionResponse>> getActiveTemplateVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String templateKey,
            @RequestParam NotificationChannel channel) {
        
        logger.info("Getting active template version for tenant: {}, template: {}, channel: {}", 
                   tenantId, templateKey, channel);
        
        NotificationTemplateVersionResponse version = templateVersionService
            .getActiveTemplateVersion(tenantId, templateKey, channel);
        
        return ResponseEntity.ok(ApiResponse.success(version));
    }

    /**
     * Get a specific template version
     */
    @GetMapping("/{versionNumber}")
    public ResponseEntity<ApiResponse<NotificationTemplateVersionResponse>> getTemplateVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String templateKey,
            @RequestParam NotificationChannel channel,
            @PathVariable Integer versionNumber) {
        
        logger.info("Getting template version {} for tenant: {}, template: {}, channel: {}", 
                   versionNumber, tenantId, templateKey, channel);
        
        NotificationTemplateVersionResponse version = templateVersionService
            .getTemplateVersion(tenantId, templateKey, channel, versionNumber);
        
        return ResponseEntity.ok(ApiResponse.success(version));
    }

    /**
     * Publish a template version (make it active)
     */
    @PostMapping("/{versionNumber}/publish")
    public ResponseEntity<ApiResponse<NotificationTemplateVersionResponse>> publishTemplateVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestParam String templateKey,
            @RequestParam NotificationChannel channel,
            @PathVariable Integer versionNumber) {
        
        logger.info("Publishing template version {} for tenant: {}, template: {}, channel: {}", 
                   versionNumber, tenantId, templateKey, channel);
        
        NotificationTemplateVersionResponse version = templateVersionService
            .publishTemplateVersion(tenantId, templateKey, channel, versionNumber, userId);
        
        return ResponseEntity.ok(ApiResponse.success(version));
    }

    /**
     * Rollback to a previous template version
     */
    @PostMapping("/{versionNumber}/rollback")
    public ResponseEntity<ApiResponse<NotificationTemplateVersionResponse>> rollbackToVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestParam String templateKey,
            @RequestParam NotificationChannel channel,
            @PathVariable Integer versionNumber) {
        
        logger.info("Rolling back to template version {} for tenant: {}, template: {}, channel: {}", 
                   versionNumber, tenantId, templateKey, channel);
        
        NotificationTemplateVersionResponse version = templateVersionService
            .rollbackToVersion(tenantId, templateKey, channel, versionNumber, userId);
        
        return ResponseEntity.ok(ApiResponse.success(version));
    }

    /**
     * Get all published versions for a tenant
     */
    @GetMapping("/published")
    public ResponseEntity<ApiResponse<List<NotificationTemplateVersionResponse>>> getPublishedVersions(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        logger.info("Getting published template versions for tenant: {}", tenantId);
        
        List<NotificationTemplateVersionResponse> versions = templateVersionService
            .getPublishedVersions(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(versions));
    }
}
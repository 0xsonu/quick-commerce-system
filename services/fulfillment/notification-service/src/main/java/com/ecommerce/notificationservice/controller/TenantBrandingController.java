package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.TenantBrandingConfigRequest;
import com.ecommerce.notificationservice.dto.TenantBrandingConfigResponse;
import com.ecommerce.notificationservice.service.TenantBrandingService;
import com.ecommerce.shared.utils.response.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/branding")
public class TenantBrandingController {

    private static final Logger logger = LoggerFactory.getLogger(TenantBrandingController.class);

    private final TenantBrandingService tenantBrandingService;

    @Autowired
    public TenantBrandingController(TenantBrandingService tenantBrandingService) {
        this.tenantBrandingService = tenantBrandingService;
    }

    /**
     * Get tenant branding configuration
     */
    @GetMapping
    public ResponseEntity<ApiResponse<TenantBrandingConfigResponse>> getTenantBranding(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        logger.info("Getting branding configuration for tenant: {}", tenantId);
        
        TenantBrandingConfigResponse branding = tenantBrandingService.getTenantBranding(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(branding));
    }

    /**
     * Create or update tenant branding configuration
     */
    @PutMapping
    public ResponseEntity<ApiResponse<TenantBrandingConfigResponse>> createOrUpdateTenantBranding(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody TenantBrandingConfigRequest request) {
        
        logger.info("Creating/updating branding configuration for tenant: {}", tenantId);
        
        TenantBrandingConfigResponse branding = tenantBrandingService
            .createOrUpdateTenantBranding(tenantId, request);
        
        return ResponseEntity.ok(ApiResponse.success(branding));
    }

    /**
     * Deactivate tenant branding configuration
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deactivateTenantBranding(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        logger.info("Deactivating branding configuration for tenant: {}", tenantId);
        
        tenantBrandingService.deactivateTenantBranding(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
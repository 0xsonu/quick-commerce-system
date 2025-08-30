package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.TenantBrandingConfigRequest;
import com.ecommerce.notificationservice.dto.TenantBrandingConfigResponse;
import com.ecommerce.notificationservice.entity.TenantBrandingConfig;
import com.ecommerce.notificationservice.repository.TenantBrandingConfigRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TenantBrandingService {

    private static final Logger logger = LoggerFactory.getLogger(TenantBrandingService.class);

    private final TenantBrandingConfigRepository brandingConfigRepository;

    @Autowired
    public TenantBrandingService(TenantBrandingConfigRepository brandingConfigRepository) {
        this.brandingConfigRepository = brandingConfigRepository;
    }

    /**
     * Get tenant branding configuration
     */
    @Transactional(readOnly = true)
    public TenantBrandingConfigResponse getTenantBranding(String tenantId) {
        logger.debug("Getting branding configuration for tenant: {}", tenantId);

        Optional<TenantBrandingConfig> config = brandingConfigRepository.findActiveBrandingByTenantId(tenantId);
        
        if (config.isEmpty()) {
            // Return default branding if none exists
            return createDefaultBrandingResponse(tenantId);
        }

        return mapToResponse(config.get());
    }

    /**
     * Create or update tenant branding configuration
     */
    public TenantBrandingConfigResponse createOrUpdateTenantBranding(String tenantId, 
                                                                   TenantBrandingConfigRequest request) {
        logger.info("Creating/updating branding configuration for tenant: {}", tenantId);

        Optional<TenantBrandingConfig> existingConfig = brandingConfigRepository.findByTenantIdOptional(tenantId);
        
        TenantBrandingConfig config;
        if (existingConfig.isPresent()) {
            config = existingConfig.get();
            updateConfigFromRequest(config, request);
            logger.debug("Updated existing branding configuration for tenant: {}", tenantId);
        } else {
            config = createConfigFromRequest(tenantId, request);
            logger.debug("Created new branding configuration for tenant: {}", tenantId);
        }

        TenantBrandingConfig savedConfig = brandingConfigRepository.save(config);
        return mapToResponse(savedConfig);
    }

    /**
     * Get branding variables for template processing
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBrandingVariables(String tenantId) {
        logger.debug("Getting branding variables for tenant: {}", tenantId);

        Optional<TenantBrandingConfig> config = brandingConfigRepository.findActiveBrandingByTenantId(tenantId);
        
        Map<String, Object> variables = new HashMap<>();
        
        if (config.isPresent()) {
            TenantBrandingConfig branding = config.get();
            variables.put("brandName", branding.getBrandName());
            variables.put("logoUrl", branding.getLogoUrl());
            variables.put("primaryColor", branding.getPrimaryColor());
            variables.put("secondaryColor", branding.getSecondaryColor());
            variables.put("fontFamily", branding.getFontFamily());
            variables.put("websiteUrl", branding.getWebsiteUrl());
            variables.put("supportEmail", branding.getSupportEmail());
            variables.put("supportPhone", branding.getSupportPhone());
            variables.put("companyAddress", branding.getCompanyAddress());
            
            // Add custom fields
            if (branding.getCustomFields() != null) {
                branding.getCustomFields().forEach((key, value) -> 
                    variables.put("custom_" + key, value));
            }
        } else {
            // Default branding variables
            variables.put("brandName", "Your Store");
            variables.put("logoUrl", "");
            variables.put("primaryColor", "#007bff");
            variables.put("secondaryColor", "#6c757d");
            variables.put("fontFamily", "Arial, sans-serif");
            variables.put("websiteUrl", "");
            variables.put("supportEmail", "support@yourstore.com");
            variables.put("supportPhone", "");
            variables.put("companyAddress", "");
        }

        return variables;
    }

    /**
     * Deactivate tenant branding configuration
     */
    public void deactivateTenantBranding(String tenantId) {
        logger.info("Deactivating branding configuration for tenant: {}", tenantId);

        Optional<TenantBrandingConfig> config = brandingConfigRepository.findByTenantIdOptional(tenantId);
        if (config.isPresent()) {
            config.get().setIsActive(false);
            brandingConfigRepository.save(config.get());
            logger.debug("Deactivated branding configuration for tenant: {}", tenantId);
        } else {
            throw new ResourceNotFoundException("Branding configuration not found for tenant: " + tenantId);
        }
    }

    private TenantBrandingConfig createConfigFromRequest(String tenantId, TenantBrandingConfigRequest request) {
        TenantBrandingConfig config = new TenantBrandingConfig(tenantId, request.getBrandName());
        updateConfigFromRequest(config, request);
        return config;
    }

    private void updateConfigFromRequest(TenantBrandingConfig config, TenantBrandingConfigRequest request) {
        config.setBrandName(request.getBrandName());
        config.setLogoUrl(request.getLogoUrl());
        config.setPrimaryColor(request.getPrimaryColor());
        config.setSecondaryColor(request.getSecondaryColor());
        config.setFontFamily(request.getFontFamily());
        config.setWebsiteUrl(request.getWebsiteUrl());
        config.setSupportEmail(request.getSupportEmail());
        config.setSupportPhone(request.getSupportPhone());
        config.setCompanyAddress(request.getCompanyAddress());
        config.setCustomFields(request.getCustomFields());
        config.setIsActive(true);
    }

    private TenantBrandingConfigResponse mapToResponse(TenantBrandingConfig config) {
        TenantBrandingConfigResponse response = new TenantBrandingConfigResponse();
        response.setId(config.getId());
        response.setTenantId(config.getTenantId());
        response.setBrandName(config.getBrandName());
        response.setLogoUrl(config.getLogoUrl());
        response.setPrimaryColor(config.getPrimaryColor());
        response.setSecondaryColor(config.getSecondaryColor());
        response.setFontFamily(config.getFontFamily());
        response.setWebsiteUrl(config.getWebsiteUrl());
        response.setSupportEmail(config.getSupportEmail());
        response.setSupportPhone(config.getSupportPhone());
        response.setCompanyAddress(config.getCompanyAddress());
        response.setCustomFields(config.getCustomFields());
        response.setIsActive(config.getIsActive());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        return response;
    }

    private TenantBrandingConfigResponse createDefaultBrandingResponse(String tenantId) {
        TenantBrandingConfigResponse response = new TenantBrandingConfigResponse();
        response.setTenantId(tenantId);
        response.setBrandName("Your Store");
        response.setPrimaryColor("#007bff");
        response.setSecondaryColor("#6c757d");
        response.setFontFamily("Arial, sans-serif");
        response.setSupportEmail("support@yourstore.com");
        response.setIsActive(true);
        return response;
    }
}
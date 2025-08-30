package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.TenantBrandingConfigRequest;
import com.ecommerce.notificationservice.dto.TenantBrandingConfigResponse;
import com.ecommerce.notificationservice.entity.TenantBrandingConfig;
import com.ecommerce.notificationservice.repository.TenantBrandingConfigRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantBrandingServiceTest {

    @Mock
    private TenantBrandingConfigRepository brandingConfigRepository;

    @InjectMocks
    private TenantBrandingService tenantBrandingService;

    private String tenantId;
    private TenantBrandingConfig testConfig;
    private TenantBrandingConfigRequest testRequest;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        
        testConfig = new TenantBrandingConfig(tenantId, "Test Brand");
        testConfig.setId(1L);
        testConfig.setLogoUrl("https://example.com/logo.png");
        testConfig.setPrimaryColor("#007bff");
        testConfig.setSecondaryColor("#6c757d");
        testConfig.setFontFamily("Arial, sans-serif");
        testConfig.setWebsiteUrl("https://testbrand.com");
        testConfig.setSupportEmail("support@testbrand.com");
        testConfig.setSupportPhone("+1-555-0123");
        testConfig.setCompanyAddress("123 Test St, Test City, TC 12345");
        
        Map<String, String> customFields = new HashMap<>();
        customFields.put("slogan", "Your trusted partner");
        customFields.put("social_media", "@testbrand");
        testConfig.setCustomFields(customFields);
        
        testRequest = new TenantBrandingConfigRequest();
        testRequest.setBrandName("Test Brand");
        testRequest.setLogoUrl("https://example.com/logo.png");
        testRequest.setPrimaryColor("#007bff");
        testRequest.setSecondaryColor("#6c757d");
        testRequest.setFontFamily("Arial, sans-serif");
        testRequest.setWebsiteUrl("https://testbrand.com");
        testRequest.setSupportEmail("support@testbrand.com");
        testRequest.setSupportPhone("+1-555-0123");
        testRequest.setCompanyAddress("123 Test St, Test City, TC 12345");
        testRequest.setCustomFields(customFields);
    }

    @Test
    void getTenantBranding_WhenConfigExists_ShouldReturnConfig() {
        // Given
        when(brandingConfigRepository.findActiveBrandingByTenantId(tenantId))
            .thenReturn(Optional.of(testConfig));

        // When
        TenantBrandingConfigResponse response = tenantBrandingService.getTenantBranding(tenantId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getBrandName()).isEqualTo("Test Brand");
        assertThat(response.getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(response.getPrimaryColor()).isEqualTo("#007bff");
        assertThat(response.getCustomFields()).containsEntry("slogan", "Your trusted partner");
        
        verify(brandingConfigRepository).findActiveBrandingByTenantId(tenantId);
    }

    @Test
    void getTenantBranding_WhenConfigNotExists_ShouldReturnDefault() {
        // Given
        when(brandingConfigRepository.findActiveBrandingByTenantId(tenantId))
            .thenReturn(Optional.empty());

        // When
        TenantBrandingConfigResponse response = tenantBrandingService.getTenantBranding(tenantId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getBrandName()).isEqualTo("Your Store");
        assertThat(response.getPrimaryColor()).isEqualTo("#007bff");
        assertThat(response.getSupportEmail()).isEqualTo("support@yourstore.com");
        assertThat(response.getIsActive()).isTrue();
        
        verify(brandingConfigRepository).findActiveBrandingByTenantId(tenantId);
    }

    @Test
    void createOrUpdateTenantBranding_WhenNewConfig_ShouldCreateNew() {
        // Given
        when(brandingConfigRepository.findByTenantIdOptional(tenantId))
            .thenReturn(Optional.empty());
        when(brandingConfigRepository.save(any(TenantBrandingConfig.class)))
            .thenReturn(testConfig);

        // When
        TenantBrandingConfigResponse response = tenantBrandingService
            .createOrUpdateTenantBranding(tenantId, testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getBrandName()).isEqualTo("Test Brand");
        
        verify(brandingConfigRepository).findByTenantIdOptional(tenantId);
        verify(brandingConfigRepository).save(any(TenantBrandingConfig.class));
    }

    @Test
    void createOrUpdateTenantBranding_WhenExistingConfig_ShouldUpdate() {
        // Given
        when(brandingConfigRepository.findByTenantIdOptional(tenantId))
            .thenReturn(Optional.of(testConfig));
        when(brandingConfigRepository.save(any(TenantBrandingConfig.class)))
            .thenReturn(testConfig);

        // When
        TenantBrandingConfigResponse response = tenantBrandingService
            .createOrUpdateTenantBranding(tenantId, testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        
        verify(brandingConfigRepository).findByTenantIdOptional(tenantId);
        verify(brandingConfigRepository).save(testConfig);
    }

    @Test
    void getBrandingVariables_WhenConfigExists_ShouldReturnVariables() {
        // Given
        when(brandingConfigRepository.findActiveBrandingByTenantId(tenantId))
            .thenReturn(Optional.of(testConfig));

        // When
        Map<String, Object> variables = tenantBrandingService.getBrandingVariables(tenantId);

        // Then
        assertThat(variables).isNotEmpty();
        assertThat(variables.get("brandName")).isEqualTo("Test Brand");
        assertThat(variables.get("logoUrl")).isEqualTo("https://example.com/logo.png");
        assertThat(variables.get("primaryColor")).isEqualTo("#007bff");
        assertThat(variables.get("custom_slogan")).isEqualTo("Your trusted partner");
        assertThat(variables.get("custom_social_media")).isEqualTo("@testbrand");
        
        verify(brandingConfigRepository).findActiveBrandingByTenantId(tenantId);
    }

    @Test
    void getBrandingVariables_WhenConfigNotExists_ShouldReturnDefaults() {
        // Given
        when(brandingConfigRepository.findActiveBrandingByTenantId(tenantId))
            .thenReturn(Optional.empty());

        // When
        Map<String, Object> variables = tenantBrandingService.getBrandingVariables(tenantId);

        // Then
        assertThat(variables).isNotEmpty();
        assertThat(variables.get("brandName")).isEqualTo("Your Store");
        assertThat(variables.get("primaryColor")).isEqualTo("#007bff");
        assertThat(variables.get("supportEmail")).isEqualTo("support@yourstore.com");
        
        verify(brandingConfigRepository).findActiveBrandingByTenantId(tenantId);
    }

    @Test
    void deactivateTenantBranding_WhenConfigExists_ShouldDeactivate() {
        // Given
        when(brandingConfigRepository.findByTenantIdOptional(tenantId))
            .thenReturn(Optional.of(testConfig));
        when(brandingConfigRepository.save(any(TenantBrandingConfig.class)))
            .thenReturn(testConfig);

        // When
        tenantBrandingService.deactivateTenantBranding(tenantId);

        // Then
        verify(brandingConfigRepository).findByTenantIdOptional(tenantId);
        verify(brandingConfigRepository).save(testConfig);
        assertThat(testConfig.getIsActive()).isFalse();
    }

    @Test
    void deactivateTenantBranding_WhenConfigNotExists_ShouldThrowException() {
        // Given
        when(brandingConfigRepository.findByTenantIdOptional(tenantId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantBrandingService.deactivateTenantBranding(tenantId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Branding configuration not found for tenant: " + tenantId);
        
        verify(brandingConfigRepository).findByTenantIdOptional(tenantId);
        verify(brandingConfigRepository, never()).save(any());
    }
}
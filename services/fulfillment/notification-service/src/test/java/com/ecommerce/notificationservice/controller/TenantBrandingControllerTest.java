package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.TenantBrandingConfigRequest;
import com.ecommerce.notificationservice.dto.TenantBrandingConfigResponse;
import com.ecommerce.notificationservice.service.TenantBrandingService;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantBrandingController.class)
class TenantBrandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantBrandingService tenantBrandingService;

    @Autowired
    private ObjectMapper objectMapper;

    private String tenantId;
    private TenantBrandingConfigRequest testRequest;
    private TenantBrandingConfigResponse testResponse;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";

        Map<String, String> customFields = new HashMap<>();
        customFields.put("slogan", "Your trusted partner");
        customFields.put("social_media", "@testbrand");

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

        testResponse = new TenantBrandingConfigResponse();
        testResponse.setId(1L);
        testResponse.setTenantId(tenantId);
        testResponse.setBrandName("Test Brand");
        testResponse.setLogoUrl("https://example.com/logo.png");
        testResponse.setPrimaryColor("#007bff");
        testResponse.setSecondaryColor("#6c757d");
        testResponse.setFontFamily("Arial, sans-serif");
        testResponse.setWebsiteUrl("https://testbrand.com");
        testResponse.setSupportEmail("support@testbrand.com");
        testResponse.setSupportPhone("+1-555-0123");
        testResponse.setCompanyAddress("123 Test St, Test City, TC 12345");
        testResponse.setCustomFields(customFields);
        testResponse.setIsActive(true);
        testResponse.setCreatedAt(LocalDateTime.now());
        testResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getTenantBranding_ShouldReturnBrandingConfig() throws Exception {
        // Given
        when(tenantBrandingService.getTenantBranding(tenantId)).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.brandName").value("Test Brand"))
                .andExpect(jsonPath("$.data.logoUrl").value("https://example.com/logo.png"))
                .andExpect(jsonPath("$.data.primaryColor").value("#007bff"))
                .andExpect(jsonPath("$.data.customFields.slogan").value("Your trusted partner"));

        verify(tenantBrandingService).getTenantBranding(tenantId);
    }

    @Test
    void createOrUpdateTenantBranding_WithValidRequest_ShouldCreateBranding() throws Exception {
        // Given
        when(tenantBrandingService.createOrUpdateTenantBranding(eq(tenantId), any(TenantBrandingConfigRequest.class)))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.brandName").value("Test Brand"))
                .andExpect(jsonPath("$.data.isActive").value(true));

        verify(tenantBrandingService).createOrUpdateTenantBranding(eq(tenantId), any(TenantBrandingConfigRequest.class));
    }

    @Test
    void createOrUpdateTenantBranding_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        testRequest.setSupportEmail("invalid-email");

        // When & Then
        mockMvc.perform(put("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(tenantBrandingService, never()).createOrUpdateTenantBranding(any(), any());
    }

    @Test
    void createOrUpdateTenantBranding_WithInvalidColor_ShouldReturnBadRequest() throws Exception {
        // Given
        testRequest.setPrimaryColor("invalid-color");

        // When & Then
        mockMvc.perform(put("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(tenantBrandingService, never()).createOrUpdateTenantBranding(any(), any());
    }

    @Test
    void createOrUpdateTenantBranding_WithTooLongBrandName_ShouldReturnBadRequest() throws Exception {
        // Given
        testRequest.setBrandName("A".repeat(256)); // Exceeds 255 character limit

        // When & Then
        mockMvc.perform(put("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(tenantBrandingService, never()).createOrUpdateTenantBranding(any(), any());
    }

    @Test
    void deactivateTenantBranding_ShouldDeactivateBranding() throws Exception {
        // Given
        doNothing().when(tenantBrandingService).deactivateTenantBranding(tenantId);

        // When & Then
        mockMvc.perform(delete("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(tenantBrandingService).deactivateTenantBranding(tenantId);
    }

    @Test
    void deactivateTenantBranding_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Branding configuration not found"))
                .when(tenantBrandingService).deactivateTenantBranding(tenantId);

        // When & Then
        mockMvc.perform(delete("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound());

        verify(tenantBrandingService).deactivateTenantBranding(tenantId);
    }

    @Test
    void getTenantBranding_WithoutTenantHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/notifications/branding"))
                .andExpect(status().isBadRequest());

        verify(tenantBrandingService, never()).getTenantBranding(any());
    }

    @Test
    void createOrUpdateTenantBranding_WithoutTenantHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/notifications/branding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(tenantBrandingService, never()).createOrUpdateTenantBranding(any(), any());
    }

    @Test
    void createOrUpdateTenantBranding_WithEmptyBody_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk()); // Empty body is valid for branding config

        verify(tenantBrandingService).createOrUpdateTenantBranding(eq(tenantId), any());
    }
}
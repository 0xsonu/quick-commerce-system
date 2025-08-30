package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationTemplateVersionRequest;
import com.ecommerce.notificationservice.dto.NotificationTemplateVersionResponse;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.service.NotificationTemplateVersionService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationTemplateVersionController.class)
class NotificationTemplateVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationTemplateVersionService templateVersionService;

    @Autowired
    private ObjectMapper objectMapper;

    private String tenantId;
    private String userId;
    private String templateKey;
    private NotificationChannel channel;
    private NotificationTemplateVersionRequest testRequest;
    private NotificationTemplateVersionResponse testResponse;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        userId = "test-user";
        templateKey = "order_created";
        channel = NotificationChannel.EMAIL;

        Map<String, String> variables = new HashMap<>();
        variables.put("orderNumber", "Order number");
        variables.put("customerName", "Customer name");

        testRequest = new NotificationTemplateVersionRequest();
        testRequest.setTemplateKey(templateKey);
        testRequest.setChannel(channel);
        testRequest.setSubject("Order Confirmation - Order #[[${orderNumber}]]");
        testRequest.setContent("Dear [[${customerName}]], your order has been created.");
        testRequest.setChangeDescription("Updated order confirmation message");
        testRequest.setVariables(variables);

        testResponse = new NotificationTemplateVersionResponse();
        testResponse.setId(1L);
        testResponse.setTenantId(tenantId);
        testResponse.setTemplateKey(templateKey);
        testResponse.setChannel(channel);
        testResponse.setVersionNumber(1);
        testResponse.setSubject("Order Confirmation - Order #[[${orderNumber}]]");
        testResponse.setContent("Dear [[${customerName}]], your order has been created.");
        testResponse.setIsActive(false);
        testResponse.setIsPublished(false);
        testResponse.setChangeDescription("Updated order confirmation message");
        testResponse.setCreatedBy(userId);
        testResponse.setVariables(variables);
        testResponse.setCreatedAt(LocalDateTime.now());
        testResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createTemplateVersion_WithValidRequest_ShouldCreateVersion() throws Exception {
        // Given
        when(templateVersionService.createTemplateVersion(eq(tenantId), any(NotificationTemplateVersionRequest.class), eq(userId)))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/templates/versions")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.templateKey").value(templateKey))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.versionNumber").value(1))
                .andExpect(jsonPath("$.data.createdBy").value(userId));

        verify(templateVersionService).createTemplateVersion(eq(tenantId), any(NotificationTemplateVersionRequest.class), eq(userId));
    }

    @Test
    void createTemplateVersion_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        testRequest.setTemplateKey(""); // Invalid empty template key

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/templates/versions")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(templateVersionService, never()).createTemplateVersion(any(), any(), any());
    }

    @Test
    void getTemplateVersions_ShouldReturnVersionList() throws Exception {
        // Given
        NotificationTemplateVersionResponse version2 = new NotificationTemplateVersionResponse();
        version2.setId(2L);
        version2.setVersionNumber(2);
        version2.setTenantId(tenantId);
        version2.setTemplateKey(templateKey);
        version2.setChannel(channel);

        List<NotificationTemplateVersionResponse> versions = Arrays.asList(version2, testResponse);
        when(templateVersionService.getTemplateVersions(tenantId, templateKey, channel))
                .thenReturn(versions);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/templates/versions")
                .header("X-Tenant-ID", tenantId)
                .param("templateKey", templateKey)
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].versionNumber").value(2))
                .andExpect(jsonPath("$.data[1].versionNumber").value(1));

        verify(templateVersionService).getTemplateVersions(tenantId, templateKey, channel);
    }

    @Test
    void getActiveTemplateVersion_WhenExists_ShouldReturnActiveVersion() throws Exception {
        // Given
        testResponse.setIsActive(true);
        when(templateVersionService.getActiveTemplateVersion(tenantId, templateKey, channel))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/templates/versions/active")
                .header("X-Tenant-ID", tenantId)
                .param("templateKey", templateKey)
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.versionNumber").value(1));

        verify(templateVersionService).getActiveTemplateVersion(tenantId, templateKey, channel);
    }

    @Test
    void getActiveTemplateVersion_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        when(templateVersionService.getActiveTemplateVersion(tenantId, templateKey, channel))
                .thenThrow(new ResourceNotFoundException("No active template version found"));

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/templates/versions/active")
                .header("X-Tenant-ID", tenantId)
                .param("templateKey", templateKey)
                .param("channel", "EMAIL"))
                .andExpect(status().isNotFound());

        verify(templateVersionService).getActiveTemplateVersion(tenantId, templateKey, channel);
    }

    @Test
    void getTemplateVersion_WhenExists_ShouldReturnVersion() throws Exception {
        // Given
        Integer versionNumber = 1;
        when(templateVersionService.getTemplateVersion(tenantId, templateKey, channel, versionNumber))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/templates/versions/{versionNumber}", versionNumber)
                .header("X-Tenant-ID", tenantId)
                .param("templateKey", templateKey)
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.versionNumber").value(versionNumber));

        verify(templateVersionService).getTemplateVersion(tenantId, templateKey, channel, versionNumber);
    }

    @Test
    void publishTemplateVersion_ShouldPublishVersion() throws Exception {
        // Given
        Integer versionNumber = 1;
        testResponse.setIsActive(true);
        testResponse.setIsPublished(true);
        testResponse.setPublishedBy(userId);
        testResponse.setPublishedAt(LocalDateTime.now());

        when(templateVersionService.publishTemplateVersion(tenantId, templateKey, channel, versionNumber, userId))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/templates/versions/{versionNumber}/publish", versionNumber)
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .param("templateKey", templateKey)
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.isPublished").value(true))
                .andExpect(jsonPath("$.data.publishedBy").value(userId));

        verify(templateVersionService).publishTemplateVersion(tenantId, templateKey, channel, versionNumber, userId);
    }

    @Test
    void rollbackToVersion_ShouldRollbackVersion() throws Exception {
        // Given
        Integer versionNumber = 1;
        testResponse.setIsActive(true);
        testResponse.setPublishedBy(userId);

        when(templateVersionService.rollbackToVersion(tenantId, templateKey, channel, versionNumber, userId))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/templates/versions/{versionNumber}/rollback", versionNumber)
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .param("templateKey", templateKey)
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.publishedBy").value(userId));

        verify(templateVersionService).rollbackToVersion(tenantId, templateKey, channel, versionNumber, userId);
    }

    @Test
    void getPublishedVersions_ShouldReturnPublishedVersions() throws Exception {
        // Given
        testResponse.setIsPublished(true);
        testResponse.setPublishedAt(LocalDateTime.now());
        testResponse.setPublishedBy("publisher");

        List<NotificationTemplateVersionResponse> publishedVersions = Arrays.asList(testResponse);
        when(templateVersionService.getPublishedVersions(tenantId)).thenReturn(publishedVersions);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/templates/versions/published")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].isPublished").value(true))
                .andExpect(jsonPath("$.data[0].publishedBy").value("publisher"));

        verify(templateVersionService).getPublishedVersions(tenantId);
    }

    @Test
    void createTemplateVersion_WithoutTenantHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/notifications/templates/versions")
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(templateVersionService, never()).createTemplateVersion(any(), any(), any());
    }

    @Test
    void createTemplateVersion_WithoutUserHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/notifications/templates/versions")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(templateVersionService, never()).createTemplateVersion(any(), any(), any());
    }

    @Test
    void getTemplateVersions_WithoutRequiredParams_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/notifications/templates/versions")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isBadRequest());

        verify(templateVersionService, never()).getTemplateVersions(any(), any(), any());
    }
}
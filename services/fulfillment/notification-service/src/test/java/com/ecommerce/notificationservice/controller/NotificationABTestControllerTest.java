package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationABTestRequest;
import com.ecommerce.notificationservice.dto.NotificationABTestResponse;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.service.NotificationABTestService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationABTestController.class)
class NotificationABTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationABTestService abTestService;

    @Autowired
    private ObjectMapper objectMapper;

    private String tenantId;
    private String userId;
    private String testName;
    private NotificationABTestRequest testRequest;
    private NotificationABTestResponse testResponse;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        userId = "test-user";
        testName = "Email Subject Test";

        testRequest = new NotificationABTestRequest();
        testRequest.setTestName(testName);
        testRequest.setTemplateKey("order_created");
        testRequest.setChannel(NotificationChannel.EMAIL);
        testRequest.setControlVersionId(1L);
        testRequest.setVariantVersionId(2L);
        testRequest.setTrafficSplitPercentage(50);
        testRequest.setStartDate(LocalDateTime.now().plusHours(1));
        testRequest.setEndDate(LocalDateTime.now().plusDays(7));
        testRequest.setDescription("Testing different email subjects");
        testRequest.setSuccessMetric("OPEN_RATE");

        testResponse = new NotificationABTestResponse();
        testResponse.setId(1L);
        testResponse.setTenantId(tenantId);
        testResponse.setTestName(testName);
        testResponse.setTemplateKey("order_created");
        testResponse.setChannel(NotificationChannel.EMAIL);
        testResponse.setControlVersionId(1L);
        testResponse.setVariantVersionId(2L);
        testResponse.setTrafficSplitPercentage(50);
        testResponse.setStartDate(LocalDateTime.now().plusHours(1));
        testResponse.setEndDate(LocalDateTime.now().plusDays(7));
        testResponse.setIsActive(true);
        testResponse.setDescription("Testing different email subjects");
        testResponse.setSuccessMetric("OPEN_RATE");
        testResponse.setCreatedBy(userId);
        testResponse.setControlSentCount(0L);
        testResponse.setVariantSentCount(0L);
        testResponse.setControlSuccessCount(0L);
        testResponse.setVariantSuccessCount(0L);
        testResponse.setCreatedAt(LocalDateTime.now());
        testResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createABTest_WithValidRequest_ShouldCreateTest() throws Exception {
        // Given
        when(abTestService.createABTest(eq(tenantId), any(NotificationABTestRequest.class), eq(userId)))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.testName").value(testName))
                .andExpect(jsonPath("$.data.templateKey").value("order_created"))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.trafficSplitPercentage").value(50))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.createdBy").value(userId));

        verify(abTestService).createABTest(eq(tenantId), any(NotificationABTestRequest.class), eq(userId));
    }

    @Test
    void createABTest_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        testRequest.setTestName(""); // Invalid empty test name

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(abTestService, never()).createABTest(any(), any(), any());
    }

    @Test
    void createABTest_WithInvalidTrafficSplit_ShouldReturnBadRequest() throws Exception {
        // Given
        testRequest.setTrafficSplitPercentage(150); // Invalid percentage > 100

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(abTestService, never()).createABTest(any(), any(), any());
    }

    @Test
    void createABTest_WithPastStartDate_ShouldReturnBadRequest() throws Exception {
        // Given
        testRequest.setStartDate(LocalDateTime.now().minusHours(1)); // Past date

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(abTestService, never()).createABTest(any(), any(), any());
    }

    @Test
    void getABTestResults_WhenTestExists_ShouldReturnResults() throws Exception {
        // Given
        testResponse.setControlSentCount(100L);
        testResponse.setControlSuccessCount(20L);
        testResponse.setVariantSentCount(100L);
        testResponse.setVariantSuccessCount(25L);
        testResponse.setControlSuccessRate(20.0);
        testResponse.setVariantSuccessRate(25.0);
        testResponse.setImprovementPercentage(25.0);
        testResponse.setIsStatisticallySignificant(true);

        when(abTestService.getABTestResults(tenantId, testName)).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/ab-tests/{testName}", testName)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.testName").value(testName))
                .andExpect(jsonPath("$.data.controlSentCount").value(100))
                .andExpect(jsonPath("$.data.controlSuccessCount").value(20))
                .andExpect(jsonPath("$.data.variantSentCount").value(100))
                .andExpect(jsonPath("$.data.variantSuccessCount").value(25))
                .andExpect(jsonPath("$.data.controlSuccessRate").value(20.0))
                .andExpect(jsonPath("$.data.variantSuccessRate").value(25.0))
                .andExpect(jsonPath("$.data.improvementPercentage").value(25.0))
                .andExpect(jsonPath("$.data.isStatisticallySignificant").value(true));

        verify(abTestService).getABTestResults(tenantId, testName);
    }

    @Test
    void getABTestResults_WhenTestNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        when(abTestService.getABTestResults(tenantId, "Nonexistent Test"))
                .thenThrow(new ResourceNotFoundException("A/B test 'Nonexistent Test' not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/ab-tests/{testName}", "Nonexistent Test")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound());

        verify(abTestService).getABTestResults(tenantId, "Nonexistent Test");
    }

    @Test
    void stopABTest_ShouldStopTest() throws Exception {
        // Given
        String stopReason = "Test completed";
        testResponse.setIsActive(false);
        testResponse.setStoppedBy(userId);
        testResponse.setStoppedAt(LocalDateTime.now());
        testResponse.setStopReason(stopReason);

        when(abTestService.stopABTest(tenantId, testName, userId, stopReason))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests/{testName}/stop", testName)
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .param("stopReason", stopReason))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.stoppedBy").value(userId))
                .andExpect(jsonPath("$.data.stopReason").value(stopReason));

        verify(abTestService).stopABTest(tenantId, testName, userId, stopReason);
    }

    @Test
    void stopABTest_WithoutStopReason_ShouldUseDefaultReason() throws Exception {
        // Given
        testResponse.setIsActive(false);
        testResponse.setStoppedBy(userId);
        testResponse.setStopReason("Manually stopped");

        when(abTestService.stopABTest(tenantId, testName, userId, "Manually stopped"))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests/{testName}/stop", testName)
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stopReason").value("Manually stopped"));

        verify(abTestService).stopABTest(tenantId, testName, userId, "Manually stopped");
    }

    @Test
    void getAllABTests_ShouldReturnAllTests() throws Exception {
        // Given
        List<NotificationABTestResponse> tests = Arrays.asList(testResponse);
        when(abTestService.getAllABTests(tenantId)).thenReturn(tests);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].testName").value(testName));

        verify(abTestService).getAllABTests(tenantId);
    }

    @Test
    void getAllABTests_WithActiveOnlyFlag_ShouldReturnActiveTests() throws Exception {
        // Given
        List<NotificationABTestResponse> activeTests = Arrays.asList(testResponse);
        when(abTestService.getActiveABTests(tenantId)).thenReturn(activeTests);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId)
                .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].isActive").value(true));

        verify(abTestService).getActiveABTests(tenantId);
        verify(abTestService, never()).getAllABTests(any());
    }

    @Test
    void getActiveABTests_ShouldReturnActiveTests() throws Exception {
        // Given
        List<NotificationABTestResponse> activeTests = Arrays.asList(testResponse);
        when(abTestService.getActiveABTests(tenantId)).thenReturn(activeTests);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/ab-tests/active")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].isActive").value(true));

        verify(abTestService).getActiveABTests(tenantId);
    }

    @Test
    void createABTest_WithoutTenantHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests")
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(abTestService, never()).createABTest(any(), any(), any());
    }

    @Test
    void createABTest_WithoutUserHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(abTestService, never()).createABTest(any(), any(), any());
    }

    @Test
    void createABTest_WithServiceException_ShouldReturnBadRequest() throws Exception {
        // Given
        when(abTestService.createABTest(eq(tenantId), any(NotificationABTestRequest.class), eq(userId)))
                .thenThrow(new IllegalArgumentException("Test name already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(abTestService).createABTest(eq(tenantId), any(NotificationABTestRequest.class), eq(userId));
    }
}
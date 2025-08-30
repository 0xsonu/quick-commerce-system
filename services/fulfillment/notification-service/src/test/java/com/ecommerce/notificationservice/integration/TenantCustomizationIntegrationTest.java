package com.ecommerce.notificationservice.integration;

import com.ecommerce.notificationservice.dto.*;
import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.repository.*;
import com.ecommerce.notificationservice.service.*;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class TenantCustomizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantBrandingService tenantBrandingService;

    @Autowired
    private NotificationTemplateVersionService templateVersionService;

    @Autowired
    private NotificationABTestService abTestService;

    @Autowired
    private NotificationTemplateEngine templateEngine;

    @Autowired
    private TenantBrandingConfigRepository brandingConfigRepository;

    @Autowired
    private NotificationTemplateVersionRepository templateVersionRepository;

    @Autowired
    private NotificationABTestRepository abTestRepository;

    private String tenantId;
    private String userId;

    @BeforeEach
    void setUp() {
        tenantId = "integration-test-tenant";
        userId = "integration-test-user";
        
        // Set tenant context
        TenantContext.setTenantId(tenantId);
        TenantContext.setUserId(userId);

        // Clean up any existing data
        abTestRepository.deleteAll();
        templateVersionRepository.deleteAll();
        brandingConfigRepository.deleteAll();
    }

    @Test
    void tenantBrandingWorkflow_ShouldWorkEndToEnd() throws Exception {
        // 1. Create tenant branding configuration
        Map<String, String> customFields = new HashMap<>();
        customFields.put("slogan", "Excellence in Every Order");
        customFields.put("social_media", "@excellentstore");

        TenantBrandingConfigRequest brandingRequest = new TenantBrandingConfigRequest();
        brandingRequest.setBrandName("Excellent Store");
        brandingRequest.setLogoUrl("https://excellentstore.com/logo.png");
        brandingRequest.setPrimaryColor("#ff6b35");
        brandingRequest.setSecondaryColor("#004e89");
        brandingRequest.setFontFamily("Roboto, sans-serif");
        brandingRequest.setWebsiteUrl("https://excellentstore.com");
        brandingRequest.setSupportEmail("support@excellentstore.com");
        brandingRequest.setSupportPhone("+1-800-EXCELLENT");
        brandingRequest.setCompanyAddress("123 Excellence Blvd, Success City, SC 12345");
        brandingRequest.setCustomFields(customFields);

        // Create branding via API
        mockMvc.perform(put("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(brandingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandName").value("Excellent Store"))
                .andExpect(jsonPath("$.data.primaryColor").value("#ff6b35"));

        // 2. Verify branding variables are available
        Map<String, Object> brandingVariables = tenantBrandingService.getBrandingVariables(tenantId);
        assertThat(brandingVariables.get("brandName")).isEqualTo("Excellent Store");
        assertThat(brandingVariables.get("primaryColor")).isEqualTo("#ff6b35");
        assertThat(brandingVariables.get("custom_slogan")).isEqualTo("Excellence in Every Order");

        // 3. Test template processing with branding
        String templateContent = "Welcome to [[${brandName}]]! [[${custom_slogan}]]";
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "John Doe");

        String processedContent = templateEngine.processTemplate(templateContent, variables, tenantId);
        assertThat(processedContent).isEqualTo("Welcome to Excellent Store! Excellence in Every Order");

        // 4. Get branding via API
        mockMvc.perform(get("/api/v1/notifications/branding")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandName").value("Excellent Store"))
                .andExpect(jsonPath("$.data.customFields.slogan").value("Excellence in Every Order"));
    }

    @Test
    void templateVersioningWorkflow_ShouldWorkEndToEnd() throws Exception {
        // 1. Create first template version
        Map<String, String> variables = new HashMap<>();
        variables.put("orderNumber", "Order number");
        variables.put("customerName", "Customer name");

        NotificationTemplateVersionRequest versionRequest = new NotificationTemplateVersionRequest();
        versionRequest.setTemplateKey("order_created");
        versionRequest.setChannel(NotificationChannel.EMAIL);
        versionRequest.setSubject("Order Confirmation - Order #[[${orderNumber}]]");
        versionRequest.setContent("Dear [[${customerName}]], your order has been created successfully.");
        versionRequest.setChangeDescription("Initial version");
        versionRequest.setVariables(variables);

        // Create version via API
        mockMvc.perform(post("/api/v1/notifications/templates/versions")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(versionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.versionNumber").value(1))
                .andExpect(jsonPath("$.data.isActive").value(false));

        // 2. Create second template version
        versionRequest.setSubject("🎉 Order Confirmation - Order #[[${orderNumber}]]");
        versionRequest.setContent("Dear [[${customerName}]], great news! Your order has been created successfully. We're excited to serve you!");
        versionRequest.setChangeDescription("Added emoji and more engaging content");

        mockMvc.perform(post("/api/v1/notifications/templates/versions")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(versionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.versionNumber").value(2));

        // 3. Get all versions
        mockMvc.perform(get("/api/v1/notifications/templates/versions")
                .header("X-Tenant-ID", tenantId)
                .param("templateKey", "order_created")
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 4. Publish version 2
        mockMvc.perform(post("/api/v1/notifications/templates/versions/2/publish")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .param("templateKey", "order_created")
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.isPublished").value(true));

        // 5. Get active version
        mockMvc.perform(get("/api/v1/notifications/templates/versions/active")
                .header("X-Tenant-ID", tenantId)
                .param("templateKey", "order_created")
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNumber").value(2))
                .andExpect(jsonPath("$.data.isActive").value(true));

        // 6. Rollback to version 1
        mockMvc.perform(post("/api/v1/notifications/templates/versions/1/rollback")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .param("templateKey", "order_created")
                .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNumber").value(1))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void abTestingWorkflow_ShouldWorkEndToEnd() throws Exception {
        // 1. Create template versions for A/B testing
        NotificationTemplateVersionRequest controlRequest = new NotificationTemplateVersionRequest();
        controlRequest.setTemplateKey("order_created");
        controlRequest.setChannel(NotificationChannel.EMAIL);
        controlRequest.setSubject("Order Confirmation");
        controlRequest.setContent("Your order has been created.");
        controlRequest.setChangeDescription("Control version");

        NotificationTemplateVersionResponse controlVersion = templateVersionService
            .createTemplateVersion(tenantId, controlRequest, userId);

        NotificationTemplateVersionRequest variantRequest = new NotificationTemplateVersionRequest();
        variantRequest.setTemplateKey("order_created");
        variantRequest.setChannel(NotificationChannel.EMAIL);
        variantRequest.setSubject("🎉 Your Order is Confirmed!");
        variantRequest.setContent("Great news! Your order has been successfully created.");
        variantRequest.setChangeDescription("Variant with emoji and excitement");

        NotificationTemplateVersionResponse variantVersion = templateVersionService
            .createTemplateVersion(tenantId, variantRequest, userId);

        // 2. Create A/B test
        NotificationABTestRequest abTestRequest = new NotificationABTestRequest();
        abTestRequest.setTestName("Subject Line Emoji Test");
        abTestRequest.setTemplateKey("order_created");
        abTestRequest.setChannel(NotificationChannel.EMAIL);
        abTestRequest.setControlVersionId(controlVersion.getId());
        abTestRequest.setVariantVersionId(variantVersion.getId());
        abTestRequest.setTrafficSplitPercentage(50);
        abTestRequest.setStartDate(LocalDateTime.now().plusMinutes(1));
        abTestRequest.setEndDate(LocalDateTime.now().plusDays(7));
        abTestRequest.setDescription("Testing emoji in subject line for better open rates");
        abTestRequest.setSuccessMetric("OPEN_RATE");

        // Create A/B test via API
        mockMvc.perform(post("/api/v1/notifications/ab-tests")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(abTestRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.testName").value("Subject Line Emoji Test"))
                .andExpect(jsonPath("$.data.trafficSplitPercentage").value(50))
                .andExpect(jsonPath("$.data.isActive").value(true));

        // 3. Test template selection for different users
        String user1 = "user1";
        String user2 = "user2";

        Long selectedVersion1 = abTestService.selectTemplateVersionForABTest(
            tenantId, "order_created", NotificationChannel.EMAIL, user1);
        Long selectedVersion2 = abTestService.selectTemplateVersionForABTest(
            tenantId, "order_created", NotificationChannel.EMAIL, user2);

        assertThat(selectedVersion1).isIn(controlVersion.getId(), variantVersion.getId());
        assertThat(selectedVersion2).isIn(controlVersion.getId(), variantVersion.getId());

        // Same user should always get the same version
        Long selectedVersion1Again = abTestService.selectTemplateVersionForABTest(
            tenantId, "order_created", NotificationChannel.EMAIL, user1);
        assertThat(selectedVersion1Again).isEqualTo(selectedVersion1);

        // 4. Record some metrics
        abTestService.recordABTestMetric(tenantId, "order_created", NotificationChannel.EMAIL, 
                                       controlVersion.getId(), "SENT");
        abTestService.recordABTestMetric(tenantId, "order_created", NotificationChannel.EMAIL, 
                                       controlVersion.getId(), "SUCCESS");
        abTestService.recordABTestMetric(tenantId, "order_created", NotificationChannel.EMAIL, 
                                       variantVersion.getId(), "SENT");

        // 5. Get test results
        mockMvc.perform(get("/api/v1/notifications/ab-tests/{testName}", "Subject Line Emoji Test")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testName").value("Subject Line Emoji Test"))
                .andExpect(jsonPath("$.data.controlSentCount").value(1))
                .andExpect(jsonPath("$.data.controlSuccessCount").value(1))
                .andExpect(jsonPath("$.data.variantSentCount").value(1))
                .andExpect(jsonPath("$.data.variantSuccessCount").value(0));

        // 6. Get all active tests
        mockMvc.perform(get("/api/v1/notifications/ab-tests/active")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].testName").value("Subject Line Emoji Test"));

        // 7. Stop the test
        mockMvc.perform(post("/api/v1/notifications/ab-tests/{testName}/stop", "Subject Line Emoji Test")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .param("stopReason", "Test completed successfully"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.stopReason").value("Test completed successfully"));
    }

    @Test
    void integratedTenantCustomization_ShouldWorkWithBrandingAndTemplates() throws Exception {
        // 1. Set up tenant branding
        TenantBrandingConfigRequest brandingRequest = new TenantBrandingConfigRequest();
        brandingRequest.setBrandName("Premium Store");
        brandingRequest.setPrimaryColor("#gold");
        brandingRequest.setSupportEmail("vip@premiumstore.com");

        tenantBrandingService.createOrUpdateTenantBranding(tenantId, brandingRequest);

        // 2. Create template version with branding variables
        NotificationTemplateVersionRequest templateRequest = new NotificationTemplateVersionRequest();
        templateRequest.setTemplateKey("order_created");
        templateRequest.setChannel(NotificationChannel.EMAIL);
        templateRequest.setSubject("[[${brandName}]] - Order Confirmation");
        templateRequest.setContent("Dear Customer, thank you for choosing [[${brandName}]]! " +
                                 "If you have questions, contact us at [[${supportEmail}]].");
        templateRequest.setChangeDescription("Template with branding variables");

        NotificationTemplateVersionResponse templateVersion = templateVersionService
            .createTemplateVersion(tenantId, templateRequest, userId);

        // 3. Publish the template
        templateVersionService.publishTemplateVersion(
            tenantId, "order_created", NotificationChannel.EMAIL, 
            templateVersion.getVersionNumber(), userId);

        // 4. Process template with branding
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "John Doe");

        String processedSubject = templateEngine.processTemplate(
            templateVersion.getSubject(), variables, tenantId);
        String processedContent = templateEngine.processTemplate(
            templateVersion.getContent(), variables, tenantId);

        assertThat(processedSubject).isEqualTo("Premium Store - Order Confirmation");
        assertThat(processedContent).contains("Premium Store");
        assertThat(processedContent).contains("vip@premiumstore.com");

        // 5. Verify tenant isolation - different tenant should get defaults
        String otherTenantId = "other-tenant";
        String processedForOtherTenant = templateEngine.processTemplate(
            templateVersion.getSubject(), variables, otherTenantId);
        
        assertThat(processedForOtherTenant).isEqualTo("Your Store - Order Confirmation");
    }
}
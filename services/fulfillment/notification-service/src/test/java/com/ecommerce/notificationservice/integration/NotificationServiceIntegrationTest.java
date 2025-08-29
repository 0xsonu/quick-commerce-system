package com.ecommerce.notificationservice.integration;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.notificationservice.repository.NotificationPreferenceRepository;
import com.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import com.ecommerce.notificationservice.service.NotificationService;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class NotificationServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("notification_service_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private NotificationLogRepository logRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // Disable external providers for testing
        registry.add("notification.email.enabled", () -> "false");
        registry.add("notification.sms.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("test-tenant");
        
        // Clean up any existing data
        logRepository.deleteAll();
        preferenceRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    void sendNotification_WithTemplate_ShouldProcessAndLog() {
        // Arrange
        NotificationTemplate template = new NotificationTemplate();
        template.setTenantId("test-tenant");
        template.setTemplateKey("order-created");
        template.setChannel(NotificationChannel.EMAIL);
        template.setSubject("Order [(${orderNumber})] Created");
        template.setContent("Your order [(${orderNumber})] has been created successfully.");
        template.setIsActive(true);
        templateRepository.save(template);

        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setTemplateKey("order-created");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderNumber", "12345");
        request.setTemplateVariables(variables);

        // Act
        NotificationResponse response = notificationService.sendNotification(request);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationStatus.FAILED, response.getStatus()); // Will fail because email provider is disabled
        assertTrue(response.getErrorMessage().contains("Provider not available"));

        // Verify log was created
        assertEquals(1, logRepository.count());
        NotificationLog log = logRepository.findAll().get(0);
        assertEquals("test-tenant", log.getTenantId());
        assertEquals(1L, log.getUserId());
        assertEquals(NotificationType.ORDER_CREATED, log.getNotificationType());
        assertEquals(NotificationChannel.EMAIL, log.getChannel());
        assertEquals("test@example.com", log.getRecipient());
        assertEquals("Order 12345 Created", log.getSubject());
        assertEquals("Your order 12345 has been created successfully.", log.getContent());
        assertEquals(NotificationStatus.FAILED, log.getStatus());
    }

    @Test
    void sendNotification_WithUserPreferenceDisabled_ShouldSkip() {
        // Arrange
        NotificationPreference preference = new NotificationPreference();
        preference.setTenantId("test-tenant");
        preference.setUserId(1L);
        preference.setNotificationType(NotificationType.ORDER_CREATED);
        preference.setChannel(NotificationChannel.EMAIL);
        preference.setIsEnabled(false);
        preferenceRepository.save(preference);

        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setContent("Test Content");

        // Act
        NotificationResponse response = notificationService.sendNotification(request);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationStatus.PENDING, response.getStatus());
        assertEquals("Disabled by user preference", response.getErrorMessage());

        // Verify no log was created
        assertEquals(0, logRepository.count());
    }

    @Test
    void sendNotification_WithoutTemplate_ShouldUseDirectContent() {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Direct Subject");
        request.setContent("Direct Content");

        // Act
        NotificationResponse response = notificationService.sendNotification(request);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationStatus.FAILED, response.getStatus()); // Will fail because email provider is disabled

        // Verify log was created with direct content
        assertEquals(1, logRepository.count());
        NotificationLog log = logRepository.findAll().get(0);
        assertEquals("Direct Subject", log.getSubject());
        assertEquals("Direct Content", log.getContent());
    }
}
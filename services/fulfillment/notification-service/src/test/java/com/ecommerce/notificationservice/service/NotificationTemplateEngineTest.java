package com.ecommerce.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTemplateEngineTest {

    private NotificationTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        templateEngine = new NotificationTemplateEngine();
    }

    @Test
    void processTemplate_WithVariables_ShouldSubstituteCorrectly() {
        // Arrange
        String template = "Hello [(${name})], your order [(${orderNumber})] is ready!";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");
        variables.put("orderNumber", "12345");

        // Act
        String result = templateEngine.processTemplate(template, variables);

        // Assert
        assertEquals("Hello John Doe, your order 12345 is ready!", result);
    }

    @Test
    void processTemplate_WithoutVariables_ShouldReturnOriginal() {
        // Arrange
        String template = "Hello, this is a static message!";

        // Act
        String result = templateEngine.processTemplate(template, null);

        // Assert
        assertEquals("Hello, this is a static message!", result);
    }

    @Test
    void processTemplate_WithEmptyVariables_ShouldReturnOriginal() {
        // Arrange
        String template = "Hello, this is a static message!";
        Map<String, Object> variables = new HashMap<>();

        // Act
        String result = templateEngine.processTemplate(template, variables);

        // Assert
        assertEquals("Hello, this is a static message!", result);
    }

    @Test
    void processTemplate_WithNullTemplate_ShouldReturnNull() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");

        // Act
        String result = templateEngine.processTemplate(null, variables);

        // Assert
        assertNull(result);
    }

    @Test
    void processSubject_ShouldProcessCorrectly() {
        // Arrange
        String subjectTemplate = "Order [(${orderNumber})] - [(${status})]";
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderNumber", "12345");
        variables.put("status", "Confirmed");

        // Act
        String result = templateEngine.processSubject(subjectTemplate, variables);

        // Assert
        assertEquals("Order 12345 - Confirmed", result);
    }

    @Test
    void processContent_WithComplexTemplate_ShouldProcessCorrectly() {
        // Arrange
        String contentTemplate = "Dear [(${customerName})],\n\n" +
                                "Your order [(${orderNumber})] has been [(${status})].\n" +
                                "Total amount: $[(${totalAmount})].\n\n" +
                                "Thank you for your business!";
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "Jane Smith");
        variables.put("orderNumber", "ORD-67890");
        variables.put("status", "shipped");
        variables.put("totalAmount", "99.99");

        // Act
        String result = templateEngine.processContent(contentTemplate, variables);

        // Assert
        String expected = "Dear Jane Smith,\n\n" +
                         "Your order ORD-67890 has been shipped.\n" +
                         "Total amount: $99.99.\n\n" +
                         "Thank you for your business!";
        assertEquals(expected, result);
    }

    @Test
    void processTemplate_WithMissingVariable_ShouldLeaveUnprocessed() {
        // Arrange
        String template = "Hello [(${name})], your order [(${orderNumber})] is ready!";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");
        // orderNumber is missing

        // Act
        String result = templateEngine.processTemplate(template, variables);

        // Assert
        // Thymeleaf should leave unresolved variables as empty or handle gracefully
        assertTrue(result.contains("John Doe"));
        assertFalse(result.contains("[(${name})]")); // Variable should be resolved
    }
}
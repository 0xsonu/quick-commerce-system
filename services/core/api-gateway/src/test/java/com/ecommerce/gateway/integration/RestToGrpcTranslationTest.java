package com.ecommerce.gateway.integration;

import com.ecommerce.gateway.dto.AuthRequest;
import com.ecommerce.gateway.dto.CreateReviewRequestDto;
import com.ecommerce.gateway.service.GatewayGrpcClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for REST-to-gRPC protocol translation in API Gateway
 * These tests verify that REST endpoints properly translate to gRPC calls
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class RestToGrpcTranslationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testGetUserProfile_RestToGrpcTranslation() throws Exception {
        // This test would verify that a REST GET request to /api/v1/grpc/users/{userId}/profile
        // is properly translated to a gRPC call to UserService.getUser()
        
        // Note: In a real integration test, you would use TestContainers to start actual gRPC services
        // or use WireMock to mock the gRPC responses
        
        String tenantId = "tenant-123";
        String userId = "456";

        mockMvc.perform(get("/api/v1/grpc/users/{userId}/profile", userId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
                // In a real test, you would also verify the response structure
    }

    @Test
    void testGetProduct_RestToGrpcTranslation() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";
        String productId = "product-789";

        mockMvc.perform(get("/api/v1/grpc/products/{productId}", productId)
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testGetProductsByIds_RestToGrpcTranslation() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";
        
        String[] productIds = {"product-1", "product-2", "product-3"};
        String requestBody = objectMapper.writeValueAsString(Arrays.asList(productIds));

        mockMvc.perform(post("/api/v1/grpc/products/batch")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testGetCart_RestToGrpcTranslation() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";

        mockMvc.perform(get("/api/v1/grpc/users/{userId}/cart", userId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testAddCartItem_RestToGrpcTranslation() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";

        GatewayGrpcClientService.AddCartItemRequest request = new GatewayGrpcClientService.AddCartItemRequest();
        request.setProductId("product-123");
        request.setSku("SKU-123");
        request.setQuantity(2);

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/grpc/users/{userId}/cart/items", userId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testAuthenticateUser_RestToGrpcTranslation() throws Exception {
        String tenantId = "tenant-123";

        AuthRequest authRequest = new AuthRequest("testuser", "password123");
        String requestBody = objectMapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/v1/grpc/auth/login")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testCreateReview_RestToGrpcTranslation() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";
        String productId = "product-789";

        CreateReviewRequestDto reviewRequest = new CreateReviewRequestDto(5, "Great product!", "I love this product. Highly recommended.");
        String requestBody = objectMapper.writeValueAsString(reviewRequest);

        mockMvc.perform(post("/api/v1/grpc/products/{productId}/reviews", productId)
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testSendNotification_RestToGrpcTranslation() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";

        GatewayGrpcClientService.SendNotificationRequest notificationRequest = new GatewayGrpcClientService.SendNotificationRequest();
        notificationRequest.setTemplateId("ORDER_CONFIRMATION");
        notificationRequest.setChannel("EMAIL");
        notificationRequest.setPriority("HIGH");
        notificationRequest.setIdempotencyKey("test-key-123");
        
        Map<String, String> templateData = new HashMap<>();
        templateData.put("orderId", "order-123");
        notificationRequest.setTemplateData(templateData);

        String requestBody = objectMapper.writeValueAsString(notificationRequest);

        mockMvc.perform(post("/api/v1/grpc/users/{userId}/notifications", userId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testErrorHandling_MissingTenantHeader() throws Exception {
        String userId = "456";

        mockMvc.perform(get("/api/v1/grpc/users/{userId}/profile", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // Expecting 400 due to missing required header
    }

    @Test
    void testErrorHandling_InvalidUserId() throws Exception {
        String tenantId = "tenant-123";
        String invalidUserId = "invalid-user-id";

        mockMvc.perform(get("/api/v1/grpc/users/{userId}/profile", invalidUserId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // Expecting 400 due to invalid user ID format
    }

    @Test
    void testCorsHeaders() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";

        mockMvc.perform(options("/api/v1/grpc/users/{userId}/profile", userId)
                .header("X-Tenant-ID", tenantId)
                .header("Origin", "https://example.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void testContentTypeHandling() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";

        // Test that the endpoint accepts JSON and returns JSON
        mockMvc.perform(get("/api/v1/grpc/users/{userId}/profile", userId)
                .header("X-Tenant-ID", tenantId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testRequestValidation_InvalidJson() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";

        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/api/v1/grpc/users/{userId}/cart/items", userId)
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLargePayloadHandling() throws Exception {
        String tenantId = "tenant-123";
        String userId = "456";

        // Create a large list of product IDs to test payload size handling
        String[] largeProductIdArray = new String[1000];
        for (int i = 0; i < 1000; i++) {
            largeProductIdArray[i] = "product-" + i;
        }

        String requestBody = objectMapper.writeValueAsString(Arrays.asList(largeProductIdArray));

        mockMvc.perform(post("/api/v1/grpc/products/batch")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
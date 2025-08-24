package com.ecommerce.orderservice.integration;

import com.ecommerce.orderservice.dto.AddressDto;
import com.ecommerce.orderservice.dto.CreateOrderItemRequest;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class OrderIdempotencyIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private CreateOrderRequest createOrderRequest;
    private String tenantId = "tenant123";
    private String userId = "1";
    private String idempotencyKey = "integration-test-key-123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TenantContext.setTenantId(tenantId);
        
        // Create test order request
        AddressDto address = new AddressDto();
        address.setStreetAddress("123 Integration Test St");
        address.setCity("Test City");
        address.setState("TS");
        address.setPostalCode("12345");
        address.setCountry("US");

        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId("integration-product-123");
        item.setSku("INT-SKU-123");
        item.setProductName("Integration Test Product");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("99.99"));

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(1L);
        createOrderRequest.setItems(List.of(item));
        createOrderRequest.setBillingAddress(address);
        createOrderRequest.setShippingAddress(address);
    }

    @Test
    void createOrder_WithIdempotencyKey_ShouldCreateOrderSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.message").value("Order created successfully"));
    }

    @Test
    void createOrder_WithoutIdempotencyKey_ShouldCreateOrderSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.message").value("Order created successfully"));
    }

    @Test
    void createOrder_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request (missing required fields)
        CreateOrderRequest invalidRequest = new CreateOrderRequest();

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void createOrder_MissingHeaders_ShouldReturnBadRequest() throws Exception {
        // When & Then - Missing tenant header
        mockMvc.perform(post("/api/v1/orders")
                .header("X-User-ID", userId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());

        // When & Then - Missing user header
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());
    }
}
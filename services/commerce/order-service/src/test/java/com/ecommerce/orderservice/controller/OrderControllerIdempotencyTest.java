package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.AddressDto;
import com.ecommerce.orderservice.dto.CreateOrderItemRequest;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.exception.DuplicateOrderException;
import com.ecommerce.orderservice.exception.IdempotencyException;
import com.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderRequest createOrderRequest;
    private OrderResponse orderResponse;
    private String tenantId = "tenant123";
    private String userId = "1";
    private String idempotencyKey = "test-idempotency-key";

    @BeforeEach
    void setUp() {
        // Create test order request
        AddressDto address = new AddressDto();
        address.setStreetAddress("123 Test St");
        address.setCity("Test City");
        address.setState("TS");
        address.setPostalCode("12345");
        address.setCountry("US");

        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId("product123");
        item.setSku("SKU123");
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("29.99"));

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(1L);
        createOrderRequest.setItems(List.of(item));
        createOrderRequest.setBillingAddress(address);
        createOrderRequest.setShippingAddress(address);

        // Create test order response
        orderResponse = new OrderResponse();
        orderResponse.setId(123L);
        orderResponse.setOrderNumber("ORD-20240101-A1B2-C3D4E5F6");
        orderResponse.setUserId(1L);
        orderResponse.setSubtotal(new BigDecimal("59.98"));
        orderResponse.setTaxAmount(new BigDecimal("4.80"));
        orderResponse.setShippingAmount(new BigDecimal("0.00"));
        orderResponse.setTotalAmount(new BigDecimal("64.78"));
    }

    @Test
    void createOrder_WithIdempotencyKey_ShouldCallIdempotentMethod() throws Exception {
        // Given
        when(orderService.createOrderWithIdempotency(any(CreateOrderRequest.class), eq(idempotencyKey)))
            .thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(123))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-20240101-A1B2-C3D4E5F6"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.message").value("Order created successfully"));
    }

    @Test
    void createOrder_WithoutIdempotencyKey_ShouldCallNormalMethod() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class)))
            .thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(123))
                .andExpect(jsonPath("$.message").value("Order created successfully"));
    }

    @Test
    void createOrder_WithEmptyIdempotencyKey_ShouldCallNormalMethod() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class)))
            .thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .header("Idempotency-Key", "")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(123));
    }

    @Test
    void createOrder_IdempotencyException_ShouldReturnConflict() throws Exception {
        // Given
        when(orderService.createOrderWithIdempotency(any(CreateOrderRequest.class), eq(idempotencyKey)))
            .thenThrow(new IdempotencyException("Request is still being processed"));

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Idempotency Error"))
                .andExpect(jsonPath("$.message").value("Request is still being processed"));
    }

    @Test
    void createOrder_DuplicateOrderException_ShouldReturnConflictWithOrderId() throws Exception {
        // Given
        when(orderService.createOrderWithIdempotency(any(CreateOrderRequest.class), eq(idempotencyKey)))
            .thenThrow(new DuplicateOrderException("Duplicate order request detected", 456L));

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Duplicate Order"))
                .andExpect(jsonPath("$.message").value("Duplicate order request detected"))
                .andExpect(jsonPath("$.existingOrderId").value(456));
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
    void createOrder_MissingTenantHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-User-ID", userId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_MissingUserHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_WithWhitespaceIdempotencyKey_ShouldCallNormalMethod() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class)))
            .thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .header("Idempotency-Key", "   ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(123));
    }

    @Test
    void createOrder_ServiceException_ShouldReturnInternalServerError() throws Exception {
        // Given
        when(orderService.createOrderWithIdempotency(any(CreateOrderRequest.class), eq(idempotencyKey)))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
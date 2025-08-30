package com.ecommerce.orderservice.integration;

import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.CreateOrderItemRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.shared.utils.TenantContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify metrics collection is working correctly
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MetricsIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("test-tenant");
        TenantContext.setUserId("test-user");
    }

    @Test
    void shouldRecordBusinessMetricsWhenCreatingOrder() {
        // Given
        CreateOrderRequest request = createTestOrderRequest();

        // Get initial metric values
        Counter orderCreatedCounter = meterRegistry.find("business.orders.created")
                .tag("tenant", "test-tenant")
                .counter();
        double initialOrderCount = orderCreatedCounter != null ? orderCreatedCounter.count() : 0;

        Timer orderCreationTimer = meterRegistry.find("order.creation.time").timer();
        long initialTimerCount = orderCreationTimer != null ? orderCreationTimer.count() : 0;

        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());

        // Verify business metrics were recorded
        Counter updatedOrderCounter = meterRegistry.find("business.orders.created")
                .tag("tenant", "test-tenant")
                .counter();
        assertNotNull(updatedOrderCounter);
        assertEquals(initialOrderCount + 1, updatedOrderCounter.count());

        // Verify timing metrics were recorded
        Timer updatedTimer = meterRegistry.find("order.creation.time").timer();
        assertNotNull(updatedTimer);
        assertEquals(initialTimerCount + 1, updatedTimer.count());
    }

    @Test
    void shouldRecordJvmMetrics() {
        // Verify JVM metrics are registered
        assertNotNull(meterRegistry.find("jvm.memory.heap.utilization").gauge());
        assertNotNull(meterRegistry.find("jvm.threads.deadlocked").gauge());
        assertNotNull(meterRegistry.find("jvm.uptime").gauge());
    }

    @Test
    void shouldRecordServiceMethodMetrics() {
        // Given
        CreateOrderRequest request = createTestOrderRequest();

        // Get initial service method timer
        Timer serviceTimer = meterRegistry.find("service.method.duration")
                .tag("class", "OrderService")
                .tag("method", "createOrder")
                .timer();
        long initialCount = serviceTimer != null ? serviceTimer.count() : 0;

        // When
        orderService.createOrder(request);

        // Then
        Timer updatedTimer = meterRegistry.find("service.method.duration")
                .tag("class", "OrderService")
                .tag("method", "createOrder")
                .timer();
        assertNotNull(updatedTimer);
        assertEquals(initialCount + 1, updatedTimer.count());
    }

    private CreateOrderRequest createTestOrderRequest() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId("test-product-1");
        item.setSku("TEST-SKU-001");
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("29.99"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setItems(List.of(item));
        request.setCurrency("USD");
        
        // Set addresses as JSON strings for simplicity
        request.setBillingAddress(createTestAddress());
        request.setShippingAddress(createTestAddress());
        
        return request;
    }

    private CreateOrderRequest.Address createTestAddress() {
        CreateOrderRequest.Address address = new CreateOrderRequest.Address();
        address.setStreetAddress("123 Test Street");
        address.setCity("Test City");
        address.setState("Test State");
        address.setPostalCode("12345");
        address.setCountry("Test Country");
        return address;
    }
}
package com.ecommerce.orderservice.integration;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class OrderServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("order_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant1");
        TenantContext.setUserId("user1");

        // Clean up any existing data
        orderRepository.deleteAll();

        // Setup test data
        AddressDto billingAddress = new AddressDto("123 Billing St", "Billing City", "BC", "12345", "USA");
        AddressDto shippingAddress = new AddressDto("456 Shipping Ave", "Shipping City", "SC", "67890", "USA");
        
        CreateOrderItemRequest item1 = new CreateOrderItemRequest("prod1", "SKU1", "Product 1", 2, new BigDecimal("25.00"));
        CreateOrderItemRequest item2 = new CreateOrderItemRequest("prod2", "SKU2", "Product 2", 1, new BigDecimal("50.00"));
        
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(1L);
        createOrderRequest.setItems(Arrays.asList(item1, item2));
        createOrderRequest.setBillingAddress(billingAddress);
        createOrderRequest.setShippingAddress(shippingAddress);
        createOrderRequest.setCurrency("USD");
    }

    @Test
    void testCreateAndRetrieveOrder() {
        // Create order
        OrderResponse createdOrder = orderService.createOrder(createOrderRequest);
        
        assertNotNull(createdOrder);
        assertNotNull(createdOrder.getId());
        assertNotNull(createdOrder.getOrderNumber());
        assertEquals(1L, createdOrder.getUserId());
        assertEquals(OrderStatus.PENDING, createdOrder.getStatus());
        assertEquals("USD", createdOrder.getCurrency());
        assertEquals(2, createdOrder.getItems().size());
        
        // Verify calculations
        assertEquals(new BigDecimal("100.00"), createdOrder.getSubtotal()); // (2*25) + (1*50) = 100
        assertEquals(new BigDecimal("8.00"), createdOrder.getTaxAmount()); // 100 * 0.08 = 8
        assertEquals(BigDecimal.ZERO, createdOrder.getShippingAmount()); // Free shipping for orders >= 100
        assertEquals(new BigDecimal("108.00"), createdOrder.getTotalAmount()); // 100 + 8 + 0 = 108

        // Retrieve order by ID
        OrderResponse retrievedOrder = orderService.getOrder(createdOrder.getId());
        assertEquals(createdOrder.getId(), retrievedOrder.getId());
        assertEquals(createdOrder.getOrderNumber(), retrievedOrder.getOrderNumber());

        // Retrieve order by number
        OrderResponse orderByNumber = orderService.getOrderByNumber(createdOrder.getOrderNumber());
        assertEquals(createdOrder.getId(), orderByNumber.getId());
    }

    @Test
    void testOrderStatusTransitions() {
        // Create order
        OrderResponse order = orderService.createOrder(createOrderRequest);
        assertEquals(OrderStatus.PENDING, order.getStatus());

        // Confirm order
        UpdateOrderStatusRequest confirmRequest = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Payment received");
        OrderResponse confirmedOrder = orderService.updateOrderStatus(order.getId(), confirmRequest);
        assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getStatus());

        // Process order
        UpdateOrderStatusRequest processRequest = new UpdateOrderStatusRequest(OrderStatus.PROCESSING, "Order being prepared");
        OrderResponse processingOrder = orderService.updateOrderStatus(order.getId(), processRequest);
        assertEquals(OrderStatus.PROCESSING, processingOrder.getStatus());

        // Ship order
        UpdateOrderStatusRequest shipRequest = new UpdateOrderStatusRequest(OrderStatus.SHIPPED, "Order shipped");
        OrderResponse shippedOrder = orderService.updateOrderStatus(order.getId(), shipRequest);
        assertEquals(OrderStatus.SHIPPED, shippedOrder.getStatus());

        // Deliver order
        UpdateOrderStatusRequest deliverRequest = new UpdateOrderStatusRequest(OrderStatus.DELIVERED, "Order delivered");
        OrderResponse deliveredOrder = orderService.updateOrderStatus(order.getId(), deliverRequest);
        assertEquals(OrderStatus.DELIVERED, deliveredOrder.getStatus());
    }

    @Test
    void testCancelOrder() {
        // Create order
        OrderResponse order = orderService.createOrder(createOrderRequest);
        
        // Cancel order
        orderService.cancelOrder(order.getId(), "Customer requested cancellation");
        
        // Verify cancellation
        OrderResponse cancelledOrder = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
    }

    @Test
    void testGetOrdersByUser() {
        // Create multiple orders for the same user
        OrderResponse order1 = orderService.createOrder(createOrderRequest);
        OrderResponse order2 = orderService.createOrder(createOrderRequest);
        
        // Retrieve orders by user
        Page<OrderResponse> userOrders = orderService.getOrdersByUser(1L, PageRequest.of(0, 10));
        
        assertEquals(2, userOrders.getContent().size());
        assertTrue(userOrders.getContent().stream().anyMatch(o -> o.getId().equals(order1.getId())));
        assertTrue(userOrders.getContent().stream().anyMatch(o -> o.getId().equals(order2.getId())));
    }

    @Test
    void testGetOrdersByStatus() {
        // Create orders with different statuses
        OrderResponse order1 = orderService.createOrder(createOrderRequest);
        OrderResponse order2 = orderService.createOrder(createOrderRequest);
        
        // Confirm one order
        UpdateOrderStatusRequest confirmRequest = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Payment received");
        orderService.updateOrderStatus(order2.getId(), confirmRequest);
        
        // Get pending orders
        List<OrderResponse> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        assertEquals(1, pendingOrders.size());
        assertEquals(order1.getId(), pendingOrders.get(0).getId());
        
        // Get confirmed orders
        List<OrderResponse> confirmedOrders = orderService.getOrdersByStatus(OrderStatus.CONFIRMED);
        assertEquals(1, confirmedOrders.size());
        assertEquals(order2.getId(), confirmedOrders.get(0).getId());
    }

    @Test
    void testOrderValidation() {
        // Create order
        OrderResponse order = orderService.createOrder(createOrderRequest);
        
        // Validate active order
        assertTrue(orderService.validateOrder(order.getId()));
        
        // Cancel order
        orderService.cancelOrder(order.getId(), "Test cancellation");
        
        // Validate cancelled order
        assertFalse(orderService.validateOrder(order.getId()));
        
        // Validate non-existent order
        assertFalse(orderService.validateOrder(999L));
    }

    @Test
    void testTenantIsolation() {
        // Create order in tenant1
        OrderResponse order1 = orderService.createOrder(createOrderRequest);
        
        // Switch to tenant2
        TenantContext.setTenantId("tenant2");
        
        // Create order in tenant2
        OrderResponse order2 = orderService.createOrder(createOrderRequest);
        
        // Verify orders are isolated by tenant
        List<OrderResponse> tenant2Orders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        assertEquals(1, tenant2Orders.size());
        assertEquals(order2.getId(), tenant2Orders.get(0).getId());
        
        // Switch back to tenant1
        TenantContext.setTenantId("tenant1");
        
        // Verify tenant1 orders
        List<OrderResponse> tenant1Orders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        assertEquals(1, tenant1Orders.size());
        assertEquals(order1.getId(), tenant1Orders.get(0).getId());
    }

    @Test
    void testOrderCalculationsWithShipping() {
        // Create order with low subtotal to trigger shipping cost
        CreateOrderItemRequest smallItem = new CreateOrderItemRequest("prod3", "SKU3", "Small Product", 1, new BigDecimal("25.00"));
        createOrderRequest.setItems(Arrays.asList(smallItem));
        
        OrderResponse order = orderService.createOrder(createOrderRequest);
        
        assertEquals(new BigDecimal("25.00"), order.getSubtotal());
        assertEquals(new BigDecimal("2.00"), order.getTaxAmount()); // 25 * 0.08 = 2
        assertEquals(new BigDecimal("9.99"), order.getShippingAmount()); // Shipping cost for orders < 100
        assertEquals(new BigDecimal("36.99"), order.getTotalAmount()); // 25 + 2 + 9.99 = 36.99
    }
}
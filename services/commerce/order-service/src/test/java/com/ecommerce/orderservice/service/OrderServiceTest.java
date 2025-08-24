package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OrderValidationException;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private Order order;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant1");
        TenantContext.setUserId("user1");

        // Setup test data
        AddressDto address = new AddressDto("123 Main St", "City", "State", "12345", "Country");
        CreateOrderItemRequest item = new CreateOrderItemRequest("prod1", "SKU1", "Product 1", 2, new BigDecimal("10.00"));
        
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(1L);
        createOrderRequest.setItems(Arrays.asList(item));
        createOrderRequest.setBillingAddress(address);
        createOrderRequest.setShippingAddress(address);

        order = new Order("tenant1", "ORD-123", 1L);
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);

        orderResponse = new OrderResponse();
        orderResponse.setId(1L);
        orderResponse.setOrderNumber("ORD-123");
        orderResponse.setUserId(1L);
        orderResponse.setStatus(OrderStatus.PENDING);
    }

    @Test
    void testCreateOrder_Success() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.createOrder(createOrderRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD-123", result.getOrderNumber());
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toResponse(any(Order.class));
    }

    @Test
    void testCreateOrder_EmptyItems() {
        // Given
        createOrderRequest.setItems(Arrays.asList());

        // When & Then
        assertThrows(OrderValidationException.class, () -> {
            orderService.createOrder(createOrderRequest);
        });
    }

    @Test
    void testCreateOrder_InvalidQuantity() {
        // Given
        CreateOrderItemRequest invalidItem = new CreateOrderItemRequest("prod1", "SKU1", "Product 1", 0, new BigDecimal("10.00"));
        createOrderRequest.setItems(Arrays.asList(invalidItem));

        // When & Then
        assertThrows(OrderValidationException.class, () -> {
            orderService.createOrder(createOrderRequest);
        });
    }

    @Test
    void testCreateOrder_InvalidPrice() {
        // Given
        CreateOrderItemRequest invalidItem = new CreateOrderItemRequest("prod1", "SKU1", "Product 1", 1, BigDecimal.ZERO);
        createOrderRequest.setItems(Arrays.asList(invalidItem));

        // When & Then
        assertThrows(OrderValidationException.class, () -> {
            orderService.createOrder(createOrderRequest);
        });
    }

    @Test
    void testGetOrder_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.getOrder(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(orderRepository).findById(1L);
    }

    @Test
    void testGetOrder_NotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrder(1L);
        });
    }

    @Test
    void testGetOrderByNumber_Success() {
        // Given
        when(orderRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.getOrderByNumber("ORD-123");

        // Then
        assertNotNull(result);
        assertEquals("ORD-123", result.getOrderNumber());
        verify(orderRepository).findByOrderNumber("ORD-123");
    }

    @Test
    void testGetOrdersByUser_Success() {
        // Given
        List<Order> orders = Arrays.asList(order);
        Page<Order> orderPage = new PageImpl<>(orders);
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findByUserId("tenant1", 1L, pageable)).thenReturn(orderPage);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        // When
        Page<OrderResponse> result = orderService.getOrdersByUser(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(orderRepository).findByUserId("tenant1", 1L, pageable);
    }

    @Test
    void testGetOrdersByStatus_Success() {
        // Given
        List<Order> orders = Arrays.asList(order);
        when(orderRepository.findByStatus("tenant1", OrderStatus.PENDING)).thenReturn(orders);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        // When
        List<OrderResponse> result = orderService.getOrdersByStatus(OrderStatus.PENDING);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findByStatus("tenant1", OrderStatus.PENDING);
    }

    @Test
    void testUpdateOrderStatus_Success() {
        // Given
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Payment received");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.updateOrderStatus(1L, request);

        // Then
        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testUpdateOrderStatus_InvalidTransition() {
        // Given
        order.setStatus(OrderStatus.DELIVERED); // Final state
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CANCELLED, "Test");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThrows(OrderValidationException.class, () -> {
            orderService.updateOrderStatus(1L, request);
        });
    }

    @Test
    void testCancelOrder_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.cancelOrder(1L, "Customer request");

        // Then
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testCancelOrder_InvalidStatus() {
        // Given
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThrows(OrderValidationException.class, () -> {
            orderService.cancelOrder(1L, "Test");
        });
    }

    @Test
    void testValidateOrder_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        boolean result = orderService.validateOrder(1L);

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateOrder_NotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        boolean result = orderService.validateOrder(1L);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateOrder_InactiveStatus() {
        // Given
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        boolean result = orderService.validateOrder(1L);

        // Then
        assertFalse(result);
    }
}
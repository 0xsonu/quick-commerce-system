package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OrderValidationException;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderService(OrderRepository orderRepository, 
                       OrderMapper orderMapper,
                       ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        logger.info("Creating order for user: {} in tenant: {}", 
                   request.getUserId(), TenantContext.getTenantId());

        validateCreateOrderRequest(request);

        // Generate unique order number
        String orderNumber = generateOrderNumber();

        // Create order entity
        Order order = new Order(TenantContext.getTenantId(), orderNumber, request.getUserId());
        
        try {
            // Convert addresses to JSON
            order.setBillingAddress(objectMapper.writeValueAsString(request.getBillingAddress()));
            order.setShippingAddress(objectMapper.writeValueAsString(request.getShippingAddress()));
        } catch (JsonProcessingException e) {
            throw new OrderValidationException("Failed to process address information", e);
        }

        order.setCurrency(request.getCurrency());

        // Add order items
        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem(
                itemRequest.getProductId(),
                itemRequest.getSku(),
                itemRequest.getProductName(),
                itemRequest.getQuantity(),
                itemRequest.getUnitPrice()
            );
            order.addItem(item);
        }

        // Calculate totals
        order.calculateTotals();

        // Save order
        Order savedOrder = orderRepository.save(order);

        logger.info("Order created successfully: {} for user: {}", 
                   savedOrder.getOrderNumber(), savedOrder.getUserId());

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = findOrderById(orderId);
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(
            TenantContext.getTenantId(), userId, pageable);
        return orders.map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(TenantContext.getTenantId(), status);
        return orders.stream()
            .map(orderMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        logger.info("Updating order status: {} to {}", orderId, request.getNewStatus());

        Order order = findOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        try {
            order.updateStatus(request.getNewStatus());
            Order savedOrder = orderRepository.save(order);

            logger.info("Order status updated: {} from {} to {}", 
                       orderId, oldStatus, request.getNewStatus());

            return orderMapper.toResponse(savedOrder);
        } catch (IllegalStateException e) {
            throw new OrderValidationException(
                String.format("Invalid status transition from %s to %s for order %d", 
                             oldStatus, request.getNewStatus(), orderId), e);
        }
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        logger.info("Cancelling order: {} with reason: {}", orderId, reason);

        Order order = findOrderById(orderId);
        
        if (!order.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new OrderValidationException(
                String.format("Cannot cancel order %d in status %s", orderId, order.getStatus()));
        }

        order.updateStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        logger.info("Order cancelled successfully: {}", orderId);
    }

    @Transactional(readOnly = true)
    public boolean validateOrder(Long orderId) {
        try {
            Order order = findOrderById(orderId);
            return order.getStatus().isActive();
        } catch (OrderNotFoundException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Long countOrdersSince(LocalDateTime startDate) {
        return orderRepository.countOrdersSince(TenantContext.getTenantId(), startDate);
    }

    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderValidationException("Order must contain at least one item");
        }

        // Validate each item
        for (CreateOrderItemRequest item : request.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new OrderValidationException("Item quantity must be positive");
            }
            if (item.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new OrderValidationException("Item unit price must be positive");
            }
        }
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + timestamp + "-" + uuid;
    }
}
package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OrderValidationException;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.saga.OrderSagaOrchestrator;
import com.ecommerce.orderservice.saga.OrderSagaState;
import com.ecommerce.shared.logging.annotation.Loggable;
import com.ecommerce.shared.logging.annotation.LogParameters;
import com.ecommerce.shared.metrics.annotations.BusinessMetric;
import com.ecommerce.shared.metrics.annotations.Timed;
import com.ecommerce.shared.metrics.collectors.BusinessMetricsCollector;
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
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final OrderEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final BusinessMetricsCollector businessMetricsCollector;

    @Autowired
    public OrderService(OrderRepository orderRepository, 
                       OrderMapper orderMapper,
                       ObjectMapper objectMapper,
                       OrderSagaOrchestrator sagaOrchestrator,
                       OrderEventPublisher eventPublisher,
                       IdempotencyService idempotencyService,
                       OrderNumberGenerator orderNumberGenerator,
                       BusinessMetricsCollector businessMetricsCollector) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.sagaOrchestrator = sagaOrchestrator;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.orderNumberGenerator = orderNumberGenerator;
        this.businessMetricsCollector = businessMetricsCollector;
    }

    @Transactional
    @Loggable
    @LogParameters
    @BusinessMetric(value = "order_created", tags = {"operation", "create"})
    @Timed(value = "order.creation.time", description = "Time taken to create an order")
    public OrderResponse createOrder(CreateOrderRequest request) {
        return createOrderInternal(request, null);
    }

    @Loggable
    @LogParameters
    @Transactional
    public OrderResponse createOrderWithIdempotency(CreateOrderRequest request, String idempotencyToken) {
        logger.info("Creating order with idempotency token for user: {} in tenant: {}", 
                   request.getUserId(), TenantContext.getTenantId());

        // Validate idempotency token and check for duplicates
        IdempotencyService.IdempotencyValidationResult validationResult = 
            idempotencyService.validateIdempotencyToken(idempotencyToken, request.getUserId(), request);

        // If we have a cached result, return it
        if (validationResult.isReturnCachedResult()) {
            logger.debug("Returning cached order result for idempotency token: {}", idempotencyToken);
            return validationResult.getCachedResponse();
        }

        try {
            // Create the order
            OrderResponse orderResponse = createOrderInternal(request, idempotencyToken);
            
            // Mark token as completed
            idempotencyService.markTokenCompleted(idempotencyToken, orderResponse.getId(), orderResponse);
            
            return orderResponse;
        } catch (Exception e) {
            // Mark token as failed
            idempotencyService.markTokenFailed(idempotencyToken, e.getMessage());
            throw e;
        }
    }

    private OrderResponse createOrderInternal(CreateOrderRequest request, String idempotencyToken) {
        logger.info("Creating order for user: {} in tenant: {}", 
                   request.getUserId(), TenantContext.getTenantId());

        validateCreateOrderRequest(request);

        // Generate unique order number
        String orderNumber = orderNumberGenerator.generateUniqueOrderNumber();

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

        // Record business metrics
        businessMetricsCollector.recordOrderCreated(TenantContext.getTenantId(), savedOrder.getTotalAmount());

        // Publish OrderCreated event
        eventPublisher.publishOrderCreated(savedOrder)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Failed to publish OrderCreated event for order: {}", 
                               savedOrder.getId(), throwable);
                } else {
                    logger.debug("Successfully published OrderCreated event for order: {}", 
                               savedOrder.getId());
                }
            });

        logger.info("Order created successfully: {} for user: {} with idempotency token: {}", 
                   savedOrder.getOrderNumber(), savedOrder.getUserId(), 
                   idempotencyToken != null ? idempotencyToken : "none");

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
    @BusinessMetric(value = "order_status_changed", tags = {"operation", "status_update"})
    @Timed(value = "order.status.update.time", description = "Time taken to update order status")
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        logger.info("Updating order status: {} to {}", orderId, request.getNewStatus());

        Order order = findOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        try {
            order.updateStatus(request.getNewStatus());
            Order savedOrder = orderRepository.save(order);

            // Record business metrics
            businessMetricsCollector.recordOrderStatusChange(
                TenantContext.getTenantId(), 
                oldStatus.toString(), 
                request.getNewStatus().toString()
            );

            // Publish appropriate event based on new status
            publishStatusChangeEvent(savedOrder, request.getNewStatus(), request.getReason());

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
        Order savedOrder = orderRepository.save(order);

        // Publish OrderCancelled event
        eventPublisher.publishOrderCancelled(savedOrder, reason)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Failed to publish OrderCancelled event for order: {}", 
                               orderId, throwable);
                } else {
                    logger.debug("Successfully published OrderCancelled event for order: {}", 
                               orderId);
                }
            });

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

    @Transactional
    public OrderResponse processOrderWithSaga(CreateOrderRequest request, String paymentMethod, String paymentToken) {
        logger.info("Processing order with saga for user: {} in tenant: {}", 
                   request.getUserId(), TenantContext.getTenantId());

        // First create the order in PENDING status
        OrderResponse orderResponse = createOrder(request);
        
        // Start the saga process asynchronously
        sagaOrchestrator.processOrder(orderResponse.getId(), paymentMethod, paymentToken)
            .whenComplete((success, throwable) -> {
                if (throwable != null) {
                    logger.error("Saga processing failed for order {}: {}", 
                               orderResponse.getId(), throwable.getMessage(), throwable);
                } else if (success) {
                    logger.info("Saga processing completed successfully for order {}", orderResponse.getId());
                } else {
                    logger.warn("Saga processing failed for order {}", orderResponse.getId());
                }
            });

        return orderResponse;
    }

    @Transactional
    public OrderResponse processExistingOrderWithSaga(Long orderId, String paymentMethod, String paymentToken) {
        logger.info("Processing existing order with saga: {} in tenant: {}", orderId, TenantContext.getTenantId());

        // Get the existing order
        OrderResponse orderResponse = getOrder(orderId);
        
        // Start the saga process asynchronously
        sagaOrchestrator.processOrder(orderId, paymentMethod, paymentToken)
            .whenComplete((success, throwable) -> {
                if (throwable != null) {
                    logger.error("Saga processing failed for order {}: {}", 
                               orderId, throwable.getMessage(), throwable);
                } else if (success) {
                    logger.info("Saga processing completed successfully for order {}", orderId);
                } else {
                    logger.warn("Saga processing failed for order {}", orderId);
                }
            });

        return orderResponse;
    }

    @Transactional(readOnly = true)
    public OrderSagaState getSagaState(Long orderId) {
        return sagaOrchestrator.getSagaState(orderId);
    }



    /**
     * Publishes appropriate event based on order status change
     */
    private void publishStatusChangeEvent(Order order, OrderStatus newStatus, String reason) {
        switch (newStatus) {
            case CONFIRMED:
                eventPublisher.publishOrderConfirmed(order, "payment-id-placeholder")
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Failed to publish OrderConfirmed event for order: {}", 
                                       order.getId(), throwable);
                        }
                    });
                break;
            case PROCESSING:
                eventPublisher.publishOrderProcessing(order)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Failed to publish OrderProcessing event for order: {}", 
                                       order.getId(), throwable);
                        }
                    });
                break;
            case SHIPPED:
                eventPublisher.publishOrderShipped(order, "tracking-placeholder", "carrier-placeholder", null)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Failed to publish OrderShipped event for order: {}", 
                                       order.getId(), throwable);
                        }
                    });
                break;
            case DELIVERED:
                eventPublisher.publishOrderDelivered(order, "tracking-placeholder", "carrier-placeholder", 
                                                   LocalDateTime.now(), null)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Failed to publish OrderDelivered event for order: {}", 
                                       order.getId(), throwable);
                        }
                    });
                break;
            case CANCELLED:
                eventPublisher.publishOrderCancelled(order, reason != null ? reason : "Status update")
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Failed to publish OrderCancelled event for order: {}", 
                                       order.getId(), throwable);
                        }
                    });
                break;
            default:
                logger.debug("No event publishing needed for status: {}", newStatus);
                break;
        }
    }
}
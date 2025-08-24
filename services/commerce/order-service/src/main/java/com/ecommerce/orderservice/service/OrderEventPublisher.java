package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.shared.models.events.*;
import com.ecommerce.shared.utils.CorrelationIdGenerator;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service responsible for publishing order-related events to Kafka
 */
@Service
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final String orderEventsTopic;

    @Autowired
    public OrderEventPublisher(KafkaTemplate<String, DomainEvent> kafkaTemplate,
                              @Value("${app.kafka.topics.order-events}") String orderEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventsTopic = orderEventsTopic;
    }

    /**
     * Publishes an OrderCreatedEvent when a new order is created
     */
    public CompletableFuture<SendResult<String, DomainEvent>> publishOrderCreated(Order order) {
        logger.info("Publishing OrderCreatedEvent for order: {} in tenant: {}", 
                   order.getId(), order.getTenantId());

        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getTenantId(),
            order.getId().toString(),
            order.getUserId().toString(),
            mapToOrderItemData(order.getItems()),
            order.getTotalAmount(),
            order.getStatus().name()
        );

        return publishEvent(event, order.getId().toString());
    }

    /**
     * Publishes an OrderConfirmedEvent when an order is confirmed (payment successful)
     */
    public CompletableFuture<SendResult<String, DomainEvent>> publishOrderConfirmed(Order order, String paymentId) {
        logger.info("Publishing OrderConfirmedEvent for order: {} in tenant: {}", 
                   order.getId(), order.getTenantId());

        OrderConfirmedEvent event = new OrderConfirmedEvent(
            order.getTenantId(),
            order.getId().toString(),
            order.getUserId().toString(),
            mapToOrderConfirmedItemData(order.getItems()),
            order.getTotalAmount(),
            paymentId
        );

        return publishEvent(event, order.getId().toString());
    }

    /**
     * Publishes an OrderProcessingEvent when an order starts processing
     */
    public CompletableFuture<SendResult<String, DomainEvent>> publishOrderProcessing(Order order) {
        logger.info("Publishing OrderProcessingEvent for order: {} in tenant: {}", 
                   order.getId(), order.getTenantId());

        OrderProcessingEvent event = new OrderProcessingEvent(
            order.getTenantId(),
            order.getId().toString(),
            order.getUserId().toString(),
            mapToOrderProcessingItemData(order.getItems())
        );

        return publishEvent(event, order.getId().toString());
    }

    /**
     * Publishes an OrderShippedEvent when an order is shipped
     */
    public CompletableFuture<SendResult<String, DomainEvent>> publishOrderShipped(Order order, 
                                                                                 String trackingNumber, 
                                                                                 String carrier, 
                                                                                 String estimatedDeliveryDate) {
        logger.info("Publishing OrderShippedEvent for order: {} in tenant: {}", 
                   order.getId(), order.getTenantId());

        OrderShippedEvent event = new OrderShippedEvent(
            order.getTenantId(),
            order.getId().toString(),
            order.getUserId().toString(),
            mapToOrderShippedItemData(order.getItems()),
            trackingNumber,
            carrier,
            estimatedDeliveryDate
        );

        return publishEvent(event, order.getId().toString());
    }

    /**
     * Publishes an OrderDeliveredEvent when an order is delivered
     */
    public CompletableFuture<SendResult<String, DomainEvent>> publishOrderDelivered(Order order, 
                                                                                   String trackingNumber, 
                                                                                   String carrier, 
                                                                                   LocalDateTime deliveredAt, 
                                                                                   String deliverySignature) {
        logger.info("Publishing OrderDeliveredEvent for order: {} in tenant: {}", 
                   order.getId(), order.getTenantId());

        OrderDeliveredEvent event = new OrderDeliveredEvent(
            order.getTenantId(),
            order.getId().toString(),
            order.getUserId().toString(),
            mapToOrderDeliveredItemData(order.getItems()),
            trackingNumber,
            carrier,
            deliveredAt,
            deliverySignature
        );

        return publishEvent(event, order.getId().toString());
    }

    /**
     * Publishes an OrderCancelledEvent when an order is cancelled
     */
    public CompletableFuture<SendResult<String, DomainEvent>> publishOrderCancelled(Order order, String reason) {
        logger.info("Publishing OrderCancelledEvent for order: {} in tenant: {}", 
                   order.getId(), order.getTenantId());

        OrderCancelledEvent event = new OrderCancelledEvent(
            order.getTenantId(),
            order.getId().toString(),
            order.getUserId().toString(),
            mapToOrderItemDataSimple(order.getItems()),
            reason
        );

        return publishEvent(event, order.getId().toString());
    }

    /**
     * Generic method to publish any domain event with correlation ID tracking
     */
    private CompletableFuture<SendResult<String, DomainEvent>> publishEvent(DomainEvent event, String orderKey) {
        // Set correlation ID from MDC or generate new one
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = CorrelationIdGenerator.generate();
        }
        event.setCorrelationId(correlationId);

        // Set tenant context
        if (event.getTenantId() == null) {
            event.setTenantId(TenantContext.getTenantId());
        }

        logger.debug("Publishing event: {} with correlation ID: {} to topic: {}", 
                    event.getEventType(), correlationId, orderEventsTopic);

        // Make variables effectively final for lambda
        final String finalCorrelationId = correlationId;
        final String finalOrderKey = orderKey;
        final String eventType = event.getEventType();

        CompletableFuture<SendResult<String, DomainEvent>> future = kafkaTemplate.send(orderEventsTopic, orderKey, event);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Failed to publish event: {} for order: {} with correlation ID: {}", 
                           eventType, finalOrderKey, finalCorrelationId, throwable);
            } else {
                logger.info("Successfully published event: {} for order: {} with correlation ID: {} to partition: {} at offset: {}", 
                           eventType, finalOrderKey, finalCorrelationId, 
                           result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    /**
     * Maps OrderItem entities to OrderCreatedEvent.OrderItemData
     */
    private List<OrderCreatedEvent.OrderItemData> mapToOrderItemData(List<OrderItem> items) {
        return items.stream()
            .map(item -> new OrderCreatedEvent.OrderItemData(
                item.getProductId(),
                item.getSku(),
                item.getQuantity(),
                item.getUnitPrice()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Maps OrderItem entities to OrderConfirmedEvent.OrderItemData
     */
    private List<OrderConfirmedEvent.OrderItemData> mapToOrderConfirmedItemData(List<OrderItem> items) {
        return items.stream()
            .map(item -> new OrderConfirmedEvent.OrderItemData(
                item.getProductId(),
                item.getSku(),
                item.getQuantity(),
                item.getUnitPrice()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Maps OrderItem entities to OrderProcessingEvent.OrderItemData
     */
    private List<OrderProcessingEvent.OrderItemData> mapToOrderProcessingItemData(List<OrderItem> items) {
        return items.stream()
            .map(item -> new OrderProcessingEvent.OrderItemData(
                item.getProductId(),
                item.getSku(),
                item.getQuantity()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Maps OrderItem entities to OrderShippedEvent.OrderItemData
     */
    private List<OrderShippedEvent.OrderItemData> mapToOrderShippedItemData(List<OrderItem> items) {
        return items.stream()
            .map(item -> new OrderShippedEvent.OrderItemData(
                item.getProductId(),
                item.getSku(),
                item.getQuantity()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Maps OrderItem entities to OrderDeliveredEvent.OrderItemData
     */
    private List<OrderDeliveredEvent.OrderItemData> mapToOrderDeliveredItemData(List<OrderItem> items) {
        return items.stream()
            .map(item -> new OrderDeliveredEvent.OrderItemData(
                item.getProductId(),
                item.getSku(),
                item.getQuantity()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Maps OrderItem entities to OrderCancelledEvent.OrderItemData
     */
    private List<OrderCancelledEvent.OrderItemData> mapToOrderItemDataSimple(List<OrderItem> items) {
        return items.stream()
            .map(item -> new OrderCancelledEvent.OrderItemData(
                item.getProductId(),
                item.getSku(),
                item.getQuantity()
            ))
            .collect(Collectors.toList());
    }
}
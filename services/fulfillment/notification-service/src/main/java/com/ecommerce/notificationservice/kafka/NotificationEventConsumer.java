package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.service.EventNotificationService;
import com.ecommerce.shared.models.events.*;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka event consumer for processing domain events and triggering notifications
 */
@Component
public class NotificationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final EventNotificationService eventNotificationService;

    @Autowired
    public NotificationEventConsumer(EventNotificationService eventNotificationService) {
        this.eventNotificationService = eventNotificationService;
    }

    /**
     * Process order created events
     */
    @KafkaListener(topics = "order-events", 
                   groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCreatedEvent(@Payload OrderCreatedEvent event,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment acknowledgment) {
        try {
            logger.info("Processing OrderCreatedEvent: orderId={}, userId={}, tenantId={}", 
                       event.getOrderId(), event.getUserId(), event.getTenantId());
            
            // Set tenant context
            TenantContext.setTenantId(event.getTenantId());
            TenantContext.setCorrelationId(event.getCorrelationId());
            
            eventNotificationService.processOrderCreatedEvent(event);
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed OrderCreatedEvent: orderId={}", event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process OrderCreatedEvent: orderId={}, error={}", 
                        event.getOrderId(), e.getMessage(), e);
            // Don't acknowledge - message will be retried
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Process order confirmed events
     */
    @KafkaListener(topics = "order-events", 
                   groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderConfirmedEvent(@Payload OrderConfirmedEvent event,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                        @Header(KafkaHeaders.OFFSET) long offset,
                                        Acknowledgment acknowledgment) {
        try {
            logger.info("Processing OrderConfirmedEvent: orderId={}, userId={}, tenantId={}", 
                       event.getOrderId(), event.getUserId(), event.getTenantId());
            
            // Set tenant context
            TenantContext.setTenantId(event.getTenantId());
            TenantContext.setCorrelationId(event.getCorrelationId());
            
            eventNotificationService.processOrderConfirmedEvent(event);
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed OrderConfirmedEvent: orderId={}", event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process OrderConfirmedEvent: orderId={}, error={}", 
                        event.getOrderId(), e.getMessage(), e);
            // Don't acknowledge - message will be retried
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Process order shipped events
     */
    @KafkaListener(topics = "shipping-events", 
                   groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderShippedEvent(@Payload OrderShippedEvent event,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment acknowledgment) {
        try {
            logger.info("Processing OrderShippedEvent: orderId={}, userId={}, tenantId={}", 
                       event.getOrderId(), event.getUserId(), event.getTenantId());
            
            // Set tenant context
            TenantContext.setTenantId(event.getTenantId());
            TenantContext.setCorrelationId(event.getCorrelationId());
            
            eventNotificationService.processOrderShippedEvent(event);
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed OrderShippedEvent: orderId={}", event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process OrderShippedEvent: orderId={}, error={}", 
                        event.getOrderId(), e.getMessage(), e);
            // Don't acknowledge - message will be retried
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Process order delivered events
     */
    @KafkaListener(topics = "shipping-events", 
                   groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderDeliveredEvent(@Payload OrderDeliveredEvent event,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                        @Header(KafkaHeaders.OFFSET) long offset,
                                        Acknowledgment acknowledgment) {
        try {
            logger.info("Processing OrderDeliveredEvent: orderId={}, userId={}, tenantId={}", 
                       event.getOrderId(), event.getUserId(), event.getTenantId());
            
            // Set tenant context
            TenantContext.setTenantId(event.getTenantId());
            TenantContext.setCorrelationId(event.getCorrelationId());
            
            eventNotificationService.processOrderDeliveredEvent(event);
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed OrderDeliveredEvent: orderId={}", event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process OrderDeliveredEvent: orderId={}, error={}", 
                        event.getOrderId(), e.getMessage(), e);
            // Don't acknowledge - message will be retried
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Process order cancelled events
     */
    @KafkaListener(topics = "order-events", 
                   groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCancelledEvent(@Payload OrderCancelledEvent event,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                        @Header(KafkaHeaders.OFFSET) long offset,
                                        Acknowledgment acknowledgment) {
        try {
            logger.info("Processing OrderCancelledEvent: orderId={}, userId={}, tenantId={}", 
                       event.getOrderId(), event.getUserId(), event.getTenantId());
            
            // Set tenant context
            TenantContext.setTenantId(event.getTenantId());
            TenantContext.setCorrelationId(event.getCorrelationId());
            
            eventNotificationService.processOrderCancelledEvent(event);
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed OrderCancelledEvent: orderId={}", event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process OrderCancelledEvent: orderId={}, error={}", 
                        event.getOrderId(), e.getMessage(), e);
            // Don't acknowledge - message will be retried
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Process inventory low stock events
     */
    @KafkaListener(topics = "inventory-events", 
                   groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleInventoryLowStockEvent(@Payload InventoryReservationFailedEvent event,
                                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                           @Header(KafkaHeaders.OFFSET) long offset,
                                           Acknowledgment acknowledgment) {
        try {
            logger.info("Processing InventoryReservationFailedEvent: orderId={}, tenantId={}", 
                       event.getOrderId(), event.getTenantId());
            
            // Set tenant context
            TenantContext.setTenantId(event.getTenantId());
            TenantContext.setCorrelationId(event.getCorrelationId());
            
            eventNotificationService.processInventoryLowStockEvent(event);
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed InventoryReservationFailedEvent: orderId={}", 
                        event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process InventoryReservationFailedEvent: orderId={}, error={}", 
                        event.getOrderId(), e.getMessage(), e);
            // Don't acknowledge - message will be retried
        } finally {
            TenantContext.clear();
        }
    }
}
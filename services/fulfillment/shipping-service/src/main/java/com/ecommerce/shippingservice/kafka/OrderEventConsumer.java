package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shared.models.events.OrderConfirmedEvent;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shippingservice.service.ShipmentOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events that trigger shipment creation
 */
@Component
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ShipmentOrchestrationService shipmentOrchestrationService;

    @Autowired
    public OrderEventConsumer(ShipmentOrchestrationService shipmentOrchestrationService) {
        this.shipmentOrchestrationService = shipmentOrchestrationService;
    }

    /**
     * Handle OrderConfirmedEvent to automatically create shipments
     */
    @KafkaListener(
        topics = "order-events",
        groupId = "shipping-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderConfirmedEvent(
            @Payload OrderConfirmedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            // Set up tenant context and correlation ID for logging
            TenantContext.setTenantId(event.getTenantId());
            TenantContext.setCorrelationId(event.getCorrelationId());
            
            MDC.put("tenantId", event.getTenantId());
            MDC.put("correlationId", event.getCorrelationId());
            MDC.put("eventId", event.getEventId());

            logger.info("Received OrderConfirmedEvent for order {} from topic {} partition {} offset {}", 
                       event.getOrderId(), topic, partition, offset);

            // Only process ORDER_CONFIRMED events
            if (!"ORDER_CONFIRMED".equals(event.getEventType())) {
                logger.debug("Ignoring event type: {}", event.getEventType());
                acknowledgment.acknowledge();
                return;
            }

            // Process the order confirmed event
            shipmentOrchestrationService.processOrderConfirmed(event);

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            
            logger.info("Successfully processed OrderConfirmedEvent for order {}", event.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing OrderConfirmedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage(), e);
            
            // Don't acknowledge - this will cause the message to be retried
            // In production, you might want to implement dead letter queue logic
            throw new RuntimeException("Failed to process OrderConfirmedEvent", e);
            
        } finally {
            // Clean up context
            TenantContext.clear();
            MDC.clear();
        }
    }
}
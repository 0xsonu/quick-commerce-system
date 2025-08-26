package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shared.models.events.OrderDeliveredEvent;
import com.ecommerce.shared.models.events.OrderShippedEvent;
import com.ecommerce.shared.utils.CorrelationIdGenerator;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for shipment-related events to Kafka
 */
@Component
public class ShipmentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventPublisher.class);
    private static final String SHIPPING_EVENTS_TOPIC = "shipping-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public ShipmentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish OrderShippedEvent when a shipment is created and in transit
     */
    public void publishOrderShippedEvent(Shipment shipment) {
        if (shipment == null) {
            logger.warn("Cannot publish OrderShippedEvent - shipment is null");
            return;
        }
        
        try {
            String tenantId = TenantContext.getTenantId();
            String correlationId = TenantContext.getCorrelationId();
            
            if (correlationId == null) {
                correlationId = CorrelationIdGenerator.generate();
            }

            // Convert shipment items to event data
            List<OrderShippedEvent.OrderItemData> items = shipment.getItems().stream()
                .map(item -> new OrderShippedEvent.OrderItemData(
                    item.getProductId(),
                    item.getSku(),
                    item.getQuantity()
                ))
                .toList();

            // Create the event
            OrderShippedEvent event = new OrderShippedEvent(
                tenantId,
                shipment.getOrderId().toString(),
                null, // userId - we don't have it in shipment, could be retrieved from order service
                items,
                shipment.getTrackingNumber(),
                shipment.getCarrierName(),
                shipment.getEstimatedDeliveryDate() != null ? 
                    shipment.getEstimatedDeliveryDate().toString() : null
            );

            event.setCorrelationId(correlationId);

            // Publish the event
            String eventKey = String.format("%s:%s", tenantId, shipment.getOrderId());
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                SHIPPING_EVENTS_TOPIC, 
                eventKey, 
                event
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Published OrderShippedEvent for order {} to topic {} with offset {}",
                               shipment.getOrderId(), SHIPPING_EVENTS_TOPIC, 
                               result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish OrderShippedEvent for order {}: {}", 
                                shipment.getOrderId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            logger.error("Error publishing OrderShippedEvent for shipment {}: {}", 
                        shipment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publish OrderDeliveredEvent when a shipment is delivered
     */
    public void publishOrderDeliveredEvent(Shipment shipment) {
        publishOrderDeliveredEvent(shipment, null);
    }

    /**
     * Publish OrderDeliveredEvent when a shipment is delivered with signature
     */
    public void publishOrderDeliveredEvent(Shipment shipment, String deliverySignature) {
        if (shipment == null) {
            logger.warn("Cannot publish OrderDeliveredEvent - shipment is null");
            return;
        }
        
        try {
            String tenantId = TenantContext.getTenantId();
            String correlationId = TenantContext.getCorrelationId();
            
            if (correlationId == null) {
                correlationId = CorrelationIdGenerator.generate();
            }

            // Convert shipment items to event data
            List<OrderDeliveredEvent.OrderItemData> items = shipment.getItems().stream()
                .map(item -> new OrderDeliveredEvent.OrderItemData(
                    item.getProductId(),
                    item.getSku(),
                    item.getQuantity()
                ))
                .toList();

            // Create the event
            OrderDeliveredEvent event = new OrderDeliveredEvent(
                tenantId,
                shipment.getOrderId().toString(),
                null, // userId - we don't have it in shipment, could be retrieved from order service
                items,
                shipment.getTrackingNumber(),
                shipment.getCarrierName(),
                shipment.getDeliveredAt() != null ? shipment.getDeliveredAt() : LocalDateTime.now(),
                deliverySignature
            );

            event.setCorrelationId(correlationId);

            // Publish the event
            String eventKey = String.format("%s:%s", tenantId, shipment.getOrderId());
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                SHIPPING_EVENTS_TOPIC, 
                eventKey, 
                event
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Published OrderDeliveredEvent for order {} to topic {} with offset {}",
                               shipment.getOrderId(), SHIPPING_EVENTS_TOPIC, 
                               result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish OrderDeliveredEvent for order {}: {}", 
                                shipment.getOrderId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            logger.error("Error publishing OrderDeliveredEvent for shipment {}: {}", 
                        shipment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publish a generic shipment status update event
     */
    public void publishShipmentStatusUpdateEvent(Shipment shipment, String previousStatus, String newStatus) {
        if (shipment == null) {
            logger.warn("Cannot publish ShipmentStatusUpdateEvent - shipment is null");
            return;
        }
        
        try {
            String tenantId = TenantContext.getTenantId();
            String correlationId = TenantContext.getCorrelationId();
            
            if (correlationId == null) {
                correlationId = CorrelationIdGenerator.generate();
            }

            // Create a custom shipment status event
            ShipmentStatusUpdateEvent event = new ShipmentStatusUpdateEvent(
                tenantId,
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getShipmentNumber(),
                shipment.getTrackingNumber(),
                shipment.getCarrierName(),
                previousStatus,
                newStatus,
                LocalDateTime.now()
            );

            event.setCorrelationId(correlationId);

            // Publish the event
            String eventKey = String.format("%s:%s", tenantId, shipment.getId());
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                SHIPPING_EVENTS_TOPIC, 
                eventKey, 
                event
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Published ShipmentStatusUpdateEvent for shipment {} to topic {} with offset {}",
                               shipment.getId(), SHIPPING_EVENTS_TOPIC, 
                               result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish ShipmentStatusUpdateEvent for shipment {}: {}", 
                                shipment.getId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            logger.error("Error publishing ShipmentStatusUpdateEvent for shipment {}: {}", 
                        shipment.getId(), e.getMessage(), e);
        }
    }
}
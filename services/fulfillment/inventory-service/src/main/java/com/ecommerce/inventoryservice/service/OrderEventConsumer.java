package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.ReservationRequest;
import com.ecommerce.inventoryservice.dto.ReservationResponse;
import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.ecommerce.shared.models.events.OrderCancelledEvent;
import com.ecommerce.shared.models.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final InventoryReservationService reservationService;
    private final InventoryReservationRepository reservationRepository;

    @Autowired
    public OrderEventConsumer(InventoryReservationService reservationService,
                            InventoryReservationRepository reservationRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Handle order created events
     */
    @KafkaListener(topics = "${app.kafka.topics.order-events:order-events}", 
                   groupId = "${spring.kafka.consumer.group-id}-order-created",
                   containerFactory = "orderCreatedKafkaListenerContainerFactory")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleOrderCreated(@Payload OrderCreatedEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment acknowledgment) {
        
        // Set up MDC context
        MDC.put("tenantId", event.getTenantId());
        MDC.put("orderId", event.getOrderId());
        MDC.put("eventId", event.getEventId());

        try {
            logger.info("Processing order created event: orderId={}, items={}", 
                       event.getOrderId(), event.getItems().size());

            // Convert order items to reservation request
            List<ReservationRequest.ReservationItemRequest> reservationItems = event.getItems().stream()
                .map(item -> new ReservationRequest.ReservationItemRequest(
                    item.getProductId(), item.getSku(), item.getQuantity()))
                .collect(Collectors.toList());

            ReservationRequest reservationRequest = new ReservationRequest(event.getOrderId(), reservationItems);

            // Attempt to reserve inventory
            ReservationResponse response = reservationService.reserveInventory(event.getTenantId(), reservationRequest);

            if (response.getStatus() == ReservationResponse.ReservationStatus.SUCCESS) {
                logger.info("Successfully reserved inventory for order: {}, reservationId: {}", 
                           event.getOrderId(), response.getReservationId());
            } else {
                logger.warn("Failed to reserve inventory for order: {}, reason: {}", 
                           event.getOrderId(), response.getFailureReason());
            }

            // Acknowledge message after successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process order created event: orderId={}, error={}", 
                        event.getOrderId(), e.getMessage(), e);
            
            // Don't acknowledge - let Kafka retry
            throw e;
        } finally {
            // Clear MDC context
            MDC.clear();
        }
    }

    /**
     * Handle order cancelled events
     */
    @KafkaListener(topics = "${app.kafka.topics.order-events:order-events}", 
                   groupId = "${spring.kafka.consumer.group-id}-order-cancelled",
                   containerFactory = "orderCancelledKafkaListenerContainerFactory")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleOrderCancelled(@Payload OrderCancelledEvent event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {
        
        // Set up MDC context
        MDC.put("tenantId", event.getTenantId());
        MDC.put("orderId", event.getOrderId());
        MDC.put("eventId", event.getEventId());

        try {
            logger.info("Processing order cancelled event: orderId={}, reason={}", 
                       event.getOrderId(), event.getReason());

            // Find and release reservations for this order
            try {
                List<InventoryReservation> activeReservations = reservationRepository
                    .findActiveReservationsByOrder(event.getTenantId(), event.getOrderId());
                
                if (!activeReservations.isEmpty()) {
                    // Release each reservation
                    for (InventoryReservation reservation : activeReservations) {
                        try {
                            reservationService.releaseReservation(event.getTenantId(), 
                                reservation.getReservationId(), "Order cancelled: " + event.getReason());
                            
                            logger.info("Released inventory reservation for cancelled order: {}, reservationId: {}", 
                                       event.getOrderId(), reservation.getReservationId());
                        } catch (Exception e) {
                            logger.error("Failed to release specific reservation: {}", 
                                        reservation.getReservationId(), e);
                        }
                    }
                } else {
                    logger.warn("No active reservations found for cancelled order: {}", event.getOrderId());
                }
            } catch (Exception e) {
                logger.error("Failed to find reservations for cancelled order: {}", event.getOrderId(), e);
                // Continue processing - don't fail the entire event
            }

            // Acknowledge message after processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process order cancelled event: orderId={}, error={}", 
                        event.getOrderId(), e.getMessage(), e);
            
            // Don't acknowledge - let Kafka retry
            throw e;
        } finally {
            // Clear MDC context
            MDC.clear();
        }
    }


}
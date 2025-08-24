package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for replaying and recovering order events
 */
@Service
public class OrderEventReplayService {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventReplayService.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Autowired
    public OrderEventReplayService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Replays all events for a specific order
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Void> replayOrderEvents(Long orderId) {
        logger.info("Replaying events for order: {} in tenant: {}", orderId, TenantContext.getTenantId());

        return CompletableFuture.runAsync(() -> {
            try {
                Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

                // Replay OrderCreated event
                eventPublisher.publishOrderCreated(order);

                // Replay status-specific events based on current order status
                switch (order.getStatus()) {
                    case CONFIRMED:
                        eventPublisher.publishOrderConfirmed(order, "replay-payment-id");
                        break;
                    case PROCESSING:
                        eventPublisher.publishOrderConfirmed(order, "replay-payment-id");
                        eventPublisher.publishOrderProcessing(order);
                        break;
                    case SHIPPED:
                        eventPublisher.publishOrderConfirmed(order, "replay-payment-id");
                        eventPublisher.publishOrderProcessing(order);
                        eventPublisher.publishOrderShipped(order, "replay-tracking", "replay-carrier", null);
                        break;
                    case DELIVERED:
                        eventPublisher.publishOrderConfirmed(order, "replay-payment-id");
                        eventPublisher.publishOrderProcessing(order);
                        eventPublisher.publishOrderShipped(order, "replay-tracking", "replay-carrier", null);
                        eventPublisher.publishOrderDelivered(order, "replay-tracking", "replay-carrier", LocalDateTime.now(), null);
                        break;
                    case CANCELLED:
                        eventPublisher.publishOrderCancelled(order, "Event replay");
                        break;
                    default:
                        // PENDING - only OrderCreated event needed
                        break;
                }

                logger.info("Successfully replayed events for order: {}", orderId);
            } catch (Exception e) {
                logger.error("Failed to replay events for order: {}", orderId, e);
                throw new RuntimeException("Event replay failed for order: " + orderId, e);
            }
        });
    }

    /**
     * Replays events for all orders within a date range
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Void> replayOrderEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Replaying events for orders between {} and {} in tenant: {}", 
                   startDate, endDate, TenantContext.getTenantId());

        return CompletableFuture.runAsync(() -> {
            try {
                int pageSize = 100;
                int pageNumber = 0;
                Page<Order> ordersPage;

                do {
                    Pageable pageable = PageRequest.of(pageNumber, pageSize);
                    ordersPage = orderRepository.findOrdersByDateRange(
                        TenantContext.getTenantId(), startDate, endDate, pageable);

                    for (Order order : ordersPage.getContent()) {
                        try {
                            replayOrderEvents(order.getId()).join();
                        } catch (Exception e) {
                            logger.error("Failed to replay events for order: {} during batch replay", 
                                       order.getId(), e);
                            // Continue with other orders
                        }
                    }

                    pageNumber++;
                } while (ordersPage.hasNext());

                logger.info("Successfully completed batch event replay for date range {} to {}", 
                           startDate, endDate);
            } catch (Exception e) {
                logger.error("Failed to complete batch event replay for date range {} to {}", 
                           startDate, endDate, e);
                throw new RuntimeException("Batch event replay failed", e);
            }
        });
    }

    /**
     * Replays events for orders with specific status
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Void> replayOrderEventsByStatus(OrderStatus status) {
        logger.info("Replaying events for orders with status: {} in tenant: {}", 
                   status, TenantContext.getTenantId());

        return CompletableFuture.runAsync(() -> {
            try {
                List<Order> orders = orderRepository.findByStatus(TenantContext.getTenantId(), status);

                for (Order order : orders) {
                    try {
                        replayOrderEvents(order.getId()).join();
                    } catch (Exception e) {
                        logger.error("Failed to replay events for order: {} during status-based replay", 
                                   order.getId(), e);
                        // Continue with other orders
                    }
                }

                logger.info("Successfully completed event replay for {} orders with status: {}", 
                           orders.size(), status);
            } catch (Exception e) {
                logger.error("Failed to complete event replay for status: {}", status, e);
                throw new RuntimeException("Status-based event replay failed", e);
            }
        });
    }

    /**
     * Recovers missing events by comparing order status with expected event sequence
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Void> recoverMissingEvents(Long orderId) {
        logger.info("Recovering missing events for order: {} in tenant: {}", orderId, TenantContext.getTenantId());

        return CompletableFuture.runAsync(() -> {
            try {
                Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

                // This is a simplified recovery - in a real system, you'd track which events were published
                // For now, we'll just republish all events based on current status
                logger.info("Republishing all events for order: {} to ensure consistency", orderId);
                replayOrderEvents(orderId).join();

                logger.info("Successfully recovered missing events for order: {}", orderId);
            } catch (Exception e) {
                logger.error("Failed to recover missing events for order: {}", orderId, e);
                throw new RuntimeException("Event recovery failed for order: " + orderId, e);
            }
        });
    }

    /**
     * Validates event consistency for an order
     */
    @Transactional(readOnly = true)
    public boolean validateEventConsistency(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            // In a real system, you'd check against an event store or audit log
            // For now, we'll just validate that the order exists and has a valid status
            boolean isValid = order.getStatus() != null && 
                             order.getItems() != null && 
                             !order.getItems().isEmpty() &&
                             order.getTotalAmount() != null;

            logger.info("Event consistency validation for order {}: {}", orderId, isValid ? "PASSED" : "FAILED");
            return isValid;
        } catch (Exception e) {
            logger.error("Failed to validate event consistency for order: {}", orderId, e);
            return false;
        }
    }
}
package com.ecommerce.orderservice.saga;

import com.ecommerce.orderservice.client.InventoryServiceClient;
import com.ecommerce.orderservice.client.PaymentServiceClient;
import com.ecommerce.orderservice.client.UserServiceClient;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.shared.proto.CommonProtos.TenantContext;
import com.ecommerce.shared.proto.CommonProtos.Money;
import com.ecommerce.shared.utils.CorrelationIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OrderSagaOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderSagaOrchestrator.class);
    
    private final UserServiceClient userServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderRepository orderRepository;
    
    // In-memory saga state storage (in production, use Redis or database)
    private final ConcurrentHashMap<Long, OrderSagaState> sagaStates = new ConcurrentHashMap<>();
    
    @Autowired
    public OrderSagaOrchestrator(UserServiceClient userServiceClient,
                               InventoryServiceClient inventoryServiceClient,
                               PaymentServiceClient paymentServiceClient,
                               OrderRepository orderRepository) {
        this.userServiceClient = userServiceClient;
        this.inventoryServiceClient = inventoryServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.orderRepository = orderRepository;
    }

    @Async
    public CompletableFuture<Boolean> processOrder(Long orderId, String paymentMethod, String paymentToken) {
        logger.info("Starting saga for order {}", orderId);
        
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
            
            OrderSagaState sagaState = new OrderSagaState(orderId, order.getTenantId());
            sagaStates.put(orderId, sagaState);
            
            // Store payment details in saga data
            sagaState.putSagaData("paymentMethod", paymentMethod);
            sagaState.putSagaData("paymentToken", paymentToken);
            sagaState.putSagaData("idempotencyKey", CorrelationIdGenerator.generate());
            
            return executeNextStep(sagaState);
            
        } catch (Exception e) {
            logger.error("Failed to start saga for order {}: {}", orderId, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private CompletableFuture<Boolean> executeNextStep(OrderSagaState sagaState) {
        try {
            sagaState.setStatus(SagaStatus.IN_PROGRESS);
            
            switch (sagaState.getCurrentStep()) {
                case USER_VALIDATION -> {
                    return executeUserValidation(sagaState);
                }
                case INVENTORY_RESERVATION -> {
                    return executeInventoryReservation(sagaState);
                }
                case PAYMENT_PROCESSING -> {
                    return executePaymentProcessing(sagaState);
                }
                case ORDER_CONFIRMATION -> {
                    return executeOrderConfirmation(sagaState);
                }
                case COMPLETED -> {
                    sagaState.markCompleted();
                    logger.info("Saga completed successfully for order {}", sagaState.getOrderId());
                    return CompletableFuture.completedFuture(true);
                }
                default -> {
                    throw new IllegalStateException("Unknown saga step: " + sagaState.getCurrentStep());
                }
            }
        } catch (Exception e) {
            logger.error("Error executing saga step {} for order {}: {}", 
                        sagaState.getCurrentStep(), sagaState.getOrderId(), e.getMessage(), e);
            return handleStepFailure(sagaState, e.getMessage());
        }
    }

    private CompletableFuture<Boolean> executeUserValidation(OrderSagaState sagaState) {
        logger.info("Executing user validation for order {}", sagaState.getOrderId());
        
        try {
            Order order = getOrder(sagaState.getOrderId());
            TenantContext context = buildTenantContext(sagaState.getTenantId(), order.getUserId().toString());
            
            var userValidation = userServiceClient.validateUser(context, order.getUserId());
            
            if (userValidation.getIsValid() && userValidation.getIsActive()) {
                logger.info("User validation successful for order {}", sagaState.getOrderId());
                sagaState.moveToNextStep();
                return executeNextStep(sagaState);
            } else {
                throw new RuntimeException("User validation failed: user is not valid or active");
            }
            
        } catch (Exception e) {
            return handleStepFailure(sagaState, "User validation failed: " + e.getMessage());
        }
    }

    private CompletableFuture<Boolean> executeInventoryReservation(OrderSagaState sagaState) {
        logger.info("Executing inventory reservation for order {}", sagaState.getOrderId());
        
        try {
            Order order = getOrder(sagaState.getOrderId());
            TenantContext context = buildTenantContext(sagaState.getTenantId(), order.getUserId().toString());
            
            // Reserve inventory for each order item
            for (OrderItem item : order.getItems()) {
                String reservationId = CorrelationIdGenerator.generate();
                
                var reservationResponse = inventoryServiceClient.reserveInventory(
                    context, 
                    item.getProductId(), 
                    item.getQuantity(), 
                    reservationId, 
                    TimeUnit.MINUTES.toSeconds(30) // 30 minute reservation
                );
                
                if (!reservationResponse.getSuccess()) {
                    throw new RuntimeException("Inventory reservation failed for product " + 
                                             item.getProductId() + ": " + reservationResponse.getErrorMessage());
                }
                
                // Store reservation ID for compensation
                sagaState.putSagaData("reservation_" + item.getProductId(), reservationId);
            }
            
            logger.info("Inventory reservation successful for order {}", sagaState.getOrderId());
            sagaState.moveToNextStep();
            return executeNextStep(sagaState);
            
        } catch (Exception e) {
            return handleStepFailure(sagaState, "Inventory reservation failed: " + e.getMessage());
        }
    }

    private CompletableFuture<Boolean> executePaymentProcessing(OrderSagaState sagaState) {
        logger.info("Executing payment processing for order {}", sagaState.getOrderId());
        
        try {
            Order order = getOrder(sagaState.getOrderId());
            TenantContext context = buildTenantContext(sagaState.getTenantId(), order.getUserId().toString());
            
            String paymentMethod = sagaState.getSagaData("paymentMethod", String.class);
            String paymentToken = sagaState.getSagaData("paymentToken", String.class);
            String idempotencyKey = sagaState.getSagaData("idempotencyKey", String.class);
            
            Money amount = Money.newBuilder()
                    .setAmountCents(order.getTotalAmount().multiply(new BigDecimal("100")).longValue())
                    .setCurrency(order.getCurrency())
                    .build();
            
            var paymentResponse = paymentServiceClient.processPayment(
                context, 
                order.getId(), 
                amount, 
                paymentMethod, 
                paymentToken, 
                idempotencyKey
            );
            
            if (paymentResponse.getSuccess() && "COMPLETED".equals(paymentResponse.getStatus())) {
                logger.info("Payment processing successful for order {}", sagaState.getOrderId());
                sagaState.putSagaData("paymentId", paymentResponse.getPaymentId());
                sagaState.putSagaData("transactionId", paymentResponse.getTransactionId());
                sagaState.moveToNextStep();
                return executeNextStep(sagaState);
            } else {
                throw new RuntimeException("Payment processing failed: " + paymentResponse.getErrorMessage());
            }
            
        } catch (Exception e) {
            return handleStepFailure(sagaState, "Payment processing failed: " + e.getMessage());
        }
    }

    @Transactional
    private CompletableFuture<Boolean> executeOrderConfirmation(OrderSagaState sagaState) {
        logger.info("Executing order confirmation for order {}", sagaState.getOrderId());
        
        try {
            Order order = getOrder(sagaState.getOrderId());
            order.updateStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            
            logger.info("Order confirmation successful for order {}", sagaState.getOrderId());
            sagaState.moveToNextStep();
            return executeNextStep(sagaState);
            
        } catch (Exception e) {
            return handleStepFailure(sagaState, "Order confirmation failed: " + e.getMessage());
        }
    }

    private CompletableFuture<Boolean> handleStepFailure(OrderSagaState sagaState, String errorMessage) {
        logger.error("Saga step failed for order {}: {}", sagaState.getOrderId(), errorMessage);
        
        if (sagaState.canRetry()) {
            sagaState.incrementRetryCount();
            logger.info("Retrying saga step for order {} (attempt {})", 
                       sagaState.getOrderId(), sagaState.getRetryCount());
            
            // Exponential backoff
            try {
                Thread.sleep(1000 * sagaState.getRetryCount());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return executeNextStep(sagaState);
        } else {
            sagaState.markFailed(errorMessage);
            return compensateSaga(sagaState);
        }
    }

    private CompletableFuture<Boolean> compensateSaga(OrderSagaState sagaState) {
        logger.info("Starting compensation for order {}", sagaState.getOrderId());
        sagaState.startCompensation();
        
        try {
            // Compensate in reverse order
            switch (sagaState.getCurrentStep()) {
                case PAYMENT_PROCESSING, ORDER_CONFIRMATION, COMPLETED -> {
                    compensatePayment(sagaState);
                    compensateInventoryReservation(sagaState);
                }
                case INVENTORY_RESERVATION -> {
                    compensateInventoryReservation(sagaState);
                }
                case USER_VALIDATION -> {
                    // No compensation needed for user validation
                }
            }
            
            // Update order status to cancelled
            updateOrderStatus(sagaState.getOrderId(), OrderStatus.CANCELLED);
            
            sagaState.markCompensated();
            logger.info("Compensation completed for order {}", sagaState.getOrderId());
            return CompletableFuture.completedFuture(false);
            
        } catch (Exception e) {
            logger.error("Compensation failed for order {}: {}", sagaState.getOrderId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private void compensatePayment(OrderSagaState sagaState) {
        String paymentId = sagaState.getSagaData("paymentId", String.class);
        if (paymentId != null) {
            try {
                Order order = getOrder(sagaState.getOrderId());
                TenantContext context = buildTenantContext(sagaState.getTenantId(), order.getUserId().toString());
                
                Money amount = Money.newBuilder()
                        .setAmountCents(order.getTotalAmount().multiply(new BigDecimal("100")).longValue())
                        .setCurrency(order.getCurrency())
                        .build();
                
                paymentServiceClient.refundPayment(context, paymentId, amount, "Order cancelled due to saga failure");
                logger.info("Payment refunded for order {}", sagaState.getOrderId());
            } catch (Exception e) {
                logger.error("Failed to refund payment for order {}: {}", sagaState.getOrderId(), e.getMessage());
            }
        }
    }

    private void compensateInventoryReservation(OrderSagaState sagaState) {
        try {
            Order order = getOrder(sagaState.getOrderId());
            TenantContext context = buildTenantContext(sagaState.getTenantId(), order.getUserId().toString());
            
            for (OrderItem item : order.getItems()) {
                String reservationId = sagaState.getSagaData("reservation_" + item.getProductId(), String.class);
                if (reservationId != null) {
                    try {
                        inventoryServiceClient.releaseInventory(context, reservationId);
                        logger.info("Inventory reservation released for product {} in order {}", 
                                   item.getProductId(), sagaState.getOrderId());
                    } catch (Exception e) {
                        logger.error("Failed to release inventory reservation for product {} in order {}: {}", 
                                    item.getProductId(), sagaState.getOrderId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to compensate inventory reservations for order {}: {}", 
                        sagaState.getOrderId(), e.getMessage());
        }
    }

    @Transactional
    private void updateOrderStatus(Long orderId, OrderStatus status) {
        try {
            Order order = getOrder(orderId);
            order.updateStatus(status);
            orderRepository.save(order);
        } catch (Exception e) {
            logger.error("Failed to update order status for order {}: {}", orderId, e.getMessage());
        }
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private TenantContext buildTenantContext(String tenantId, String userId) {
        return TenantContext.newBuilder()
                .setTenantId(tenantId)
                .setUserId(userId)
                .setCorrelationId(com.ecommerce.shared.utils.TenantContext.getCorrelationId() != null ? 
                                com.ecommerce.shared.utils.TenantContext.getCorrelationId() : CorrelationIdGenerator.generate())
                .build();
    }

    // Timeout handling method
    public void handleTimeouts() {
        sagaStates.values().stream()
                .filter(state -> state.getStatus().isActive() && state.isTimedOut())
                .forEach(state -> {
                    logger.warn("Saga timeout for order {}", state.getOrderId());
                    state.markFailed("Saga timeout");
                    compensateSaga(state);
                });
    }

    // Get saga state for monitoring
    public OrderSagaState getSagaState(Long orderId) {
        return sagaStates.get(orderId);
    }

    // Remove completed sagas to prevent memory leaks
    public void cleanupCompletedSagas() {
        sagaStates.entrySet().removeIf(entry -> {
            OrderSagaState state = entry.getValue();
            return state.getStatus().isFinal() && 
                   state.getLastUpdatedAt().isBefore(LocalDateTime.now().minusHours(1));
        });
    }

    // Package-private method for testing
    ConcurrentHashMap<Long, OrderSagaState> getSagaStates() {
        return sagaStates;
    }
}
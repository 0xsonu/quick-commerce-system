package com.ecommerce.shared.tracing.example;

import com.ecommerce.shared.tracing.annotation.Traced;
import com.ecommerce.shared.tracing.util.TracingUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Example service demonstrating various tracing patterns
 */
@Service
@Traced(operation = "order-processing")
public class BusinessServiceTracingExample {

    /**
     * Example of tracing a complete business workflow
     */
    @Traced(value = "process-order", operation = "order-workflow", includeParameters = true)
    public String processOrder(String orderId, String customerId, List<String> productIds) {
        
        // Step 1: Validate order
        validateOrder(orderId, customerId);
        
        // Step 2: Check inventory
        boolean inventoryAvailable = checkInventoryAvailability(productIds);
        if (!inventoryAvailable) {
            throw new RuntimeException("Insufficient inventory");
        }
        
        // Step 3: Process payment
        String paymentId = processPayment(customerId, calculateTotal(productIds));
        
        // Step 4: Create shipment
        String shipmentId = createShipment(orderId, customerId);
        
        // Step 5: Send notifications
        sendNotifications(customerId, orderId, paymentId, shipmentId);
        
        return "Order processed successfully: " + orderId;
    }

    @Traced(value = "validate-order", operation = "validation")
    private void validateOrder(String orderId, String customerId) {
        // Add business context to span
        Span currentSpan = Span.current();
        TracingUtils.addBusinessContext(currentSpan, "order", orderId, "validation");
        
        // Simulate validation logic
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
        
        currentSpan.setAttribute("validation.result", "success");
    }

    @Traced(value = "check-inventory", operation = "inventory-check", includeParameters = true)
    private boolean checkInventoryAvailability(List<String> productIds) {
        return TracingUtils.executeInSpan("inventory-service-call", SpanKind.CLIENT, () -> {
            // Simulate external service call
            Span currentSpan = Span.current();
            TracingUtils.addHttpContext(currentSpan, "POST", "/api/v1/inventory/check", 200);
            
            // Simulate processing time
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            currentSpan.setAttribute("inventory.products.count", productIds.size());
            currentSpan.setAttribute("inventory.check.result", "available");
            
            return true;
        });
    }

    @Traced(value = "process-payment", operation = "payment-processing")
    private String processPayment(String customerId, double amount) {
        return TracingUtils.executeInSpan("payment-gateway-call", SpanKind.CLIENT, () -> {
            Span currentSpan = Span.current();
            TracingUtils.addBusinessContext(currentSpan, "payment", "payment-123", "process");
            
            // Add payment context
            currentSpan.setAttribute("payment.amount", amount);
            currentSpan.setAttribute("payment.currency", "USD");
            currentSpan.setAttribute("payment.method", "credit_card");
            
            // Simulate payment processing
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            String paymentId = "payment-" + System.currentTimeMillis();
            currentSpan.setAttribute("payment.id", paymentId);
            currentSpan.setAttribute("payment.status", "completed");
            
            return paymentId;
        });
    }

    @Traced(value = "calculate-total", operation = "calculation")
    private double calculateTotal(List<String> productIds) {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("calculation.products.count", productIds.size());
        
        // Simulate price calculation
        double total = productIds.size() * 29.99;
        currentSpan.setAttribute("calculation.total", total);
        
        return total;
    }

    @Traced(value = "create-shipment", operation = "shipment-creation")
    private String createShipment(String orderId, String customerId) {
        return TracingUtils.executeInSpan("shipping-service-call", SpanKind.CLIENT, () -> {
            Span currentSpan = Span.current();
            TracingUtils.addBusinessContext(currentSpan, "shipment", "shipment-123", "create");
            
            // Simulate shipment creation
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            String shipmentId = "shipment-" + System.currentTimeMillis();
            currentSpan.setAttribute("shipment.id", shipmentId);
            currentSpan.setAttribute("shipment.carrier", "UPS");
            currentSpan.setAttribute("shipment.estimated_delivery", "2024-01-15");
            
            return shipmentId;
        });
    }

    @Traced(value = "send-notifications", operation = "notification-dispatch")
    private void sendNotifications(String customerId, String orderId, String paymentId, String shipmentId) {
        // Send notifications asynchronously
        CompletableFuture.runAsync(() -> {
            TracingUtils.executeInSpan("email-notification", SpanKind.PRODUCER, () -> {
                Span currentSpan = Span.current();
                currentSpan.setAttribute("notification.type", "email");
                currentSpan.setAttribute("notification.recipient", customerId);
                currentSpan.setAttribute("notification.template", "order_confirmation");
                
                // Simulate email sending
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                currentSpan.setAttribute("notification.status", "sent");
            });
        });

        CompletableFuture.runAsync(() -> {
            TracingUtils.executeInSpan("sms-notification", SpanKind.PRODUCER, () -> {
                Span currentSpan = Span.current();
                currentSpan.setAttribute("notification.type", "sms");
                currentSpan.setAttribute("notification.recipient", customerId);
                currentSpan.setAttribute("notification.message", "Order confirmed");
                
                // Simulate SMS sending
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                currentSpan.setAttribute("notification.status", "sent");
            });
        });
    }

    /**
     * Example of database operation tracing
     */
    @Traced(value = "database-operation", operation = "data-access")
    public void performDatabaseOperation(String entityId) {
        TracingUtils.executeInSpan("database-query", SpanKind.CLIENT, () -> {
            Span currentSpan = Span.current();
            TracingUtils.addDatabaseContext(currentSpan, "mysql", "ecommerce", "orders", "SELECT");
            
            currentSpan.setAttribute("db.statement", "SELECT * FROM orders WHERE id = ?");
            currentSpan.setAttribute("db.rows_affected", 1);
            
            // Simulate database query
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Example of Kafka message tracing
     */
    @Traced(value = "publish-event", operation = "event-publishing")
    public void publishOrderEvent(String orderId, String eventType) {
        TracingUtils.executeInSpan("kafka-publish", SpanKind.PRODUCER, () -> {
            Span currentSpan = Span.current();
            TracingUtils.addKafkaContext(currentSpan, "order-events", "publish", 0, 12345L);
            
            currentSpan.setAttribute("messaging.message_id", "msg-" + System.currentTimeMillis());
            currentSpan.setAttribute("messaging.payload_size", 256);
            currentSpan.setAttribute("event.type", eventType);
            currentSpan.setAttribute("event.entity_id", orderId);
            
            // Simulate message publishing
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
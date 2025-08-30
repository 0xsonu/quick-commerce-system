package com.ecommerce.shared.metrics.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collector for business-specific metrics across all services
 */
@Component
public class BusinessMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

    public BusinessMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // Order Service Metrics
    public void recordOrderCreated(String tenantId, BigDecimal orderValue) {
        Counter.builder("business.orders.created")
                .description("Number of orders created")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .increment();

        DistributionSummary.builder("business.orders.value")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .record(orderValue.doubleValue());
    }

    public void recordOrderStatusChange(String tenantId, String fromStatus, String toStatus) {
        Counter.builder("business.orders.status.changed")
                .description("Order status changes")
                .tag("tenant", tenantId)
                .tag("from_status", fromStatus)
                .tag("to_status", toStatus)
                .register(meterRegistry)
                .increment();
    }

    public void recordOrderProcessingTime(String tenantId, long processingTimeMs) {
        Timer.builder("business.orders.processing.time")
                .description("Order processing time")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    // Payment Service Metrics
    public void recordPaymentAttempt(String tenantId, String paymentMethod, boolean success) {
        Counter.builder("business.payments.attempts")
                .description("Payment attempts")
                .tag("tenant", tenantId)
                .tag("method", paymentMethod)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentAmount(String tenantId, String paymentMethod, BigDecimal amount) {
        DistributionSummary.builder("business.payments.amount")
                .tag("tenant", tenantId)
                .tag("method", paymentMethod)
                .register(meterRegistry)
                .record(amount.doubleValue());
    }

    public void recordPaymentProcessingTime(String tenantId, String paymentMethod, long processingTimeMs) {
        Timer.builder("business.payments.processing.time")
                .description("Payment processing time")
                .tag("tenant", tenantId)
                .tag("method", paymentMethod)
                .register(meterRegistry)
                .record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    // Cart Service Metrics
    public void recordCartOperation(String tenantId, String operation) {
        Counter.builder("business.cart.operations")
                .description("Cart operations")
                .tag("tenant", tenantId)
                .tag("operation", operation) // add, remove, update, checkout
                .register(meterRegistry)
                .increment();
    }

    public void recordCartValue(String tenantId, BigDecimal cartValue) {
        DistributionSummary.builder("business.cart.value")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .record(cartValue.doubleValue());
    }

    public void recordCartAbandonmentRate(String tenantId, boolean abandoned) {
        Counter.builder("business.cart.abandonment")
                .description("Cart abandonment tracking")
                .tag("tenant", tenantId)
                .tag("abandoned", String.valueOf(abandoned))
                .register(meterRegistry)
                .increment();
    }

    // Product Service Metrics
    public void recordProductView(String tenantId, String productId, String category) {
        Counter.builder("business.products.views")
                .description("Product views")
                .tag("tenant", tenantId)
                .tag("category", category)
                .register(meterRegistry)
                .increment();
    }

    public void recordProductSearch(String tenantId, String searchTerm, int resultCount) {
        Counter.builder("business.products.searches")
                .description("Product searches")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .increment();

        DistributionSummary.builder("business.products.search.results")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .record(resultCount);
    }

    // Inventory Service Metrics
    public void recordInventoryOperation(String tenantId, String operation, String productId) {
        Counter.builder("business.inventory.operations")
                .description("Inventory operations")
                .tag("tenant", tenantId)
                .tag("operation", operation) // reserve, release, update
                .register(meterRegistry)
                .increment();
    }

    public void recordStockLevel(String tenantId, String productId, int stockLevel) {
        String gaugeKey = String.format("stock_%s_%s", tenantId, productId);
        gaugeValues.computeIfAbsent(gaugeKey, k -> {
            AtomicLong value = new AtomicLong(stockLevel);
            meterRegistry.gauge("business.inventory.stock.level", 
                io.micrometer.core.instrument.Tags.of("tenant", tenantId, "product", productId), 
                value, AtomicLong::get);
            return value;
        }).set(stockLevel);
    }

    public void recordStockOutEvent(String tenantId, String productId) {
        Counter.builder("business.inventory.stock.out")
                .description("Stock out events")
                .tag("tenant", tenantId)
                .tag("product", productId)
                .register(meterRegistry)
                .increment();
    }

    // User Service Metrics
    public void recordUserRegistration(String tenantId) {
        Counter.builder("business.users.registrations")
                .description("User registrations")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .increment();
    }

    public void recordUserLogin(String tenantId, boolean success) {
        Counter.builder("business.users.logins")
                .description("User login attempts")
                .tag("tenant", tenantId)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    // Review Service Metrics
    public void recordReviewSubmission(String tenantId, String productId, int rating) {
        Counter.builder("business.reviews.submitted")
                .description("Reviews submitted")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .increment();

        DistributionSummary.builder("business.reviews.rating")
                .tag("tenant", tenantId)
                .register(meterRegistry)
                .record(rating);
    }

    // Notification Service Metrics
    public void recordNotificationSent(String tenantId, String channel, boolean success) {
        Counter.builder("business.notifications.sent")
                .description("Notifications sent")
                .tag("tenant", tenantId)
                .tag("channel", channel) // email, sms, push
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    // Shipping Service Metrics
    public void recordShipmentCreated(String tenantId, String carrier) {
        Counter.builder("business.shipments.created")
                .description("Shipments created")
                .tag("tenant", tenantId)
                .tag("carrier", carrier)
                .register(meterRegistry)
                .increment();
    }

    public void recordShipmentStatusUpdate(String tenantId, String status) {
        Counter.builder("business.shipments.status.updated")
                .description("Shipment status updates")
                .tag("tenant", tenantId)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    // Generic business metrics
    public void recordBusinessEvent(String tenantId, String eventType, String... tags) {
        Counter.Builder builder = Counter.builder("business.events")
                .description("Generic business events")
                .tag("tenant", tenantId)
                .tag("event_type", eventType);

        // Add additional tags in pairs
        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }

        builder.register(meterRegistry).increment();
    }

    public void recordBusinessTimer(String tenantId, String operation, long durationMs, String... tags) {
        Timer.Builder builder = Timer.builder("business.operation.duration")
                .description("Business operation duration")
                .tag("tenant", tenantId)
                .tag("operation", operation);

        // Add additional tags in pairs
        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }

        builder.register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
    }
}
package com.ecommerce.shared.metrics.collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BusinessMetricsCollectorTest {

    private MeterRegistry meterRegistry;
    private BusinessMetricsCollector businessMetricsCollector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        businessMetricsCollector = new BusinessMetricsCollector(meterRegistry);
    }

    @Test
    void shouldRecordOrderCreated() {
        // Given
        String tenantId = "tenant1";
        BigDecimal orderValue = new BigDecimal("99.99");

        // When
        businessMetricsCollector.recordOrderCreated(tenantId, orderValue);

        // Then
        Counter counter = meterRegistry.find("business.orders.created")
                .tag("tenant", tenantId)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());

        DistributionSummary summary = meterRegistry.find("business.orders.value")
                .tag("tenant", tenantId)
                .summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(orderValue.doubleValue(), summary.totalAmount());
    }

    @Test
    void shouldRecordOrderStatusChange() {
        // Given
        String tenantId = "tenant1";
        String fromStatus = "PENDING";
        String toStatus = "CONFIRMED";

        // When
        businessMetricsCollector.recordOrderStatusChange(tenantId, fromStatus, toStatus);

        // Then
        Counter counter = meterRegistry.find("business.orders.status.changed")
                .tag("tenant", tenantId)
                .tag("from_status", fromStatus)
                .tag("to_status", toStatus)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordOrderProcessingTime() {
        // Given
        String tenantId = "tenant1";
        long processingTimeMs = 1500L;

        // When
        businessMetricsCollector.recordOrderProcessingTime(tenantId, processingTimeMs);

        // Then
        Timer timer = meterRegistry.find("business.orders.processing.time")
                .tag("tenant", tenantId)
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRecordPaymentAttempt() {
        // Given
        String tenantId = "tenant1";
        String paymentMethod = "CREDIT_CARD";
        boolean success = true;

        // When
        businessMetricsCollector.recordPaymentAttempt(tenantId, paymentMethod, success);

        // Then
        Counter counter = meterRegistry.find("business.payments.attempts")
                .tag("tenant", tenantId)
                .tag("method", paymentMethod)
                .tag("success", String.valueOf(success))
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordPaymentAmount() {
        // Given
        String tenantId = "tenant1";
        String paymentMethod = "CREDIT_CARD";
        BigDecimal amount = new BigDecimal("149.99");

        // When
        businessMetricsCollector.recordPaymentAmount(tenantId, paymentMethod, amount);

        // Then
        DistributionSummary summary = meterRegistry.find("business.payments.amount")
                .tag("tenant", tenantId)
                .tag("method", paymentMethod)
                .summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(amount.doubleValue(), summary.totalAmount());
    }

    @Test
    void shouldRecordCartOperation() {
        // Given
        String tenantId = "tenant1";
        String operation = "add";

        // When
        businessMetricsCollector.recordCartOperation(tenantId, operation);

        // Then
        Counter counter = meterRegistry.find("business.cart.operations")
                .tag("tenant", tenantId)
                .tag("operation", operation)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordCartValue() {
        // Given
        String tenantId = "tenant1";
        BigDecimal cartValue = new BigDecimal("75.50");

        // When
        businessMetricsCollector.recordCartValue(tenantId, cartValue);

        // Then
        DistributionSummary summary = meterRegistry.find("business.cart.value")
                .tag("tenant", tenantId)
                .summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(cartValue.doubleValue(), summary.totalAmount());
    }

    @Test
    void shouldRecordProductView() {
        // Given
        String tenantId = "tenant1";
        String productId = "product123";
        String category = "electronics";

        // When
        businessMetricsCollector.recordProductView(tenantId, productId, category);

        // Then
        Counter counter = meterRegistry.find("business.products.views")
                .tag("tenant", tenantId)
                .tag("category", category)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordProductSearch() {
        // Given
        String tenantId = "tenant1";
        String searchTerm = "laptop";
        int resultCount = 25;

        // When
        businessMetricsCollector.recordProductSearch(tenantId, searchTerm, resultCount);

        // Then
        Counter counter = meterRegistry.find("business.products.searches")
                .tag("tenant", tenantId)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());

        DistributionSummary summary = meterRegistry.find("business.products.search.results")
                .tag("tenant", tenantId)
                .summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(resultCount, summary.totalAmount());
    }

    @Test
    void shouldRecordStockLevel() {
        // Given
        String tenantId = "tenant1";
        String productId = "product123";
        int stockLevel = 50;

        // When
        businessMetricsCollector.recordStockLevel(tenantId, productId, stockLevel);

        // Then
        Gauge gauge = meterRegistry.find("business.inventory.stock.level")
                .tag("tenant", tenantId)
                .tag("product", productId)
                .gauge();
        assertNotNull(gauge);
        assertEquals(stockLevel, gauge.value());
    }

    @Test
    void shouldRecordUserRegistration() {
        // Given
        String tenantId = "tenant1";

        // When
        businessMetricsCollector.recordUserRegistration(tenantId);

        // Then
        Counter counter = meterRegistry.find("business.users.registrations")
                .tag("tenant", tenantId)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordReviewSubmission() {
        // Given
        String tenantId = "tenant1";
        String productId = "product123";
        int rating = 5;

        // When
        businessMetricsCollector.recordReviewSubmission(tenantId, productId, rating);

        // Then
        Counter counter = meterRegistry.find("business.reviews.submitted")
                .tag("tenant", tenantId)
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());

        DistributionSummary summary = meterRegistry.find("business.reviews.rating")
                .tag("tenant", tenantId)
                .summary();
        assertNotNull(summary);
        assertEquals(1, summary.count());
        assertEquals(rating, summary.totalAmount());
    }

    @Test
    void shouldRecordGenericBusinessEvent() {
        // Given
        String tenantId = "tenant1";
        String eventType = "custom_event";
        String[] tags = {"key1", "value1", "key2", "value2"};

        // When
        businessMetricsCollector.recordBusinessEvent(tenantId, eventType, tags);

        // Then
        Counter counter = meterRegistry.find("business.events")
                .tag("tenant", tenantId)
                .tag("event_type", eventType)
                .tag("key1", "value1")
                .tag("key2", "value2")
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordBusinessTimer() {
        // Given
        String tenantId = "tenant1";
        String operation = "custom_operation";
        long durationMs = 2000L;
        String[] tags = {"service", "test-service"};

        // When
        businessMetricsCollector.recordBusinessTimer(tenantId, operation, durationMs, tags);

        // Then
        Timer timer = meterRegistry.find("business.operation.duration")
                .tag("tenant", tenantId)
                .tag("operation", operation)
                .tag("service", "test-service")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }
}
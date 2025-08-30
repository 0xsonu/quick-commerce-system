package com.ecommerce.shared.tracing.simple;

import com.ecommerce.shared.tracing.annotation.Traced;
import com.ecommerce.shared.tracing.util.TracingUtils;
import com.ecommerce.shared.utils.TenantContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tracing tests without Spring context
 */
class SimpleTracingTest {

    private InMemorySpanExporter spanExporter;
    private OpenTelemetry openTelemetry;
    private Tracer tracer;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        
        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        
        tracer = openTelemetry.getTracer("test-tracer");
    }

    @Test
    void testBasicSpanCreation() {
        // Create a simple span
        Span span = tracer.spanBuilder("test-span")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        
        span.setAttribute("test.attribute", "test-value");
        span.end();

        // Verify span was created
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        SpanData spanData = spans.get(0);
        assertEquals("test-span", spanData.getName());
        assertEquals("test-value", spanData.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("test.attribute")));
    }

    @Test
    void testTenantContextInSpan() {
        // Set tenant context
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("user456");
        TenantContext.setCorrelationId("corr789");

        try {
            // Create span with tenant context
            Span span = tracer.spanBuilder("tenant-span")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan();
            
            // Add tenant context to span
            String tenantId = TenantContext.getTenantId();
            String userId = TenantContext.getUserId();
            String correlationId = TenantContext.getCorrelationId();
            
            if (tenantId != null) {
                span.setAttribute("tenant.id", tenantId);
            }
            if (userId != null) {
                span.setAttribute("user.id", userId);
            }
            if (correlationId != null) {
                span.setAttribute("correlation.id", correlationId);
            }
            
            span.end();

            // Verify tenant context in span
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertEquals(1, spans.size());
            
            SpanData spanData = spans.get(0);
            assertEquals("tenant123", spanData.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id")));
            assertEquals("user456", spanData.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("user.id")));
            assertEquals("corr789", spanData.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("correlation.id")));
            
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void testNestedSpans() {
        Span parentSpan = tracer.spanBuilder("parent-span")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        
        try (var scope = parentSpan.makeCurrent()) {
            Span childSpan = tracer.spanBuilder("child-span")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan();
            
            childSpan.setAttribute("child.attribute", "child-value");
            childSpan.end();
            
        } finally {
            parentSpan.end();
        }

        // Verify both spans were created
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(2, spans.size());
        
        // Find parent and child spans
        SpanData parentSpanData = spans.stream()
                .filter(s -> "parent-span".equals(s.getName()))
                .findFirst()
                .orElseThrow();
        
        SpanData childSpanData = spans.stream()
                .filter(s -> "child-span".equals(s.getName()))
                .findFirst()
                .orElseThrow();
        
        // Verify parent-child relationship
        assertEquals(parentSpanData.getSpanContext().getTraceId(), childSpanData.getSpanContext().getTraceId());
        assertEquals(parentSpanData.getSpanContext().getSpanId(), childSpanData.getParentSpanContext().getSpanId());
    }

    @Test
    void testSpanWithException() {
        Span span = tracer.spanBuilder("error-span")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        
        try {
            throw new RuntimeException("Test exception");
        } catch (Exception e) {
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            span.recordException(e);
        } finally {
            span.end();
        }

        // Verify error span
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        SpanData spanData = spans.get(0);
        assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, spanData.getStatus().getStatusCode());
        assertEquals("Test exception", spanData.getStatus().getDescription());
        
        // Verify exception event was recorded
        assertTrue(spanData.getEvents().size() > 0);
        assertTrue(spanData.getEvents().stream()
                .anyMatch(event -> "exception".equals(event.getName())));
    }

    @Test
    void testSanitizeValue() {
        // Test normal value
        assertEquals("normal-value", TracingUtils.sanitizeValue("normal-value"));
        
        // Test sensitive data
        assertEquals("[REDACTED]", TracingUtils.sanitizeValue("password=secret123"));
        assertEquals("[REDACTED]", TracingUtils.sanitizeValue("Authorization: Bearer token123"));
        assertEquals("[REDACTED]", TracingUtils.sanitizeValue("secret-key-value"));
        
        // Test long value
        String longValue = "a".repeat(1500);
        String sanitized = TracingUtils.sanitizeValue(longValue);
        assertTrue(sanitized.length() <= 1003); // 1000 + "..."
        assertTrue(sanitized.endsWith("..."));
        
        // Test null value
        assertNull(TracingUtils.sanitizeValue(null));
    }

    @Test
    void testSanitizeUrl() {
        // Test normal URL
        assertEquals("https://example.com/api/users", 
                    TracingUtils.sanitizeUrl("https://example.com/api/users"));
        
        // Test URL with sensitive query parameters
        assertEquals("https://example.com/api/users?id=123", 
                    TracingUtils.sanitizeUrl("https://example.com/api/users?id=123&password=secret"));
        
        assertEquals("https://example.com/api/users", 
                    TracingUtils.sanitizeUrl("https://example.com/api/users?token=abc123"));
        
        // Test null URL
        assertNull(TracingUtils.sanitizeUrl(null));
    }
}
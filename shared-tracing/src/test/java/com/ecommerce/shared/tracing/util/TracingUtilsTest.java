package com.ecommerce.shared.tracing.util;

import com.ecommerce.shared.utils.TenantContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TracingUtilsTest {

    private InMemorySpanExporter spanExporter;
    private TracingUtils tracingUtils;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        
        tracingUtils = new TracingUtils(openTelemetry);
    }

    @Test
    void testExecuteInSpanWithSupplier() {
        // Arrange
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("user456");

        // Act
        String result = TracingUtils.executeInSpan("test-span", SpanKind.INTERNAL, () -> "test-result");

        // Assert
        assertEquals("test-result", result);
        
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        SpanData span = spans.get(0);
        assertEquals("test-span", span.getName());
        assertEquals("tenant123", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id")));
        assertEquals("user456", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("user.id")));
    }

    @Test
    void testExecuteInSpanWithRunnable() {
        // Arrange
        final boolean[] executed = {false};

        // Act
        TracingUtils.executeInSpan("test-span", SpanKind.INTERNAL, () -> executed[0] = true);

        // Assert
        assertTrue(executed[0]);
        
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        assertEquals("test-span", spans.get(0).getName());
    }

    @Test
    void testExecuteInSpanWithException() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            TracingUtils.executeInSpan("test-span", SpanKind.INTERNAL, () -> {
                throw new RuntimeException("Test exception");
            })
        );
        
        assertEquals("Test exception", exception.getMessage());
        
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        SpanData span = spans.get(0);
        assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, span.getStatus().getStatusCode());
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

    @Test
    void testCreateSpanWithTenantContext() {
        // Arrange
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("user456");
        TenantContext.setCorrelationId("corr789");

        // Act
        var span = TracingUtils.createSpan("test-span", SpanKind.INTERNAL);
        span.end();

        // Assert
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        SpanData spanData = spans.get(0);
        assertEquals("test-span", spanData.getName());
        assertEquals("tenant123", spanData.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id")));
        assertEquals("user456", spanData.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("user.id")));
        assertEquals("corr789", spanData.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("correlation.id")));
    }
}
package com.ecommerce.shared.tracing.util;

import com.ecommerce.shared.utils.TenantContext;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Utility class for OpenTelemetry tracing operations
 */
@Component
public class TracingUtils {

    private static final Pattern SENSITIVE_DATA_PATTERN = Pattern.compile(
            "(?i)(password|token|secret|key|authorization|credential)", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final int MAX_ATTRIBUTE_LENGTH = 1000;

    private static volatile Tracer tracer;

    @Autowired
    public TracingUtils(OpenTelemetry openTelemetry) {
        TracingUtils.tracer = openTelemetry.getTracer("ecommerce-utils");
    }
    
    private static Tracer getTracer() {
        if (tracer == null) {
            // Fallback to GlobalOpenTelemetry if not initialized via Spring
            tracer = GlobalOpenTelemetry.getTracer("ecommerce-utils");
        }
        return tracer;
    }

    /**
     * Create a new span with tenant context
     */
    public static Span createSpan(String spanName, SpanKind spanKind) {
        SpanBuilder spanBuilder = getTracer().spanBuilder(spanName)
                .setSpanKind(spanKind);
        
        Span span = spanBuilder.startSpan();
        addTenantContext(span);
        return span;
    }

    /**
     * Execute a function within a traced span
     */
    public static <T> T executeInSpan(String spanName, SpanKind spanKind, Supplier<T> function) {
        Span span = createSpan(spanName, spanKind);
        try (Scope scope = span.makeCurrent()) {
            T result = function.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Execute a runnable within a traced span
     */
    public static void executeInSpan(String spanName, SpanKind spanKind, Runnable runnable) {
        executeInSpan(spanName, spanKind, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Add tenant context to current span
     */
    public static void addTenantContext(Span span) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            span.setAttribute("tenant.id", tenantId);
        }

        String userId = TenantContext.getUserId();
        if (userId != null) {
            span.setAttribute("user.id", userId);
        }

        String correlationId = TenantContext.getCorrelationId();
        if (correlationId != null) {
            span.setAttribute("correlation.id", correlationId);
        }
    }

    /**
     * Add tenant context to current span
     */
    public static void addTenantContextToCurrentSpan() {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            addTenantContext(currentSpan);
        }
    }

    /**
     * Add business context attributes to span
     */
    public static void addBusinessContext(Span span, String entityType, String entityId, String operation) {
        span.setAttribute("business.entity.type", entityType);
        span.setAttribute("business.entity.id", entityId);
        span.setAttribute("business.operation", operation);
    }

    /**
     * Add database context to span
     */
    public static void addDatabaseContext(Span span, String dbType, String dbName, String table, String operation) {
        span.setAttribute("db.system", dbType);
        span.setAttribute("db.name", dbName);
        span.setAttribute("db.sql.table", table);
        span.setAttribute("db.operation", operation);
    }

    /**
     * Add HTTP context to span
     */
    public static void addHttpContext(Span span, String method, String url, int statusCode) {
        span.setAttribute("http.method", method);
        span.setAttribute("http.url", sanitizeUrl(url));
        span.setAttribute("http.status_code", statusCode);
    }

    /**
     * Add gRPC context to span
     */
    public static void addGrpcContext(Span span, String service, String method, String statusCode) {
        span.setAttribute("rpc.system", "grpc");
        span.setAttribute("rpc.service", service);
        span.setAttribute("rpc.method", method);
        span.setAttribute("rpc.grpc.status_code", statusCode);
    }

    /**
     * Add Kafka context to span
     */
    public static void addKafkaContext(Span span, String topic, String operation, int partition, long offset) {
        span.setAttribute("messaging.system", "kafka");
        span.setAttribute("messaging.destination", topic);
        span.setAttribute("messaging.operation", operation);
        span.setAttribute("messaging.kafka.partition", partition);
        span.setAttribute("messaging.kafka.offset", offset);
    }

    /**
     * Sanitize sensitive data from values
     */
    public static String sanitizeValue(String value) {
        if (value == null) {
            return null;
        }
        
        // Truncate long values
        if (value.length() > MAX_ATTRIBUTE_LENGTH) {
            value = value.substring(0, MAX_ATTRIBUTE_LENGTH) + "...";
        }
        
        // Mask sensitive data
        if (SENSITIVE_DATA_PATTERN.matcher(value).find()) {
            return "[REDACTED]";
        }
        
        return value;
    }

    /**
     * Sanitize URLs to remove sensitive query parameters
     */
    public static String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        
        // Remove query parameters that might contain sensitive data
        return url.replaceAll("(?i)[?&](password|token|secret|key|authorization)=[^&]*", "");
    }

    /**
     * Get current trace ID
     */
    public static String getCurrentTraceId() {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return null;
    }

    /**
     * Get current span ID
     */
    public static String getCurrentSpanId() {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            return currentSpan.getSpanContext().getSpanId();
        }
        return null;
    }

    /**
     * Check if tracing is enabled for current context
     */
    public static boolean isTracingEnabled() {
        return Span.current().getSpanContext().isValid();
    }
}
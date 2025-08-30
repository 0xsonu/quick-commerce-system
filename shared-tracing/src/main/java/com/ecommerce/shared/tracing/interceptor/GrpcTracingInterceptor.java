package com.ecommerce.shared.tracing.interceptor;

import com.ecommerce.shared.tracing.util.TracingUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * gRPC server interceptor for automatic tracing
 * Note: This is a placeholder implementation. Full gRPC integration requires
 * the gRPC dependencies to be available at runtime.
 */
@Component
@ConditionalOnClass(name = "io.grpc.ServerInterceptor")
public class GrpcTracingInterceptor {

    /**
     * Create a span for gRPC method calls
     */
    public Span createGrpcSpan(String serviceName, String methodName) {
        String spanName = "grpc." + serviceName + "/" + methodName;
        Span span = TracingUtils.createSpan(spanName, SpanKind.SERVER);
        
        // Add gRPC context
        TracingUtils.addGrpcContext(span, serviceName, methodName, "OK");
        
        return span;
    }

    /**
     * Add gRPC context to existing span
     */
    public void addGrpcContextToSpan(Span span, String serviceName, String methodName, String statusCode) {
        TracingUtils.addGrpcContext(span, serviceName, methodName, statusCode);
    }
}
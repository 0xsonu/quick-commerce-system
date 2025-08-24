package com.ecommerce.shared.grpc;

import com.ecommerce.shared.proto.CommonProtos;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * gRPC Server Interceptor for handling tenant context
 */
public class TenantContextInterceptor implements ServerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, 
            Metadata headers, 
            ServerCallHandler<ReqT, RespT> next) {

        // Extract tenant context from metadata
        CommonProtos.TenantContext context = GrpcContextUtils.extractTenantContext(headers);
        
        // Set thread context
        GrpcContextUtils.setThreadContext(context);
        
        // Set MDC for logging
        if (!context.getTenantId().isEmpty()) {
            MDC.put("tenantId", context.getTenantId());
        }
        if (!context.getUserId().isEmpty()) {
            MDC.put("userId", context.getUserId());
        }
        if (!context.getCorrelationId().isEmpty()) {
            MDC.put("correlationId", context.getCorrelationId());
        }

        logger.debug("gRPC call intercepted - Method: {}, TenantId: {}, UserId: {}, CorrelationId: {}", 
                    call.getMethodDescriptor().getFullMethodName(),
                    context.getTenantId(),
                    context.getUserId(),
                    context.getCorrelationId());

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {
            
            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } finally {
                    // Clear context after call completion
                    clearContext();
                }
            }

            @Override
            public void onCancel() {
                try {
                    super.onCancel();
                } finally {
                    // Clear context after call cancellation
                    clearContext();
                }
            }

            private void clearContext() {
                MDC.clear();
                // Note: TenantContext clearing should be handled by the TenantContext class
                // if it supports thread-local cleanup
            }
        };
    }
}
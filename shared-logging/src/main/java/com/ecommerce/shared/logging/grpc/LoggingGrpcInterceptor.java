package com.ecommerce.shared.logging.grpc;

import com.ecommerce.shared.logging.LoggingContext;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC server interceptor for logging context propagation
 */
public class LoggingGrpcInterceptor implements ServerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingGrpcInterceptor.class);
    
    private static final Metadata.Key<String> CORRELATION_ID_KEY = 
            Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TENANT_ID_KEY = 
            Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USER_ID_KEY = 
            Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER);
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, 
            Metadata headers, 
            ServerCallHandler<ReqT, RespT> next) {
        
        // Extract context from gRPC headers
        String correlationId = headers.get(CORRELATION_ID_KEY);
        String tenantId = headers.get(TENANT_ID_KEY);
        String userId = headers.get(USER_ID_KEY);
        
        // Set up logging context
        if (correlationId != null) {
            LoggingContext.setCorrelationId(correlationId);
        } else {
            LoggingContext.ensureCorrelationId();
        }
        
        if (tenantId != null) {
            LoggingContext.setTenantId(tenantId);
        }
        
        if (userId != null) {
            LoggingContext.setUserId(userId);
        }
        
        String methodName = call.getMethodDescriptor().getFullMethodName();
        logger.debug("gRPC call started: {}", methodName);
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {
            
            @Override
            public void onComplete() {
                try {
                    logger.debug("gRPC call completed: {}", methodName);
                    super.onComplete();
                } finally {
                    LoggingContext.clear();
                }
            }
            
            @Override
            public void onCancel() {
                try {
                    logger.debug("gRPC call cancelled: {}", methodName);
                    super.onCancel();
                } finally {
                    LoggingContext.clear();
                }
            }
        };
    }
}
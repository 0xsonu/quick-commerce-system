package com.ecommerce.shared.logging.grpc;

import com.ecommerce.shared.logging.LoggingContext;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC client interceptor for logging context propagation
 */
public class LoggingGrpcClientInterceptor implements ClientInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingGrpcClientInterceptor.class);
    
    private static final Metadata.Key<String> CORRELATION_ID_KEY = 
            Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TENANT_ID_KEY = 
            Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USER_ID_KEY = 
            Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER);
    
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, 
            CallOptions callOptions, 
            Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Propagate logging context to downstream services
                String correlationId = LoggingContext.getCorrelationId();
                if (correlationId != null) {
                    headers.put(CORRELATION_ID_KEY, correlationId);
                }
                
                String tenantId = LoggingContext.getTenantId();
                if (tenantId != null) {
                    headers.put(TENANT_ID_KEY, tenantId);
                }
                
                String userId = LoggingContext.getUserId();
                if (userId != null) {
                    headers.put(USER_ID_KEY, userId);
                }
                
                String methodName = method.getFullMethodName();
                logger.debug("gRPC client call started: {}", methodName);
                
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        logger.debug("gRPC client call completed: {} - Status: {}", methodName, status.getCode());
                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }
}
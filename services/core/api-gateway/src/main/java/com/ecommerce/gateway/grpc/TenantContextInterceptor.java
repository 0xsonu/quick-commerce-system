package com.ecommerce.gateway.grpc;

import io.grpc.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * gRPC client interceptor for propagating tenant context and correlation ID
 */
@Component
public class TenantContextInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> TENANT_ID_KEY =
        Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> USER_ID_KEY =
        Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> CORRELATION_ID_KEY =
        Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Propagate context from current thread/request
                String tenantId = getCurrentTenantId();
                String userId = getCurrentUserId();
                String correlationId = MDC.get("correlationId");

                if (tenantId != null && !tenantId.isEmpty()) {
                    headers.put(TENANT_ID_KEY, tenantId);
                }
                if (userId != null && !userId.isEmpty()) {
                    headers.put(USER_ID_KEY, userId);
                }
                if (correlationId != null && !correlationId.isEmpty()) {
                    headers.put(CORRELATION_ID_KEY, correlationId);
                }

                super.start(responseListener, headers);
            }
        };
    }

    private String getCurrentTenantId() {
        // In a real implementation, this would get the tenant ID from the current request context
        // For now, we'll get it from MDC or return a default
        return MDC.get("tenantId");
    }

    private String getCurrentUserId() {
        // In a real implementation, this would get the user ID from the current request context
        // For now, we'll get it from MDC or return a default
        return MDC.get("userId");
    }
}
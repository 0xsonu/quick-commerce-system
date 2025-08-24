package com.ecommerce.shared.grpc;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC Client Interceptor for propagating tenant context
 */
public class TenantContextClientInterceptor implements ClientInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextClientInterceptor.class);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, 
            CallOptions callOptions, 
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Add tenant context to outgoing metadata
                Metadata contextMetadata = GrpcContextUtils.createMetadataFromContext();
                headers.merge(contextMetadata);

                logger.debug("gRPC client call - Method: {}, TenantId: {}, UserId: {}, CorrelationId: {}", 
                            method.getFullMethodName(),
                            headers.get(GrpcContextUtils.TENANT_ID_KEY),
                            headers.get(GrpcContextUtils.USER_ID_KEY),
                            headers.get(GrpcContextUtils.CORRELATION_ID_KEY));

                super.start(responseListener, headers);
            }
        };
    }
}
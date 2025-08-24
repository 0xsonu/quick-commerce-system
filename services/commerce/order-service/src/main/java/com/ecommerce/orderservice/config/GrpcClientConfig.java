package com.ecommerce.orderservice.config;

import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.ForwardingClientCall;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shared.utils.CorrelationIdGenerator;

@Configuration
public class GrpcClientConfig {

    @Bean
    @GrpcGlobalClientInterceptor
    public ClientInterceptor tenantContextClientInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                
                return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                        next.newCall(method, callOptions)) {
                    
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        // Add tenant context to headers
                        String tenantId = TenantContext.getTenantId();
                        String userId = TenantContext.getUserId();
                        String correlationId = TenantContext.getCorrelationId();
                        
                        if (tenantId != null) {
                            headers.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
                        }
                        if (userId != null) {
                            headers.put(Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER), userId);
                        }
                        if (correlationId != null) {
                            headers.put(Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER), correlationId);
                        } else {
                            headers.put(Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER), 
                                      CorrelationIdGenerator.generate());
                        }
                        
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }
}
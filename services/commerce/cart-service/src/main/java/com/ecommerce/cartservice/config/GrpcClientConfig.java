package com.ecommerce.cartservice.config;

import com.ecommerce.shared.grpc.TenantContextClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC client configuration for Cart Service
 */
@Configuration
public class GrpcClientConfig {

    /**
     * Register tenant context client interceptor globally for all gRPC clients
     */
    @GrpcGlobalClientInterceptor
    TenantContextClientInterceptor tenantContextClientInterceptor() {
        return new TenantContextClientInterceptor();
    }
}
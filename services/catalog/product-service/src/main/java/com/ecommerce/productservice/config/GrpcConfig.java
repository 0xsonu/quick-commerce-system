package com.ecommerce.productservice.config;

import com.ecommerce.shared.grpc.TenantContextInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC configuration for Product Service
 */
@Configuration
public class GrpcConfig {

    /**
     * Register tenant context interceptor globally for all gRPC services
     */
    @GrpcGlobalServerInterceptor
    TenantContextInterceptor tenantContextInterceptor() {
        return new TenantContextInterceptor();
    }
}
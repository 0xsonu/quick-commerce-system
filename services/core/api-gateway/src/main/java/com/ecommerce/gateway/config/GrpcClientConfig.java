package com.ecommerce.gateway.config;

import com.ecommerce.gateway.grpc.GrpcMetricsInterceptor;
import com.ecommerce.gateway.grpc.TenantContextInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC clients in the API Gateway
 */
@Configuration
public class GrpcClientConfig {

    /**
     * Register the tenant context interceptor globally for all gRPC clients
     */
    @GrpcGlobalClientInterceptor
    TenantContextInterceptor tenantContextInterceptor() {
        return new TenantContextInterceptor();
    }

    /**
     * Register the metrics interceptor globally for all gRPC clients
     */
    @GrpcGlobalClientInterceptor
    GrpcMetricsInterceptor grpcMetricsInterceptor(MeterRegistry meterRegistry) {
        return new GrpcMetricsInterceptor(meterRegistry);
    }
}
package com.ecommerce.shared.logging.config;

import com.ecommerce.shared.logging.LoggingContext;
import com.ecommerce.shared.logging.aspect.LoggingAspect;
import com.ecommerce.shared.logging.filter.LoggingFilter;
import com.ecommerce.shared.logging.grpc.LoggingGrpcClientInterceptor;
import com.ecommerce.shared.logging.grpc.LoggingGrpcInterceptor;
import io.grpc.ServerInterceptor;
import io.grpc.ClientInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import jakarta.annotation.PostConstruct;

/**
 * Auto-configuration for shared logging components
 */
@Configuration
@EnableAspectJAutoProxy
@ConditionalOnClass({LoggingContext.class})
public class LoggingAutoConfiguration {
    
    @Value("${spring.application.name:unknown-service}")
    private String serviceName;
    
    @PostConstruct
    public void initializeServiceName() {
        LoggingContext.setServiceName(serviceName);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
    
    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    public LoggingFilter loggingFilter(MeterRegistry meterRegistry) {
        return new LoggingFilter(meterRegistry);
    }
    
    /**
     * gRPC server interceptor configuration
     */
    @Configuration
    @ConditionalOnClass({ServerInterceptor.class, GrpcGlobalServerInterceptor.class})
    static class GrpcServerLoggingConfiguration {
        
        @Bean
        @GrpcGlobalServerInterceptor
        @ConditionalOnMissingBean
        public LoggingGrpcInterceptor loggingGrpcInterceptor() {
            return new LoggingGrpcInterceptor();
        }
    }
    
    /**
     * gRPC client interceptor configuration
     */
    @Configuration
    @ConditionalOnClass({ClientInterceptor.class, GrpcGlobalClientInterceptor.class})
    static class GrpcClientLoggingConfiguration {
        
        @Bean
        @GrpcGlobalClientInterceptor
        @ConditionalOnMissingBean
        public LoggingGrpcClientInterceptor loggingGrpcClientInterceptor() {
            return new LoggingGrpcClientInterceptor();
        }
    }
}
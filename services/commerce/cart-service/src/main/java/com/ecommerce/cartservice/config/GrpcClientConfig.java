package com.ecommerce.cartservice.config;

import com.ecommerce.shared.grpc.TenantContextClientInterceptor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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

    /**
     * Circuit breaker for Product Service gRPC calls
     */
    @Bean
    public CircuitBreaker productServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% failure rate threshold
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds before trying again
            .slidingWindowSize(10) // Consider last 10 calls
            .minimumNumberOfCalls(5) // Minimum 5 calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
            .recordException(throwable -> 
                throwable instanceof StatusRuntimeException &&
                ((StatusRuntimeException) throwable).getStatus().getCode() != Status.Code.NOT_FOUND)
            .build();
        
        return CircuitBreaker.of("product-service", config);
    }

    /**
     * Circuit breaker for Inventory Service gRPC calls
     */
    @Bean
    public CircuitBreaker inventoryServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% failure rate threshold
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds before trying again
            .slidingWindowSize(10) // Consider last 10 calls
            .minimumNumberOfCalls(5) // Minimum 5 calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
            .recordException(throwable -> 
                throwable instanceof StatusRuntimeException &&
                ((StatusRuntimeException) throwable).getStatus().getCode() != Status.Code.NOT_FOUND)
            .build();
        
        return CircuitBreaker.of("inventory-service", config);
    }

    /**
     * Retry configuration for Product Service gRPC calls
     */
    @Bean
    public Retry productServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3) // Maximum 3 attempts
            .waitDuration(Duration.ofMillis(500)) // Wait 500ms between attempts
            .retryOnException(throwable ->
                throwable instanceof StatusRuntimeException &&
                (((StatusRuntimeException) throwable).getStatus().getCode() == Status.Code.UNAVAILABLE ||
                 ((StatusRuntimeException) throwable).getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED))
            .build();
        
        return Retry.of("product-service", config);
    }

    /**
     * Retry configuration for Inventory Service gRPC calls
     */
    @Bean
    public Retry inventoryServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3) // Maximum 3 attempts
            .waitDuration(Duration.ofMillis(500)) // Wait 500ms between attempts
            .retryOnException(throwable ->
                throwable instanceof StatusRuntimeException &&
                (((StatusRuntimeException) throwable).getStatus().getCode() == Status.Code.UNAVAILABLE ||
                 ((StatusRuntimeException) throwable).getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED))
            .build();
        
        return Retry.of("inventory-service", config);
    }

    /**
     * Time limiter for Product Service gRPC calls
     */
    @Bean
    public TimeLimiter productServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10)) // 10 second timeout
            .cancelRunningFuture(true)
            .build();
        
        return TimeLimiter.of("product-service", config);
    }

    /**
     * Time limiter for Inventory Service gRPC calls
     */
    @Bean
    public TimeLimiter inventoryServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10)) // 10 second timeout
            .cancelRunningFuture(true)
            .build();
        
        return TimeLimiter.of("inventory-service", config);
    }
}
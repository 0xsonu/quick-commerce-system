package com.ecommerce.shippingservice.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Configuration for resilience patterns in shipping service
 */
@Configuration
public class ResilienceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(IOException.class, TimeoutException.class, RuntimeException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
    }

    @Bean
    public RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(IOException.class, TimeoutException.class, RuntimeException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
    }

    @Bean
    public BulkheadConfig defaultBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(10)
                .maxWaitDuration(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public TimeLimiterConfig defaultTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))
                .cancelRunningFuture(true)
                .build();
    }

    @Bean
    public CircuitBreaker carrierApiCircuitBreaker() {
        CircuitBreaker circuitBreaker = CircuitBreaker.of("carrier-api", defaultCircuitBreakerConfig());
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    logger.info("Carrier API Circuit breaker state transition: {} -> {}", 
                        event.getStateTransition().getFromState(), 
                        event.getStateTransition().getToState()))
                .onFailureRateExceeded(event -> 
                    logger.warn("Carrier API Circuit breaker failure rate exceeded: {}%", 
                        event.getFailureRate()))
                .onCallNotPermitted(event -> 
                    logger.warn("Carrier API Circuit breaker call not permitted"))
                .onError(event -> 
                    logger.error("Carrier API Circuit breaker error: {}", 
                        event.getThrowable().getMessage()));
        
        return circuitBreaker;
    }

    @Bean
    public Retry carrierApiRetry() {
        Retry retry = Retry.of("carrier-api", defaultRetryConfig());
        
        retry.getEventPublisher()
                .onRetry(event -> 
                    logger.warn("Carrier API Retry attempt {} for: {}", 
                        event.getNumberOfRetryAttempts(), 
                        event.getLastThrowable().getMessage()))
                .onSuccess(event -> 
                    logger.info("Carrier API Retry succeeded after {} attempts", 
                        event.getNumberOfRetryAttempts()))
                .onError(event -> 
                    logger.error("Carrier API Retry failed after {} attempts: {}", 
                        event.getNumberOfRetryAttempts(), 
                        event.getLastThrowable().getMessage()));
        
        return retry;
    }

    @Bean
    public Bulkhead carrierApiBulkhead() {
        Bulkhead bulkhead = Bulkhead.of("carrier-api", defaultBulkheadConfig());
        
        bulkhead.getEventPublisher()
                .onCallPermitted(event -> 
                    logger.debug("Carrier API Bulkhead call permitted"))
                .onCallRejected(event -> 
                    logger.warn("Carrier API Bulkhead call rejected"))
                .onCallFinished(event -> 
                    logger.debug("Carrier API Bulkhead call finished"));
        
        return bulkhead;
    }


}
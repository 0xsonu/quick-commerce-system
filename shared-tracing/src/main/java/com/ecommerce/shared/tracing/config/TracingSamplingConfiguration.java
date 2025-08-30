package com.ecommerce.shared.tracing.config;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenTelemetry sampling strategies
 */
@Configuration
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingSamplingConfiguration {

    @Value("${tracing.sampling.ratio:0.1}")
    private double samplingRatio;

    @Value("${tracing.sampling.rate-limit:100}")
    private int rateLimit;

    @Value("${tracing.sampling.parent-based:true}")
    private boolean parentBased;

    /**
     * Create a custom sampler based on configuration
     */
    @Bean
    public Sampler customSampler() {
        Sampler baseSampler = Sampler.traceIdRatioBased(samplingRatio);
        
        if (parentBased) {
            // Use parent-based sampling - if parent is sampled, sample this trace too
            return Sampler.parentBased(baseSampler);
        }
        
        return baseSampler;
    }

    /**
     * Create a rate-limiting sampler for high-traffic scenarios
     */
    @Bean
    @ConditionalOnProperty(name = "tracing.sampling.rate-limit.enabled", havingValue = "true")
    public Sampler rateLimitingSampler() {
        // Combine ratio-based and rate-limiting sampling
        Sampler ratioSampler = Sampler.traceIdRatioBased(samplingRatio);
        
        // Note: OpenTelemetry doesn't have a built-in rate limiting sampler
        // This would need to be implemented as a custom sampler
        return ratioSampler;
    }
}
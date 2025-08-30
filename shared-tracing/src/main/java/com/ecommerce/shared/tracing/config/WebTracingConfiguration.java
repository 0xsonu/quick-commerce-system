package com.ecommerce.shared.tracing.config;

import com.ecommerce.shared.tracing.interceptor.HttpTracingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for HTTP tracing
 */
@Configuration
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class WebTracingConfiguration implements WebMvcConfigurer {

    private final HttpTracingInterceptor httpTracingInterceptor;

    @Autowired
    public WebTracingConfiguration(HttpTracingInterceptor httpTracingInterceptor) {
        this.httpTracingInterceptor = httpTracingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpTracingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/health", "/metrics");
    }
}
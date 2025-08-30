package com.ecommerce.shared.metrics.config;

import com.ecommerce.shared.metrics.aspects.BusinessMetricsAspect;
import com.ecommerce.shared.metrics.aspects.DatabaseMetricsAspect;
import com.ecommerce.shared.metrics.aspects.MethodTimingAspect;
import com.ecommerce.shared.metrics.collectors.BusinessMetricsCollector;
import com.ecommerce.shared.metrics.collectors.CacheMetricsCollector;
import com.ecommerce.shared.metrics.collectors.DatabaseMetricsCollector;
import com.ecommerce.shared.metrics.collectors.JvmMetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for comprehensive metrics collection
 */
@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(MetricsProperties.class)
@ConditionalOnProperty(name = "ecommerce.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(MetricsProperties properties) {
        return registry -> {
            registry.config().commonTags(
                "application", properties.getApplicationName(),
                "version", properties.getApplicationVersion(),
                "environment", properties.getEnvironment()
            );
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public JvmMetricsCollector jvmMetricsCollector(MeterRegistry meterRegistry) {
        return new JvmMetricsCollector(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessMetricsCollector businessMetricsCollector(MeterRegistry meterRegistry) {
        return new BusinessMetricsCollector(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "javax.sql.DataSource")
    public DatabaseMetricsCollector databaseMetricsCollector(MeterRegistry meterRegistry) {
        return new DatabaseMetricsCollector(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
    public CacheMetricsCollector cacheMetricsCollector(MeterRegistry meterRegistry) {
        return new CacheMetricsCollector(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodTimingAspect methodTimingAspect(MeterRegistry meterRegistry) {
        return new MethodTimingAspect(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessMetricsAspect businessMetricsAspect(BusinessMetricsCollector businessMetricsCollector) {
        return new BusinessMetricsAspect(businessMetricsCollector);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "javax.sql.DataSource")
    public DatabaseMetricsAspect databaseMetricsAspect(DatabaseMetricsCollector databaseMetricsCollector) {
        return new DatabaseMetricsAspect(databaseMetricsCollector);
    }
}
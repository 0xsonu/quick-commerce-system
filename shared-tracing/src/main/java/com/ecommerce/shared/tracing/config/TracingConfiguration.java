package com.ecommerce.shared.tracing.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * OpenTelemetry configuration for distributed tracing
 */
@Configuration
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfiguration {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${tracing.jaeger.endpoint:http://localhost:14250}")
    private String jaegerEndpoint;

    @Value("${tracing.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Value("${tracing.sampling.ratio:0.1}")
    private double samplingRatio;

    @Value("${tracing.export.timeout:30}")
    private int exportTimeoutSeconds;

    @Value("${tracing.export.batch.size:512}")
    private int batchSize;

    @Value("${tracing.export.batch.delay:2}")
    private int batchDelaySeconds;

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.SERVICE_VERSION, "1.0.0"
                )));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter())
                        .setMaxExportBatchSize(batchSize)
                        .setScheduleDelay(Duration.ofSeconds(batchDelaySeconds))
                        .build())
                .setResource(resource)
                .setSampler(Sampler.traceIdRatioBased(samplingRatio))
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        
        // Register globally for automatic instrumentation
        GlobalOpenTelemetry.set(openTelemetry);
        
        return openTelemetry;
    }

    @Bean
    public SpanExporter spanExporter() {
        // Use Jaeger exporter for simplicity
        return JaegerGrpcSpanExporter.builder()
                .setEndpoint(jaegerEndpoint)
                .build();
    }
}
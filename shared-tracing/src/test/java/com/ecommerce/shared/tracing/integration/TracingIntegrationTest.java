package com.ecommerce.shared.tracing.integration;

import com.ecommerce.shared.tracing.annotation.Traced;
import com.ecommerce.shared.tracing.aspect.TracingAspect;
import com.ecommerce.shared.tracing.config.TracingConfiguration;
import com.ecommerce.shared.tracing.util.TracingUtils;
import com.ecommerce.shared.utils.TenantContext;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
    TracingIntegrationTest.TestConfiguration.class
})
@TestPropertySource(properties = {
    "spring.application.name=test-service",
    "tracing.enabled=true",
    "tracing.sampling.ratio=1.0"
})
class TracingIntegrationTest {

    @Autowired
    private SpanExporter spanExporter;

    private InMemorySpanExporter inMemorySpanExporter;

    @BeforeEach
    void setUp() {
        inMemorySpanExporter = (InMemorySpanExporter) spanExporter;
        inMemorySpanExporter.reset();
    }

    @Autowired
    private TestService testService;

    @Test
    void testEndToEndTracing() {
        // Arrange
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("user456");
        TenantContext.setCorrelationId("corr789");

        // Act
        String result = TracingUtils.executeInSpan("integration-test", SpanKind.INTERNAL, () -> {
            return testService.businessOperation("test-data");
        });

        // Assert
        assertEquals("processed: test-data", result);
        
        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();
        assertTrue(spans.size() >= 2); // At least the main span and business operation span
        
        // Verify span hierarchy and context propagation
        boolean foundMainSpan = false;
        boolean foundBusinessSpan = false;
        
        for (SpanData span : spans) {
            if ("integration-test".equals(span.getName())) {
                foundMainSpan = true;
                assertEquals("tenant123", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id")));
            }
            if ("business-operation".equals(span.getName())) {
                foundBusinessSpan = true;
                assertEquals("tenant123", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id")));
            }
        }
        
        assertTrue(foundMainSpan, "Main span should be present");
        assertTrue(foundBusinessSpan, "Business operation span should be present");
    }

    @Test
    void testAsyncTracing() throws ExecutionException, InterruptedException {
        // Arrange
        TenantContext.setTenantId("tenant123");

        // Act
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return TracingUtils.executeInSpan("async-operation", SpanKind.INTERNAL, () -> {
                return testService.asyncOperation();
            });
        });

        String result = future.get();

        // Assert
        assertEquals("async-result", result);
        
        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();
        assertTrue(spans.size() >= 1);
        
        boolean foundAsyncSpan = false;
        for (SpanData span : spans) {
            if ("async-operation".equals(span.getName())) {
                foundAsyncSpan = true;
                break;
            }
        }
        assertTrue(foundAsyncSpan, "Async operation span should be present");
    }

    @Test
    void testErrorTracing() {
        // Arrange

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            TracingUtils.executeInSpan("error-test", SpanKind.INTERNAL, () -> {
                return testService.errorOperation();
            });
        });
        
        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();
        assertTrue(spans.size() >= 1);
        
        boolean foundErrorSpan = false;
        for (SpanData span : spans) {
            if ("error-test".equals(span.getName())) {
                foundErrorSpan = true;
                assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, span.getStatus().getStatusCode());
                break;
            }
        }
        assertTrue(foundErrorSpan, "Error span should be present with error status");
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfiguration {
        
        @Bean
        @Primary
        public SpanExporter spanExporter() {
            return InMemorySpanExporter.create();
        }
        
        @Bean
        @Primary
        public OpenTelemetry openTelemetry(SpanExporter spanExporter) {
            Resource resource = Resource.getDefault()
                    .merge(Resource.create(Attributes.of(
                            ResourceAttributes.SERVICE_NAME, "test-service",
                            ResourceAttributes.SERVICE_VERSION, "1.0.0"
                    )));

            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                    .setResource(resource)
                    .setSampler(Sampler.traceIdRatioBased(1.0))
                    .build();

            return OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .build();
        }
        
        @Bean
        public TracingUtils tracingUtils(OpenTelemetry openTelemetry) {
            return new TracingUtils(openTelemetry);
        }
        
        @Bean
        public TracingAspect tracingAspect(OpenTelemetry openTelemetry) {
            return new TracingAspect(openTelemetry);
        }
        
        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    @Service
    static class TestService {
        
        @Traced(value = "business-operation", includeParameters = true, includeReturnValue = true)
        public String businessOperation(String input) {
            // Simulate some business logic
            TracingUtils.executeInSpan("data-processing", SpanKind.INTERNAL, () -> {
                // Simulate processing time
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            return "processed: " + input;
        }
        
        @Traced("async-operation")
        public String asyncOperation() {
            return "async-result";
        }
        
        @Traced("error-operation")
        public String errorOperation() {
            throw new RuntimeException("Simulated error");
        }
    }
}
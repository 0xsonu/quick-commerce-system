package com.ecommerce.shared.tracing.config;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TracingConfiguration.class)
@TestPropertySource(properties = {
    "spring.application.name=test-service",
    "tracing.enabled=true",
    "tracing.jaeger.endpoint=http://localhost:14250",
    "tracing.sampling.ratio=1.0"
})
class TracingConfigurationTest {

    @Test
    void testOpenTelemetryBeanCreation() {
        // Arrange
        TracingConfiguration config = new TracingConfiguration();
        
        // Use reflection to set private fields for testing
        try {
            var serviceNameField = TracingConfiguration.class.getDeclaredField("serviceName");
            serviceNameField.setAccessible(true);
            serviceNameField.set(config, "test-service");
            
            var jaegerEndpointField = TracingConfiguration.class.getDeclaredField("jaegerEndpoint");
            jaegerEndpointField.setAccessible(true);
            jaegerEndpointField.set(config, "http://localhost:14250");
            
            var samplingRatioField = TracingConfiguration.class.getDeclaredField("samplingRatio");
            samplingRatioField.setAccessible(true);
            samplingRatioField.set(config, 1.0);
            
            var exportTimeoutField = TracingConfiguration.class.getDeclaredField("exportTimeout");
            exportTimeoutField.setAccessible(true);
            exportTimeoutField.set(config, java.time.Duration.ofSeconds(30));
            
            var batchSizeField = TracingConfiguration.class.getDeclaredField("batchSize");
            batchSizeField.setAccessible(true);
            batchSizeField.set(config, 512);
            
            var batchDelayField = TracingConfiguration.class.getDeclaredField("batchDelay");
            batchDelayField.setAccessible(true);
            batchDelayField.set(config, java.time.Duration.ofSeconds(2));
            
        } catch (Exception e) {
            fail("Failed to set up test configuration: " + e.getMessage());
        }

        // Act
        OpenTelemetry openTelemetry = config.openTelemetry();

        // Assert
        assertNotNull(openTelemetry);
        assertNotNull(openTelemetry.getTracer("test-tracer"));
    }

    @Test
    void testSpanExporterCreation() {
        // Arrange
        TracingConfiguration config = new TracingConfiguration();
        
        try {
            var jaegerEndpointField = TracingConfiguration.class.getDeclaredField("jaegerEndpoint");
            jaegerEndpointField.setAccessible(true);
            jaegerEndpointField.set(config, "http://localhost:14250");
            
            var exportTimeoutField = TracingConfiguration.class.getDeclaredField("exportTimeout");
            exportTimeoutField.setAccessible(true);
            exportTimeoutField.set(config, java.time.Duration.ofSeconds(30));
            
        } catch (Exception e) {
            fail("Failed to set up test configuration: " + e.getMessage());
        }

        // Act
        var spanExporter = config.spanExporter();

        // Assert
        assertNotNull(spanExporter);
    }
}
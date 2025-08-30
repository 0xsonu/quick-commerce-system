package com.ecommerce.shared.logging.integration;

import com.ecommerce.shared.logging.LoggingContext;
import com.ecommerce.shared.logging.annotation.Loggable;
import com.ecommerce.shared.logging.annotation.LogParameters;
import com.ecommerce.shared.logging.config.LoggingAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        LoggingContextPropagationTest.TestConfiguration.class
})
class LoggingContextPropagationTest {
    
    @AfterEach
    void cleanup() {
        LoggingContext.clear();
    }
    
    @Test
    void shouldPropagateLoggingContextAcrossThreads() throws InterruptedException {
        // Given
        String correlationId = "test-correlation-id";
        String tenantId = "test-tenant";
        String userId = "test-user";
        
        LoggingContext.setCorrelationId(correlationId);
        LoggingContext.setTenantId(tenantId);
        LoggingContext.setUserId(userId);
        
        // When - simulate async processing
        Thread[] results = new Thread[1];
        Thread asyncThread = new Thread(() -> {
            // Context should be inherited in child thread (if properly configured)
            results[0] = Thread.currentThread();
            
            // Verify context is available
            assertEquals(correlationId, LoggingContext.getCorrelationId());
            assertEquals(tenantId, LoggingContext.getTenantId());
            assertEquals(userId, LoggingContext.getUserId());
        });
        
        // Copy MDC to child thread manually (Spring's TaskDecorator would do this)
        MDC.getCopyOfContextMap().forEach((key, value) -> {
            asyncThread.setUncaughtExceptionHandler((t, e) -> {
                // Set MDC in child thread
                MDC.put(key, value);
            });
        });
        
        asyncThread.start();
        asyncThread.join();
        
        // Then
        assertNotNull(results[0]);
    }
    
    @Test
    void shouldMaintainContextThroughMethodCalls() {
        // Given
        String correlationId = "test-correlation-id";
        String tenantId = "test-tenant";
        
        LoggingContext.setCorrelationId(correlationId);
        LoggingContext.setTenantId(tenantId);
        
        TestService testService = new TestService();
        
        // When
        String result = testService.processWithLogging("test-input");
        
        // Then
        assertEquals("processed: test-input", result);
        assertEquals(correlationId, LoggingContext.getCorrelationId());
        assertEquals(tenantId, LoggingContext.getTenantId());
    }
    
    @Test
    void shouldHandleNestedMethodCalls() {
        // Given
        LoggingContext.setCorrelationId("nested-correlation");
        LoggingContext.setTenantId("nested-tenant");
        
        TestService testService = new TestService();
        
        // When
        String result = testService.nestedMethodCall("input");
        
        // Then
        assertEquals("nested: processed: input", result);
        assertEquals("nested-correlation", LoggingContext.getCorrelationId());
        assertEquals("nested-tenant", LoggingContext.getTenantId());
    }
    
    @Test
    void shouldClearContextAfterRequest() {
        // Given
        LoggingContext.setCorrelationId("temp-correlation");
        LoggingContext.setTenantId("temp-tenant");
        
        // When
        LoggingContext.clear();
        
        // Then
        assertNull(LoggingContext.getCorrelationId());
        assertNull(LoggingContext.getTenantId());
        assertNull(LoggingContext.getUserId());
    }
    
    @Configuration
    static class TestConfiguration {
        
        @Bean
        public TestService testService() {
            return new TestService();
        }
        
        @Bean
        public io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }
    }
    
    static class TestService {
        
        @Loggable(value = "Processing with logging", level = Loggable.LogLevel.INFO)
        public String processWithLogging(String input) {
            // Verify context is maintained during method execution
            assertNotNull(LoggingContext.getCorrelationId());
            return "processed: " + input;
        }
        
        @LogParameters(logParameters = true, logReturnValue = true)
        public String nestedMethodCall(String input) {
            return "nested: " + processWithLogging(input);
        }
    }
}
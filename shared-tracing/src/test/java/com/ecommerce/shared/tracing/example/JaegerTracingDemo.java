package com.ecommerce.shared.tracing.example;

import com.ecommerce.shared.tracing.config.TracingConfiguration;
import com.ecommerce.shared.tracing.util.TracingUtils;
import com.ecommerce.shared.utils.TenantContext;
import io.opentelemetry.api.trace.SpanKind;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Demo application to show tracing working with Jaeger
 * Run this to send sample traces to Jaeger UI
 */
@SpringBootApplication
@Import(TracingConfiguration.class)
public class JaegerTracingDemo implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(JaegerTracingDemo.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Sending sample traces to Jaeger...");
        
        // Set tenant context
        TenantContext.setTenantId("demo-tenant");
        TenantContext.setUserId("demo-user");
        TenantContext.setCorrelationId("demo-correlation-123");
        
        try {
            // Create some sample traces
            TracingUtils.executeInSpan("demo-order-processing", SpanKind.INTERNAL, () -> {
                System.out.println("Processing order...");
                
                // Simulate user validation
                TracingUtils.executeInSpan("validate-user", SpanKind.INTERNAL, () -> {
                    simulateWork(100);
                    System.out.println("User validated");
                });
                
                // Simulate inventory check
                TracingUtils.executeInSpan("check-inventory", SpanKind.CLIENT, () -> {
                    simulateWork(200);
                    System.out.println("Inventory checked");
                });
                
                // Simulate payment processing
                TracingUtils.executeInSpan("process-payment", SpanKind.CLIENT, () -> {
                    simulateWork(300);
                    System.out.println("Payment processed");
                });
                
                System.out.println("Order processing complete");
            });
            
            // Create another trace for product search
            TracingUtils.executeInSpan("product-search", SpanKind.SERVER, () -> {
                System.out.println("Searching products...");
                
                TracingUtils.executeInSpan("database-query", SpanKind.CLIENT, () -> {
                    simulateWork(150);
                    System.out.println("Database query executed");
                });
                
                TracingUtils.executeInSpan("apply-filters", SpanKind.INTERNAL, () -> {
                    simulateWork(50);
                    System.out.println("Filters applied");
                });
                
                System.out.println("Product search complete");
            });
            
            System.out.println("Sample traces sent! Check Jaeger UI at http://localhost:16686");
            
        } finally {
            TenantContext.clear();
        }
        
        // Give some time for traces to be exported
        Thread.sleep(2000);
    }
    
    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
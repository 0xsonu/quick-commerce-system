package com.ecommerce.shared.logging.example;

import com.ecommerce.shared.logging.LoggingContext;
import com.ecommerce.shared.logging.annotation.Loggable;
import com.ecommerce.shared.logging.annotation.LogParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Example usage of the shared logging functionality
 */
@Service
public class LoggingUsageExample {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingUsageExample.class);
    
    /**
     * Example of manual logging context usage
     */
    public void manualLoggingExample() {
        // Set up logging context
        LoggingContext.setCorrelationId("example-correlation-123");
        LoggingContext.setTenantId("tenant-abc");
        LoggingContext.setUserId("user-456");
        
        try {
            logger.info("Processing user request");
            
            // Simulate some business logic
            processBusinessLogic();
            
            logger.info("Request processing completed successfully");
            
        } finally {
            // Clear context when done
            LoggingContext.clear();
        }
    }
    
    /**
     * Example of using @Loggable annotation for automatic execution time logging
     */
    @Loggable(value = "Business logic processing", level = Loggable.LogLevel.INFO)
    public String processBusinessLogic() {
        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return "Processing completed";
    }
    
    /**
     * Example of using @LogParameters annotation for parameter and return value logging
     */
    @LogParameters(logParameters = true, logReturnValue = true, logExceptions = true)
    public String processWithParameters(String input, int count, boolean flag) {
        logger.debug("Processing input with parameters");
        
        if ("error".equals(input)) {
            throw new RuntimeException("Simulated error for testing");
        }
        
        return String.format("Processed: %s (count: %d, flag: %s)", input, count, flag);
    }
    
    /**
     * Example of combined annotations
     */
    @Loggable(value = "Combined logging example", level = Loggable.LogLevel.DEBUG)
    @LogParameters(logParameters = true, logReturnValue = false)
    public void combinedAnnotationsExample(String data) {
        logger.info("Executing combined annotations example with data: {}", data);
        
        // Simulate some processing
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Example of nested method calls maintaining context
     */
    public void nestedCallsExample() {
        LoggingContext.setCorrelationId("nested-example-789");
        LoggingContext.setTenantId("tenant-xyz");
        
        logger.info("Starting nested calls example");
        
        try {
            String result = processBusinessLogic();
            String processed = processWithParameters(result, 5, true);
            combinedAnnotationsExample(processed);
            
            logger.info("Nested calls completed successfully");
            
        } finally {
            LoggingContext.clear();
        }
    }
    
    /**
     * Example of handling sensitive data
     */
    @LogParameters(logParameters = true, logReturnValue = false)
    public void handleSensitiveData(String username, String password, String token) {
        // The aspect will automatically sanitize sensitive parameters
        logger.info("Processing authentication request");
        
        // Business logic here
        logger.debug("Authentication processing completed");
    }
    
    /**
     * Example of error handling with logging context
     */
    @Loggable(value = "Error handling example", level = Loggable.LogLevel.WARN)
    public void errorHandlingExample() {
        LoggingContext.setCorrelationId("error-example-999");
        
        try {
            // Simulate an error
            processWithParameters("error", 1, false);
            
        } catch (Exception e) {
            logger.error("Error occurred during processing - CorrelationId: {}", 
                    LoggingContext.getCorrelationId(), e);
            
            // Re-throw or handle as needed
            throw new RuntimeException("Processing failed", e);
            
        } finally {
            LoggingContext.clear();
        }
    }
}
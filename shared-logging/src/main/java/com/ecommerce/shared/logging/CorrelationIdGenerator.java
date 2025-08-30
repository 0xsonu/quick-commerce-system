package com.ecommerce.shared.logging;

import java.util.UUID;

/**
 * Utility class for generating correlation IDs
 */
public class CorrelationIdGenerator {
    
    private static final String CORRELATION_ID_PREFIX = "corr-";
    
    /**
     * Generate a new correlation ID
     * @return A unique correlation ID
     */
    public static String generate() {
        return CORRELATION_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * Check if a string is a valid correlation ID format
     * @param correlationId The correlation ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String correlationId) {
        return correlationId != null && 
               correlationId.startsWith(CORRELATION_ID_PREFIX) && 
               correlationId.length() == CORRELATION_ID_PREFIX.length() + 16;
    }
}
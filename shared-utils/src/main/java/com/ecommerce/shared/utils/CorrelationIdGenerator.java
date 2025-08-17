package com.ecommerce.shared.utils;

import java.util.UUID;

/**
 * Utility class for generating correlation IDs
 */
public class CorrelationIdGenerator {
    
    private static final String PREFIX = "corr-";

    public static String generate() {
        return PREFIX + UUID.randomUUID().toString();
    }

    public static String generateShort() {
        return PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    public static boolean isValid(String correlationId) {
        return correlationId != null && 
               correlationId.startsWith(PREFIX) && 
               correlationId.length() > PREFIX.length();
    }
}
package com.ecommerce.shared.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdGeneratorTest {
    
    @Test
    void shouldGenerateValidCorrelationId() {
        String correlationId = CorrelationIdGenerator.generate();
        
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("corr-"));
        assertEquals(21, correlationId.length()); // "corr-" + 16 characters
        assertTrue(CorrelationIdGenerator.isValid(correlationId));
    }
    
    @Test
    void shouldGenerateUniqueCorrelationIds() {
        String id1 = CorrelationIdGenerator.generate();
        String id2 = CorrelationIdGenerator.generate();
        
        assertNotEquals(id1, id2);
    }
    
    @Test
    void shouldValidateCorrelationIdFormat() {
        // Valid correlation IDs
        assertTrue(CorrelationIdGenerator.isValid("corr-1234567890abcdef"));
        
        // Invalid correlation IDs
        assertFalse(CorrelationIdGenerator.isValid(null));
        assertFalse(CorrelationIdGenerator.isValid(""));
        assertFalse(CorrelationIdGenerator.isValid("invalid-format"));
        assertFalse(CorrelationIdGenerator.isValid("corr-short"));
        assertFalse(CorrelationIdGenerator.isValid("corr-toolongcorrelationid"));
        assertFalse(CorrelationIdGenerator.isValid("wrong-1234567890abcdef"));
    }
}
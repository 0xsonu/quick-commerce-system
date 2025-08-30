package com.ecommerce.shared.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class LoggingContextTest {
    
    @AfterEach
    void cleanup() {
        LoggingContext.clear();
    }
    
    @Test
    void shouldSetAndGetCorrelationId() {
        String correlationId = "test-correlation-id";
        
        LoggingContext.setCorrelationId(correlationId);
        
        assertEquals(correlationId, LoggingContext.getCorrelationId());
        assertEquals(correlationId, MDC.get(LoggingContext.CORRELATION_ID_KEY));
    }
    
    @Test
    void shouldSetAndGetTenantId() {
        String tenantId = "test-tenant";
        
        LoggingContext.setTenantId(tenantId);
        
        assertEquals(tenantId, LoggingContext.getTenantId());
        assertEquals(tenantId, MDC.get(LoggingContext.TENANT_ID_KEY));
    }
    
    @Test
    void shouldSetAndGetUserId() {
        String userId = "test-user";
        
        LoggingContext.setUserId(userId);
        
        assertEquals(userId, LoggingContext.getUserId());
        assertEquals(userId, MDC.get(LoggingContext.USER_ID_KEY));
    }
    
    @Test
    void shouldHandleNullValues() {
        LoggingContext.setCorrelationId(null);
        LoggingContext.setTenantId(null);
        LoggingContext.setUserId(null);
        
        assertNull(LoggingContext.getCorrelationId());
        assertNull(LoggingContext.getTenantId());
        assertNull(LoggingContext.getUserId());
    }
    
    @Test
    void shouldSetAndGetRequestStartTime() {
        long startTime = System.currentTimeMillis();
        
        LoggingContext.setRequestStartTime(startTime);
        
        assertEquals(startTime, LoggingContext.getRequestStartTime());
    }
    
    @Test
    void shouldSetServiceName() {
        String serviceName = "test-service";
        
        LoggingContext.setServiceName(serviceName);
        
        assertEquals(serviceName, MDC.get(LoggingContext.SERVICE_NAME_KEY));
    }
    
    @Test
    void shouldClearAllContext() {
        LoggingContext.setCorrelationId("test-correlation");
        LoggingContext.setTenantId("test-tenant");
        LoggingContext.setUserId("test-user");
        LoggingContext.setServiceName("test-service");
        
        LoggingContext.clear();
        
        assertNull(LoggingContext.getCorrelationId());
        assertNull(LoggingContext.getTenantId());
        assertNull(LoggingContext.getUserId());
        assertNull(MDC.get(LoggingContext.SERVICE_NAME_KEY));
    }
    
    @Test
    void shouldClearRequestContext() {
        LoggingContext.setRequestUri("/test/uri");
        LoggingContext.setRequestMethod("GET");
        LoggingContext.setRequestStartTime(System.currentTimeMillis());
        LoggingContext.setCorrelationId("test-correlation");
        
        LoggingContext.clearRequestContext();
        
        // Request context should be cleared
        assertNull(MDC.get(LoggingContext.REQUEST_URI_KEY));
        assertNull(MDC.get(LoggingContext.REQUEST_METHOD_KEY));
        assertNull(MDC.get(LoggingContext.REQUEST_START_TIME_KEY));
        
        // Other context should remain
        assertEquals("test-correlation", LoggingContext.getCorrelationId());
    }
    
    @Test
    void shouldEnsureCorrelationId() {
        // When no correlation ID exists
        String correlationId = LoggingContext.ensureCorrelationId();
        
        assertNotNull(correlationId);
        assertTrue(CorrelationIdGenerator.isValid(correlationId));
        assertEquals(correlationId, LoggingContext.getCorrelationId());
        
        // When correlation ID already exists
        String existingId = "existing-correlation-id";
        LoggingContext.setCorrelationId(existingId);
        
        String returnedId = LoggingContext.ensureCorrelationId();
        
        assertEquals(existingId, returnedId);
        assertEquals(existingId, LoggingContext.getCorrelationId());
    }
}
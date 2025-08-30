package com.ecommerce.shared.logging.grpc;

import com.ecommerce.shared.logging.LoggingContext;
import io.grpc.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoggingGrpcInterceptorTest {
    
    private LoggingGrpcInterceptor interceptor;
    
    @Mock
    private ServerCall<String, String> serverCall;
    
    @Mock
    private ServerCallHandler<String, String> serverCallHandler;
    
    @Mock
    private ServerCall.Listener<String> listener;
    
    @Mock
    private MethodDescriptor<String, String> methodDescriptor;
    
    private Metadata headers;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new LoggingGrpcInterceptor();
        headers = new Metadata();
        
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);
        when(methodDescriptor.getFullMethodName()).thenReturn("test.service/TestMethod");
        when(serverCallHandler.startCall(any(), any())).thenReturn(listener);
    }
    
    @AfterEach
    void cleanup() {
        LoggingContext.clear();
    }
    
    @Test
    void shouldExtractAndSetLoggingContextFromHeaders() {
        // Given
        String correlationId = "test-correlation-id";
        String tenantId = "test-tenant";
        String userId = "test-user";
        
        headers.put(Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER), correlationId);
        headers.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        headers.put(Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER), userId);
        
        // When
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // Then
        assertEquals(correlationId, LoggingContext.getCorrelationId());
        assertEquals(tenantId, LoggingContext.getTenantId());
        assertEquals(userId, LoggingContext.getUserId());
        assertNotNull(result);
    }
    
    @Test
    void shouldGenerateCorrelationIdWhenNotProvided() {
        // Given - no correlation ID in headers
        
        // When
        interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // Then
        assertNotNull(LoggingContext.getCorrelationId());
        assertTrue(LoggingContext.getCorrelationId().startsWith("corr-"));
    }
    
    @Test
    void shouldHandleNullHeaders() {
        // Given - empty headers (no values set)
        
        // When
        interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // Then
        assertNotNull(LoggingContext.getCorrelationId()); // Should be generated
        assertNull(LoggingContext.getTenantId());
        assertNull(LoggingContext.getUserId());
    }
    
    @Test
    void shouldClearContextOnComplete() {
        // Given
        String correlationId = "test-correlation-id";
        headers.put(Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER), correlationId);
        
        // When
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        assertEquals(correlationId, LoggingContext.getCorrelationId());
        
        // Simulate completion
        result.onComplete();
        
        // Then
        assertNull(LoggingContext.getCorrelationId());
    }
    
    @Test
    void shouldClearContextOnCancel() {
        // Given
        String correlationId = "test-correlation-id";
        headers.put(Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER), correlationId);
        
        // When
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        assertEquals(correlationId, LoggingContext.getCorrelationId());
        
        // Simulate cancellation
        result.onCancel();
        
        // Then
        assertNull(LoggingContext.getCorrelationId());
    }
}
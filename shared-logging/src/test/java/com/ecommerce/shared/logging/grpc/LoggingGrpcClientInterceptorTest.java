package com.ecommerce.shared.logging.grpc;

import com.ecommerce.shared.logging.LoggingContext;
import io.grpc.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoggingGrpcClientInterceptorTest {
    
    private LoggingGrpcClientInterceptor interceptor;
    
    @Mock
    private Channel channel;
    
    @Mock
    private ClientCall<String, String> clientCall;
    
    @Mock
    private MethodDescriptor<String, String> methodDescriptor;
    
    @Mock
    private ClientCall.Listener<String> responseListener;
    
    private CallOptions callOptions;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new LoggingGrpcClientInterceptor();
        callOptions = CallOptions.DEFAULT;
        
        when(channel.newCall(any(MethodDescriptor.class), any(CallOptions.class))).thenReturn(clientCall);
        when(methodDescriptor.getFullMethodName()).thenReturn("test.service/TestMethod");
    }
    
    @AfterEach
    void cleanup() {
        LoggingContext.clear();
    }
    
    @Test
    void shouldPropagateLoggingContextToHeaders() {
        // Given
        String correlationId = "test-correlation-id";
        String tenantId = "test-tenant";
        String userId = "test-user";
        
        LoggingContext.setCorrelationId(correlationId);
        LoggingContext.setTenantId(tenantId);
        LoggingContext.setUserId(userId);
        
        // When
        ClientCall<String, String> result = interceptor.interceptCall(methodDescriptor, callOptions, channel);
        
        // Capture the headers when start is called
        ArgumentCaptor<Metadata> headersCaptor = ArgumentCaptor.forClass(Metadata.class);
        result.start(responseListener, new Metadata());
        
        // Then
        verify(clientCall).start(any(), headersCaptor.capture());
        Metadata capturedHeaders = headersCaptor.getValue();
        
        assertEquals(correlationId, 
                capturedHeaders.get(Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER)));
        assertEquals(tenantId, 
                capturedHeaders.get(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER)));
        assertEquals(userId, 
                capturedHeaders.get(Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER)));
    }
    
    @Test
    void shouldHandleNullLoggingContext() {
        // Given - no logging context set
        
        // When
        ClientCall<String, String> result = interceptor.interceptCall(methodDescriptor, callOptions, channel);
        
        // Capture the headers when start is called
        ArgumentCaptor<Metadata> headersCaptor = ArgumentCaptor.forClass(Metadata.class);
        result.start(responseListener, new Metadata());
        
        // Then
        verify(clientCall).start(any(), headersCaptor.capture());
        Metadata capturedHeaders = headersCaptor.getValue();
        
        assertNull(capturedHeaders.get(Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER)));
        assertNull(capturedHeaders.get(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER)));
        assertNull(capturedHeaders.get(Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER)));
    }
    
    @Test
    void shouldOnlyPropagateNonNullValues() {
        // Given
        String correlationId = "test-correlation-id";
        LoggingContext.setCorrelationId(correlationId);
        // tenantId and userId are null
        
        // When
        ClientCall<String, String> result = interceptor.interceptCall(methodDescriptor, callOptions, channel);
        
        // Capture the headers when start is called
        ArgumentCaptor<Metadata> headersCaptor = ArgumentCaptor.forClass(Metadata.class);
        result.start(responseListener, new Metadata());
        
        // Then
        verify(clientCall).start(any(), headersCaptor.capture());
        Metadata capturedHeaders = headersCaptor.getValue();
        
        assertEquals(correlationId, 
                capturedHeaders.get(Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER)));
        assertNull(capturedHeaders.get(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER)));
        assertNull(capturedHeaders.get(Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER)));
    }
}
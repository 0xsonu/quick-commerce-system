package com.ecommerce.shared.logging.filter;

import com.ecommerce.shared.logging.LoggingContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private MeterRegistry meterRegistry;
    private LoggingFilter loggingFilter;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        loggingFilter = new LoggingFilter(meterRegistry);
    }
    
    @AfterEach
    void cleanup() {
        LoggingContext.clear();
    }
    
    @Test
    void shouldSetupLoggingContextFromHeaders() throws ServletException, IOException {
        // Given
        String correlationId = "test-correlation-id";
        String tenantId = "test-tenant";
        String userId = "test-user";
        String requestUri = "/api/test";
        String requestMethod = "GET";
        
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);
        when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
        when(request.getHeader("X-User-ID")).thenReturn(userId);
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getMethod()).thenReturn(requestMethod);
        when(response.getStatus()).thenReturn(200);
        
        // When
        loggingFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        // Context should be cleared after request
        assertNull(LoggingContext.getCorrelationId());
    }
    
    @Test
    void shouldGenerateCorrelationIdWhenNotProvided() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant");
        when(request.getHeader("X-User-ID")).thenReturn("test-user");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(response.getStatus()).thenReturn(200);
        
        // Capture the correlation ID during filter execution
        final String[] capturedCorrelationId = new String[1];
        doAnswer(invocation -> {
            capturedCorrelationId[0] = LoggingContext.getCorrelationId();
            return null;
        }).when(filterChain).doFilter(request, response);
        
        // When
        loggingFilter.doFilter(request, response, filterChain);
        
        // Then
        assertNotNull(capturedCorrelationId[0]);
        assertTrue(capturedCorrelationId[0].startsWith("corr-"));
    }
    
    @Test
    void shouldClearContextAfterRequest() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant");
        when(request.getHeader("X-User-ID")).thenReturn("test-user");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(response.getStatus()).thenReturn(200);
        
        // When
        loggingFilter.doFilter(request, response, filterChain);
        
        // Then
        assertNull(LoggingContext.getCorrelationId());
        assertNull(LoggingContext.getTenantId());
        assertNull(LoggingContext.getUserId());
    }
    
    @Test
    void shouldClearContextEvenWhenExceptionOccurs() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        
        doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);
        
        // When & Then
        assertThrows(ServletException.class, () -> 
            loggingFilter.doFilter(request, response, filterChain));
        
        // Context should still be cleared
        assertNull(LoggingContext.getCorrelationId());
    }
    
    @Test
    void shouldHandleNonHttpRequests() throws ServletException, IOException {
        // Given - non-HTTP servlet request/response
        jakarta.servlet.ServletRequest nonHttpRequest = mock(jakarta.servlet.ServletRequest.class);
        jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);
        
        // When
        loggingFilter.doFilter(nonHttpRequest, nonHttpResponse, filterChain);
        
        // Then
        verify(filterChain).doFilter(nonHttpRequest, nonHttpResponse);
    }
    
    @Test
    void shouldLogRequestWithQueryParameters() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant");
        when(request.getHeader("X-User-ID")).thenReturn("test-user");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn("param1=value1&param2=value2");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent/1.0");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);
        when(response.getHeader("Content-Length")).thenReturn("1024");
        
        // When
        loggingFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void shouldExtractClientIpFromXForwardedFor() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant");
        when(request.getHeader("X-User-ID")).thenReturn("test-user");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent/1.0");
        when(response.getStatus()).thenReturn(200);
        
        // When
        loggingFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void shouldExtractClientIpFromXRealIp() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant");
        when(request.getHeader("X-User-ID")).thenReturn("test-user");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.100");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent/1.0");
        when(response.getStatus()).thenReturn(200);
        
        // When
        loggingFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void shouldFallbackToRemoteAddrForClientIp() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation");
        when(request.getHeader("X-Tenant-ID")).thenReturn("test-tenant");
        when(request.getHeader("X-User-ID")).thenReturn("test-user");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent/1.0");
        when(response.getStatus()).thenReturn(200);
        
        // When
        loggingFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
    }
}
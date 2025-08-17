package com.ecommerce.shared.security;

import com.ecommerce.shared.utils.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantContextFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TenantContextFilter tenantContextFilter;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    void shouldExtractContextFromJwtToken() throws ServletException, IOException {
        // Given
        String token = "Bearer valid.jwt.token";
        String userId = "user123";
        String tenantId = "tenant456";

        when(request.getHeader("Authorization")).thenReturn(token);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(jwtTokenProvider.getTenantIdFromToken("valid.jwt.token")).thenReturn(tenantId);
        when(jwtTokenProvider.getUserIdFromToken("valid.jwt.token")).thenReturn(userId);

        // When
        tenantContextFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-Correlation-ID"), anyString());
        
        // Context should be cleared after processing
        assertNull(TenantContext.getTenantId());
        assertNull(TenantContext.getUserId());
    }

    @Test
    void shouldFallbackToHeaders() throws ServletException, IOException {
        // Given
        String tenantId = "tenant456";
        String userId = "user123";

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Tenant-ID")).thenReturn(tenantId);
        when(request.getHeader("X-User-ID")).thenReturn(userId);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        // When
        tenantContextFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-Correlation-ID"), anyString());
    }

    @Test
    void shouldUseExistingCorrelationId() throws ServletException, IOException {
        // Given
        String correlationId = "existing-correlation-id";

        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);

        // When
        tenantContextFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).setHeader("X-Correlation-ID", correlationId);
    }

    @Test
    void shouldHandleInvalidJwtToken() throws ServletException, IOException {
        // Given
        String token = "Bearer invalid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn(token);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(jwtTokenProvider.getTenantIdFromToken("invalid.jwt.token")).thenThrow(new RuntimeException("Invalid token"));

        // When
        tenantContextFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq("X-Correlation-ID"), anyString());
        // Should continue processing even with invalid token
    }

    @Test
    void shouldAlwaysClearContextAfterProcessing() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            tenantContextFilter.doFilterInternal(request, response, filterChain);
        });

        // Context should still be cleared even when exception occurs
        assertNull(TenantContext.getTenantId());
        assertNull(TenantContext.getUserId());
        assertNull(TenantContext.getCorrelationId());
    }
}
package com.ecommerce.shared.security;

import com.ecommerce.shared.utils.CorrelationIdGenerator;
import com.ecommerce.shared.utils.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to extract and set tenant context from JWT token or headers
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            extractAndSetContext(request, response);
            filterChain.doFilter(request, response);
        } finally {
            // Clean up context after request processing
            TenantContext.clear();
            MDC.clear();
        }
    }

    private void extractAndSetContext(HttpServletRequest request, HttpServletResponse response) {
        // Extract correlation ID or generate new one
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = CorrelationIdGenerator.generate();
        }
        TenantContext.setCorrelationId(correlationId);
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // Try to extract from JWT token first
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                String tenantId = jwtTokenProvider.getTenantIdFromToken(token);
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                
                if (StringUtils.hasText(tenantId)) {
                    TenantContext.setTenantId(tenantId);
                    MDC.put("tenantId", tenantId);
                }
                
                if (StringUtils.hasText(userId)) {
                    TenantContext.setUserId(userId);
                    MDC.put("userId", userId);
                }
            } catch (Exception e) {
                // Token validation failed, continue without setting context
                logger.debug("Failed to extract context from JWT token", e);
            }
        }

        // Fallback to headers if not extracted from token
        if (!TenantContext.hasTenantId()) {
            String tenantId = request.getHeader(TENANT_ID_HEADER);
            if (StringUtils.hasText(tenantId)) {
                TenantContext.setTenantId(tenantId);
                MDC.put("tenantId", tenantId);
            }
        }

        if (!TenantContext.hasUserId()) {
            String userId = request.getHeader(USER_ID_HEADER);
            if (StringUtils.hasText(userId)) {
                TenantContext.setUserId(userId);
                MDC.put("userId", userId);
            }
        }
    }
}
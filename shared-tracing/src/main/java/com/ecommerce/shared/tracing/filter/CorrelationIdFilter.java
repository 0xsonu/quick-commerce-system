package com.ecommerce.shared.tracing.filter;

import com.ecommerce.shared.tracing.util.TracingUtils;
import com.ecommerce.shared.utils.CorrelationIdGenerator;
import com.ecommerce.shared.utils.TenantContext;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to handle correlation ID propagation and tracing context
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String SPAN_ID_HEADER = "X-Span-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = CorrelationIdGenerator.generate();
            }

            // Extract tenant and user context
            String tenantId = httpRequest.getHeader(TENANT_ID_HEADER);
            String userId = httpRequest.getHeader(USER_ID_HEADER);

            // Set context
            TenantContext.setCorrelationId(correlationId);
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            if (userId != null) {
                TenantContext.setUserId(userId);
            }

            // Set MDC for logging
            MDC.put("correlationId", correlationId);
            if (tenantId != null) {
                MDC.put("tenantId", tenantId);
            }
            if (userId != null) {
                MDC.put("userId", userId);
            }

            // Add tracing context to MDC and response headers
            String traceId = TracingUtils.getCurrentTraceId();
            String spanId = TracingUtils.getCurrentSpanId();
            
            if (traceId != null) {
                MDC.put("traceId", traceId);
                httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            }
            if (spanId != null) {
                MDC.put("spanId", spanId);
                httpResponse.setHeader(SPAN_ID_HEADER, spanId);
            }

            // Add correlation ID to response
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Add tenant context to current span
            TracingUtils.addTenantContextToCurrentSpan();

            logger.debug("Processing request with correlationId: {}, tenantId: {}, userId: {}, traceId: {}", 
                        correlationId, tenantId, userId, traceId);

            chain.doFilter(request, response);

        } finally {
            // Clean up context
            TenantContext.clear();
            MDC.clear();
        }
    }
}
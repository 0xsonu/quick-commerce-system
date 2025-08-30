package com.ecommerce.shared.logging.filter;

import com.ecommerce.shared.logging.CorrelationIdGenerator;
import com.ecommerce.shared.logging.LoggingContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter to handle logging context setup and request/response logging
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    
    private final MeterRegistry meterRegistry;
    private final Timer requestTimer;
    
    public LoggingFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestTimer = Timer.builder("http.server.requests")
                .description("HTTP request duration")
                .register(meterRegistry);
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest httpRequest) || 
            !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            setupLoggingContext(httpRequest, startTime);
            logRequestStart(httpRequest);
            
            Timer.Sample sample = Timer.start(meterRegistry);
            
            chain.doFilter(request, response);
            
            sample.stop(Timer.builder("http.server.requests")
                    .tag("method", httpRequest.getMethod())
                    .tag("uri", getUriTemplate(httpRequest))
                    .tag("status", String.valueOf(httpResponse.getStatus()))
                    .register(meterRegistry));
            
            logRequestEnd(httpRequest, httpResponse, startTime);
            
        } finally {
            LoggingContext.clear();
        }
    }
    
    private void setupLoggingContext(HttpServletRequest request, long startTime) {
        // Set correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = CorrelationIdGenerator.generate();
        }
        LoggingContext.setCorrelationId(correlationId);
        
        // Set tenant ID
        String tenantId = request.getHeader(TENANT_ID_HEADER);
        LoggingContext.setTenantId(tenantId);
        
        // Set user ID
        String userId = request.getHeader(USER_ID_HEADER);
        LoggingContext.setUserId(userId);
        
        // Set request context
        LoggingContext.setRequestUri(request.getRequestURI());
        LoggingContext.setRequestMethod(request.getMethod());
        LoggingContext.setRequestStartTime(startTime);
    }
    
    private void logRequestStart(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String fullUri = request.getRequestURI() + (queryString != null ? "?" + queryString : "");
        
        logger.info("Request started: {} {} - User-Agent: {} - Remote-Addr: {}", 
                request.getMethod(), 
                fullUri,
                request.getHeader("User-Agent"),
                getClientIpAddress(request));
    }
    
    private void logRequestEnd(HttpServletRequest request, HttpServletResponse response, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        String queryString = request.getQueryString();
        String fullUri = request.getRequestURI() + (queryString != null ? "?" + queryString : "");
        
        // Log performance metrics
        if (duration > 5000) { // Log slow requests (>5s) as warnings
            logger.warn("Slow request completed: {} {} - Status: {} - Duration: {}ms - Content-Length: {}",
                    request.getMethod(),
                    fullUri,
                    response.getStatus(),
                    duration,
                    response.getHeader("Content-Length"));
        } else if (duration > 1000) { // Log medium requests (>1s) as info
            logger.info("Request completed: {} {} - Status: {} - Duration: {}ms - Content-Length: {}",
                    request.getMethod(),
                    fullUri,
                    response.getStatus(),
                    duration,
                    response.getHeader("Content-Length"));
        } else { // Log fast requests as debug
            logger.debug("Request completed: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(),
                    fullUri,
                    response.getStatus(),
                    duration);
        }
        
        // Log error responses
        if (response.getStatus() >= 400) {
            logger.error("Request failed: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(),
                    fullUri,
                    response.getStatus(),
                    duration);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String getUriTemplate(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Simple URI template extraction - replace IDs with placeholders
        return uri.replaceAll("/\\d+", "/{id}")
                 .replaceAll("/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "/{uuid}");
    }
}
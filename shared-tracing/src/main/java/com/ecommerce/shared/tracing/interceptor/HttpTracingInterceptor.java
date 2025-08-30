package com.ecommerce.shared.tracing.interceptor;

import com.ecommerce.shared.tracing.util.TracingUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP interceptor for automatic request tracing
 */
@Component
public class HttpTracingInterceptor implements HandlerInterceptor {

    private static final String SPAN_ATTRIBUTE = "tracing.span";
    private static final String SCOPE_ATTRIBUTE = "tracing.scope";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String spanName = request.getMethod() + " " + request.getRequestURI();
        Span span = TracingUtils.createSpan(spanName, SpanKind.SERVER);
        
        // Add HTTP context
        TracingUtils.addHttpContext(span, request.getMethod(), request.getRequestURL().toString(), 0);
        
        // Add additional HTTP attributes
        span.setAttribute("http.scheme", request.getScheme());
        span.setAttribute("http.host", request.getServerName());
        span.setAttribute("http.target", request.getRequestURI());
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            span.setAttribute("http.user_agent", TracingUtils.sanitizeValue(userAgent));
        }
        
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null) {
            span.setAttribute("http.client_ip", remoteAddr);
        }

        Scope scope = span.makeCurrent();
        
        // Store span and scope in request attributes for cleanup
        request.setAttribute(SPAN_ATTRIBUTE, span);
        request.setAttribute(SCOPE_ATTRIBUTE, scope);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        Span span = (Span) request.getAttribute(SPAN_ATTRIBUTE);
        Scope scope = (Scope) request.getAttribute(SCOPE_ATTRIBUTE);
        
        if (span != null) {
            try {
                // Update span with response information
                span.setAttribute("http.status_code", response.getStatus());
                
                if (ex != null) {
                    span.setStatus(StatusCode.ERROR, ex.getMessage());
                    span.recordException(ex);
                } else if (response.getStatus() >= 400) {
                    span.setStatus(StatusCode.ERROR, "HTTP " + response.getStatus());
                } else {
                    span.setStatus(StatusCode.OK);
                }
                
            } finally {
                span.end();
            }
        }
        
        if (scope != null) {
            scope.close();
        }
    }
}
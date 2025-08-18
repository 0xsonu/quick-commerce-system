package com.ecommerce.gateway.filter;

import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Filter for logging requests and responses with correlation IDs and performance metrics
 */
@Component
public class RequestResponseLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    
    // Paths to exclude from detailed logging
    private static final List<String> EXCLUDED_PATHS = List.of(
        "/actuator/health",
        "/actuator/metrics",
        "/actuator/prometheus"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        
        // Skip logging for excluded paths
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        long startTime = Instant.now().toEpochMilli();
        String correlationId = request.getHeaders().getFirst("X-Correlation-ID");
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        // Log incoming request
        logger.info("Incoming request: {} {} from tenant: {}, user: {}, correlationId: {}, userAgent: {}, remoteAddr: {}",
            method, path, tenantId, userId, correlationId,
            request.getHeaders().getFirst("User-Agent"),
            getClientIp(request));

        return chain.filter(exchange)
            .doOnSuccess(aVoid -> {
                long duration = Instant.now().toEpochMilli() - startTime;
                ServerHttpResponse response = exchange.getResponse();
                
                logger.info("Outgoing response: {} {} -> {} in {}ms, correlationId: {}, contentLength: {}",
                    method, path, response.getStatusCode(), duration, correlationId,
                    response.getHeaders().getContentLength());
                
                // Log slow requests
                if (duration > 1000) {
                    logger.warn("Slow request detected: {} {} took {}ms, correlationId: {}",
                        method, path, duration, correlationId);
                }
            })
            .doOnError(error -> {
                long duration = Instant.now().toEpochMilli() - startTime;
                logger.error("Request failed: {} {} in {}ms, correlationId: {}, error: {}",
                    method, path, duration, correlationId, error.getMessage(), error);
            });
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null 
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    @Override
    public int getOrder() {
        return 0; // Run after authentication and rate limiting
    }
}
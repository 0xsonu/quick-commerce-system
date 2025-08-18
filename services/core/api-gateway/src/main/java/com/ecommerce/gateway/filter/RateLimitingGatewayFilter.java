package com.ecommerce.gateway.filter;

import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Rate limiting filter using Redis for per-tenant rate limiting
 */
@Component
public class RateLimitingGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingGatewayFilter.class);
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 1000;
    private static final int PREMIUM_REQUESTS_PER_MINUTE = 5000;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    // Paths that are exempt from rate limiting
    private static final List<String> EXEMPT_PATHS = List.of(
        "/actuator/health",
        "/actuator/info"
    );

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip rate limiting for exempt paths
        if (isExemptPath(path)) {
            return chain.filter(exchange);
        }

        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // For unauthenticated requests, use IP-based rate limiting
            tenantId = "ip:" + getClientIp(exchange);
        }

        final String finalTenantId = tenantId; // Make it effectively final for lambda
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + tenantId;
        int requestLimit = getRequestLimit(tenantId);

        return checkRateLimit(rateLimitKey, requestLimit)
            .flatMap(allowed -> {
                if (allowed) {
                    logger.debug("Request allowed for tenant: {}", finalTenantId);
                    return chain.filter(exchange);
                } else {
                    logger.warn("Rate limit exceeded for tenant: {}", finalTenantId);
                    return rateLimitExceededResponse(exchange.getResponse());
                }
            })
            .onErrorResume(error -> {
                logger.error("Error checking rate limit for tenant: {}", finalTenantId, error);
                // On Redis error, allow the request to proceed
                return chain.filter(exchange);
            });
    }

    private boolean isExemptPath(String path) {
        return EXEMPT_PATHS.stream().anyMatch(path::startsWith);
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRequest().getRemoteAddress() != null 
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    private int getRequestLimit(String tenantId) {
        // In a real implementation, this would come from tenant configuration
        // For now, use simple logic based on tenant ID
        if (tenantId.startsWith("premium_") || tenantId.startsWith("enterprise_")) {
            return PREMIUM_REQUESTS_PER_MINUTE;
        }
        return DEFAULT_REQUESTS_PER_MINUTE;
    }

    private Mono<Boolean> checkRateLimit(String key, int limit) {
        return redisTemplate.opsForValue()
            .increment(key)
            .flatMap(currentCount -> {
                if (currentCount == 1) {
                    // First request in the window, set expiration
                    return redisTemplate.expire(key, WINDOW_DURATION)
                        .thenReturn(true);
                } else if (currentCount <= limit) {
                    return Mono.just(true);
                } else {
                    return Mono.just(false);
                }
            });
    }

    private Mono<Void> rateLimitExceededResponse(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(DEFAULT_REQUESTS_PER_MINUTE));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("Retry-After", "60");
        
        String body = "{\"error\":\"Rate Limit Exceeded\",\"message\":\"Too many requests. Please try again later.\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -50; // Run after authentication but before other filters
    }
}
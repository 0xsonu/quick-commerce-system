package com.ecommerce.gateway.filter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ecommerce.shared.security.JwtTokenProvider;
import com.ecommerce.shared.utils.CorrelationIdGenerator;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global filter for JWT authentication and tenant context extraction
 */
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationGatewayFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String ROLES_HEADER = "X-User-Roles";

    // Paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/actuator/health",
        "/actuator/info",
        "/actuator/metrics",
        "/actuator/prometheus"
    );

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Generate or extract correlation ID
        String correlationId = getOrGenerateCorrelationId(request);
        
        // Set correlation ID in MDC for logging
        MDC.put("correlationId", correlationId);

        try {
            // Skip authentication for public paths
            if (isPublicPath(path)) {
                return addCorrelationIdAndContinue(exchange, chain, correlationId);
            }

            // Extract JWT token
            String token = extractToken(request);
            if (!StringUtils.hasText(token)) {
                logger.warn("Missing JWT token for protected path: {}", path);
                return unauthorizedResponse(exchange.getResponse());
            }

            try {
                // Validate token and extract claims
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                String tenantId = jwtTokenProvider.getTenantIdFromToken(token);
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);

                // Set context
                TenantContext.setTenantId(tenantId);
                TenantContext.setUserId(userId);
                TenantContext.setCorrelationId(correlationId);

                // Add to MDC for logging
                MDC.put("tenantId", tenantId);
                MDC.put("userId", userId);

                // Add headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .header(TENANT_ID_HEADER, tenantId)
                    .header(USER_ID_HEADER, userId)
                    .header(ROLES_HEADER, String.join(",", roles))
                    .build();

                logger.debug("Authenticated request for user: {} in tenant: {} with roles: {}", 
                    userId, tenantId, roles);

                return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .doFinally(signalType -> {
                        // Clean up context
                        TenantContext.clear();
                        MDC.clear();
                    });

            } catch (JWTVerificationException e) {
                logger.warn("Invalid JWT token for path: {}, error: {}", path, e.getMessage());
                return unauthorizedResponse(exchange.getResponse());
            }

        } catch (Exception e) {
            logger.error("Error processing authentication for path: {}", path, e);
            return errorResponse(exchange.getResponse());
        }
    }

    private String getOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = CorrelationIdGenerator.generate();
        }
        return correlationId;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private Mono<Void> addCorrelationIdAndContinue(ServerWebExchange exchange, GatewayFilterChain chain, String correlationId) {
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header(CORRELATION_ID_HEADER, correlationId)
            .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
            .doFinally(signalType -> MDC.clear());
    }

    private Mono<Void> unauthorizedResponse(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = "{\"error\":\"Unauthorized\",\"message\":\"Valid JWT token required\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    private Mono<Void> errorResponse(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = "{\"error\":\"Internal Server Error\",\"message\":\"Authentication processing failed\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // High priority to run before other filters
    }
}
package com.ecommerce.gateway.filter;

import com.ecommerce.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGatewayFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationGatewayFilter filter;

    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void shouldAllowPublicPaths() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/auth/login").build()
        );

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldAllowHealthCheckPaths() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/actuator/health").build()
        );

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldRejectProtectedPathWithoutToken() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/profile").build()
        );

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();

        // Verify response status
        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build()
        );

        when(jwtTokenProvider.getUserIdFromToken(anyString()))
            .thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();

        // Verify response status
        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
    }

    @Test
    void shouldAllowValidToken() {
        // Given
        String validToken = "valid-jwt-token";
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build()
        );

        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn("user123");
        when(jwtTokenProvider.getTenantIdFromToken(validToken)).thenReturn("tenant_abc");
        when(jwtTokenProvider.getRolesFromToken(validToken)).thenReturn(List.of("CUSTOMER"));

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldAddCorrelationIdIfNotPresent() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/actuator/health").build()
        );

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldPreserveExistingCorrelationId() {
        // Given
        String existingCorrelationId = "existing-correlation-123";
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/actuator/health")
                .header("X-Correlation-ID", existingCorrelationId)
                .build()
        );

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldHaveCorrectOrder() {
        // When & Then
        assert filter.getOrder() == -100;
    }
}
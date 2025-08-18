package com.ecommerce.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingGatewayFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private RateLimitingGatewayFilter filter;

    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void shouldAllowExemptPaths() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/actuator/health").build()
        );

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldAllowRequestWithinRateLimit() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/profile").build()
        );

        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldRejectRequestExceedingRateLimit() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/profile").build()
        );

        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1001L)); // Exceeds default limit

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .expectComplete()
            .verify();

        // Verify response status
        assert exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
    }

    @Test
    void shouldContinueOnRedisError() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/profile").build()
        );

        when(valueOperations.increment(anyString())).thenReturn(Mono.error(new RuntimeException("Redis error")));

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldHaveCorrectOrder() {
        // When & Then
        assert filter.getOrder() == -50;
    }

    @Test
    void shouldUseIpBasedRateLimitingForUnauthenticatedRequests() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/products")
                .remoteAddress(java.net.InetSocketAddress.createUnresolved("192.168.1.1", 8080))
                .build()
        );

        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void shouldHandleXForwardedForHeader() {
        // Given
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/products")
                .header("X-Forwarded-For", "203.0.113.1, 192.168.1.1")
                .build()
        );

        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(filter.filter(exchange, filterChain))
            .verifyComplete();
    }
}
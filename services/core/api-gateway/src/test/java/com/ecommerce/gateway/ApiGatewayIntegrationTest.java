package com.ecommerce.gateway;

import com.ecommerce.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String validToken;

    @BeforeEach
    void setUp() {
        // Generate a valid JWT token for testing
        validToken = jwtTokenProvider.createAccessToken("user123", "tenant_abc", List.of("CUSTOMER"));
    }

    @Test
    void shouldAllowAccessToPublicHealthEndpoint() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void shouldAllowAccessToPublicAuthLoginEndpoint() {
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .exchange()
            .expectStatus().is5xxServerError(); // Service not running, but should pass gateway
    }

    @Test
    void shouldRejectRequestWithoutTokenForProtectedEndpoint() {
        webTestClient.get()
            .uri("/api/v1/users/profile")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.message").isEqualTo("Valid JWT token required");
    }

    @Test
    void shouldRejectRequestWithInvalidToken() {
        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer invalid-token")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    void shouldAllowRequestWithValidToken() {
        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer " + validToken)
            .exchange()
            .expectStatus().is5xxServerError() // Service not running, but should pass authentication
            .expectHeader().exists("X-Correlation-ID")
            .expectHeader().valueEquals("X-Tenant-ID", "tenant_abc")
            .expectHeader().valueEquals("X-User-ID", "user123");
    }

    @Test
    void shouldAddCorrelationIdToRequest() {
        String correlationId = "test-correlation-123";
        
        webTestClient.get()
            .uri("/actuator/health")
            .header("X-Correlation-ID", correlationId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("X-Correlation-ID", correlationId);
    }

    @Test
    void shouldGenerateCorrelationIdIfNotProvided() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Correlation-ID");
    }

    @Test
    void shouldReturnCircuitBreakerFallbackWhenServiceUnavailable() {
        // This test would require the actual service to be down
        // For now, we'll test that the fallback endpoints exist
        webTestClient.get()
            .uri("/fallback/auth")
            .exchange()
            .expectStatus().isEqualTo(503)
            .expectBody()
            .jsonPath("$.error").isEqualTo("AUTH_SERVICE_UNAVAILABLE")
            .jsonPath("$.message").isEqualTo("Authentication service is temporarily unavailable");
    }

    @Test
    void shouldReturnUserServiceFallback() {
        webTestClient.get()
            .uri("/fallback/user")
            .exchange()
            .expectStatus().isEqualTo(503)
            .expectBody()
            .jsonPath("$.error").isEqualTo("USER_SERVICE_UNAVAILABLE")
            .jsonPath("$.message").isEqualTo("User service is temporarily unavailable");
    }

    @Test
    void shouldReturnProductServiceFallback() {
        webTestClient.get()
            .uri("/fallback/product")
            .exchange()
            .expectStatus().isEqualTo(503)
            .expectBody()
            .jsonPath("$.error").isEqualTo("PRODUCT_SERVICE_UNAVAILABLE")
            .jsonPath("$.message").isEqualTo("Product service is temporarily unavailable");
    }
}
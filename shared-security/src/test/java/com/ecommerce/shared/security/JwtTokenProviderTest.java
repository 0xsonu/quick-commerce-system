package com.ecommerce.shared.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 3600L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 86400L);
        ReflectionTestUtils.setField(jwtTokenProvider, "issuer", "test-issuer");
    }

    @Test
    void shouldCreateAccessToken() {
        // Given
        String userId = "user123";
        String tenantId = "tenant456";
        List<String> roles = Arrays.asList("CUSTOMER", "ADMIN");

        // When
        String token = jwtTokenProvider.createAccessToken(userId, tenantId, roles);

        // Then
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void shouldCreateRefreshToken() {
        // Given
        String userId = "user123";
        String tenantId = "tenant456";

        // When
        String token = jwtTokenProvider.createRefreshToken(userId, tenantId);

        // Then
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void shouldValidateValidToken() {
        // Given
        String userId = "user123";
        String tenantId = "tenant456";
        List<String> roles = Arrays.asList("CUSTOMER");
        String token = jwtTokenProvider.createAccessToken(userId, tenantId, roles);

        // When
        DecodedJWT decodedJWT = jwtTokenProvider.validateToken(token);

        // Then
        assertNotNull(decodedJWT);
        assertEquals(userId, decodedJWT.getSubject());
        assertEquals(tenantId, decodedJWT.getClaim("tenant_id").asString());
        assertEquals("access", decodedJWT.getClaim("token_type").asString());
    }

    @Test
    void shouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(JWTVerificationException.class, () -> {
            jwtTokenProvider.validateToken(invalidToken);
        });
    }

    @Test
    void shouldExtractUserIdFromToken() {
        // Given
        String userId = "user123";
        String tenantId = "tenant456";
        List<String> roles = Arrays.asList("CUSTOMER");
        String token = jwtTokenProvider.createAccessToken(userId, tenantId, roles);

        // When
        String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    void shouldExtractTenantIdFromToken() {
        // Given
        String userId = "user123";
        String tenantId = "tenant456";
        List<String> roles = Arrays.asList("CUSTOMER");
        String token = jwtTokenProvider.createAccessToken(userId, tenantId, roles);

        // When
        String extractedTenantId = jwtTokenProvider.getTenantIdFromToken(token);

        // Then
        assertEquals(tenantId, extractedTenantId);
    }

    @Test
    void shouldExtractRolesFromToken() {
        // Given
        String userId = "user123";
        String tenantId = "tenant456";
        List<String> roles = Arrays.asList("CUSTOMER", "ADMIN");
        String token = jwtTokenProvider.createAccessToken(userId, tenantId, roles);

        // When
        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertEquals(roles, extractedRoles);
    }

    @Test
    void shouldExtractTokenTypeFromToken() {
        // Given
        String userId = "user123";
        String tenantId = "tenant456";
        List<String> roles = Arrays.asList("CUSTOMER");
        String token = jwtTokenProvider.createAccessToken(userId, tenantId, roles);

        // When
        String tokenType = jwtTokenProvider.getTokenTypeFromToken(token);

        // Then
        assertEquals("access", tokenType);
    }

    @Test
    void shouldDetectExpiredToken() {
        // Given - create provider with very short expiration
        JwtTokenProvider shortExpiryProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortExpiryProvider, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(shortExpiryProvider, "accessTokenExpiration", -1L); // Already expired
        ReflectionTestUtils.setField(shortExpiryProvider, "issuer", "test-issuer");

        String userId = "user123";
        String tenantId = "tenant456";
        List<String> roles = Arrays.asList("CUSTOMER");
        String token = shortExpiryProvider.createAccessToken(userId, tenantId, roles);

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertTrue(isExpired);
    }

    @Test
    void shouldDetectValidNonExpiredToken() {
        // Given
        String userId = "user123";
        String tenantId = "tenant456";
        List<String> roles = Arrays.asList("CUSTOMER");
        String token = jwtTokenProvider.createAccessToken(userId, tenantId, roles);

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }
}
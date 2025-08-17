package com.ecommerce.shared.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationProviderTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private DecodedJWT decodedJWT;

    @InjectMocks
    private JwtAuthenticationProvider authenticationProvider;

    private String testToken;
    private String userId;
    private String tenantId;
    private List<String> roles;

    @BeforeEach
    void setUp() {
        testToken = "test.jwt.token";
        userId = "user123";
        tenantId = "tenant456";
        roles = Arrays.asList("CUSTOMER", "ADMIN");
    }

    @Test
    void shouldAuthenticateValidAccessToken() {
        // Given
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(testToken);
        
        when(jwtTokenProvider.validateToken(testToken)).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn(userId);
        when(decodedJWT.getClaim("tenant_id")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
        when(decodedJWT.getClaim("tenant_id").asString()).thenReturn(tenantId);
        when(decodedJWT.getClaim("roles")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
        when(decodedJWT.getClaim("roles").asList(String.class)).thenReturn(roles);
        when(decodedJWT.getClaim("token_type")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
        when(decodedJWT.getClaim("token_type").asString()).thenReturn("access");

        // When
        Authentication result = authenticationProvider.authenticate(authToken);

        // Then
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertTrue(result instanceof JwtAuthenticationToken);
        
        JwtAuthenticationToken jwtResult = (JwtAuthenticationToken) result;
        assertEquals(userId, jwtResult.getUserId());
        assertEquals(tenantId, jwtResult.getTenantId());
        assertEquals(testToken, jwtResult.getToken());
        
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER")));
        assertTrue(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void shouldRejectRefreshToken() {
        // Given
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(testToken);
        
        when(jwtTokenProvider.validateToken(testToken)).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn(userId);
        when(decodedJWT.getClaim("tenant_id")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
        when(decodedJWT.getClaim("tenant_id").asString()).thenReturn(tenantId);
        when(decodedJWT.getClaim("roles")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
        when(decodedJWT.getClaim("roles").asList(String.class)).thenReturn(roles);
        when(decodedJWT.getClaim("token_type")).thenReturn(mock(com.auth0.jwt.interfaces.Claim.class));
        when(decodedJWT.getClaim("token_type").asString()).thenReturn("refresh");

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(authToken);
        });
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(testToken);
        
        when(jwtTokenProvider.validateToken(testToken)).thenThrow(new JWTVerificationException("Invalid token"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(authToken);
        });
    }

    @Test
    void shouldSupportJwtAuthenticationToken() {
        // When
        boolean supports = authenticationProvider.supports(JwtAuthenticationToken.class);

        // Then
        assertTrue(supports);
    }

    @Test
    void shouldNotSupportOtherAuthenticationTypes() {
        // When
        boolean supports = authenticationProvider.supports(Authentication.class);

        // Then
        assertFalse(supports);
    }
}
package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.UserAuth;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.UserAuthRepository;
import com.ecommerce.auth.util.PasswordUtil;
import com.ecommerce.shared.security.JwtTokenProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthenticationService authenticationService;

    private UserAuth testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
            userAuthRepository,
            refreshTokenRepository,
            passwordUtil,
            jwtTokenProvider
        );

        // Set private fields using reflection
        ReflectionTestUtils.setField(authenticationService, "accessTokenExpiration", 3600L);
        ReflectionTestUtils.setField(authenticationService, "refreshTokenExpiration", 86400L);

        // Setup test data
        testUser = new UserAuth();
        testUser.setId(1L);
        testUser.setTenantId("tenant1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRoles(Arrays.asList(Role.CUSTOMER));
        testUser.setIsActive(true);
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("plainPassword");
        loginRequest.setTenantId("tenant1");
    }

    @Test
    void testLogin_ValidCredentials_ShouldReturnLoginResponse() {
        // Arrange
        when(userAuthRepository.findActiveUserByTenantIdAndUsernameOrEmail("tenant1", "testuser"))
            .thenReturn(Optional.of(testUser));
        when(passwordUtil.verifyPassword("plainPassword", "hashedPassword"))
            .thenReturn(true);
        when(jwtTokenProvider.createAccessToken(eq("1"), eq("tenant1"), any()))
            .thenReturn("accessToken");
        when(jwtTokenProvider.createRefreshToken("1", "tenant1"))
            .thenReturn("refreshToken");

        // Act
        LoginResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("1", response.getUserId());
        assertEquals("tenant1", response.getTenantId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(Arrays.asList(Role.CUSTOMER), response.getRoles());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testLogin_UserNotFound_ShouldThrowException() {
        // Arrange
        when(userAuthRepository.findActiveUserByTenantIdAndUsernameOrEmail("tenant1", "testuser"))
            .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.login(loginRequest);
        });

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void testLogin_AccountLocked_ShouldThrowException() {
        // Arrange
        testUser.setAccountLocked(true);
        when(userAuthRepository.findActiveUserByTenantIdAndUsernameOrEmail("tenant1", "testuser"))
            .thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.login(loginRequest);
        });

        assertEquals("Account is locked due to too many failed login attempts", exception.getMessage());
    }

    @Test
    void testLogin_InvalidPassword_ShouldThrowException() {
        // Arrange
        when(userAuthRepository.findActiveUserByTenantIdAndUsernameOrEmail("tenant1", "testuser"))
            .thenReturn(Optional.of(testUser));
        when(passwordUtil.verifyPassword("plainPassword", "hashedPassword"))
            .thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.login(loginRequest);
        });

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userAuthRepository).save(testUser);
        assertEquals(1, testUser.getFailedLoginAttempts());
    }

    @Test
    void testLogin_ResetFailedAttemptsOnSuccess_ShouldResetCounter() {
        // Arrange
        testUser.setFailedLoginAttempts(3);
        when(userAuthRepository.findActiveUserByTenantIdAndUsernameOrEmail("tenant1", "testuser"))
            .thenReturn(Optional.of(testUser));
        when(passwordUtil.verifyPassword("plainPassword", "hashedPassword"))
            .thenReturn(true);
        when(jwtTokenProvider.createAccessToken(eq("1"), eq("tenant1"), any()))
            .thenReturn("accessToken");
        when(jwtTokenProvider.createRefreshToken("1", "tenant1"))
            .thenReturn("refreshToken");

        // Act
        authenticationService.login(loginRequest);

        // Assert
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertFalse(testUser.getAccountLocked());
        verify(userAuthRepository).save(testUser);
    }

    @Test
    void testRefreshToken_ValidToken_ShouldReturnTokenResponse() {
        // Arrange
        String refreshToken = createTestRefreshToken("1", "tenant1");
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUserId(1L);
        storedToken.setTokenHash("hashedToken");
        storedToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        storedToken.setIsRevoked(false);

        when(jwtTokenProvider.validateToken(refreshToken))
            .thenReturn(JWT.decode(refreshToken));
        when(refreshTokenRepository.findValidTokenByHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(Optional.of(storedToken));
        when(userAuthRepository.findById(1L))
            .thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.createAccessToken(eq("1"), eq("tenant1"), any()))
            .thenReturn("newAccessToken");

        // Act
        TokenResponse response = authenticationService.refreshToken(request);

        // Assert
        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("1", response.getUserId());
        assertEquals("tenant1", response.getTenantId());
    }

    @Test
    void testRefreshToken_InvalidTokenType_ShouldThrowException() {
        // Arrange
        String accessToken = createTestAccessToken("1", "tenant1");
        RefreshTokenRequest request = new RefreshTokenRequest(accessToken);

        when(jwtTokenProvider.validateToken(accessToken))
            .thenReturn(JWT.decode(accessToken));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.refreshToken(request);
        });

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void testValidateToken_ValidToken_ShouldReturnValidResponse() {
        // Arrange
        String accessToken = createTestAccessToken("1", "tenant1");
        TokenValidationRequest request = new TokenValidationRequest(accessToken);

        when(jwtTokenProvider.validateToken(accessToken))
            .thenReturn(JWT.decode(accessToken));
        when(userAuthRepository.findById(1L))
            .thenReturn(Optional.of(testUser));

        // Act
        TokenValidationResponse response = authenticationService.validateToken(request);

        // Assert
        assertTrue(response.isValid());
        assertEquals("1", response.getUserId());
        assertEquals("tenant1", response.getTenantId());
        assertEquals("testuser", response.getUsername());
        assertEquals(Arrays.asList(Role.CUSTOMER), response.getRoles());
    }

    @Test
    void testValidateToken_InvalidTokenType_ShouldReturnInvalidResponse() {
        // Arrange
        String refreshToken = createTestRefreshToken("1", "tenant1");
        TokenValidationRequest request = new TokenValidationRequest(refreshToken);

        when(jwtTokenProvider.validateToken(refreshToken))
            .thenReturn(JWT.decode(refreshToken));

        // Act
        TokenValidationResponse response = authenticationService.validateToken(request);

        // Assert
        assertFalse(response.isValid());
        assertEquals("Invalid token type", response.getErrorMessage());
    }

    @Test
    void testValidateToken_UserNotFound_ShouldReturnInvalidResponse() {
        // Arrange
        String accessToken = createTestAccessToken("1", "tenant1");
        TokenValidationRequest request = new TokenValidationRequest(accessToken);

        when(jwtTokenProvider.validateToken(accessToken))
            .thenReturn(JWT.decode(accessToken));
        when(userAuthRepository.findById(1L))
            .thenReturn(Optional.empty());

        // Act
        TokenValidationResponse response = authenticationService.validateToken(request);

        // Assert
        assertFalse(response.isValid());
        assertEquals("User account is inactive or locked", response.getErrorMessage());
    }

    @Test
    void testLogout_ShouldRevokeTokens() {
        // Act
        authenticationService.logout("1");

        // Assert
        verify(refreshTokenRepository).revokeAllTokensForUser(1L);
    }

    @Test
    void testCleanupExpiredTokens_ShouldDeleteExpiredTokens() {
        // Act
        authenticationService.cleanupExpiredTokens();

        // Assert
        verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteRevokedTokensOlderThan(any(LocalDateTime.class));
    }

    private String createTestAccessToken(String userId, String tenantId) {
        return JWT.create()
            .withSubject(userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("roles", Arrays.asList("CUSTOMER"))
            .withClaim("token_type", "access")
            .sign(Algorithm.HMAC256("test-secret"));
    }

    private String createTestRefreshToken(String userId, String tenantId) {
        return JWT.create()
            .withSubject(userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("token_type", "refresh")
            .sign(Algorithm.HMAC256("test-secret"));
    }
}
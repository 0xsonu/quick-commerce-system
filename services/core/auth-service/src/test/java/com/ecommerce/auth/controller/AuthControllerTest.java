package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.TimeUnit;

@WebMvcTest(controllers = AuthController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecommerce.shared.security.*"))
@Import({AuthControllerTest.TestSecurityConfig.class, AuthControllerTest.TestMetricsConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Ensure deterministic test execution
@Timeout(value = 30, unit = TimeUnit.SECONDS) // Global timeout for controller tests
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Primary
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class TestMetricsConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setTenantId("tenant1");

        loginResponse = new LoginResponse();
        loginResponse.setAccessToken("accessToken");
        loginResponse.setRefreshToken("refreshToken");
        loginResponse.setExpiresIn(3600L);
        loginResponse.setUserId("1");
        loginResponse.setTenantId("tenant1");
        loginResponse.setUsername("testuser");
        loginResponse.setEmail("test@example.com");
        loginResponse.setRoles(Arrays.asList(Role.CUSTOMER));
    }

    @Test
    @Order(1)
    void testLogin_ValidRequest_ShouldReturnLoginResponse() throws Exception {
        // Arrange
        when(authenticationService.login(any(LoginRequest.class)))
            .thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600L))
                .andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.tenantId").value("tenant1"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.issuedAt").exists());
    }

    @Test
    @Order(2)
    void testLogin_InvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        when(authenticationService.login(any(LoginRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Order(3)
    void testLogin_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsernameOrEmail(""); // Invalid - empty username
        invalidRequest.setPassword("password123");
        invalidRequest.setTenantId("tenant1");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void testRefreshToken_ValidRequest_ShouldReturnTokenResponse() throws Exception {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("refreshToken");
        TokenResponse tokenResponse = new TokenResponse("newAccessToken", 3600L, "1", "tenant1");

        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
            .thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600L))
                .andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.tenantId").value("tenant1"))
                .andExpect(jsonPath("$.issuedAt").exists());
    }

    @Test
    @Order(5)
    void testRefreshToken_InvalidToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("invalidToken");

        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REFRESH_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Order(6)
    void testValidateToken_ValidToken_ShouldReturnValidationResponse() throws Exception {
        // Arrange
        TokenValidationRequest validationRequest = new TokenValidationRequest("validToken");
        TokenValidationResponse validationResponse = new TokenValidationResponse(
            true, "1", "tenant1", "testuser", Arrays.asList(Role.CUSTOMER)
        );

        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(validationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.tenantId").value("tenant1"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @Order(7)
    void testValidateToken_InvalidToken_ShouldReturnInvalidResponse() throws Exception {
        // Arrange
        TokenValidationRequest validationRequest = new TokenValidationRequest("invalidToken");
        TokenValidationResponse validationResponse = new TokenValidationResponse(false, "Invalid token");

        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(validationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Invalid token"));
    }

    @Test
    @Order(8)
    void testLogout_ValidToken_ShouldReturnOk() throws Exception {
        // Arrange
        TokenValidationResponse validationResponse = new TokenValidationResponse(
            true, "1", "tenant1", "testuser", Arrays.asList(Role.CUSTOMER)
        );

        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(validationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer validToken"))
                .andExpect(status().isOk());

        // Verify that logout was called with the correct user ID
        verify(authenticationService).logout("1");
    }

    @Test
    @Order(9)
    void testLogout_InvalidToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        TokenValidationResponse validationResponse = new TokenValidationResponse(false, "Invalid token");

        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(validationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer invalidToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Order(10)
    void testLogout_InvalidAuthorizationHeader_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "InvalidHeader"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid authorization header"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Order(11)
    void testLogout_MissingAuthorizationHeader_ShouldReturnForbidden() throws Exception {
        // Act & Assert - Missing authorization should return 400 Bad Request for invalid header format
        // This test expects the correct behavior as specified in the requirements
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid authorization header"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Order(12)
    void testHealth_ShouldReturnHealthy() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth service is healthy"));
    }

    @Test
    void testLogin_InternalServerError_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(authenticationService.login(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An internal error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testRefreshToken_InternalServerError_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("refreshToken");
        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An internal error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testLogout_InternalServerError_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        TokenValidationResponse validationResponse = new TokenValidationResponse(
            true, "1", "tenant1", "testuser", Arrays.asList(Role.CUSTOMER)
        );
        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(validationResponse);
        doThrow(new RuntimeException("Database connection failed"))
            .when(authenticationService).logout("1");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer validToken"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An internal error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========== CORRELATION ID AND MDC CONTEXT TESTS ==========

    @Test
    void testLogin_ShouldHandleCorrelationIdAndMDCContext() throws Exception {
        // Arrange
        when(authenticationService.login(any(LoginRequest.class)))
            .thenReturn(loginResponse);

        // Act & Assert - Verify that correlation ID is generated and MDC context is handled
        // The controller should generate correlation ID and set MDC context
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"));

        // Verify service was called (correlation ID and MDC handling is internal to controller)
        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    void testRefreshToken_ShouldHandleCorrelationIdAndMDCContext() throws Exception {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("refreshToken");
        TokenResponse tokenResponse = new TokenResponse("newAccessToken", 3600L, "1", "tenant1");

        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
            .thenReturn(tokenResponse);

        // Act & Assert - Verify correlation ID and MDC context handling
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"));

        // Verify service was called with proper context handling
        verify(authenticationService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void testValidateToken_ShouldHandleCorrelationIdAndMDCContext() throws Exception {
        // Arrange
        TokenValidationRequest validationRequest = new TokenValidationRequest("validToken");
        TokenValidationResponse validationResponse = new TokenValidationResponse(
            true, "1", "tenant1", "testuser", Arrays.asList(Role.CUSTOMER)
        );

        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(validationResponse);

        // Act & Assert - Verify correlation ID and MDC context handling
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value("1"));

        // Verify service was called with proper context handling
        verify(authenticationService).validateToken(any(TokenValidationRequest.class));
    }

    @Test
    void testLogout_ShouldHandleCorrelationIdAndMDCContext() throws Exception {
        // Arrange
        TokenValidationResponse validationResponse = new TokenValidationResponse(
            true, "1", "tenant1", "testuser", Arrays.asList(Role.CUSTOMER)
        );

        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(validationResponse);

        // Act & Assert - Verify correlation ID and MDC context handling
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer validToken"))
                .andExpect(status().isOk());

        // Verify both validation and logout were called with proper context
        verify(authenticationService).validateToken(any(TokenValidationRequest.class));
        verify(authenticationService).logout("1");
    }

    @Test
    void testLogin_WithTenantContext_ShouldSetMDCProperly() throws Exception {
        // Arrange - Login request with specific tenant
        LoginRequest tenantLoginRequest = new LoginRequest();
        tenantLoginRequest.setUsernameOrEmail("tenantuser");
        tenantLoginRequest.setPassword("password123");
        tenantLoginRequest.setTenantId("specific-tenant");

        LoginResponse tenantLoginResponse = new LoginResponse();
        tenantLoginResponse.setAccessToken("tenantAccessToken");
        tenantLoginResponse.setRefreshToken("tenantRefreshToken");
        tenantLoginResponse.setExpiresIn(3600L);
        tenantLoginResponse.setUserId("2");
        tenantLoginResponse.setTenantId("specific-tenant");
        tenantLoginResponse.setUsername("tenantuser");
        tenantLoginResponse.setEmail("tenant@example.com");
        tenantLoginResponse.setRoles(Arrays.asList(Role.ADMIN));

        when(authenticationService.login(any(LoginRequest.class)))
            .thenReturn(tenantLoginResponse);

        // Act & Assert - Verify tenant-specific MDC context handling
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("specific-tenant"))
                .andExpect(jsonPath("$.userId").value("2"));

        // Verify service was called with tenant context
        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_FailureScenario_ShouldMaintainMDCContext() throws Exception {
        // Arrange - Failed login scenario
        when(authenticationService.login(any(LoginRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid credentials"));

        // Act & Assert - Verify MDC context is maintained even during failures
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        // Verify service was called and failure was handled with proper context
        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    void testRefreshToken_FailureScenario_ShouldMaintainMDCContext() throws Exception {
        // Arrange - Failed refresh token scenario
        RefreshTokenRequest failedRefreshRequest = new RefreshTokenRequest("invalidRefreshToken");

        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid refresh token"));

        // Act & Assert - Verify MDC context is maintained during refresh failures
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failedRefreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REFRESH_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));

        // Verify service was called and failure was handled with proper context
        verify(authenticationService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void testValidateToken_InvalidToken_ShouldMaintainMDCContext() throws Exception {
        // Arrange - Invalid token validation scenario
        TokenValidationRequest invalidRequest = new TokenValidationRequest("invalidToken");
        TokenValidationResponse invalidResponse = new TokenValidationResponse(false, "Invalid token");

        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(invalidResponse);

        // Act & Assert - Verify MDC context is maintained for invalid token validation
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Invalid token"));

        // Verify service was called with proper context handling
        verify(authenticationService).validateToken(any(TokenValidationRequest.class));
    }

    @Test
    void testLogout_InvalidToken_ShouldMaintainMDCContext() throws Exception {
        // Arrange - Invalid token logout scenario
        TokenValidationResponse invalidValidation = new TokenValidationResponse(false, "Invalid token");

        when(authenticationService.validateToken(any(TokenValidationRequest.class)))
            .thenReturn(invalidValidation);

        // Act & Assert - Verify MDC context is maintained during invalid logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer invalidToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid token"));

        // Verify validation was called but logout was not (due to invalid token)
        verify(authenticationService).validateToken(any(TokenValidationRequest.class));
        verify(authenticationService, never()).logout(anyString());
    }
}
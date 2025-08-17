package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;

    @Configuration
    static class TestConfig {
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
                .andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.tenantId").value("tenant1"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
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
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
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
                .andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.tenantId").value("tenant1"));
    }

    @Test
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
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
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
    }

    @Test
    void testLogout_InvalidAuthorizationHeader_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "InvalidHeader"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void testLogout_MissingAuthorizationHeader_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void testHealth_ShouldReturnHealthy() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth service is healthy"));
    }
}
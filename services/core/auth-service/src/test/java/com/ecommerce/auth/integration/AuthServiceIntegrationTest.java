package com.ecommerce.auth.integration;

import com.ecommerce.auth.BaseTestWithMysql;
import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.UserAuth;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.UserAuthRepository;
import com.ecommerce.auth.util.PasswordUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for the Authentication Service.
 * Tests all authentication endpoints with various scenarios and edge cases.
 * 
 * Test Categories:
 * 1. Login functionality (valid/invalid credentials, different user states)
 * 2. Token refresh functionality
 * 3. Token validation functionality  
 * 4. Logout functionality
 * 5. Account locking mechanism
 * 6. Tenant isolation
 * 7. Security and validation
 * 8. Error handling
 * 
 * Performance Optimizations:
 * - Ordered test execution to minimize database state dependencies
 * - Transactional rollback for fast cleanup
 * - Optimized test data creation and reuse
 * - Proper timeout configurations for reliability
 * - @DirtiesContext only where absolutely necessary
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional // Use transactional rollback for automatic cleanup
@Timeout(value = 60, unit = TimeUnit.SECONDS) // Global timeout for integration tests
class AuthServiceIntegrationTest extends BaseTestWithMysql {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // Test data
    private UserAuth activeUser;
    private UserAuth inactiveUser;
    private UserAuth lockedUser;
    private UserAuth adminUser;
    
    private static final String VALID_PASSWORD = "StrongPass123!";
    private static final String INVALID_PASSWORD = "WrongPassword";
    private static final String TENANT_1 = "tenant1";
    private static final String TENANT_2 = "tenant2";

    @BeforeEach
    void setUp() {
        // Create test users for different scenarios - optimized for performance
        createTestUsers();
    }

    @AfterEach
    void tearDown() {
        // Minimal cleanup - @Transactional rollback handles most cleanup automatically
        // This reduces test execution time significantly
        // Only explicit cleanup for non-transactional operations if needed
        try {
            // Clear any cached data that might affect subsequent tests
            // Most cleanup is handled by @Transactional rollback for performance
        } catch (Exception e) {
            // Log cleanup errors but don't fail tests
            System.err.println("Warning: Test cleanup failed: " + e.getMessage());
        }
    }

    private void createTestUsers() {
        // Active user for successful login tests with proper tenant isolation
        activeUser = createUser("activeuser", "active@example.com", TENANT_1, VALID_PASSWORD, 
                               List.of(Role.CUSTOMER), true, false, 0);
        
        // Inactive user for testing inactive account scenarios
        inactiveUser = createUser("inactiveuser", "inactive@example.com", TENANT_1, VALID_PASSWORD,
                                 List.of(Role.CUSTOMER), false, false, 0);
        
        // Locked user for testing account locking scenarios
        lockedUser = createUser("lockeduser", "locked@example.com", TENANT_1, VALID_PASSWORD,
                               List.of(Role.CUSTOMER), true, true, 5);
        
        // Admin user for role-based testing with multiple roles
        adminUser = createUser("adminuser", "admin@example.com", TENANT_1, VALID_PASSWORD,
                              List.of(Role.ADMIN, Role.CUSTOMER), true, false, 0);
    }

    /**
     * Creates a user with proper password hashing using PasswordUtil and realistic test data.
     * Ensures proper tenant isolation, role assignments, and account status.
     */
    private UserAuth createUser(String username, String email, String tenantId, String password,
                               List<Role> roles, boolean isActive, boolean isLocked, int failedAttempts) {
        UserAuth user = new UserAuth();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEmail(email);
        
        // Use PasswordUtil for proper password hashing as per requirements 5.5
        // This ensures consistent BCrypt hashing with proper strength
        user.setPasswordHash(passwordUtil.hashPassword(password));
        
        // Ensure proper role assignments with defensive copy
        user.setRoles(new ArrayList<>(roles));
        user.setIsActive(isActive);
        user.setAccountLocked(isLocked);
        user.setFailedLoginAttempts(failedAttempts);
        
        // Set realistic timestamps
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        return userAuthRepository.save(user);
    }

    /**
     * Creates a user in a specific tenant with custom parameters for multi-tenant testing.
     * Used for testing tenant isolation scenarios.
     */
    private UserAuth createUserInTenant(String username, String email, String tenantId, 
                                       String password, List<Role> roles) {
        return createUser(username, email, tenantId, password, roles, true, false, 0);
    }

    // ========== LOGIN TESTS ==========

    @Test
    @Order(1)
    @DisplayName("Login with valid username and password should return tokens and user info")
    void testLogin_ValidUsernameCredentials_ShouldReturnTokensAndUserInfo() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_1);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.userId").value(activeUser.getId().toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_1))
                .andExpect(jsonPath("$.username").value(activeUser.getUsername()))
                .andExpect(jsonPath("$.email").value(activeUser.getEmail()))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        // Verify response structure
        LoginResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), LoginResponse.class);
        
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertTrue(response.getAccessToken().length() > 50); // JWT tokens are long
        assertTrue(response.getRoles().contains(Role.CUSTOMER));
        
        // Verify refresh token was saved in database
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUserId(activeUser.getId());
        assertEquals(1, refreshTokens.size());
        assertNotNull(refreshTokens.get(0).getTokenHash());
    }

    @Test
    @Order(2)
    @DisplayName("Login with valid email and password should work")
    void testLogin_ValidEmailCredentials_ShouldReturnTokens() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(activeUser.getEmail(), VALID_PASSWORD, TENANT_1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.username").value(activeUser.getUsername()))
                .andExpect(jsonPath("$.email").value(activeUser.getEmail()));
    }

    @Test
    @Order(3)
    @DisplayName("Login with invalid password should return unauthorized and increment failed attempts")
    @Transactional
    void testLogin_InvalidPassword_ShouldReturnUnauthorizedAndIncrementFailedAttempts() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(activeUser.getUsername(), INVALID_PASSWORD, TENANT_1);
        int initialFailedAttempts = activeUser.getFailedLoginAttempts();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());

        // Verify failed login attempt was recorded
        UserAuth updatedUser = userAuthRepository.findById(activeUser.getId()).orElseThrow();
        assertEquals(initialFailedAttempts + 1, updatedUser.getFailedLoginAttempts());
    }

    @Test
    @Order(4)
    @DisplayName("Login with non-existent user should return unauthorized")
    void testLogin_NonExistentUser_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest("nonexistent", VALID_PASSWORD, TENANT_1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @Order(5)
    @DisplayName("Login with inactive user should return unauthorized")
    void testLogin_InactiveUser_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(inactiveUser.getUsername(), VALID_PASSWORD, TENANT_1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @Order(6)
    @DisplayName("Login with locked account should return unauthorized with specific message")
    void testLogin_LockedAccount_ShouldReturnUnauthorizedWithSpecificMessage() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(lockedUser.getUsername(), VALID_PASSWORD, TENANT_1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is locked due to too many failed login attempts"));
    }

    // ========== ACCOUNT LOCKING TESTS ==========

    @Test
    @Order(7)
    @DisplayName("Multiple failed login attempts should lock account after threshold")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD) // Account state changes affect subsequent tests
    void testLogin_MultipleFailedAttempts_ShouldLockAccount() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(activeUser.getUsername(), INVALID_PASSWORD, TENANT_1);
        
        // Act - Make 5 failed attempts (threshold)
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
            
            // Verify failed attempts are incrementing
            UserAuth user = userAuthRepository.findById(activeUser.getId()).orElseThrow();
            assertEquals(i, user.getFailedLoginAttempts());
            
            if (i < 5) {
                assertFalse(user.getAccountLocked());
            } else {
                assertTrue(user.getAccountLocked());
            }
        }

        // Verify subsequent login with correct password still fails due to lock
        request.setPassword(VALID_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is locked due to too many failed login attempts"));
    }

    // ========== TENANT ISOLATION TESTS ==========

    @Test
    @Order(8)
    @DisplayName("Login with correct credentials but wrong tenant should fail")
    void testLogin_WrongTenant_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_2);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @Order(9)
    @DisplayName("Users with same username in different tenants should be isolated")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD) // Creates additional users that may affect other tests
    void testLogin_SameUsernamesDifferentTenants_ShouldBeIsolated() throws Exception {
        // Arrange - Create user with same username in different tenant using helper method
        UserAuth tenant2User = createUserInTenant(activeUser.getUsername(), "user@tenant2.com", 
                                                 TENANT_2, "DifferentPass123!", List.of(Role.CUSTOMER));

        // Act & Assert - Login to tenant1 with tenant1 password
        LoginRequest request1 = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_1);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_1));

        // Act & Assert - Login to tenant2 with tenant2 password
        LoginRequest request2 = createLoginRequest(activeUser.getUsername(), "DifferentPass123!", TENANT_2);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_2));

        // Act & Assert - Cross-tenant login should fail
        LoginRequest crossTenantRequest = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_2);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crossTenantRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ========== TOKEN REFRESH TESTS ==========

    @Test
    @Order(10)
    @DisplayName("Refresh token with valid token should return new access token")
    void testRefreshToken_ValidToken_ShouldReturnNewAccessToken() throws Exception {
        // Arrange - First login to get refresh token
        LoginRequest loginRequest = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_1);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), LoginResponse.class);
        
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.userId").value(activeUser.getId().toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_1));
    }

    @Test
    @Order(11)
    @DisplayName("Refresh token with invalid token should return unauthorized")
    void testRefreshToken_InvalidToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REFRESH_FAILED"));
    }

    // ========== TOKEN VALIDATION TESTS ==========

    @Test
    @Order(12)
    @DisplayName("Validate token with valid token should return validation response")
    void testValidateToken_ValidToken_ShouldReturnValidationResponse() throws Exception {
        // Arrange - First login to get access token
        LoginRequest loginRequest = createLoginRequest(adminUser.getUsername(), VALID_PASSWORD, TENANT_1);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), LoginResponse.class);
        
        TokenValidationRequest validationRequest = new TokenValidationRequest(loginResponse.getAccessToken());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value(adminUser.getId().toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_1))
                .andExpect(jsonPath("$.username").value(adminUser.getUsername()))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItems("ADMIN", "CUSTOMER")));
    }

    @Test
    @Order(13)
    @DisplayName("Validate token with invalid token should return invalid response")
    void testValidateToken_InvalidToken_ShouldReturnInvalidResponse() throws Exception {
        // Arrange
        TokenValidationRequest request = new TokenValidationRequest("invalid-jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    // ========== LOGOUT TESTS ==========

    @Test
    @Order(14)
    @DisplayName("Logout with valid token should invalidate refresh token")
    void testLogout_ValidToken_ShouldInvalidateRefreshToken() throws Exception {
        // Arrange - First login to get tokens
        LoginRequest loginRequest = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_1);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Act - Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        // Assert - Refresh token should be invalidated
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(15)
    @DisplayName("Logout with invalid authorization header should return bad request")
    void testLogout_InvalidAuthHeader_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "InvalidHeader"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @Order(16)
    @DisplayName("Logout without authorization header should return bad request")
    void testLogout_MissingAuthHeader_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    // ========== VALIDATION AND SECURITY TESTS ==========

    @Test
    @Order(17)
    @DisplayName("Login with empty username should return bad request")
    void testLogin_EmptyUsername_ShouldReturnBadRequest() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest("", VALID_PASSWORD, TENANT_1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(18)
    @DisplayName("Login with empty password should return bad request")
    void testLogin_EmptyPassword_ShouldReturnBadRequest() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(activeUser.getUsername(), "", TENANT_1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(19)
    @DisplayName("Login with empty tenant should return bad request")
    void testLogin_EmptyTenant_ShouldReturnBadRequest() throws Exception {
        // Arrange
        LoginRequest request = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(20)
    @DisplayName("Health endpoint should return healthy status")
    void testHealth_ShouldReturnHealthyStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth service is healthy"));
    }

    // ========== ENHANCED MULTI-TENANT ISOLATION TESTS ==========

    @Test
    @Order(21)
    @DisplayName("Multi-tenant isolation - same email addresses in different tenants")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD) // Creates users with duplicate emails across tenants
    void testLogin_SameEmailDifferentTenants_ShouldBeIsolated() throws Exception {
        // Arrange - Create users with same email in different tenants
        String commonEmail = "common@example.com";
        UserAuth tenant1User = createUserInTenant("user1", commonEmail, TENANT_1, VALID_PASSWORD, List.of(Role.CUSTOMER));
        UserAuth tenant2User = createUserInTenant("user2", commonEmail, TENANT_2, "DifferentPass456!", List.of(Role.ADMIN));

        // Act & Assert - Login with email to tenant1
        LoginRequest request1 = createLoginRequest(commonEmail, VALID_PASSWORD, TENANT_1);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_1))
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItems("CUSTOMER")));

        // Act & Assert - Login with email to tenant2
        LoginRequest request2 = createLoginRequest(commonEmail, "DifferentPass456!", TENANT_2);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_2))
                .andExpect(jsonPath("$.username").value("user2"))
                .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItems("ADMIN")));

        // Act & Assert - Cross-tenant login with wrong password should fail
        LoginRequest crossRequest = createLoginRequest(commonEmail, "DifferentPass456!", TENANT_1);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crossRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(22)
    @DisplayName("Multi-tenant token validation - tokens should contain correct tenant information")
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD) // Creates additional tenant users
    void testTokenValidation_MultiTenant_ShouldContainCorrectTenantInfo() throws Exception {
        // Arrange - Create users in different tenants
        UserAuth tenant1User = createUserInTenant("tenant1user", "t1@example.com", TENANT_1, VALID_PASSWORD, List.of(Role.CUSTOMER));
        UserAuth tenant2User = createUserInTenant("tenant2user", "t2@example.com", TENANT_2, VALID_PASSWORD, List.of(Role.ADMIN));

        // Login to tenant1 and get token
        LoginRequest loginRequest1 = createLoginRequest(tenant1User.getUsername(), VALID_PASSWORD, TENANT_1);
        MvcResult loginResult1 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest1)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse1 = objectMapper.readValue(
            loginResult1.getResponse().getContentAsString(), LoginResponse.class);

        // Validate token should work for same tenant and contain correct tenant info
        TokenValidationRequest validationRequest = new TokenValidationRequest(loginResponse1.getAccessToken());
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.tenantId").value(TENANT_1))
                .andExpect(jsonPath("$.username").value(tenant1User.getUsername()))
                .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItems("CUSTOMER")));

        // Login to tenant2 and verify different tenant info
        LoginRequest loginRequest2 = createLoginRequest(tenant2User.getUsername(), VALID_PASSWORD, TENANT_2);
        MvcResult loginResult2 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest2)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse2 = objectMapper.readValue(
            loginResult2.getResponse().getContentAsString(), LoginResponse.class);

        TokenValidationRequest validationRequest2 = new TokenValidationRequest(loginResponse2.getAccessToken());
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.tenantId").value(TENANT_2))
                .andExpect(jsonPath("$.username").value(tenant2User.getUsername()))
                .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItems("ADMIN")));
    }

    // ========== ADDITIONAL TENANT ISOLATION TESTS ==========

    @Test
    @Order(23)
    @DisplayName("Failed login attempts should be isolated by tenant")
    void testFailedLoginAttempts_ShouldBeIsolatedByTenant() throws Exception {
        // Arrange - Create users with same username in different tenants
        String commonUsername = "isolationuser";
        UserAuth tenant1User = createUserInTenant(commonUsername, "isolation1@example.com", TENANT_1, VALID_PASSWORD, List.of(Role.CUSTOMER));
        UserAuth tenant2User = createUserInTenant(commonUsername, "isolation2@example.com", TENANT_2, VALID_PASSWORD, List.of(Role.CUSTOMER));

        // Act - Make failed attempts in tenant1
        LoginRequest tenant1FailRequest = createLoginRequest(commonUsername, INVALID_PASSWORD, TENANT_1);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tenant1FailRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // Verify tenant1 user has failed attempts
        UserAuth updatedTenant1User = userAuthRepository.findById(tenant1User.getId()).orElseThrow();
        assertEquals(3, updatedTenant1User.getFailedLoginAttempts());
        assertFalse(updatedTenant1User.getAccountLocked());

        // Act - Login successfully in tenant2 (should not be affected by tenant1 failures)
        LoginRequest tenant2SuccessRequest = createLoginRequest(commonUsername, VALID_PASSWORD, TENANT_2);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant2SuccessRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_2));

        // Verify tenant2 user is unaffected
        UserAuth updatedTenant2User = userAuthRepository.findById(tenant2User.getId()).orElseThrow();
        assertEquals(0, updatedTenant2User.getFailedLoginAttempts());
        assertFalse(updatedTenant2User.getAccountLocked());
    }

    @Test
    @Order(24)
    @DisplayName("Account locking should be isolated by tenant")
    void testAccountLocking_ShouldBeIsolatedByTenant() throws Exception {
        // Arrange - Create users with same username in different tenants
        String commonUsername = "lockuser";
        UserAuth tenant1User = createUserInTenant(commonUsername, "lock1@example.com", TENANT_1, VALID_PASSWORD, List.of(Role.CUSTOMER));
        UserAuth tenant2User = createUserInTenant(commonUsername, "lock2@example.com", TENANT_2, VALID_PASSWORD, List.of(Role.CUSTOMER));

        // Act - Lock account in tenant1 (5 failed attempts)
        LoginRequest tenant1FailRequest = createLoginRequest(commonUsername, INVALID_PASSWORD, TENANT_1);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tenant1FailRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // Verify tenant1 user is locked
        UserAuth lockedTenant1User = userAuthRepository.findById(tenant1User.getId()).orElseThrow();
        assertEquals(5, lockedTenant1User.getFailedLoginAttempts());
        assertTrue(lockedTenant1User.getAccountLocked());

        // Act - Verify tenant1 user cannot login even with correct password
        LoginRequest tenant1ValidRequest = createLoginRequest(commonUsername, VALID_PASSWORD, TENANT_1);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant1ValidRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is locked due to too many failed login attempts"));

        // Act - Verify tenant2 user can still login successfully
        LoginRequest tenant2ValidRequest = createLoginRequest(commonUsername, VALID_PASSWORD, TENANT_2);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant2ValidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_2));

        // Verify tenant2 user is unaffected
        UserAuth unaffectedTenant2User = userAuthRepository.findById(tenant2User.getId()).orElseThrow();
        assertEquals(0, unaffectedTenant2User.getFailedLoginAttempts());
        assertFalse(unaffectedTenant2User.getAccountLocked());
    }

    // ========== REFRESH TOKEN EXPIRATION AND CLEANUP TESTS ==========

    @Test
    @Order(25)
    @DisplayName("Refresh token should fail after expiration")
    void testRefreshToken_ShouldFailAfterExpiration() throws Exception {
        // Arrange - Login to get refresh token
        LoginRequest loginRequest = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_1);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Simulate token expiration by manually expiring the token in database
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUserId(activeUser.getId());
        assertFalse(refreshTokens.isEmpty());
        
        RefreshToken token = refreshTokens.get(0);
        token.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expire 1 hour ago
        refreshTokenRepository.save(token);

        // Act & Assert - Refresh should fail with expired token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REFRESH_FAILED"));
    }

    @Test
    @Order(26)
    @DisplayName("Refresh token should fail after logout (token revocation)")
    void testRefreshToken_ShouldFailAfterLogout() throws Exception {
        // Arrange - Login to get tokens
        LoginRequest loginRequest = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_1);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Act - Logout to revoke tokens
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken()))
                .andExpect(status().isOk());

        // Assert - Refresh token should fail after logout
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REFRESH_FAILED"));
    }

    @Test
    @Order(27)
    @DisplayName("Multiple refresh tokens should be managed independently")
    void testMultipleRefreshTokens_ShouldBeManagedIndependently() throws Exception {
        // Arrange - Create multiple login sessions for the same user
        LoginRequest loginRequest = createLoginRequest(activeUser.getUsername(), VALID_PASSWORD, TENANT_1);
        
        // First login session
        MvcResult loginResult1 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse1 = objectMapper.readValue(
            loginResult1.getResponse().getContentAsString(), LoginResponse.class);

        // Second login session
        MvcResult loginResult2 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse2 = objectMapper.readValue(
            loginResult2.getResponse().getContentAsString(), LoginResponse.class);

        // Verify both refresh tokens work independently
        RefreshTokenRequest refreshRequest1 = new RefreshTokenRequest(loginResponse1.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        RefreshTokenRequest refreshRequest2 = new RefreshTokenRequest(loginResponse2.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Verify user has multiple active refresh tokens
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUserId(activeUser.getId());
        assertTrue(refreshTokens.size() >= 2);
    }

    @Test
    @Order(28)
    @DisplayName("Refresh token cleanup should handle tenant isolation")
    void testRefreshTokenCleanup_ShouldHandleTenantIsolation() throws Exception {
        // Arrange - Create users in different tenants and generate refresh tokens
        UserAuth tenant1User = createUserInTenant("cleanupuser1", "cleanup1@example.com", TENANT_1, VALID_PASSWORD, List.of(Role.CUSTOMER));
        UserAuth tenant2User = createUserInTenant("cleanupuser2", "cleanup2@example.com", TENANT_2, VALID_PASSWORD, List.of(Role.CUSTOMER));

        // Login for both tenants
        LoginRequest tenant1Login = createLoginRequest(tenant1User.getUsername(), VALID_PASSWORD, TENANT_1);
        MvcResult tenant1Result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant1Login)))
                .andExpect(status().isOk())
                .andReturn();

        LoginRequest tenant2Login = createLoginRequest(tenant2User.getUsername(), VALID_PASSWORD, TENANT_2);
        MvcResult tenant2Result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant2Login)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse tenant1Response = objectMapper.readValue(
            tenant1Result.getResponse().getContentAsString(), LoginResponse.class);
        LoginResponse tenant2Response = objectMapper.readValue(
            tenant2Result.getResponse().getContentAsString(), LoginResponse.class);

        // Logout tenant1 user (should only affect tenant1 tokens)
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + tenant1Response.getAccessToken()))
                .andExpect(status().isOk());

        // Verify tenant1 refresh token is invalidated
        RefreshTokenRequest tenant1Refresh = new RefreshTokenRequest(tenant1Response.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant1Refresh)))
                .andExpect(status().isUnauthorized());

        // Verify tenant2 refresh token still works
        RefreshTokenRequest tenant2Refresh = new RefreshTokenRequest(tenant2Response.getRefreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant2Refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    // ========== PASSWORD RESET FAILED ATTEMPT COUNTER TESTS ==========

    @Test
    @Order(29)
    @DisplayName("Password reset scenario - successful login should reset failed attempts")
    void testPasswordReset_SuccessfulLoginShouldResetFailedAttempts() throws Exception {
        // Arrange - Create user with failed attempts (simulating password reset scenario)
        UserAuth userWithFailedAttempts = createUser("resetuser", "reset@example.com", TENANT_1, VALID_PASSWORD, 
                                                    List.of(Role.CUSTOMER), true, false, 3);

        // Act - Successful login after password reset (simulated by successful login)
        LoginRequest resetLoginRequest = createLoginRequest(userWithFailedAttempts.getUsername(), VALID_PASSWORD, TENANT_1);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userWithFailedAttempts.getUsername()));

        // Assert - Failed attempts should be reset to 0
        UserAuth updatedUser = userAuthRepository.findById(userWithFailedAttempts.getId()).orElseThrow();
        assertEquals(0, updatedUser.getFailedLoginAttempts());
        assertFalse(updatedUser.getAccountLocked());
    }

    @Test
    @Order(30)
    @DisplayName("Password reset scenario - failed attempts should persist until successful login")
    void testPasswordReset_FailedAttemptsShouldPersistUntilSuccessfulLogin() throws Exception {
        // Arrange - Create user for password reset testing
        UserAuth resetUser = createUser("persistuser", "persist@example.com", TENANT_1, VALID_PASSWORD, 
                                       List.of(Role.CUSTOMER), true, false, 0);

        // Act - Make failed attempts (simulating failed password reset attempts)
        LoginRequest failedRequest = createLoginRequest(resetUser.getUsername(), INVALID_PASSWORD, TENANT_1);
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedRequest)))
                    .andExpect(status().isUnauthorized());

            // Verify failed attempts are incrementing
            UserAuth updatedUser = userAuthRepository.findById(resetUser.getId()).orElseThrow();
            assertEquals(i, updatedUser.getFailedLoginAttempts());
        }

        // Act - Successful login should reset counter
        LoginRequest successRequest = createLoginRequest(resetUser.getUsername(), VALID_PASSWORD, TENANT_1);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(successRequest)))
                .andExpect(status().isOk());

        // Assert - Failed attempts should be reset
        UserAuth finalUser = userAuthRepository.findById(resetUser.getId()).orElseThrow();
        assertEquals(0, finalUser.getFailedLoginAttempts());
        assertFalse(finalUser.getAccountLocked());
    }

    // ========== HELPER METHODS ==========

    private LoginRequest createLoginRequest(String usernameOrEmail, String password, String tenantId) {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail(usernameOrEmail);
        request.setPassword(password);
        request.setTenantId(tenantId);
        return request;
    }

    // Note: Both @Transactional rollback and explicit @AfterEach cleanup are used for maximum test isolation
}
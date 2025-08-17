package com.ecommerce.auth.util;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.UserAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify TestDataFactory utility methods work correctly
 */
class TestDataFactoryTest {

    @BeforeEach
    void setUp() {
        TestDataFactory.resetCounters();
    }

    @Test
    @DisplayName("Should create active user with correct properties")
    void testCreateActiveUser() {
        // Act
        UserAuth user = TestDataFactory.createActiveUser(TestDataFactory.DEFAULT_TENANT);

        // Assert
        assertNotNull(user);
        assertEquals(TestDataFactory.DEFAULT_TENANT, user.getTenantId());
        assertEquals("activeuser", user.getUsername());
        assertEquals("active@example.com", user.getEmail());
        assertEquals(TestDataFactory.DEFAULT_PASSWORD_HASH, user.getPasswordHash());
        assertTrue(user.getIsActive());
        assertFalse(user.getAccountLocked());
        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(Arrays.asList(Role.CUSTOMER), user.getRoles());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create inactive user with correct properties")
    void testCreateInactiveUser() {
        // Act
        UserAuth user = TestDataFactory.createInactiveUser(TestDataFactory.DEFAULT_TENANT);

        // Assert
        assertNotNull(user);
        assertEquals(TestDataFactory.DEFAULT_TENANT, user.getTenantId());
        assertEquals("inactiveuser", user.getUsername());
        assertEquals("inactive@example.com", user.getEmail());
        assertFalse(user.getIsActive());
        assertFalse(user.getAccountLocked());
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should create locked user with correct properties")
    void testCreateLockedUser() {
        // Act
        UserAuth user = TestDataFactory.createLockedUser(TestDataFactory.DEFAULT_TENANT);

        // Assert
        assertNotNull(user);
        assertEquals(TestDataFactory.DEFAULT_TENANT, user.getTenantId());
        assertEquals("lockeduser", user.getUsername());
        assertEquals("locked@example.com", user.getEmail());
        assertTrue(user.getIsActive());
        assertTrue(user.getAccountLocked());
        assertEquals(5, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should create admin user with multiple roles")
    void testCreateAdminUser() {
        // Act
        UserAuth user = TestDataFactory.createAdminUser(TestDataFactory.DEFAULT_TENANT);

        // Assert
        assertNotNull(user);
        assertEquals("adminuser", user.getUsername());
        assertEquals("admin@example.com", user.getEmail());
        assertEquals(TestDataFactory.ADMIN_PASSWORD_HASH, user.getPasswordHash());
        assertTrue(user.getRoles().contains(Role.ADMIN));
        assertTrue(user.getRoles().contains(Role.CUSTOMER));
        assertEquals(2, user.getRoles().size());
    }

    @Test
    @DisplayName("Should create users for tenant isolation testing")
    void testCreateTenantIsolationTestUsers() {
        // Act
        List<UserAuth> users = TestDataFactory.createTenantIsolationTestUsers();

        // Assert
        assertEquals(2, users.size());
        
        UserAuth tenant1User = users.get(0);
        UserAuth tenant2User = users.get(1);
        
        assertEquals("testuser", tenant1User.getUsername());
        assertEquals("testuser", tenant2User.getUsername());
        assertEquals(TestDataFactory.DEFAULT_TENANT, tenant1User.getTenantId());
        assertEquals(TestDataFactory.SECONDARY_TENANT, tenant2User.getTenantId());
        assertNotEquals(tenant1User.getEmail(), tenant2User.getEmail());
    }

    @Test
    @DisplayName("Should create complete test scenario with multiple user types")
    void testCreateCompleteTestScenario() {
        // Act
        List<UserAuth> users = TestDataFactory.createCompleteTestScenario(TestDataFactory.DEFAULT_TENANT);

        // Assert
        assertEquals(6, users.size());
        
        // Verify we have different user types
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("activeuser")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("inactiveuser")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("lockeduser")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("adminuser")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("manageruser")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("supportuser")));
        
        // Verify all users belong to the same tenant
        assertTrue(users.stream().allMatch(u -> TestDataFactory.DEFAULT_TENANT.equals(u.getTenantId())));
    }

    @Test
    @DisplayName("Should create custom user with specified parameters")
    void testCreateCustomUser() {
        // Arrange
        String tenantId = "custom-tenant";
        String username = "customuser";
        String email = "custom@example.com";
        List<Role> roles = Arrays.asList(Role.MANAGER, Role.SUPPORT);
        
        // Act
        UserAuth user = TestDataFactory.createCustomUser(tenantId, username, email, roles, true, false, 2);

        // Assert
        assertEquals(tenantId, user.getTenantId());
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals(roles, user.getRoles());
        assertTrue(user.getIsActive());
        assertFalse(user.getAccountLocked());
        assertEquals(2, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should create valid refresh token")
    void testCreateRefreshToken() {
        // Arrange
        Long userId = 1L;
        String tokenHash = "test-token-hash";

        // Act
        RefreshToken token = TestDataFactory.createRefreshToken(userId, tokenHash);

        // Assert
        assertNotNull(token);
        assertEquals(userId, token.getUserId());
        assertEquals(tokenHash, token.getTokenHash());
        assertFalse(token.getIsRevoked());
        assertTrue(token.getExpiresAt().isAfter(LocalDateTime.now()));
        assertNotNull(token.getCreatedAt());
    }

    @Test
    @DisplayName("Should create expired refresh token")
    void testCreateExpiredRefreshToken() {
        // Arrange
        Long userId = 1L;
        String tokenHash = "expired-token-hash";

        // Act
        RefreshToken token = TestDataFactory.createExpiredRefreshToken(userId, tokenHash);

        // Assert
        assertNotNull(token);
        assertEquals(userId, token.getUserId());
        assertEquals(tokenHash, token.getTokenHash());
        assertTrue(token.getExpiresAt().isBefore(LocalDateTime.now()));
        assertTrue(token.isExpired());
    }

    @Test
    @DisplayName("Should create revoked refresh token")
    void testCreateRevokedRefreshToken() {
        // Arrange
        Long userId = 1L;
        String tokenHash = "revoked-token-hash";

        // Act
        RefreshToken token = TestDataFactory.createRevokedRefreshToken(userId, tokenHash);

        // Assert
        assertNotNull(token);
        assertEquals(userId, token.getUserId());
        assertEquals(tokenHash, token.getTokenHash());
        assertTrue(token.getIsRevoked());
        assertFalse(token.isExpired());
    }

    @Test
    @DisplayName("Should create multiple refresh tokens for user")
    void testCreateMultipleRefreshTokens() {
        // Arrange
        Long userId = 1L;

        // Act
        List<RefreshToken> tokens = TestDataFactory.createMultipleRefreshTokens(userId);

        // Assert
        assertEquals(4, tokens.size());
        assertTrue(tokens.stream().allMatch(t -> t.getUserId().equals(userId)));
        
        // Verify we have different token types
        assertTrue(tokens.stream().anyMatch(t -> !t.isExpired() && !t.getIsRevoked()));
        assertTrue(tokens.stream().anyMatch(t -> t.isExpired()));
        assertTrue(tokens.stream().anyMatch(t -> t.getIsRevoked()));
    }

    @Test
    @DisplayName("Should reset counters for predictable test data")
    void testResetCounters() {
        // Arrange - Create some users to increment counters
        TestDataFactory.createActiveUser(TestDataFactory.DEFAULT_TENANT);
        TestDataFactory.createInactiveUser(TestDataFactory.DEFAULT_TENANT);

        // Act
        TestDataFactory.resetCounters();
        UserAuth user = TestDataFactory.createActiveUser(TestDataFactory.DEFAULT_TENANT);

        // Assert
        assertEquals(1L, user.getId()); // Should start from 1 again
    }

    @Test
    @DisplayName("Should create user near lockout with 4 failed attempts")
    void testCreateUserNearLockout() {
        // Act
        UserAuth user = TestDataFactory.createUserNearLockout(TestDataFactory.DEFAULT_TENANT);

        // Assert
        assertEquals(4, user.getFailedLoginAttempts());
        assertFalse(user.getAccountLocked());
        assertTrue(user.getIsActive());
    }

    @Test
    @DisplayName("Should provide consistent password constants")
    void testPasswordConstants() {
        // Assert
        assertNotNull(TestDataFactory.DEFAULT_PASSWORD);
        assertNotNull(TestDataFactory.DEFAULT_PASSWORD_HASH);
        assertNotNull(TestDataFactory.ADMIN_PASSWORD);
        assertNotNull(TestDataFactory.ADMIN_PASSWORD_HASH);
        
        // Verify password hashes are proper BCrypt format
        assertTrue(TestDataFactory.DEFAULT_PASSWORD_HASH.startsWith("$2a$10$"));
        assertTrue(TestDataFactory.ADMIN_PASSWORD_HASH.startsWith("$2a$10$"));
    }

    @Test
    @DisplayName("Should provide consistent tenant constants")
    void testTenantConstants() {
        // Assert
        assertNotNull(TestDataFactory.DEFAULT_TENANT);
        assertNotNull(TestDataFactory.SECONDARY_TENANT);
        assertNotNull(TestDataFactory.ADMIN_TENANT);
        
        assertNotEquals(TestDataFactory.DEFAULT_TENANT, TestDataFactory.SECONDARY_TENANT);
        assertNotEquals(TestDataFactory.DEFAULT_TENANT, TestDataFactory.ADMIN_TENANT);
    }
}
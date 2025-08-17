package com.ecommerce.auth.util;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.UserAuth;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Factory class for creating realistic test data objects with proper tenant isolation
 * and role assignments. Provides utility methods for creating various test scenarios.
 */
public class TestDataFactory {

    private static final AtomicLong USER_ID_COUNTER = new AtomicLong(1);
    private static final AtomicLong TOKEN_ID_COUNTER = new AtomicLong(1);
    
    // Common test passwords and their BCrypt hashes
    public static final String DEFAULT_PASSWORD = "password123";
    public static final String DEFAULT_PASSWORD_HASH = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFe5ldjoiKDpjIsxvE0Q2AW";
    public static final String ADMIN_PASSWORD = "admin123";
    public static final String ADMIN_PASSWORD_HASH = "$2a$10$8K1p/a0dQ2jH.lh/sB6/v.RV.bGHzXvfUNDwJB8.nxw2hf8/W9F8W";
    
    // Common tenant IDs for testing
    public static final String DEFAULT_TENANT = "tenant1";
    public static final String SECONDARY_TENANT = "tenant2";
    public static final String ADMIN_TENANT = "admin-tenant";

    /**
     * Creates an active user with CUSTOMER role
     */
    public static UserAuth createActiveUser(String tenantId) {
        return createUser(tenantId, "activeuser", "active@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.CUSTOMER), 
                         true, false, 0);
    }

    /**
     * Creates an inactive user with CUSTOMER role
     */
    public static UserAuth createInactiveUser(String tenantId) {
        return createUser(tenantId, "inactiveuser", "inactive@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.CUSTOMER), 
                         false, false, 0);
    }

    /**
     * Creates a locked user with CUSTOMER role and 5 failed attempts
     */
    public static UserAuth createLockedUser(String tenantId) {
        return createUser(tenantId, "lockeduser", "locked@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.CUSTOMER), 
                         true, true, 5);
    }

    /**
     * Creates an admin user with ADMIN and CUSTOMER roles
     */
    public static UserAuth createAdminUser(String tenantId) {
        return createUser(tenantId, "adminuser", "admin@example.com", 
                         ADMIN_PASSWORD_HASH, Arrays.asList(Role.ADMIN, Role.CUSTOMER), 
                         true, false, 0);
    }

    /**
     * Creates a manager user with MANAGER and CUSTOMER roles
     */
    public static UserAuth createManagerUser(String tenantId) {
        return createUser(tenantId, "manageruser", "manager@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.MANAGER, Role.CUSTOMER), 
                         true, false, 0);
    }

    /**
     * Creates a support user with SUPPORT role
     */
    public static UserAuth createSupportUser(String tenantId) {
        return createUser(tenantId, "supportuser", "support@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.SUPPORT), 
                         true, false, 0);
    }

    /**
     * Creates a system user with SYSTEM role
     */
    public static UserAuth createSystemUser(String tenantId) {
        return createUser(tenantId, "systemuser", "system@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.SYSTEM), 
                         true, false, 0);
    }

    /**
     * Creates a user with multiple roles for testing complex scenarios
     */
    public static UserAuth createMultiRoleUser(String tenantId) {
        return createUser(tenantId, "multiroleuser", "multirole@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.ADMIN, Role.MANAGER, Role.CUSTOMER), 
                         true, false, 0);
    }

    /**
     * Creates a user with custom parameters for specific test scenarios
     */
    public static UserAuth createCustomUser(String tenantId, String username, String email, 
                                           List<Role> roles, boolean isActive, 
                                           boolean isLocked, int failedAttempts) {
        return createUser(tenantId, username, email, DEFAULT_PASSWORD_HASH, 
                         roles, isActive, isLocked, failedAttempts);
    }

    /**
     * Creates a user with custom password hash
     */
    public static UserAuth createUserWithPassword(String tenantId, String username, String email, 
                                                 String passwordHash, List<Role> roles) {
        return createUser(tenantId, username, email, passwordHash, roles, true, false, 0);
    }

    /**
     * Creates a user that's about to be locked (4 failed attempts)
     */
    public static UserAuth createUserNearLockout(String tenantId) {
        return createUser(tenantId, "nearlocked", "nearlocked@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.CUSTOMER), 
                         true, false, 4);
    }

    /**
     * Creates users for tenant isolation testing - same username in different tenants
     */
    public static List<UserAuth> createTenantIsolationTestUsers() {
        List<UserAuth> users = new ArrayList<>();
        users.add(createUser(DEFAULT_TENANT, "testuser", "test@tenant1.com", 
                           DEFAULT_PASSWORD_HASH, Arrays.asList(Role.CUSTOMER), true, false, 0));
        users.add(createUser(SECONDARY_TENANT, "testuser", "test@tenant2.com", 
                           DEFAULT_PASSWORD_HASH, Arrays.asList(Role.CUSTOMER), true, false, 0));
        return users;
    }

    /**
     * Creates a complete test scenario with users of different roles in the same tenant
     */
    public static List<UserAuth> createCompleteTestScenario(String tenantId) {
        List<UserAuth> users = new ArrayList<>();
        users.add(createActiveUser(tenantId));
        users.add(createInactiveUser(tenantId));
        users.add(createLockedUser(tenantId));
        users.add(createAdminUser(tenantId));
        users.add(createManagerUser(tenantId));
        users.add(createSupportUser(tenantId));
        return users;
    }

    /**
     * Core method to create a user with all parameters
     */
    private static UserAuth createUser(String tenantId, String username, String email, 
                                      String passwordHash, List<Role> roles, 
                                      boolean isActive, boolean isLocked, int failedAttempts) {
        UserAuth user = new UserAuth();
        user.setId(USER_ID_COUNTER.getAndIncrement());
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRoles(new ArrayList<>(roles));
        user.setIsActive(isActive);
        user.setAccountLocked(isLocked);
        user.setFailedLoginAttempts(failedAttempts);
        
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        return user;
    }

    /**
     * Creates a valid refresh token for a user
     */
    public static RefreshToken createRefreshToken(Long userId, String tokenHash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(TOKEN_ID_COUNTER.getAndIncrement());
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 days validity
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setIsRevoked(false);
        return refreshToken;
    }

    /**
     * Creates an expired refresh token
     */
    public static RefreshToken createExpiredRefreshToken(Long userId, String tokenHash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(TOKEN_ID_COUNTER.getAndIncrement());
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        refreshToken.setCreatedAt(LocalDateTime.now().minusDays(8));
        refreshToken.setIsRevoked(false);
        return refreshToken;
    }

    /**
     * Creates a revoked refresh token
     */
    public static RefreshToken createRevokedRefreshToken(Long userId, String tokenHash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(TOKEN_ID_COUNTER.getAndIncrement());
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setCreatedAt(LocalDateTime.now().minusHours(2));
        refreshToken.setIsRevoked(true);
        return refreshToken;
    }

    /**
     * Creates a refresh token that expires soon (for testing expiration logic)
     */
    public static RefreshToken createSoonToExpireRefreshToken(Long userId, String tokenHash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(TOKEN_ID_COUNTER.getAndIncrement());
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusMinutes(5)); // Expires in 5 minutes
        refreshToken.setCreatedAt(LocalDateTime.now().minusDays(6));
        refreshToken.setIsRevoked(false);
        return refreshToken;
    }

    /**
     * Creates multiple refresh tokens for the same user (for testing token management)
     */
    public static List<RefreshToken> createMultipleRefreshTokens(Long userId) {
        List<RefreshToken> tokens = new ArrayList<>();
        tokens.add(createRefreshToken(userId, "hash1"));
        tokens.add(createRefreshToken(userId, "hash2"));
        tokens.add(createExpiredRefreshToken(userId, "expiredHash"));
        tokens.add(createRevokedRefreshToken(userId, "revokedHash"));
        return tokens;
    }

    /**
     * Resets the ID counters for predictable test data
     */
    public static void resetCounters() {
        USER_ID_COUNTER.set(1);
        TOKEN_ID_COUNTER.set(1);
    }

    /**
     * Creates test data for password reset scenarios
     */
    public static UserAuth createUserForPasswordReset(String tenantId) {
        return createUser(tenantId, "resetuser", "reset@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.CUSTOMER), 
                         true, false, 3); // 3 failed attempts, not locked yet
    }

    /**
     * Creates test data for account recovery scenarios
     */
    public static UserAuth createUserForAccountRecovery(String tenantId) {
        return createUser(tenantId, "recoveryuser", "recovery@example.com", 
                         DEFAULT_PASSWORD_HASH, Arrays.asList(Role.CUSTOMER), 
                         false, true, 5); // Inactive and locked
    }
}
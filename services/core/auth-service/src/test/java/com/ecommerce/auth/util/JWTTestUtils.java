package com.ecommerce.auth.util;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ecommerce.auth.entity.Role;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Utility class for creating comprehensive JWT test mocks with all required claims.
 * Provides methods for generating proper mock JWT tokens for various test scenarios.
 */
public class JWTTestUtils {

    // Common test values
    public static final String DEFAULT_USER_ID = "1";
    public static final String DEFAULT_TENANT_ID = "tenant1";
    public static final String SECONDARY_TENANT_ID = "tenant2";
    public static final String ADMIN_TENANT_ID = "admin-tenant";
    
    // Token types
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";
    
    // Common role combinations
    public static final List<String> CUSTOMER_ROLES = Arrays.asList("CUSTOMER");
    public static final List<String> ADMIN_ROLES = Arrays.asList("ADMIN", "CUSTOMER");
    public static final List<String> MANAGER_ROLES = Arrays.asList("MANAGER", "CUSTOMER");
    public static final List<String> SUPPORT_ROLES = Arrays.asList("SUPPORT");
    public static final List<String> SYSTEM_ROLES = Arrays.asList("SYSTEM");
    public static final List<String> MULTI_ROLES = Arrays.asList("ADMIN", "MANAGER", "CUSTOMER");

    /**
     * Creates a valid access token with all required claims
     */
    public static DecodedJWT createValidAccessToken(String userId, String tenantId, List<String> roles) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createStringClaim(tenantId);
        Claim rolesClaim = createListClaim(roles);
        Claim tokenTypeClaim = createStringClaim(ACCESS_TOKEN_TYPE);
        Claim userIdClaim = createStringClaim(userId);
        
        // Mock basic JWT properties
        lenient().when(jwt.getSubject()).thenReturn(userId);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        // Mock expiration times (1 hour from now for access tokens)
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + 3600000); // 1 hour
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        // Mock custom claims
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates a valid refresh token with required claims
     */
    public static DecodedJWT createValidRefreshToken(String userId, String tenantId) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createStringClaim(tenantId);
        Claim tokenTypeClaim = createStringClaim(REFRESH_TOKEN_TYPE);
        Claim userIdClaim = createStringClaim(userId);
        
        // Mock basic JWT properties
        lenient().when(jwt.getSubject()).thenReturn(userId);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        // Mock expiration times (7 days from now for refresh tokens)
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + 604800000L); // 7 days
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        // Mock custom claims (refresh tokens don't include roles)
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates an expired access token
     */
    public static DecodedJWT createExpiredAccessToken(String userId, String tenantId, List<String> roles) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createStringClaim(tenantId);
        Claim rolesClaim = createListClaim(roles);
        Claim tokenTypeClaim = createStringClaim(ACCESS_TOKEN_TYPE);
        Claim userIdClaim = createStringClaim(userId);
        
        lenient().when(jwt.getSubject()).thenReturn(userId);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        // Mock expired times (1 hour ago)
        Date issuedAt = new Date(System.currentTimeMillis() - 7200000L); // 2 hours ago
        Date expiresAt = new Date(System.currentTimeMillis() - 3600000L); // 1 hour ago
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates an expired refresh token
     */
    public static DecodedJWT createExpiredRefreshToken(String userId, String tenantId) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createStringClaim(tenantId);
        Claim tokenTypeClaim = createStringClaim(REFRESH_TOKEN_TYPE);
        Claim userIdClaim = createStringClaim(userId);
        
        lenient().when(jwt.getSubject()).thenReturn(userId);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        // Mock expired times (1 day ago)
        Date issuedAt = new Date(System.currentTimeMillis() - 172800000L); // 2 days ago
        Date expiresAt = new Date(System.currentTimeMillis() - 86400000L); // 1 day ago
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates a token with invalid token type
     */
    public static DecodedJWT createInvalidTokenType(String userId, String tenantId) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createStringClaim(tenantId);
        Claim tokenTypeClaim = createStringClaim("invalid");
        Claim userIdClaim = createStringClaim(userId);
        
        lenient().when(jwt.getSubject()).thenReturn(userId);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + 3600000);
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates a token with missing tenant_id claim
     */
    public static DecodedJWT createTokenWithMissingTenantId(String userId, List<String> roles) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createNullClaim();
        Claim rolesClaim = createListClaim(roles);
        Claim tokenTypeClaim = createStringClaim(ACCESS_TOKEN_TYPE);
        Claim userIdClaim = createStringClaim(userId);
        
        lenient().when(jwt.getSubject()).thenReturn(userId);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + 3600000);
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates a token with missing roles claim
     */
    public static DecodedJWT createTokenWithMissingRoles(String userId, String tenantId) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createStringClaim(tenantId);
        Claim rolesClaim = createNullClaim();
        Claim tokenTypeClaim = createStringClaim(ACCESS_TOKEN_TYPE);
        Claim userIdClaim = createStringClaim(userId);
        
        lenient().when(jwt.getSubject()).thenReturn(userId);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + 3600000);
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates a token with null subject
     */
    public static DecodedJWT createTokenWithNullSubject(String tenantId, List<String> roles) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createStringClaim(tenantId);
        Claim rolesClaim = createListClaim(roles);
        Claim tokenTypeClaim = createStringClaim(ACCESS_TOKEN_TYPE);
        Claim userIdClaim = createNullClaim();
        
        lenient().when(jwt.getSubject()).thenReturn(null);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + 3600000);
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates tokens for tenant isolation testing
     */
    public static DecodedJWT createTokenForTenant(String userId, String tenantId, List<String> roles) {
        return createValidAccessToken(userId, tenantId, roles);
    }

    /**
     * Creates tokens with different role combinations for testing
     */
    public static DecodedJWT createCustomerToken(String userId, String tenantId) {
        return createValidAccessToken(userId, tenantId, CUSTOMER_ROLES);
    }

    public static DecodedJWT createAdminToken(String userId, String tenantId) {
        return createValidAccessToken(userId, tenantId, ADMIN_ROLES);
    }

    public static DecodedJWT createManagerToken(String userId, String tenantId) {
        return createValidAccessToken(userId, tenantId, MANAGER_ROLES);
    }

    public static DecodedJWT createSupportToken(String userId, String tenantId) {
        return createValidAccessToken(userId, tenantId, SUPPORT_ROLES);
    }

    public static DecodedJWT createSystemToken(String userId, String tenantId) {
        return createValidAccessToken(userId, tenantId, SYSTEM_ROLES);
    }

    public static DecodedJWT createMultiRoleToken(String userId, String tenantId) {
        return createValidAccessToken(userId, tenantId, MULTI_ROLES);
    }

    /**
     * Converts Role enum list to string list for JWT claims
     */
    public static List<String> rolesToStringList(List<Role> roles) {
        return roles.stream()
                   .map(Role::name)
                   .collect(Collectors.toList());
    }

    /**
     * Creates a string claim mock
     */
    private static Claim createStringClaim(String value) {
        Claim claim = mock(Claim.class);
        if (value != null) {
            lenient().when(claim.asString()).thenReturn(value);
            lenient().when(claim.isNull()).thenReturn(false);
        } else {
            lenient().when(claim.asString()).thenReturn(null);
            lenient().when(claim.isNull()).thenReturn(true);
        }
        return claim;
    }

    /**
     * Creates a list claim mock
     */
    private static Claim createListClaim(List<String> values) {
        Claim claim = mock(Claim.class);
        if (values != null) {
            lenient().when(claim.asList(String.class)).thenReturn(values);
            lenient().when(claim.isNull()).thenReturn(false);
        } else {
            lenient().when(claim.asList(String.class)).thenReturn(null);
            lenient().when(claim.isNull()).thenReturn(true);
        }
        return claim;
    }

    /**
     * Creates a null claim mock that throws exceptions when accessed
     */
    private static Claim createNullClaim() {
        Claim claim = mock(Claim.class);
        // When accessing null claims, throw NullPointerException to simulate real JWT behavior
        lenient().when(claim.asString()).thenThrow(new NullPointerException("Cannot call asString() on null claim"));
        lenient().when(claim.asList(String.class)).thenThrow(new NullPointerException("Cannot call asList() on null claim"));
        lenient().when(claim.isNull()).thenReturn(true);
        return claim;
    }

    /**
     * Creates a token that will be valid for a specific duration
     */
    public static DecodedJWT createTokenWithCustomExpiry(String userId, String tenantId, 
                                                        List<String> roles, long expiryMinutes) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        
        // Create claims first to avoid unfinished stubbing
        Claim tenantIdClaim = createStringClaim(tenantId);
        Claim rolesClaim = createListClaim(roles);
        Claim tokenTypeClaim = createStringClaim(ACCESS_TOKEN_TYPE);
        Claim userIdClaim = createStringClaim(userId);
        
        lenient().when(jwt.getSubject()).thenReturn(userId);
        lenient().when(jwt.getIssuer()).thenReturn("auth-service");
        lenient().when(jwt.getAudience()).thenReturn(Arrays.asList("ecommerce-api"));
        
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + (expiryMinutes * 60 * 1000));
        lenient().when(jwt.getIssuedAt()).thenReturn(issuedAt);
        lenient().when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        lenient().when(jwt.getClaim("tenant_id")).thenReturn(tenantIdClaim);
        lenient().when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        lenient().when(jwt.getClaim("token_type")).thenReturn(tokenTypeClaim);
        lenient().when(jwt.getClaim("user_id")).thenReturn(userIdClaim);
        
        return jwt;
    }

    /**
     * Creates a token for testing correlation ID and context
     */
    public static DecodedJWT createTokenWithCorrelationId(String userId, String tenantId, 
                                                         List<String> roles, String correlationId) {
        DecodedJWT jwt = createValidAccessToken(userId, tenantId, roles);
        Claim correlationIdClaim = createStringClaim(correlationId);
        lenient().when(jwt.getClaim("correlation_id")).thenReturn(correlationIdClaim);
        return jwt;
    }
}
package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.UserAuth;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.UserAuthRepository;
import com.ecommerce.auth.util.PasswordUtil;
import com.ecommerce.shared.security.JwtTokenProvider;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Authentication service for user login, token management, and validation
 */
@Service
@Transactional
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserAuthRepository userAuthRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordUtil passwordUtil;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token.expiration:3600}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:86400}")
    private long refreshTokenExpiration;

    @Autowired
    public AuthenticationService(UserAuthRepository userAuthRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               PasswordUtil passwordUtil,
                               JwtTokenProvider jwtTokenProvider) {
        this.userAuthRepository = userAuthRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordUtil = passwordUtil;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticate user and generate tokens
     */
    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("Login attempt for user: {} in tenant: {}", 
                   loginRequest.getUsernameOrEmail(), loginRequest.getTenantId());

        // Find user by username or email within tenant
        Optional<UserAuth> userOptional = userAuthRepository
            .findByTenantIdAndUsernameOrEmail(
                loginRequest.getTenantId(), 
                loginRequest.getUsernameOrEmail()
            );

        if (userOptional.isEmpty()) {
            logger.warn("Login failed - user not found: {} in tenant: {}", 
                       loginRequest.getUsernameOrEmail(), loginRequest.getTenantId());
            throw new IllegalArgumentException("Invalid credentials");
        }

        UserAuth user = userOptional.get();

        // Check if account is locked
        if (user.getAccountLocked()) {
            logger.warn("Login failed - account locked for user: {} in tenant: {}", 
                       user.getUsername(), user.getTenantId());
            throw new IllegalArgumentException("Account is locked due to too many failed login attempts");
        }

        // Check if account is active
        if (!user.getIsActive()) {
            logger.warn("Login failed - account inactive for user: {} in tenant: {}", 
                       user.getUsername(), user.getTenantId());
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Verify password
        if (!passwordUtil.verifyPassword(loginRequest.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            logger.warn("Login failed - invalid password for user: {} in tenant: {}", 
                       user.getUsername(), user.getTenantId());
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Reset failed login attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            user.resetFailedLoginAttempts();
            userAuthRepository.save(user);
        }

        // Generate tokens
        List<String> roleStrings = user.getRoles().stream()
            .map(Role::name)
            .collect(Collectors.toList());

        String accessToken = jwtTokenProvider.createAccessToken(
            user.getId().toString(), 
            user.getTenantId(), 
            roleStrings
        );

        String refreshToken = jwtTokenProvider.createRefreshToken(
            user.getId().toString(), 
            user.getTenantId()
        );

        // Store refresh token
        storeRefreshToken(user.getId(), refreshToken);

        logger.info("Login successful for user: {} in tenant: {}", 
                   user.getUsername(), user.getTenantId());

        return new LoginResponse(
            accessToken,
            refreshToken,
            accessTokenExpiration,
            user.getId().toString(),
            user.getTenantId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles()
        );
    }

    /**
     * Refresh access token using refresh token
     */
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Token refresh attempt");

        try {
            // Validate refresh token
            var decodedJWT = jwtTokenProvider.validateToken(request.getRefreshToken());
            String tokenType = decodedJWT.getClaim("token_type").asString();
            
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("Invalid token type");
            }

            String userId = decodedJWT.getSubject();
            String tenantId = decodedJWT.getClaim("tenant_id").asString();

            // Check if refresh token exists and is valid
            String tokenHash = hashToken(request.getRefreshToken());
            Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository
                .findValidTokenByHash(tokenHash, LocalDateTime.now());

            if (refreshTokenOptional.isEmpty()) {
                logger.warn("Token refresh failed - invalid or expired refresh token for user: {}", userId);
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }

            // Get user details
            UserAuth user = userAuthRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!user.getIsActive() || user.getAccountLocked()) {
                logger.warn("Token refresh failed - user account inactive or locked: {}", userId);
                throw new IllegalArgumentException("User account is inactive or locked");
            }

            // Generate new access token
            List<String> roleStrings = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());

            String newAccessToken = jwtTokenProvider.createAccessToken(
                userId, 
                tenantId, 
                roleStrings
            );

            logger.info("Token refresh successful for user: {}", userId);

            return new TokenResponse(
                newAccessToken,
                accessTokenExpiration,
                userId,
                tenantId
            );

        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }

    /**
     * Validate access token
     */
    public TokenValidationResponse validateToken(TokenValidationRequest request) {
        try {
            var decodedJWT = jwtTokenProvider.validateToken(request.getToken());
            String tokenType = decodedJWT.getClaim("token_type").asString();
            
            if (!"access".equals(tokenType)) {
                return new TokenValidationResponse(false, "Invalid token type");
            }

            String userId = decodedJWT.getSubject();
            String tenantId = decodedJWT.getClaim("tenant_id").asString();
            List<String> roleStrings = decodedJWT.getClaim("roles").asList(String.class);

            // Verify user still exists and is active
            UserAuth user = userAuthRepository.findById(Long.valueOf(userId))
                .orElse(null);

            if (user == null || !user.getIsActive() || user.getAccountLocked()) {
                return new TokenValidationResponse(false, "User account is inactive or locked");
            }

            List<Role> roles = roleStrings.stream()
                .map(Role::valueOf)
                .collect(Collectors.toList());

            return new TokenValidationResponse(
                true, 
                userId, 
                tenantId, 
                user.getUsername(), 
                roles
            );

        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return new TokenValidationResponse(false, "Invalid token");
        }
    }

    /**
     * Logout user by revoking refresh tokens
     */
    public void logout(String userId) {
        logger.info("Logout for user: {}", userId);
        refreshTokenRepository.revokeAllTokensForUser(Long.valueOf(userId));
    }

    /**
     * Handle failed login attempt
     */
    private void handleFailedLogin(UserAuth user) {
        user.incrementFailedLoginAttempts();
        userAuthRepository.save(user);
        
        logger.warn("Failed login attempt {} for user: {} in tenant: {}", 
                   user.getFailedLoginAttempts(), user.getUsername(), user.getTenantId());
    }

    /**
     * Store refresh token in database
     */
    private void storeRefreshToken(Long userId, String refreshToken) {
        try {
            String tokenHash = hashToken(refreshToken);
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration);
            
            RefreshToken refreshTokenEntity = new RefreshToken(userId, tokenHash, expiresAt);
            refreshTokenRepository.save(refreshTokenEntity);
            
        } catch (Exception e) {
            logger.error("Failed to store refresh token for user: {}", userId, e);
            throw new RuntimeException("Failed to store refresh token");
        }
    }

    /**
     * Hash token for secure storage
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Clean up expired refresh tokens (scheduled task)
     */
    public void cleanupExpiredTokens() {
        logger.info("Cleaning up expired refresh tokens");
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        
        // Also clean up old revoked tokens (older than 30 days)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        refreshTokenRepository.deleteRevokedTokensOlderThan(cutoffDate);
    }
}
package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.service.AuthenticationService;
import com.ecommerce.shared.models.dto.ErrorResponse;
import com.ecommerce.shared.utils.CorrelationIdGenerator;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for login, token management, and validation
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;
    private final Counter loginAttemptCounter;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter tokenValidationCounter;

    @Autowired
    public AuthController(AuthenticationService authenticationService, MeterRegistry meterRegistry) {
        this.authenticationService = authenticationService;
        this.loginAttemptCounter = Counter.builder("auth.login.attempts")
            .description("Total login attempts")
            .register(meterRegistry);
        this.loginSuccessCounter = Counter.builder("auth.login.success")
            .description("Successful login attempts")
            .register(meterRegistry);
        this.loginFailureCounter = Counter.builder("auth.login.failure")
            .description("Failed login attempts")
            .register(meterRegistry);
        this.tokenValidationCounter = Counter.builder("auth.token.validation")
            .description("Token validation requests")
            .register(meterRegistry);
    }

    /**
     * User login endpoint
     */
    @PostMapping("/login")
    @Timed
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        String correlationId = CorrelationIdGenerator.generate();
        MDC.put("correlationId", correlationId);
        MDC.put("tenantId", loginRequest.getTenantId());

        try {
            logger.info("Login request received for user: {} in tenant: {}", 
                       loginRequest.getUsernameOrEmail(), loginRequest.getTenantId());
            
            loginAttemptCounter.increment();
            
            LoginResponse response = authenticationService.login(loginRequest);
            
            MDC.put("userId", response.getUserId());
            loginSuccessCounter.increment();
            
            logger.info("Login successful for user: {} in tenant: {}", 
                       response.getUsername(), response.getTenantId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            loginFailureCounter.increment();
            logger.warn("Login failed for user: {} in tenant: {} - {}", 
                       loginRequest.getUsernameOrEmail(), loginRequest.getTenantId(), e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTHENTICATION_FAILED", e.getMessage()));
                
        } catch (Exception e) {
            loginFailureCounter.increment();
            logger.error("Login error for user: {} in tenant: {}", 
                        loginRequest.getUsernameOrEmail(), loginRequest.getTenantId(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
                
        } finally {
            MDC.clear();
        }
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    @Timed
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        String correlationId = CorrelationIdGenerator.generate();
        MDC.put("correlationId", correlationId);

        try {
            logger.info("Token refresh request received");
            
            TokenResponse response = authenticationService.refreshToken(refreshTokenRequest);
            
            MDC.put("userId", response.getUserId());
            MDC.put("tenantId", response.getTenantId());
            
            logger.info("Token refresh successful for user: {}", response.getUserId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Token refresh failed - {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("TOKEN_REFRESH_FAILED", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Token refresh error", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
                
        } finally {
            MDC.clear();
        }
    }

    /**
     * Token validation endpoint for other services
     */
    @PostMapping("/validate")
    @Timed
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody TokenValidationRequest request) {
        String correlationId = CorrelationIdGenerator.generate();
        MDC.put("correlationId", correlationId);

        try {
            tokenValidationCounter.increment();
            
            TokenValidationResponse response = authenticationService.validateToken(request);
            
            if (response.isValid()) {
                MDC.put("userId", response.getUserId());
                MDC.put("tenantId", response.getTenantId());
                logger.debug("Token validation successful for user: {}", response.getUserId());
            } else {
                logger.debug("Token validation failed: {}", response.getErrorMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Token validation error", e);
            
            return ResponseEntity.ok(new TokenValidationResponse(false, "Token validation failed"));
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    @Timed
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) {
        String correlationId = CorrelationIdGenerator.generate();
        MDC.put("correlationId", correlationId);

        try {
            // Extract token from Authorization header
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_TOKEN", "Invalid authorization header"));
            }

            String token = authorizationHeader.substring(7);
            
            // Validate token to get user ID
            TokenValidationResponse validation = authenticationService.validateToken(
                new TokenValidationRequest(token)
            );
            
            if (!validation.isValid()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_TOKEN", "Invalid token"));
            }

            MDC.put("userId", validation.getUserId());
            MDC.put("tenantId", validation.getTenantId());
            
            authenticationService.logout(validation.getUserId());
            
            logger.info("Logout successful for user: {}", validation.getUserId());
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Logout error", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
                
        } finally {
            MDC.clear();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is healthy");
    }
}
package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserProfileRequest;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.service.UserService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for user management operations
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create a new user profile
     */
    @PostMapping
    @Timed(value = "user.create", description = "Time taken to create user")
    public ResponseEntity<UserProfileResponse> createUser(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateUserRequest request) {
        
        logger.info("Creating user profile for tenant: {} with authUserId: {}", tenantId, request.getAuthUserId());
        
        UserProfileResponse response = userService.createUser(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get user profile by auth user ID
     */
    @GetMapping("/profile")
    @Timed(value = "user.get_profile", description = "Time taken to get user profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.debug("Getting user profile for tenant: {} with userId: {}", tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        UserProfileResponse response = userService.getUserProfile(tenantId, authUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user profile by user ID
     */
    @GetMapping("/{userId}")
    @Timed(value = "user.get_by_id", description = "Time taken to get user by ID")
    public ResponseEntity<UserProfileResponse> getUserById(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable Long userId) {
        
        logger.debug("Getting user by ID for tenant: {} with userId: {}", tenantId, userId);
        
        UserProfileResponse response = userService.getUserProfileById(tenantId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    @Timed(value = "user.update_profile", description = "Time taken to update user profile")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        
        logger.info("Updating user profile for tenant: {} with userId: {}", tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        UserProfileResponse response = userService.updateUserProfile(tenantId, authUserId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete user profile
     */
    @DeleteMapping("/profile")
    @Timed(value = "user.delete_profile", description = "Time taken to delete user profile")
    public ResponseEntity<Void> deleteUserProfile(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.info("Deleting user profile for tenant: {} with userId: {}", tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        userService.deleteUser(tenantId, authUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if user exists by auth user ID
     */
    @GetMapping("/exists")
    @Timed(value = "user.exists", description = "Time taken to check if user exists")
    public ResponseEntity<Boolean> userExists(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam Long authUserId) {
        
        logger.debug("Checking if user exists for tenant: {} with authUserId: {}", tenantId, authUserId);
        
        boolean exists = userService.userExists(tenantId, authUserId);
        return ResponseEntity.ok(exists);
    }

    /**
     * Check if user exists by email
     */
    @GetMapping("/exists/email")
    @Timed(value = "user.exists_by_email", description = "Time taken to check if user exists by email")
    public ResponseEntity<Boolean> userExistsByEmail(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String email) {
        
        logger.debug("Checking if user exists by email for tenant: {} with email: {}", tenantId, email);
        
        boolean exists = userService.userExistsByEmail(tenantId, email);
        return ResponseEntity.ok(exists);
    }

    /**
     * Get user by email
     */
    @GetMapping("/email/{email}")
    @Timed(value = "user.get_by_email", description = "Time taken to get user by email")
    public ResponseEntity<UserProfileResponse> getUserByEmail(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String email) {
        
        logger.debug("Getting user by email for tenant: {} with email: {}", tenantId, email);
        
        Optional<UserProfileResponse> response = userService.getUserByEmail(tenantId, email);
        return response.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search users by name or email
     */
    @GetMapping("/search")
    @Timed(value = "user.search", description = "Time taken to search users")
    public ResponseEntity<List<UserProfileResponse>> searchUsers(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String q) {
        
        logger.debug("Searching users for tenant: {} with query: {}", tenantId, q);
        
        List<UserProfileResponse> response = userService.searchUsers(tenantId, q);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all users for a tenant
     */
    @GetMapping
    @Timed(value = "user.get_all", description = "Time taken to get all users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        logger.debug("Getting all users for tenant: {}", tenantId);
        
        List<UserProfileResponse> response = userService.getAllUsers(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user count for tenant
     */
    @GetMapping("/count")
    @Timed(value = "user.count", description = "Time taken to count users")
    public ResponseEntity<Long> getUserCount(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        logger.debug("Getting user count for tenant: {}", tenantId);
        
        long count = userService.getUserCount(tenantId);
        return ResponseEntity.ok(count);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is healthy");
    }
}
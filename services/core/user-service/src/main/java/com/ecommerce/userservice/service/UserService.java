package com.ecommerce.userservice.service;

import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserProfileRequest;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.entity.UserPreferences;
import com.ecommerce.userservice.repository.UserPreferencesRepository;
import com.ecommerce.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for user management operations
 */
@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    @Autowired
    public UserService(UserRepository userRepository, 
                      UserPreferencesRepository userPreferencesRepository) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
    }

    /**
     * Create a new user profile
     */
    public UserProfileResponse createUser(String tenantId, CreateUserRequest request) {
        logger.info("Creating user profile for tenant: {} with authUserId: {}", tenantId, request.getAuthUserId());

        // Check if user already exists
        if (userRepository.existsByAuthUserIdAndTenantId(request.getAuthUserId(), tenantId)) {
            throw new DataIntegrityViolationException("User already exists with auth user ID: " + request.getAuthUserId());
        }

        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new DataIntegrityViolationException("User already exists with email: " + request.getEmail());
        }

        // Create user entity
        User user = new User(tenantId, request.getAuthUserId(), request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setProfileImageUrl(request.getProfileImageUrl());
        user.setPreferences(request.getPreferences());

        // Save user
        User savedUser = userRepository.save(user);

        // Create default user preferences
        UserPreferences preferences = new UserPreferences(savedUser);
        userPreferencesRepository.save(preferences);
        savedUser.setUserPreferences(preferences);

        logger.info("Successfully created user profile with ID: {} for tenant: {}", savedUser.getId(), tenantId);
        return new UserProfileResponse(savedUser);
    }

    /**
     * Get user profile by auth user ID
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String tenantId, Long authUserId) {
        logger.debug("Retrieving user profile for tenant: {} with authUserId: {}", tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        return new UserProfileResponse(user);
    }

    /**
     * Get user profile by user ID
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileById(String tenantId, Long userId) {
        logger.debug("Retrieving user profile for tenant: {} with userId: {}", tenantId, userId);

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return new UserProfileResponse(user);
    }

    /**
     * Update user profile
     */
    public UserProfileResponse updateUserProfile(String tenantId, Long authUserId, UpdateUserProfileRequest request) {
        logger.info("Updating user profile for tenant: {} with authUserId: {}", tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        // Check if email is being changed and if it's already taken
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
                throw new DataIntegrityViolationException("Email already exists: " + request.getEmail());
            }
            // Clear email cache
            userRepository.evictUserCacheByEmail(tenantId, user.getEmail());
            user.setEmail(request.getEmail());
        }

        // Update user fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getPreferences() != null) {
            user.setPreferences(request.getPreferences());
        }

        User updatedUser = userRepository.save(user);
        
        // Clear cache
        userRepository.evictUserCache(tenantId, authUserId);

        logger.info("Successfully updated user profile with ID: {} for tenant: {}", updatedUser.getId(), tenantId);
        return new UserProfileResponse(updatedUser);
    }

    /**
     * Delete user profile
     */
    public void deleteUser(String tenantId, Long authUserId) {
        logger.info("Deleting user profile for tenant: {} with authUserId: {}", tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        // Clear cache before deletion
        userRepository.evictUserCache(tenantId, authUserId);
        userRepository.evictUserCacheByEmail(tenantId, user.getEmail());

        userRepository.delete(user);
        logger.info("Successfully deleted user profile with ID: {} for tenant: {}", user.getId(), tenantId);
    }

    /**
     * Check if user exists by auth user ID
     */
    @Transactional(readOnly = true)
    public boolean userExists(String tenantId, Long authUserId) {
        return userRepository.existsByAuthUserIdAndTenantId(authUserId, tenantId);
    }

    /**
     * Check if user exists by email
     */
    @Transactional(readOnly = true)
    public boolean userExistsByEmail(String tenantId, String email) {
        return userRepository.existsByEmailAndTenantId(email, tenantId);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public Optional<UserProfileResponse> getUserByEmail(String tenantId, String email) {
        return userRepository.findByEmailAndTenantId(email, tenantId)
                .map(UserProfileResponse::new);
    }

    /**
     * Search users by name or email
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsers(String tenantId, String searchTerm) {
        logger.debug("Searching users for tenant: {} with term: {}", tenantId, searchTerm);

        List<User> users = userRepository.searchUsersByNameOrEmail(tenantId, searchTerm);
        return users.stream()
                .map(UserProfileResponse::new)
                .toList();
    }

    /**
     * Get all users for a tenant
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers(String tenantId) {
        logger.debug("Retrieving all users for tenant: {}", tenantId);

        List<User> users = userRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return users.stream()
                .map(UserProfileResponse::new)
                .toList();
    }

    /**
     * Get user count for tenant
     */
    @Transactional(readOnly = true)
    public long getUserCount(String tenantId) {
        return userRepository.countByTenantId(tenantId);
    }
}
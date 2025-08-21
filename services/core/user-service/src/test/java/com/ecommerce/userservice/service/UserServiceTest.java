package com.ecommerce.userservice.service;

import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserProfileRequest;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.entity.UserPreferences;
import com.ecommerce.userservice.repository.UserPreferencesRepository;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @InjectMocks
    private UserService userService;

    private String tenantId;
    private Long authUserId;
    private User testUser;
    private CreateUserRequest createUserRequest;
    private UpdateUserProfileRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        tenantId = "tenant123";
        authUserId = 1L;

        testUser = new User(tenantId, authUserId, "test@example.com");
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhone("+1234567890");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 1));

        createUserRequest = new CreateUserRequest();
        createUserRequest.setAuthUserId(authUserId);
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setFirstName("John");
        createUserRequest.setLastName("Doe");
        createUserRequest.setPhone("+1234567890");
        createUserRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        updateUserRequest = new UpdateUserProfileRequest();
        updateUserRequest.setFirstName("Jane");
        updateUserRequest.setLastName("Smith");
        updateUserRequest.setPhone("+0987654321");
    }

    @Test
    void createUser_Success() {
        // Given
        when(userRepository.existsByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(false);
        when(userRepository.existsByEmailAndTenantId("test@example.com", tenantId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(new UserPreferences(testUser));

        // When
        UserProfileResponse response = userService.createUser(tenantId, createUserRequest);

        // Then
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getFirstName(), response.getFirstName());
        assertEquals(testUser.getLastName(), response.getLastName());

        verify(userRepository).existsByAuthUserIdAndTenantId(authUserId, tenantId);
        verify(userRepository).existsByEmailAndTenantId("test@example.com", tenantId);
        verify(userRepository).save(any(User.class));
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    void createUser_UserAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(true);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, 
                    () -> userService.createUser(tenantId, createUserRequest));

        verify(userRepository).existsByAuthUserIdAndTenantId(authUserId, tenantId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(false);
        when(userRepository.existsByEmailAndTenantId("test@example.com", tenantId)).thenReturn(true);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, 
                    () -> userService.createUser(tenantId, createUserRequest));

        verify(userRepository).existsByAuthUserIdAndTenantId(authUserId, tenantId);
        verify(userRepository).existsByEmailAndTenantId("test@example.com", tenantId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserProfile_Success() {
        // Given
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(Optional.of(testUser));

        // When
        UserProfileResponse response = userService.getUserProfile(tenantId, authUserId);

        // Then
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getFirstName(), response.getFirstName());

        verify(userRepository).findByAuthUserIdAndTenantId(authUserId, tenantId);
    }

    @Test
    void getUserProfile_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
                    () -> userService.getUserProfile(tenantId, authUserId));

        verify(userRepository).findByAuthUserIdAndTenantId(authUserId, tenantId);
    }

    @Test
    void getUserProfileById_Success() {
        // Given
        Long userId = 1L;
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(testUser));

        // When
        UserProfileResponse response = userService.getUserProfileById(tenantId, userId);

        // Then
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getEmail(), response.getEmail());

        verify(userRepository).findByIdAndTenantId(userId, tenantId);
    }

    @Test
    void updateUserProfile_Success() {
        // Given
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserProfileResponse response = userService.updateUserProfile(tenantId, authUserId, updateUserRequest);

        // Then
        assertNotNull(response);
        verify(userRepository).findByAuthUserIdAndTenantId(authUserId, tenantId);
        verify(userRepository).save(testUser);
        verify(userRepository).evictUserCache(tenantId, authUserId);
    }

    @Test
    void updateUserProfile_EmailChange_Success() {
        // Given
        updateUserRequest.setEmail("newemail@example.com");
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndTenantId("newemail@example.com", tenantId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserProfileResponse response = userService.updateUserProfile(tenantId, authUserId, updateUserRequest);

        // Then
        assertNotNull(response);
        verify(userRepository).existsByEmailAndTenantId("newemail@example.com", tenantId);
        verify(userRepository).evictUserCacheByEmail(tenantId, "test@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfile_EmailAlreadyExists_ThrowsException() {
        // Given
        updateUserRequest.setEmail("existing@example.com");
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndTenantId("existing@example.com", tenantId)).thenReturn(true);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, 
                    () -> userService.updateUserProfile(tenantId, authUserId, updateUserRequest));

        verify(userRepository).existsByEmailAndTenantId("existing@example.com", tenantId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        // Given
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(tenantId, authUserId);

        // Then
        verify(userRepository).findByAuthUserIdAndTenantId(authUserId, tenantId);
        verify(userRepository).evictUserCache(tenantId, authUserId);
        verify(userRepository).evictUserCacheByEmail(tenantId, testUser.getEmail());
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
                    () -> userService.deleteUser(tenantId, authUserId));

        verify(userRepository).findByAuthUserIdAndTenantId(authUserId, tenantId);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void userExists_ReturnsTrue() {
        // Given
        when(userRepository.existsByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(true);

        // When
        boolean exists = userService.userExists(tenantId, authUserId);

        // Then
        assertTrue(exists);
        verify(userRepository).existsByAuthUserIdAndTenantId(authUserId, tenantId);
    }

    @Test
    void userExists_ReturnsFalse() {
        // Given
        when(userRepository.existsByAuthUserIdAndTenantId(authUserId, tenantId)).thenReturn(false);

        // When
        boolean exists = userService.userExists(tenantId, authUserId);

        // Then
        assertFalse(exists);
        verify(userRepository).existsByAuthUserIdAndTenantId(authUserId, tenantId);
    }

    @Test
    void userExistsByEmail_ReturnsTrue() {
        // Given
        String email = "test@example.com";
        when(userRepository.existsByEmailAndTenantId(email, tenantId)).thenReturn(true);

        // When
        boolean exists = userService.userExistsByEmail(tenantId, email);

        // Then
        assertTrue(exists);
        verify(userRepository).existsByEmailAndTenantId(email, tenantId);
    }

    @Test
    void getUserByEmail_Success() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmailAndTenantId(email, tenantId)).thenReturn(Optional.of(testUser));

        // When
        Optional<UserProfileResponse> response = userService.getUserByEmail(tenantId, email);

        // Then
        assertTrue(response.isPresent());
        assertEquals(testUser.getId(), response.get().getId());
        verify(userRepository).findByEmailAndTenantId(email, tenantId);
    }

    @Test
    void searchUsers_Success() {
        // Given
        String searchTerm = "John";
        List<User> users = Arrays.asList(testUser);
        when(userRepository.searchUsersByNameOrEmail(tenantId, searchTerm)).thenReturn(users);

        // When
        List<UserProfileResponse> response = userService.searchUsers(tenantId, searchTerm);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testUser.getId(), response.get(0).getId());
        verify(userRepository).searchUsersByNameOrEmail(tenantId, searchTerm);
    }

    @Test
    void getAllUsers_Success() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(users);

        // When
        List<UserProfileResponse> response = userService.getAllUsers(tenantId);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testUser.getId(), response.get(0).getId());
        verify(userRepository).findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Test
    void getUserCount_Success() {
        // Given
        long expectedCount = 5L;
        when(userRepository.countByTenantId(tenantId)).thenReturn(expectedCount);

        // When
        long count = userService.getUserCount(tenantId);

        // Then
        assertEquals(expectedCount, count);
        verify(userRepository).countByTenantId(tenantId);
    }
}
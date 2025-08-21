package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserRepository
 */
@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private String tenantId;
    private Long authUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        authUserId = 1L;
        
        testUser = new User(tenantId, authUserId, "test@example.com");
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }

    @Test
    void findByAuthUserIdAndTenantId_Success() {
        // Given
        when(userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId))
                .thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByAuthUserIdAndTenantId(authUserId, tenantId);
    }

    @Test
    void findByEmailAndTenantId_Success() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmailAndTenantId(email, tenantId))
                .thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userRepository.findByEmailAndTenantId(email, tenantId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByEmailAndTenantId(email, tenantId);
    }

    @Test
    void existsByAuthUserIdAndTenantId_ReturnsTrue() {
        // Given
        when(userRepository.existsByAuthUserIdAndTenantId(authUserId, tenantId))
                .thenReturn(true);

        // When
        boolean exists = userRepository.existsByAuthUserIdAndTenantId(authUserId, tenantId);

        // Then
        assertTrue(exists);
        verify(userRepository).existsByAuthUserIdAndTenantId(authUserId, tenantId);
    }

    @Test
    void existsByEmailAndTenantId_ReturnsTrue() {
        // Given
        String email = "test@example.com";
        when(userRepository.existsByEmailAndTenantId(email, tenantId))
                .thenReturn(true);

        // When
        boolean exists = userRepository.existsByEmailAndTenantId(email, tenantId);

        // Then
        assertTrue(exists);
        verify(userRepository).existsByEmailAndTenantId(email, tenantId);
    }

    @Test
    void save_Success() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User savedUser = userRepository.save(testUser);

        // Then
        assertNotNull(savedUser);
        assertEquals(testUser.getId(), savedUser.getId());
        verify(userRepository).save(testUser);
    }

    @Test
    void delete_Success() {
        // When
        userRepository.delete(testUser);

        // Then
        verify(userRepository).delete(testUser);
    }

    @Test
    void countByTenantId_ReturnsCount() {
        // Given
        long expectedCount = 5L;
        when(userRepository.countByTenantId(tenantId)).thenReturn(expectedCount);

        // When
        long count = userRepository.countByTenantId(tenantId);

        // Then
        assertEquals(expectedCount, count);
        verify(userRepository).countByTenantId(tenantId);
    }
}
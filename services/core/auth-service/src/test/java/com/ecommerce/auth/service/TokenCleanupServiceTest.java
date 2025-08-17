package com.ecommerce.auth.service;

import com.ecommerce.auth.config.TokenCleanupProperties;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for TokenCleanupService
 */
@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenCleanupProperties cleanupProperties;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(refreshTokenRepository, cleanupProperties);
    }

    @Test
    void cleanupTokens_ShouldCallBothCleanupMethods() {
        // Given
        doNothing().when(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        doNothing().when(refreshTokenRepository).deleteRevokedTokensOlderThan(any(LocalDateTime.class));

        // When
        tokenCleanupService.cleanupTokens();

        // Then
        verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteRevokedTokensOlderThan(any(LocalDateTime.class));
    }

    @Test
    void cleanupExpiredTokens_ShouldCallRepositoryDeleteExpiredTokens() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        doNothing().when(refreshTokenRepository).deleteExpiredTokens(now);

        // When
        int result = tokenCleanupService.cleanupExpiredTokens(now);

        // Then
        verify(refreshTokenRepository).deleteExpiredTokens(now);
        // Note: Result is 0 because JPA bulk delete doesn't return count
    }

    @Test
    void cleanupOldRevokedTokens_ShouldCallRepositoryWithCorrectCutoffDate() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Duration retentionPeriod = Duration.ofDays(30);
        LocalDateTime expectedCutoffDate = now.minus(retentionPeriod);
        
        when(cleanupProperties.getRevokedTokenRetentionPeriod()).thenReturn(retentionPeriod);
        doNothing().when(refreshTokenRepository).deleteRevokedTokensOlderThan(any(LocalDateTime.class));

        // When
        int result = tokenCleanupService.cleanupOldRevokedTokens(now);

        // Then
        verify(refreshTokenRepository).deleteRevokedTokensOlderThan(argThat(cutoffDate -> 
            cutoffDate.isEqual(expectedCutoffDate) || 
            cutoffDate.isBefore(expectedCutoffDate.plusSeconds(1)) && cutoffDate.isAfter(expectedCutoffDate.minusSeconds(1))
        ));
    }

    @Test
    void cleanupExpiredTokens_ShouldPropagateException() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        RuntimeException expectedException = new RuntimeException("Database error");
        doThrow(expectedException).when(refreshTokenRepository).deleteExpiredTokens(now);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            tokenCleanupService.cleanupExpiredTokens(now);
        });
        
        verify(refreshTokenRepository).deleteExpiredTokens(now);
        assert thrown == expectedException;
    }

    @Test
    void cleanupOldRevokedTokens_ShouldPropagateException() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        RuntimeException expectedException = new RuntimeException("Database error");
        when(cleanupProperties.getRevokedTokenRetentionPeriod()).thenReturn(Duration.ofDays(30));
        doThrow(expectedException).when(refreshTokenRepository).deleteRevokedTokensOlderThan(any(LocalDateTime.class));

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            tokenCleanupService.cleanupOldRevokedTokens(now);
        });
        
        verify(refreshTokenRepository).deleteRevokedTokensOlderThan(any(LocalDateTime.class));
        assert thrown == expectedException;
    }

    @Test
    void performManualCleanup_ShouldCallCleanupTokens() {
        // Given
        doNothing().when(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        doNothing().when(refreshTokenRepository).deleteRevokedTokensOlderThan(any(LocalDateTime.class));

        // When
        tokenCleanupService.performManualCleanup();

        // Then
        verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteRevokedTokensOlderThan(any(LocalDateTime.class));
    }

    @Test
    void cleanupTokens_ShouldHandleExceptionAndRethrow() {
        // Given
        RuntimeException expectedException = new RuntimeException("Database connection failed");
        doThrow(expectedException).when(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            tokenCleanupService.cleanupTokens();
        });
        
        verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        assert thrown == expectedException;
    }
}
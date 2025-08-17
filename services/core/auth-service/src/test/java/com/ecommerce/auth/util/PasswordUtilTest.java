package com.ecommerce.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
    }

    @Test
    void testHashPassword_ValidPassword_ShouldReturnHashedPassword() {
        String plainPassword = "StrongPass123!";
        
        String hashedPassword = passwordUtil.hashPassword(plainPassword);
        
        assertNotNull(hashedPassword);
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"));
    }

    @Test
    void testHashPassword_NullPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.hashPassword(null);
        });
    }

    @Test
    void testHashPassword_EmptyPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.hashPassword("");
        });
    }

    @Test
    void testHashPassword_WeakPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.hashPassword("weak");
        });
    }

    @Test
    void testVerifyPassword_CorrectPassword_ShouldReturnTrue() {
        String plainPassword = "StrongPass123!";
        String hashedPassword = passwordUtil.hashPassword(plainPassword);
        
        boolean result = passwordUtil.verifyPassword(plainPassword, hashedPassword);
        
        assertTrue(result);
    }

    @Test
    void testVerifyPassword_IncorrectPassword_ShouldReturnFalse() {
        String plainPassword = "StrongPass123!";
        String wrongPassword = "WrongPass123!";
        String hashedPassword = passwordUtil.hashPassword(plainPassword);
        
        boolean result = passwordUtil.verifyPassword(wrongPassword, hashedPassword);
        
        assertFalse(result);
    }

    @Test
    void testVerifyPassword_NullPassword_ShouldReturnFalse() {
        String hashedPassword = passwordUtil.hashPassword("StrongPass123!");
        
        boolean result = passwordUtil.verifyPassword(null, hashedPassword);
        
        assertFalse(result);
    }

    @Test
    void testVerifyPassword_NullHash_ShouldReturnFalse() {
        boolean result = passwordUtil.verifyPassword("StrongPass123!", null);
        
        assertFalse(result);
    }

    @Test
    void testValidatePasswordStrength_ValidPassword_ShouldNotThrow() {
        assertDoesNotThrow(() -> {
            passwordUtil.validatePasswordStrength("StrongPass123!");
        });
    }

    @Test
    void testValidatePasswordStrength_TooShort_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.validatePasswordStrength("Short1!");
        });
    }

    @Test
    void testValidatePasswordStrength_NoUppercase_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.validatePasswordStrength("strongpass123!");
        });
    }

    @Test
    void testValidatePasswordStrength_NoLowercase_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.validatePasswordStrength("STRONGPASS123!");
        });
    }

    @Test
    void testValidatePasswordStrength_NoDigit_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.validatePasswordStrength("StrongPass!");
        });
    }

    @Test
    void testValidatePasswordStrength_NoSpecialChar_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.validatePasswordStrength("StrongPass123");
        });
    }

    @Test
    void testValidatePasswordStrength_CommonPassword_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.validatePasswordStrength("Password123!");
        });
    }

    @Test
    void testGenerateSecurePassword_ShouldReturnValidPassword() {
        String password = passwordUtil.generateSecurePassword(12);
        
        assertNotNull(password);
        assertEquals(12, password.length());
        assertDoesNotThrow(() -> {
            passwordUtil.validatePasswordStrength(password);
        });
    }

    @Test
    void testGenerateSecurePassword_MinLength_ShouldReturnMinLengthPassword() {
        String password = passwordUtil.generateSecurePassword(4);
        
        assertNotNull(password);
        assertEquals(8, password.length()); // Should use minimum length
    }

    @Test
    void testNeedsRehash_OldHash_ShouldReturnTrue() {
        // Simulate old hash with lower strength
        String oldHash = "$2a$10$abcdefghijklmnopqrstuvwxyz";
        
        boolean result = passwordUtil.needsRehash(oldHash);
        
        assertTrue(result);
    }

    @Test
    void testNeedsRehash_CurrentHash_ShouldReturnFalse() {
        String password = "StrongPass123!";
        String currentHash = passwordUtil.hashPassword(password);
        
        boolean result = passwordUtil.needsRehash(currentHash);
        
        assertFalse(result);
    }

    @Test
    void testNeedsRehash_InvalidHash_ShouldReturnTrue() {
        boolean result = passwordUtil.needsRehash("invalid-hash");
        
        assertTrue(result);
    }

    @Test
    void testNeedsRehash_NullHash_ShouldReturnTrue() {
        boolean result = passwordUtil.needsRehash(null);
        
        assertTrue(result);
    }
}
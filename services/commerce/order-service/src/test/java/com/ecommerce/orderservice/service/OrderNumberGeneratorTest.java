package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderNumberGeneratorTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderNumberGenerator orderNumberGenerator;

    private String tenantId = "tenant123";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
        // Use reflection to set the orderPrefix field
        try {
            var field = OrderNumberGenerator.class.getDeclaredField("orderPrefix");
            field.setAccessible(true);
            field.set(orderNumberGenerator, "ORD");
        } catch (Exception e) {
            // Fallback - create new instance with constructor
            orderNumberGenerator = new OrderNumberGenerator(orderRepository, "ORD");
        }
    }

    @Test
    void generateUniqueOrderNumber_FirstAttempt_ShouldReturnValidOrderNumber() {
        // Given
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);

        // When
        String orderNumber = orderNumberGenerator.generateUniqueOrderNumber();

        // Then
        assertNotNull(orderNumber);
        assertTrue(orderNumber.startsWith("ORD-"));
        assertTrue(isValidOrderNumberFormat(orderNumber));
        verify(orderRepository, times(1)).existsByOrderNumber(anyString());
    }

    @Test
    void generateUniqueOrderNumber_WithCollisions_ShouldRetryAndSucceed() {
        // Given
        when(orderRepository.existsByOrderNumber(anyString()))
            .thenReturn(true)  // First attempt - collision
            .thenReturn(true)  // Second attempt - collision
            .thenReturn(false); // Third attempt - success

        // When
        String orderNumber = orderNumberGenerator.generateUniqueOrderNumber();

        // Then
        assertNotNull(orderNumber);
        assertTrue(orderNumber.startsWith("ORD-"));
        assertTrue(isValidOrderNumberFormat(orderNumber));
        verify(orderRepository, times(3)).existsByOrderNumber(anyString());
    }

    @Test
    void generateUniqueOrderNumber_MaxCollisions_ShouldUseFallback() {
        // Given
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(true);

        // When
        String orderNumber = orderNumberGenerator.generateUniqueOrderNumber();

        // Then
        assertNotNull(orderNumber);
        assertTrue(orderNumber.startsWith("ORD-"));
        // Fallback format has more parts (includes timestamp)
        String[] parts = orderNumber.split("-");
        assertTrue(parts.length >= 5); // ORD-YYYYMMDDHHMMSS-XXXX-XXXXXXXX-TIMESTAMP
        verify(orderRepository, times(5)).existsByOrderNumber(anyString()); // 5 attempts before fallback
    }

    @Test
    void isValidOrderNumberFormat_ValidFormat_ShouldReturnTrue() {
        // Given
        String validOrderNumber = "ORD-20240101-A1B2-C3D4E5F6";

        // When
        boolean isValid = orderNumberGenerator.isValidOrderNumberFormat(validOrderNumber);

        // Then
        assertTrue(isValid);
    }

    @Test
    void isValidOrderNumberFormat_InvalidPrefix_ShouldReturnFalse() {
        // Given
        String invalidOrderNumber = "INV-20240101-A1B2-C3D4E5F6";

        // When
        boolean isValid = orderNumberGenerator.isValidOrderNumberFormat(invalidOrderNumber);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isValidOrderNumberFormat_InvalidDatePart_ShouldReturnFalse() {
        // Given
        String invalidOrderNumber = "ORD-2024010-A1B2-C3D4E5F6"; // 7 digits instead of 8

        // When
        boolean isValid = orderNumberGenerator.isValidOrderNumberFormat(invalidOrderNumber);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isValidOrderNumberFormat_InvalidTenantHash_ShouldReturnFalse() {
        // Given
        String invalidOrderNumber = "ORD-20240101-A1B-C3D4E5F6"; // 3 chars instead of 4

        // When
        boolean isValid = orderNumberGenerator.isValidOrderNumberFormat(invalidOrderNumber);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isValidOrderNumberFormat_InvalidRandomPart_ShouldReturnFalse() {
        // Given
        String invalidOrderNumber = "ORD-20240101-A1B2-C3D4E5"; // 6 chars instead of 8

        // When
        boolean isValid = orderNumberGenerator.isValidOrderNumberFormat(invalidOrderNumber);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isValidOrderNumberFormat_NullOrEmpty_ShouldReturnFalse() {
        // When & Then
        assertFalse(orderNumberGenerator.isValidOrderNumberFormat(null));
        assertFalse(orderNumberGenerator.isValidOrderNumberFormat(""));
        assertFalse(orderNumberGenerator.isValidOrderNumberFormat("   "));
    }

    @Test
    void extractDateFromOrderNumber_ValidOrderNumber_ShouldReturnCorrectDate() {
        // Given
        String orderNumber = "ORD-20240315-A1B2-C3D4E5F6";

        // When
        LocalDateTime extractedDate = orderNumberGenerator.extractDateFromOrderNumber(orderNumber);

        // Then
        assertNotNull(extractedDate);
        assertEquals(2024, extractedDate.getYear());
        assertEquals(3, extractedDate.getMonthValue());
        assertEquals(15, extractedDate.getDayOfMonth());
        assertEquals(0, extractedDate.getHour());
        assertEquals(0, extractedDate.getMinute());
        assertEquals(0, extractedDate.getSecond());
    }

    @Test
    void extractDateFromOrderNumber_InvalidOrderNumber_ShouldThrowException() {
        // Given
        String invalidOrderNumber = "INVALID-ORDER-NUMBER";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            orderNumberGenerator.extractDateFromOrderNumber(invalidOrderNumber));
    }

    @Test
    void orderNumberFormat_ShouldContainCurrentDate() {
        // Given
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        String expectedDatePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // When
        String orderNumber = orderNumberGenerator.generateUniqueOrderNumber();

        // Then
        assertTrue(orderNumber.contains(expectedDatePart));
    }

    @Test
    void orderNumberFormat_ShouldContainTenantHash() {
        // Given
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);

        // When
        String orderNumber = orderNumberGenerator.generateUniqueOrderNumber();

        // Then
        String[] parts = orderNumber.split("-");
        assertEquals(4, parts.length);
        assertEquals(4, parts[2].length()); // Tenant hash should be 4 characters
        assertTrue(parts[2].matches("[0-9A-F]{4}")); // Should be hex
    }

    @Test
    void orderNumberFormat_ShouldHaveRandomPart() {
        // Given
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);

        // When
        String orderNumber1 = orderNumberGenerator.generateUniqueOrderNumber();
        String orderNumber2 = orderNumberGenerator.generateUniqueOrderNumber();

        // Then
        String[] parts1 = orderNumber1.split("-");
        String[] parts2 = orderNumber2.split("-");
        
        // Random parts should be different (very high probability)
        assertNotEquals(parts1[3], parts2[3]);
        
        // Both should be 8 alphanumeric characters
        assertTrue(parts1[3].matches("[0-9A-Z]{8}"));
        assertTrue(parts2[3].matches("[0-9A-Z]{8}"));
    }

    private boolean isValidOrderNumberFormat(String orderNumber) {
        return orderNumberGenerator.isValidOrderNumberFormat(orderNumber);
    }
}
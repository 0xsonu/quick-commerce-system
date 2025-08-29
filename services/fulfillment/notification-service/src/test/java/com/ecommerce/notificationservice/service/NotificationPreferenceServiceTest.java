package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.client.UserServiceClient;
import com.ecommerce.notificationservice.dto.NotificationPreferenceRequest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationPreference;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.repository.NotificationPreferenceRepository;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.userservice.proto.UserServiceProtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserServiceClient userServiceClient;

    private NotificationPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new NotificationPreferenceService(preferenceRepository, userServiceClient);
        
        // Set up tenant context
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("456");
    }

    @Test
    void testUpdatePreference_NewPreference() {
        // Given
        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setUserId(456L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setIsEnabled(true);

        when(userServiceClient.validateUser(456L)).thenReturn(true);
        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            "tenant123", 456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL))
            .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationPreference result = preferenceService.updatePreference(request);

        // Then
        assertNotNull(result);
        assertEquals("tenant123", result.getTenantId());
        assertEquals(456L, result.getUserId());
        assertEquals(NotificationType.ORDER_CREATED, result.getNotificationType());
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
        assertTrue(result.getIsEnabled());

        verify(userServiceClient).validateUser(456L);
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void testUpdatePreference_ExistingPreference() {
        // Given
        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setUserId(456L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setIsEnabled(false);

        NotificationPreference existing = new NotificationPreference();
        existing.setTenantId("tenant123");
        existing.setUserId(456L);
        existing.setNotificationType(NotificationType.ORDER_CREATED);
        existing.setChannel(NotificationChannel.EMAIL);
        existing.setIsEnabled(true);

        when(userServiceClient.validateUser(456L)).thenReturn(true);
        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            "tenant123", 456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL))
            .thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(NotificationPreference.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationPreference result = preferenceService.updatePreference(request);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsEnabled());
        verify(preferenceRepository).save(existing);
    }

    @Test
    void testUpdatePreference_InvalidUser() {
        // Given
        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setUserId(456L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setIsEnabled(true);

        when(userServiceClient.validateUser(456L)).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> preferenceService.updatePreference(request));
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void testGetUserPreferences() {
        // Given
        List<NotificationPreference> preferences = Arrays.asList(
            createTestPreference(NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, true),
            createTestPreference(NotificationType.ORDER_SHIPPED, NotificationChannel.SMS, false)
        );

        when(userServiceClient.validateUser(456L)).thenReturn(true);
        when(preferenceRepository.findByTenantIdAndUserId("tenant123", 456L))
            .thenReturn(preferences);

        // When
        List<NotificationPreference> result = preferenceService.getUserPreferences(456L);

        // Then
        assertEquals(2, result.size());
        assertEquals(preferences, result);
        verify(userServiceClient).validateUser(456L);
    }

    @Test
    void testGetUserPreferences_InvalidUser() {
        // Given
        when(userServiceClient.validateUser(456L)).thenReturn(false);

        // When
        List<NotificationPreference> result = preferenceService.getUserPreferences(456L);

        // Then
        assertTrue(result.isEmpty());
        verify(preferenceRepository, never()).findByTenantIdAndUserId(anyString(), anyLong());
    }

    @Test
    void testIsNotificationEnabled_PreferenceExists() {
        // Given
        NotificationPreference preference = createTestPreference(
            NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, false);

        when(userServiceClient.validateUser(456L)).thenReturn(true);
        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            "tenant123", 456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL))
            .thenReturn(Optional.of(preference));

        // When
        boolean result = preferenceService.isNotificationEnabled(
            456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsNotificationEnabled_NoPreference_DefaultEnabled() {
        // Given
        when(userServiceClient.validateUser(456L)).thenReturn(true);
        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            "tenant123", 456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL))
            .thenReturn(Optional.empty());

        // When
        boolean result = preferenceService.isNotificationEnabled(
            456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL);

        // Then
        assertTrue(result); // Default is enabled
    }

    @Test
    void testIsNotificationEnabled_InvalidUser() {
        // Given
        when(userServiceClient.validateUser(456L)).thenReturn(false);

        // When
        boolean result = preferenceService.isNotificationEnabled(
            456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL);

        // Then
        assertFalse(result);
        verify(preferenceRepository, never()).findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            anyString(), anyLong(), any(), any());
    }

    @Test
    void testOptOutAllNotifications() {
        // Given
        when(userServiceClient.validateUser(456L)).thenReturn(true);
        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            anyString(), anyLong(), any(), any()))
            .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        preferenceService.optOutAllNotifications(456L);

        // Then
        verify(userServiceClient).validateUser(456L);
        
        // Should save preferences for all notification types and channels
        int expectedSaves = NotificationType.values().length * NotificationChannel.values().length;
        verify(preferenceRepository, times(expectedSaves)).save(any(NotificationPreference.class));

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(preferenceRepository, times(expectedSaves)).save(captor.capture());
        
        // All saved preferences should be disabled
        captor.getAllValues().forEach(pref -> assertFalse(pref.getIsEnabled()));
    }

    @Test
    void testOptInDefaultNotifications() {
        // Given
        when(userServiceClient.validateUser(456L)).thenReturn(true);
        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            anyString(), anyLong(), any(), any()))
            .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        preferenceService.optInDefaultNotifications(456L);

        // Then
        verify(userServiceClient).validateUser(456L);
        
        // Should save preferences for default notification types
        verify(preferenceRepository, atLeastOnce()).save(any(NotificationPreference.class));

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(preferenceRepository, atLeastOnce()).save(captor.capture());
        
        // All saved preferences should be enabled
        captor.getAllValues().forEach(pref -> assertTrue(pref.getIsEnabled()));
    }

    @Test
    void testGetUserContactInfo() {
        // Given
        UserServiceProtos.User user = UserServiceProtos.User.newBuilder()
            .setId(456L)
            .setEmail("test@example.com")
            .setPhone("+1234567890")
            .build();

        List<UserServiceProtos.UserAddress> addresses = Arrays.asList(
            UserServiceProtos.UserAddress.newBuilder()
                .setId(1L)
                .setType("BILLING")
                .setIsDefault(true)
                .build()
        );

        when(userServiceClient.getUser(456L)).thenReturn(user);
        when(userServiceClient.getUserAddresses(456L)).thenReturn(addresses);

        // When
        NotificationPreferenceService.UserContactInfo result = preferenceService.getUserContactInfo(456L);

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("+1234567890", result.getPhone());
        assertEquals(1, result.getAddresses().size());
        assertEquals("BILLING", result.getAddresses().get(0).getType());
    }

    @Test
    void testGetUserContactInfo_ServiceFailure() {
        // Given
        when(userServiceClient.getUser(456L)).thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        assertThrows(RuntimeException.class, () -> preferenceService.getUserContactInfo(456L));
    }

    @Test
    void testDeletePreference() {
        // Given
        NotificationPreference preference = createTestPreference(
            NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, true);

        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            "tenant123", 456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL))
            .thenReturn(Optional.of(preference));

        // When
        preferenceService.deletePreference(456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL);

        // Then
        verify(preferenceRepository).delete(preference);
    }

    @Test
    void testDeletePreference_NotFound() {
        // Given
        when(preferenceRepository.findByTenantIdAndUserIdAndNotificationTypeAndChannel(
            "tenant123", 456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL))
            .thenReturn(Optional.empty());

        // When
        preferenceService.deletePreference(456L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL);

        // Then
        verify(preferenceRepository, never()).delete(any());
    }

    private NotificationPreference createTestPreference(NotificationType type, NotificationChannel channel, boolean enabled) {
        NotificationPreference preference = new NotificationPreference();
        preference.setTenantId("tenant123");
        preference.setUserId(456L);
        preference.setNotificationType(type);
        preference.setChannel(channel);
        preference.setIsEnabled(enabled);
        return preference;
    }
}
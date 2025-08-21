package com.ecommerce.userservice.service;

import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import com.ecommerce.userservice.dto.CreateAddressRequest;
import com.ecommerce.userservice.dto.UpdateAddressRequest;
import com.ecommerce.userservice.dto.UserAddressResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.entity.UserAddress;
import com.ecommerce.userservice.repository.UserAddressRepository;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAddressService addressService;

    private User testUser;
    private UserAddress testAddress;
    private CreateAddressRequest createRequest;
    private UpdateAddressRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("tenant1", 123L, "test@example.com");
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        testAddress = new UserAddress(testUser, UserAddress.AddressType.SHIPPING,
                "123 Main St", "New York", "NY", "10001", "USA");
        testAddress.setId(1L);
        testAddress.setIsDefault(true);

        createRequest = new CreateAddressRequest(UserAddress.AddressType.SHIPPING,
                "456 Oak Ave", "Los Angeles", "CA", "90210", "USA");
        createRequest.setIsDefault(false);

        updateRequest = new UpdateAddressRequest();
        updateRequest.setStreetAddress("789 Pine St");
        updateRequest.setCity("Chicago");
        updateRequest.setState("IL");
        updateRequest.setPostalCode("60601");
        updateRequest.setCountry("USA");
    }

    @Test
    void createAddress_Success() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(2L);
        when(addressRepository.save(any(UserAddress.class))).thenReturn(testAddress);

        // Act
        UserAddressResponse response = addressService.createAddress("tenant1", 123L, createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testAddress.getId(), response.getId());
        assertEquals(testAddress.getType(), response.getType());
        assertEquals(testAddress.getStreetAddress(), response.getStreetAddress());
        verify(userRepository).findByAuthUserIdAndTenantId(123L, "tenant1");
        verify(addressRepository).countByUserId(1L);
        verify(addressRepository).save(any(UserAddress.class));
    }

    @Test
    void createAddress_UserNotFound() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                addressService.createAddress("tenant1", 123L, createRequest));
        verify(userRepository).findByAuthUserIdAndTenantId(123L, "tenant1");
        verify(addressRepository, never()).save(any());
    }

    @Test
    void createAddress_MaxAddressesReached() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(10L);

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () ->
                addressService.createAddress("tenant1", 123L, createRequest));
        verify(addressRepository, never()).save(any());
    }

    @Test
    void createAddress_SetAsDefault() {
        // Arrange
        createRequest.setIsDefault(true);
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(2L);
        when(addressRepository.save(any(UserAddress.class))).thenReturn(testAddress);

        // Act
        UserAddressResponse response = addressService.createAddress("tenant1", 123L, createRequest);

        // Assert
        assertNotNull(response);
        verify(addressRepository).setAllAddressesNonDefaultByUserIdAndType(1L, UserAddress.AddressType.SHIPPING);
        verify(addressRepository).save(any(UserAddress.class));
    }

    @Test
    void createAddress_InvalidStreetAddress() {
        // Arrange
        createRequest.setStreetAddress("");
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(2L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                addressService.createAddress("tenant1", 123L, createRequest));
        verify(addressRepository, never()).save(any());
    }

    @Test
    void getUserAddresses_Success() {
        // Arrange
        List<UserAddress> addresses = Arrays.asList(testAddress);
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByUserId(1L)).thenReturn(addresses);

        // Act
        List<UserAddressResponse> response = addressService.getUserAddresses("tenant1", 123L);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testAddress.getId(), response.get(0).getId());
        verify(userRepository).findByAuthUserIdAndTenantId(123L, "tenant1");
        verify(addressRepository).findByUserId(1L);
    }

    @Test
    void getUserAddressesByType_Success() {
        // Arrange
        List<UserAddress> addresses = Arrays.asList(testAddress);
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByUserIdAndType(1L, UserAddress.AddressType.SHIPPING))
                .thenReturn(addresses);

        // Act
        List<UserAddressResponse> response = addressService.getUserAddressesByType(
                "tenant1", 123L, UserAddress.AddressType.SHIPPING);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testAddress.getId(), response.get(0).getId());
        verify(addressRepository).findByUserIdAndType(1L, UserAddress.AddressType.SHIPPING);
    }

    @Test
    void getDefaultAddress_Success() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByUserIdAndTypeAndIsDefaultTrue(1L, UserAddress.AddressType.SHIPPING))
                .thenReturn(Optional.of(testAddress));

        // Act
        UserAddressResponse response = addressService.getDefaultAddress(
                "tenant1", 123L, UserAddress.AddressType.SHIPPING);

        // Assert
        assertNotNull(response);
        assertEquals(testAddress.getId(), response.getId());
        assertTrue(response.getIsDefault());
        verify(addressRepository).findByUserIdAndTypeAndIsDefaultTrue(1L, UserAddress.AddressType.SHIPPING);
    }

    @Test
    void getDefaultAddress_NotFound() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByUserIdAndTypeAndIsDefaultTrue(1L, UserAddress.AddressType.SHIPPING))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                addressService.getDefaultAddress("tenant1", 123L, UserAddress.AddressType.SHIPPING));
    }

    @Test
    void getAddress_Success() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testAddress));

        // Act
        UserAddressResponse response = addressService.getAddress("tenant1", 123L, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(testAddress.getId(), response.getId());
        verify(addressRepository).findByIdAndUserId(1L, 1L);
    }

    @Test
    void getAddress_NotFound() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                addressService.getAddress("tenant1", 123L, 1L));
    }

    @Test
    void updateAddress_Success() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testAddress));
        when(addressRepository.save(any(UserAddress.class))).thenReturn(testAddress);

        // Act
        UserAddressResponse response = addressService.updateAddress("tenant1", 123L, 1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(addressRepository).save(testAddress);
    }

    @Test
    void updateAddress_SetAsDefault() {
        // Arrange
        updateRequest.setIsDefault(true);
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testAddress));
        when(addressRepository.save(any(UserAddress.class))).thenReturn(testAddress);

        // Act
        UserAddressResponse response = addressService.updateAddress("tenant1", 123L, 1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(addressRepository).setAllAddressesNonDefaultByUserIdAndType(1L, testAddress.getType());
        verify(addressRepository).save(testAddress);
    }

    @Test
    void setDefaultAddress_Success() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testAddress));
        when(addressRepository.save(any(UserAddress.class))).thenReturn(testAddress);

        // Act
        UserAddressResponse response = addressService.setDefaultAddress("tenant1", 123L, 1L);

        // Assert
        assertNotNull(response);
        verify(addressRepository).setAllAddressesNonDefaultByUserIdAndType(1L, testAddress.getType());
        verify(addressRepository).save(testAddress);
    }

    @Test
    void deleteAddress_Success() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.existsByIdAndUserId(1L, 1L)).thenReturn(true);

        // Act
        addressService.deleteAddress("tenant1", 123L, 1L);

        // Assert
        verify(addressRepository).deleteByIdAndUserId(1L, 1L);
    }

    @Test
    void deleteAddress_NotFound() {
        // Arrange
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.existsByIdAndUserId(1L, 1L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                addressService.deleteAddress("tenant1", 123L, 1L));
        verify(addressRepository, never()).deleteByIdAndUserId(any(), any());
    }

    @Test
    void validateAddressFields_InvalidCharacters() {
        // Arrange
        createRequest.setStreetAddress("123 Main St @#$");
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(2L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                addressService.createAddress("tenant1", 123L, createRequest));
    }

    @Test
    void validateAddressFields_EmptyCity() {
        // Arrange
        createRequest.setCity("");
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(2L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                addressService.createAddress("tenant1", 123L, createRequest));
    }

    @Test
    void validateAddressFields_InvalidPostalCode() {
        // Arrange
        createRequest.setPostalCode("12345@");
        when(userRepository.findByAuthUserIdAndTenantId(123L, "tenant1"))
                .thenReturn(Optional.of(testUser));
        when(addressRepository.countByUserId(1L)).thenReturn(2L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                addressService.createAddress("tenant1", 123L, createRequest));
    }
}
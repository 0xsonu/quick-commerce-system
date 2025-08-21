package com.ecommerce.userservice.service;

import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import com.ecommerce.userservice.dto.CreateAddressRequest;
import com.ecommerce.userservice.dto.UpdateAddressRequest;
import com.ecommerce.userservice.dto.UserAddressResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.entity.UserAddress;
import com.ecommerce.userservice.repository.UserAddressRepository;
import com.ecommerce.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for user address management operations
 */
@Service
@Transactional
public class UserAddressService {

    private static final Logger logger = LoggerFactory.getLogger(UserAddressService.class);
    private static final int MAX_ADDRESSES_PER_USER = 10;

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserAddressService(UserAddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new address for a user
     */
    public UserAddressResponse createAddress(String tenantId, Long authUserId, CreateAddressRequest request) {
        logger.info("Creating address for tenant: {} with authUserId: {}", tenantId, authUserId);

        // Find the user
        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        // Check address limit
        long addressCount = addressRepository.countByUserId(user.getId());
        if (addressCount >= MAX_ADDRESSES_PER_USER) {
            throw new DataIntegrityViolationException("Maximum number of addresses (" + MAX_ADDRESSES_PER_USER + ") reached");
        }

        // Validate address fields
        validateAddressFields(request.getStreetAddress(), request.getCity(), request.getState(), 
                            request.getPostalCode(), request.getCountry());

        // If this is set as default, unset other default addresses of the same type
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.setAllAddressesNonDefaultByUserIdAndType(user.getId(), request.getType());
        }

        // Create new address
        UserAddress address = new UserAddress(user, request.getType(), request.getStreetAddress(),
                request.getCity(), request.getState(), request.getPostalCode(), request.getCountry());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));

        UserAddress savedAddress = addressRepository.save(address);

        logger.info("Successfully created address with ID: {} for user: {} in tenant: {}", 
                   savedAddress.getId(), user.getId(), tenantId);
        return new UserAddressResponse(savedAddress);
    }

    /**
     * Get all addresses for a user
     */
    @Transactional(readOnly = true)
    public List<UserAddressResponse> getUserAddresses(String tenantId, Long authUserId) {
        logger.debug("Retrieving addresses for tenant: {} with authUserId: {}", tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        List<UserAddress> addresses = addressRepository.findByUserId(user.getId());
        return addresses.stream()
                .map(UserAddressResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get addresses by type for a user
     */
    @Transactional(readOnly = true)
    public List<UserAddressResponse> getUserAddressesByType(String tenantId, Long authUserId, UserAddress.AddressType type) {
        logger.debug("Retrieving {} addresses for tenant: {} with authUserId: {}", type, tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        List<UserAddress> addresses = addressRepository.findByUserIdAndType(user.getId(), type);
        return addresses.stream()
                .map(UserAddressResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get default address by type for a user
     */
    @Transactional(readOnly = true)
    public UserAddressResponse getDefaultAddress(String tenantId, Long authUserId, UserAddress.AddressType type) {
        logger.debug("Retrieving default {} address for tenant: {} with authUserId: {}", type, tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        UserAddress address = addressRepository.findByUserIdAndTypeAndIsDefaultTrue(user.getId(), type)
                .orElseThrow(() -> new ResourceNotFoundException("Default " + type + " address not found"));

        return new UserAddressResponse(address);
    }

    /**
     * Get a specific address by ID
     */
    @Transactional(readOnly = true)
    public UserAddressResponse getAddress(String tenantId, Long authUserId, Long addressId) {
        logger.debug("Retrieving address {} for tenant: {} with authUserId: {}", addressId, tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        UserAddress address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: " + addressId));

        return new UserAddressResponse(address);
    }

    /**
     * Update an existing address
     */
    public UserAddressResponse updateAddress(String tenantId, Long authUserId, Long addressId, UpdateAddressRequest request) {
        logger.info("Updating address {} for tenant: {} with authUserId: {}", addressId, tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        UserAddress address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: " + addressId));

        // Update fields if provided
        if (request.getType() != null) {
            address.setType(request.getType());
        }
        if (request.getStreetAddress() != null) {
            validateStreetAddress(request.getStreetAddress());
            address.setStreetAddress(request.getStreetAddress());
        }
        if (request.getCity() != null) {
            validateCity(request.getCity());
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            validateState(request.getState());
            address.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            validatePostalCode(request.getPostalCode());
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            validateCountry(request.getCountry());
            address.setCountry(request.getCountry());
        }

        // Handle default flag
        if (request.getIsDefault() != null) {
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                // Unset other default addresses of the same type
                addressRepository.setAllAddressesNonDefaultByUserIdAndType(user.getId(), address.getType());
            }
            address.setIsDefault(request.getIsDefault());
        }

        UserAddress updatedAddress = addressRepository.save(address);

        logger.info("Successfully updated address with ID: {} for user: {} in tenant: {}", 
                   updatedAddress.getId(), user.getId(), tenantId);
        return new UserAddressResponse(updatedAddress);
    }

    /**
     * Set an address as default for its type
     */
    public UserAddressResponse setDefaultAddress(String tenantId, Long authUserId, Long addressId) {
        logger.info("Setting address {} as default for tenant: {} with authUserId: {}", addressId, tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        UserAddress address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: " + addressId));

        // Unset other default addresses of the same type
        addressRepository.setAllAddressesNonDefaultByUserIdAndType(user.getId(), address.getType());

        // Set this address as default
        address.setIsDefault(true);
        UserAddress updatedAddress = addressRepository.save(address);

        logger.info("Successfully set address {} as default for user: {} in tenant: {}", 
                   addressId, user.getId(), tenantId);
        return new UserAddressResponse(updatedAddress);
    }

    /**
     * Delete an address
     */
    public void deleteAddress(String tenantId, Long authUserId, Long addressId) {
        logger.info("Deleting address {} for tenant: {} with authUserId: {}", addressId, tenantId, authUserId);

        User user = userRepository.findByAuthUserIdAndTenantId(authUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with auth user ID: " + authUserId));

        if (!addressRepository.existsByIdAndUserId(addressId, user.getId())) {
            throw new ResourceNotFoundException("Address not found with ID: " + addressId);
        }

        addressRepository.deleteByIdAndUserId(addressId, user.getId());

        logger.info("Successfully deleted address {} for user: {} in tenant: {}", 
                   addressId, user.getId(), tenantId);
    }

    /**
     * Validate all address fields
     */
    private void validateAddressFields(String streetAddress, String city, String state, String postalCode, String country) {
        validateStreetAddress(streetAddress);
        validateCity(city);
        validateState(state);
        validatePostalCode(postalCode);
        validateCountry(country);
    }

    /**
     * Validate street address
     */
    private void validateStreetAddress(String streetAddress) {
        if (streetAddress == null || streetAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Street address cannot be empty");
        }
        if (streetAddress.length() > 255) {
            throw new IllegalArgumentException("Street address must not exceed 255 characters");
        }
        // Basic validation for common address patterns
        if (!streetAddress.matches("^[a-zA-Z0-9\\s,.-]+$")) {
            throw new IllegalArgumentException("Street address contains invalid characters");
        }
    }

    /**
     * Validate city
     */
    private void validateCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be empty");
        }
        if (city.length() > 100) {
            throw new IllegalArgumentException("City must not exceed 100 characters");
        }
        if (!city.matches("^[a-zA-Z\\s.-]+$")) {
            throw new IllegalArgumentException("City contains invalid characters");
        }
    }

    /**
     * Validate state
     */
    private void validateState(String state) {
        if (state == null || state.trim().isEmpty()) {
            throw new IllegalArgumentException("State cannot be empty");
        }
        if (state.length() > 100) {
            throw new IllegalArgumentException("State must not exceed 100 characters");
        }
        if (!state.matches("^[a-zA-Z\\s.-]+$")) {
            throw new IllegalArgumentException("State contains invalid characters");
        }
    }

    /**
     * Validate postal code
     */
    private void validatePostalCode(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code cannot be empty");
        }
        if (postalCode.length() > 20) {
            throw new IllegalArgumentException("Postal code must not exceed 20 characters");
        }
        if (!postalCode.matches("^[A-Za-z0-9\\s-]+$")) {
            throw new IllegalArgumentException("Postal code contains invalid characters");
        }
    }

    /**
     * Validate country
     */
    private void validateCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be empty");
        }
        if (country.length() > 100) {
            throw new IllegalArgumentException("Country must not exceed 100 characters");
        }
        if (!country.matches("^[a-zA-Z\\s.-]+$")) {
            throw new IllegalArgumentException("Country contains invalid characters");
        }
    }
}
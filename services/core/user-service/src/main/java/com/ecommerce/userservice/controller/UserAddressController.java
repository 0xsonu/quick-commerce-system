package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.CreateAddressRequest;
import com.ecommerce.userservice.dto.UpdateAddressRequest;
import com.ecommerce.userservice.dto.UserAddressResponse;
import com.ecommerce.userservice.entity.UserAddress;
import com.ecommerce.userservice.service.UserAddressService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user address management operations
 */
@RestController
@RequestMapping("/api/v1/users/addresses")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserAddressController {

    private static final Logger logger = LoggerFactory.getLogger(UserAddressController.class);

    private final UserAddressService addressService;

    @Autowired
    public UserAddressController(UserAddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * Create a new address for the authenticated user
     */
    @PostMapping
    @Timed(value = "address.create", description = "Time taken to create address")
    public ResponseEntity<UserAddressResponse> createAddress(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateAddressRequest request) {
        
        logger.info("Creating address for tenant: {} with userId: {}", tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        UserAddressResponse response = addressService.createAddress(tenantId, authUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all addresses for the authenticated user
     */
    @GetMapping
    @Timed(value = "address.get_all", description = "Time taken to get all addresses")
    public ResponseEntity<List<UserAddressResponse>> getUserAddresses(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId) {
        
        logger.debug("Getting addresses for tenant: {} with userId: {}", tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        List<UserAddressResponse> response = addressService.getUserAddresses(tenantId, authUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get addresses by type for the authenticated user
     */
    @GetMapping("/type/{type}")
    @Timed(value = "address.get_by_type", description = "Time taken to get addresses by type")
    public ResponseEntity<List<UserAddressResponse>> getUserAddressesByType(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable UserAddress.AddressType type) {
        
        logger.debug("Getting {} addresses for tenant: {} with userId: {}", type, tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        List<UserAddressResponse> response = addressService.getUserAddressesByType(tenantId, authUserId, type);
        return ResponseEntity.ok(response);
    }

    /**
     * Get default address by type for the authenticated user
     */
    @GetMapping("/default/{type}")
    @Timed(value = "address.get_default", description = "Time taken to get default address")
    public ResponseEntity<UserAddressResponse> getDefaultAddress(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable UserAddress.AddressType type) {
        
        logger.debug("Getting default {} address for tenant: {} with userId: {}", type, tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        UserAddressResponse response = addressService.getDefaultAddress(tenantId, authUserId, type);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific address by ID
     */
    @GetMapping("/{addressId}")
    @Timed(value = "address.get_by_id", description = "Time taken to get address by ID")
    public ResponseEntity<UserAddressResponse> getAddress(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long addressId) {
        
        logger.debug("Getting address {} for tenant: {} with userId: {}", addressId, tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        UserAddressResponse response = addressService.getAddress(tenantId, authUserId, addressId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing address
     */
    @PutMapping("/{addressId}")
    @Timed(value = "address.update", description = "Time taken to update address")
    public ResponseEntity<UserAddressResponse> updateAddress(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        
        logger.info("Updating address {} for tenant: {} with userId: {}", addressId, tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        UserAddressResponse response = addressService.updateAddress(tenantId, authUserId, addressId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Set an address as default for its type
     */
    @PutMapping("/{addressId}/default")
    @Timed(value = "address.set_default", description = "Time taken to set default address")
    public ResponseEntity<UserAddressResponse> setDefaultAddress(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long addressId) {
        
        logger.info("Setting address {} as default for tenant: {} with userId: {}", addressId, tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        UserAddressResponse response = addressService.setDefaultAddress(tenantId, authUserId, addressId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an address
     */
    @DeleteMapping("/{addressId}")
    @Timed(value = "address.delete", description = "Time taken to delete address")
    public ResponseEntity<Void> deleteAddress(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long addressId) {
        
        logger.info("Deleting address {} for tenant: {} with userId: {}", addressId, tenantId, userId);
        
        Long authUserId = Long.parseLong(userId);
        addressService.deleteAddress(tenantId, authUserId, addressId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Address Service is healthy");
    }
}
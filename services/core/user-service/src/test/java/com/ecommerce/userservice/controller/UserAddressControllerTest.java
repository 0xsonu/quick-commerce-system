package com.ecommerce.userservice.controller;

import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import com.ecommerce.userservice.config.TestSecurityConfig;
import com.ecommerce.userservice.dto.CreateAddressRequest;
import com.ecommerce.userservice.dto.UpdateAddressRequest;
import com.ecommerce.userservice.dto.UserAddressResponse;
import com.ecommerce.userservice.entity.UserAddress;
import com.ecommerce.userservice.service.UserAddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAddressController.class)
@ContextConfiguration(classes = {UserAddressController.class, TestSecurityConfig.class})
class UserAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAddressService addressService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserAddressResponse addressResponse;
    private CreateAddressRequest createRequest;
    private UpdateAddressRequest updateRequest;

    @BeforeEach
    void setUp() {
        addressResponse = new UserAddressResponse();
        addressResponse.setId(1L);
        addressResponse.setType(UserAddress.AddressType.SHIPPING);
        addressResponse.setStreetAddress("123 Main St");
        addressResponse.setCity("New York");
        addressResponse.setState("NY");
        addressResponse.setPostalCode("10001");
        addressResponse.setCountry("USA");
        addressResponse.setIsDefault(true);
        addressResponse.setCreatedAt(LocalDateTime.now());
        addressResponse.setUpdatedAt(LocalDateTime.now());

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
    void createAddress_Success() throws Exception {
        // Arrange
        when(addressService.createAddress(eq("tenant1"), eq(123L), any(CreateAddressRequest.class)))
                .thenReturn(addressResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("SHIPPING"))
                .andExpect(jsonPath("$.streetAddress").value("123 Main St"))
                .andExpect(jsonPath("$.city").value("New York"))
                .andExpect(jsonPath("$.state").value("NY"))
                .andExpect(jsonPath("$.postalCode").value("10001"))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(addressService).createAddress(eq("tenant1"), eq(123L), any(CreateAddressRequest.class));
    }

    @Test
    void createAddress_ValidationError() throws Exception {
        // Arrange
        createRequest.setStreetAddress(""); // Invalid empty street address

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(any(), any(), any());
    }

    @Test
    void createAddress_MaxAddressesReached() throws Exception {
        // Arrange
        when(addressService.createAddress(eq("tenant1"), eq(123L), any(CreateAddressRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Maximum number of addresses reached"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void getUserAddresses_Success() throws Exception {
        // Arrange
        List<UserAddressResponse> addresses = Arrays.asList(addressResponse);
        when(addressService.getUserAddresses("tenant1", 123L)).thenReturn(addresses);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/addresses")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].type").value("SHIPPING"));

        verify(addressService).getUserAddresses("tenant1", 123L);
    }

    @Test
    void getUserAddressesByType_Success() throws Exception {
        // Arrange
        List<UserAddressResponse> addresses = Arrays.asList(addressResponse);
        when(addressService.getUserAddressesByType("tenant1", 123L, UserAddress.AddressType.SHIPPING))
                .thenReturn(addresses);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/addresses/type/SHIPPING")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].type").value("SHIPPING"));

        verify(addressService).getUserAddressesByType("tenant1", 123L, UserAddress.AddressType.SHIPPING);
    }

    @Test
    void getDefaultAddress_Success() throws Exception {
        // Arrange
        when(addressService.getDefaultAddress("tenant1", 123L, UserAddress.AddressType.SHIPPING))
                .thenReturn(addressResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/addresses/default/SHIPPING")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("SHIPPING"))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(addressService).getDefaultAddress("tenant1", 123L, UserAddress.AddressType.SHIPPING);
    }

    @Test
    void getDefaultAddress_NotFound() throws Exception {
        // Arrange
        when(addressService.getDefaultAddress("tenant1", 123L, UserAddress.AddressType.SHIPPING))
                .thenThrow(new ResourceNotFoundException("Default SHIPPING address not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/addresses/default/SHIPPING")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAddress_Success() throws Exception {
        // Arrange
        when(addressService.getAddress("tenant1", 123L, 1L)).thenReturn(addressResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/addresses/1")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("SHIPPING"));

        verify(addressService).getAddress("tenant1", 123L, 1L);
    }

    @Test
    void getAddress_NotFound() throws Exception {
        // Arrange
        when(addressService.getAddress("tenant1", 123L, 1L))
                .thenThrow(new ResourceNotFoundException("Address not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/addresses/1")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAddress_Success() throws Exception {
        // Arrange
        when(addressService.updateAddress(eq("tenant1"), eq(123L), eq(1L), any(UpdateAddressRequest.class)))
                .thenReturn(addressResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/addresses/1")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("SHIPPING"));

        verify(addressService).updateAddress(eq("tenant1"), eq(123L), eq(1L), any(UpdateAddressRequest.class));
    }

    @Test
    void updateAddress_ValidationError() throws Exception {
        // Arrange
        updateRequest.setPostalCode("invalid@postal"); // Invalid postal code

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/addresses/1")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).updateAddress(any(), any(), any(), any());
    }

    @Test
    void setDefaultAddress_Success() throws Exception {
        // Arrange
        when(addressService.setDefaultAddress("tenant1", 123L, 1L)).thenReturn(addressResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/addresses/1/default")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(addressService).setDefaultAddress("tenant1", 123L, 1L);
    }

    @Test
    void deleteAddress_Success() throws Exception {
        // Arrange
        doNothing().when(addressService).deleteAddress("tenant1", 123L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/addresses/1")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isNoContent());

        verify(addressService).deleteAddress("tenant1", 123L, 1L);
    }

    @Test
    void deleteAddress_NotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Address not found"))
                .when(addressService).deleteAddress("tenant1", 123L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/addresses/1")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void health_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/addresses/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("User Address Service is healthy"));
    }

    @Test
    void createAddress_MissingTenantHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-User-ID", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(any(), any(), any());
    }

    @Test
    void createAddress_MissingUserHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", "tenant1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(any(), any(), any());
    }

    @Test
    void createAddress_InvalidUserIdFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-User-ID", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(any(), any(), any());
    }
}
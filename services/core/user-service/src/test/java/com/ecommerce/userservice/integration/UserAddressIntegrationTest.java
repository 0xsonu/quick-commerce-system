package com.ecommerce.userservice.integration;

import com.ecommerce.userservice.dto.CreateAddressRequest;
import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateAddressRequest;
import com.ecommerce.userservice.dto.UserAddressResponse;
import com.ecommerce.userservice.entity.UserAddress;
import com.ecommerce.userservice.repository.UserAddressRepository;
import com.ecommerce.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class UserAddressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAddressRepository addressRepository;

    private static final String TENANT_ID = "tenant1";
    private static final String USER_ID = "123";

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        addressRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user first
        CreateUserRequest userRequest = new CreateUserRequest();
        userRequest.setAuthUserId(123L);
        userRequest.setEmail("test@example.com");
        userRequest.setFirstName("John");
        userRequest.setLastName("Doe");
        userRequest.setPhone("555-1234");
        userRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/api/v1/users")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void createAddress_FullWorkflow() throws Exception {
        // Create address request
        CreateAddressRequest request = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA"
        );
        request.setIsDefault(true);

        // Create address
        MvcResult result = mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("SHIPPING"))
                .andExpect(jsonPath("$.streetAddress").value("123 Main St"))
                .andExpect(jsonPath("$.city").value("New York"))
                .andExpect(jsonPath("$.state").value("NY"))
                .andExpect(jsonPath("$.postalCode").value("10001"))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andReturn();

        UserAddressResponse createdAddress = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserAddressResponse.class);
        assertNotNull(createdAddress.getId());

        // Verify address was saved in database
        assertTrue(addressRepository.existsById(createdAddress.getId()));
    }

    @Test
    void getUserAddresses_Success() throws Exception {
        // Create two addresses
        CreateAddressRequest shippingRequest = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "123 Main St", "New York", "NY", "10001", "USA");
        shippingRequest.setIsDefault(true);

        CreateAddressRequest billingRequest = new CreateAddressRequest(
                UserAddress.AddressType.BILLING,
                "456 Oak Ave", "Los Angeles", "CA", "90210", "USA");
        billingRequest.setIsDefault(false);

        // Create shipping address
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shippingRequest)))
                .andExpect(status().isCreated());

        // Create billing address
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billingRequest)))
                .andExpect(status().isCreated());

        // Get all addresses
        mockMvc.perform(get("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAddressesByType_Success() throws Exception {
        // Create addresses of different types
        CreateAddressRequest shippingRequest = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "123 Main St", "New York", "NY", "10001", "USA");

        CreateAddressRequest billingRequest = new CreateAddressRequest(
                UserAddress.AddressType.BILLING,
                "456 Oak Ave", "Los Angeles", "CA", "90210", "USA");

        // Create both addresses
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shippingRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billingRequest)))
                .andExpect(status().isCreated());

        // Get only shipping addresses
        mockMvc.perform(get("/api/v1/users/addresses/type/SHIPPING")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("SHIPPING"));
    }

    @Test
    void getDefaultAddress_Success() throws Exception {
        // Create default shipping address
        CreateAddressRequest request = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "123 Main St", "New York", "NY", "10001", "USA");
        request.setIsDefault(true);

        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get default shipping address
        mockMvc.perform(get("/api/v1/users/addresses/default/SHIPPING")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SHIPPING"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void updateAddress_Success() throws Exception {
        // Create address
        CreateAddressRequest createRequest = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "123 Main St", "New York", "NY", "10001", "USA");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UserAddressResponse createdAddress = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserAddressResponse.class);

        // Update address
        UpdateAddressRequest updateRequest = new UpdateAddressRequest();
        updateRequest.setStreetAddress("789 Pine St");
        updateRequest.setCity("Chicago");
        updateRequest.setState("IL");
        updateRequest.setPostalCode("60601");

        mockMvc.perform(put("/api/v1/users/addresses/" + createdAddress.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streetAddress").value("789 Pine St"))
                .andExpect(jsonPath("$.city").value("Chicago"))
                .andExpect(jsonPath("$.state").value("IL"))
                .andExpect(jsonPath("$.postalCode").value("60601"));
    }

    @Test
    void setDefaultAddress_Success() throws Exception {
        // Create two shipping addresses
        CreateAddressRequest request1 = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "123 Main St", "New York", "NY", "10001", "USA");
        request1.setIsDefault(true);

        CreateAddressRequest request2 = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "456 Oak Ave", "Los Angeles", "CA", "90210", "USA");
        request2.setIsDefault(false);

        // Create first address (default)
        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Create second address (non-default)
        MvcResult result2 = mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn();

        UserAddressResponse address2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(), UserAddressResponse.class);

        // Set second address as default
        mockMvc.perform(put("/api/v1/users/addresses/" + address2.getId() + "/default")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));

        // Verify new default address
        mockMvc.perform(get("/api/v1/users/addresses/default/SHIPPING")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(address2.getId()))
                .andExpect(jsonPath("$.streetAddress").value("456 Oak Ave"));
    }

    @Test
    void deleteAddress_Success() throws Exception {
        // Create address
        CreateAddressRequest request = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "123 Main St", "New York", "NY", "10001", "USA");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UserAddressResponse createdAddress = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserAddressResponse.class);

        // Delete address
        mockMvc.perform(delete("/api/v1/users/addresses/" + createdAddress.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isNoContent());

        // Verify address is deleted
        mockMvc.perform(get("/api/v1/users/addresses/" + createdAddress.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isNotFound());

        // Verify address is deleted from database
        assertFalse(addressRepository.existsById(createdAddress.getId()));
    }

    @Test
    void addressValidation_InvalidData() throws Exception {
        // Test with empty street address
        CreateAddressRequest invalidRequest = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "", // Empty street address
                "New York", "NY", "10001", "USA");

        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void maxAddressLimit_Exceeded() throws Exception {
        // Create 10 addresses (max limit)
        for (int i = 1; i <= 10; i++) {
            CreateAddressRequest request = new CreateAddressRequest(
                    UserAddress.AddressType.SHIPPING,
                    i + " Main St", "City" + i, "ST", "1000" + i, "USA");

            mockMvc.perform(post("/api/v1/users/addresses")
                            .header("X-Tenant-ID", TENANT_ID)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Try to create 11th address (should fail)
        CreateAddressRequest request = new CreateAddressRequest(
                UserAddress.AddressType.SHIPPING,
                "11 Main St", "City11", "ST", "10011", "USA");

        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
package com.ecommerce.userservice.integration;

import com.ecommerce.userservice.config.TestSecurityConfig;
import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserProfileRequest;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for User Service
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class UserServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Security components are mocked in TestSecurityConfig

    private String tenantId;
    private CreateUserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        
        createUserRequest = new CreateUserRequest();
        createUserRequest.setAuthUserId(1L);
        createUserRequest.setEmail("integration@test.com");
        createUserRequest.setFirstName("Integration");
        createUserRequest.setLastName("Test");
        createUserRequest.setPhone("+1234567890");
        createUserRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // Clean up any existing test data
        userRepository.deleteAll();
    }

    @Test
    void createAndRetrieveUser_Success() {
        // Create user
        UserProfileResponse createdUser = userService.createUser(tenantId, createUserRequest);
        
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals(createUserRequest.getEmail(), createdUser.getEmail());
        assertEquals(createUserRequest.getFirstName(), createdUser.getFirstName());
        assertEquals(createUserRequest.getLastName(), createdUser.getLastName());

        // Retrieve user
        UserProfileResponse retrievedUser = userService.getUserProfile(tenantId, createUserRequest.getAuthUserId());
        
        assertNotNull(retrievedUser);
        assertEquals(createdUser.getId(), retrievedUser.getId());
        assertEquals(createdUser.getEmail(), retrievedUser.getEmail());
    }

    @Test
    void updateUser_Success() {
        // Create user first
        UserProfileResponse createdUser = userService.createUser(tenantId, createUserRequest);
        
        // Update user
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setPhone("+0987654321");
        
        UserProfileResponse updatedUser = userService.updateUserProfile(tenantId, createUserRequest.getAuthUserId(), updateRequest);
        
        assertNotNull(updatedUser);
        assertEquals("Updated", updatedUser.getFirstName());
        assertEquals("Name", updatedUser.getLastName());
        assertEquals("+0987654321", updatedUser.getPhone());
        assertEquals(createdUser.getEmail(), updatedUser.getEmail()); // Email should remain unchanged
    }

    @Test
    void deleteUser_Success() {
        // Create user first
        UserProfileResponse createdUser = userService.createUser(tenantId, createUserRequest);
        
        // Verify user exists
        assertTrue(userService.userExists(tenantId, createUserRequest.getAuthUserId()));
        
        // Delete user
        userService.deleteUser(tenantId, createUserRequest.getAuthUserId());
        
        // Verify user no longer exists
        assertFalse(userService.userExists(tenantId, createUserRequest.getAuthUserId()));
    }

    @Test
    void searchUsers_Success() {
        // Create multiple users
        UserProfileResponse user1 = userService.createUser(tenantId, createUserRequest);
        
        CreateUserRequest request2 = new CreateUserRequest();
        request2.setAuthUserId(2L);
        request2.setEmail("search@test.com");
        request2.setFirstName("Search");
        request2.setLastName("User");
        
        UserProfileResponse user2 = userService.createUser(tenantId, request2);
        
        // Search by first name
        var searchResults = userService.searchUsers(tenantId, "Integration");
        assertEquals(1, searchResults.size());
        assertEquals(user1.getId(), searchResults.get(0).getId());
        
        // Search by email
        searchResults = userService.searchUsers(tenantId, "search@test.com");
        assertEquals(1, searchResults.size());
        assertEquals(user2.getId(), searchResults.get(0).getId());
    }

    @Test
    @WithMockUser
    void createUserEndpoint_Success() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(createUserRequest.getEmail()))
                .andExpect(jsonPath("$.firstName").value(createUserRequest.getFirstName()));
    }

    @Test
    void tenantIsolation_Success() {
        String tenant1 = "tenant1";
        String tenant2 = "tenant2";
        
        // Create user in tenant1
        UserProfileResponse user1 = userService.createUser(tenant1, createUserRequest);
        
        // Create user with same auth user ID in tenant2
        CreateUserRequest request2 = new CreateUserRequest();
        request2.setAuthUserId(createUserRequest.getAuthUserId()); // Same auth user ID
        request2.setEmail("tenant2@test.com");
        request2.setFirstName("Tenant2");
        request2.setLastName("User");
        
        UserProfileResponse user2 = userService.createUser(tenant2, request2);
        
        // Verify users are isolated by tenant
        assertNotEquals(user1.getId(), user2.getId());
        
        // Verify tenant1 user is not visible from tenant2
        assertFalse(userService.userExists(tenant2, createUserRequest.getAuthUserId()));
        assertTrue(userService.userExists(tenant1, createUserRequest.getAuthUserId()));
        
        // Verify tenant2 user is not visible from tenant1
        assertTrue(userService.userExists(tenant2, request2.getAuthUserId()));
    }

    @Test
    void userCount_Success() {
        // Initially no users
        assertEquals(0, userService.getUserCount(tenantId));
        
        // Create a user
        userService.createUser(tenantId, createUserRequest);
        assertEquals(1, userService.getUserCount(tenantId));
        
        // Create another user
        CreateUserRequest request2 = new CreateUserRequest();
        request2.setAuthUserId(2L);
        request2.setEmail("count@test.com");
        request2.setFirstName("Count");
        request2.setLastName("Test");
        
        userService.createUser(tenantId, request2);
        assertEquals(2, userService.getUserCount(tenantId));
    }
}
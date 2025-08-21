package com.ecommerce.userservice.controller;

import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import com.ecommerce.userservice.config.TestSecurityConfig;
import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserProfileRequest;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController
 */
@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private String tenantId;
    private String userId;
    private UserProfileResponse userProfileResponse;
    private CreateUserRequest createUserRequest;
    private UpdateUserProfileRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        tenantId = "tenant123";
        userId = "1";

        userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(1L);
        userProfileResponse.setAuthUserId(1L);
        userProfileResponse.setEmail("test@example.com");
        userProfileResponse.setFirstName("John");
        userProfileResponse.setLastName("Doe");
        userProfileResponse.setPhone("+1234567890");
        userProfileResponse.setDateOfBirth(LocalDate.of(1990, 1, 1));
        userProfileResponse.setCreatedAt(LocalDateTime.now());
        userProfileResponse.setUpdatedAt(LocalDateTime.now());

        createUserRequest = new CreateUserRequest();
        createUserRequest.setAuthUserId(1L);
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setFirstName("John");
        createUserRequest.setLastName("Doe");
        createUserRequest.setPhone("+1234567890");
        createUserRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        updateUserRequest = new UpdateUserProfileRequest();
        updateUserRequest.setFirstName("Jane");
        updateUserRequest.setLastName("Smith");
        updateUserRequest.setPhone("+0987654321");
    }

    @Test
    @WithMockUser
    void createUser_Success() throws Exception {
        // Given
        when(userService.createUser(eq(tenantId), any(CreateUserRequest.class)))
                .thenReturn(userProfileResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @WithMockUser
    void createUser_ValidationError() throws Exception {
        // Given
        createUserRequest.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createUser_UserAlreadyExists() throws Exception {
        // Given
        when(userService.createUser(eq(tenantId), any(CreateUserRequest.class)))
                .thenThrow(new DataIntegrityViolationException("User already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest))
                .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void getUserProfile_Success() throws Exception {
        // Given
        when(userService.getUserProfile(tenantId, 1L)).thenReturn(userProfileResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/profile")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser
    void getUserProfile_UserNotFound() throws Exception {
        // Given
        when(userService.getUserProfile(tenantId, 1L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/users/profile")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getUserById_Success() throws Exception {
        // Given
        when(userService.getUserProfileById(tenantId, 1L)).thenReturn(userProfileResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/1")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    void updateUserProfile_Success() throws Exception {
        // Given
        UserProfileResponse updatedResponse = new UserProfileResponse();
        updatedResponse.setId(1L);
        updatedResponse.setFirstName("Jane");
        updatedResponse.setLastName("Smith");
        updatedResponse.setEmail("test@example.com");

        when(userService.updateUserProfile(eq(tenantId), eq(1L), any(UpdateUserProfileRequest.class)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/users/profile")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    @WithMockUser
    void deleteUserProfile_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/users/profile")
                .header("X-Tenant-ID", tenantId)
                .header("X-User-ID", userId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void userExists_ReturnsTrue() throws Exception {
        // Given
        when(userService.userExists(tenantId, 1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/users/exists")
                .header("X-Tenant-ID", tenantId)
                .param("authUserId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser
    void userExistsByEmail_ReturnsTrue() throws Exception {
        // Given
        when(userService.userExistsByEmail(tenantId, "test@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/users/exists/email")
                .header("X-Tenant-ID", tenantId)
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser
    void getUserByEmail_Success() throws Exception {
        // Given
        when(userService.getUserByEmail(tenantId, "test@example.com"))
                .thenReturn(Optional.of(userProfileResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/users/email/test@example.com")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    void getUserByEmail_NotFound() throws Exception {
        // Given
        when(userService.getUserByEmail(tenantId, "notfound@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/users/email/notfound@example.com")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void searchUsers_Success() throws Exception {
        // Given
        List<UserProfileResponse> users = Arrays.asList(userProfileResponse);
        when(userService.searchUsers(tenantId, "John")).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/v1/users/search")
                .header("X-Tenant-ID", tenantId)
                .param("q", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    @WithMockUser
    void getAllUsers_Success() throws Exception {
        // Given
        List<UserProfileResponse> users = Arrays.asList(userProfileResponse);
        when(userService.getAllUsers(tenantId)).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @WithMockUser
    void getUserCount_Success() throws Exception {
        // Given
        when(userService.getUserCount(tenantId)).thenReturn(5L);

        // When & Then
        mockMvc.perform(get("/api/v1/users/count")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser
    void health_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("User Service is healthy"));
    }
}
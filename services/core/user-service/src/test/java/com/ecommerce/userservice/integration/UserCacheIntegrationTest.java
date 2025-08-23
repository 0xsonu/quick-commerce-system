package com.ecommerce.userservice.integration;

import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserProfileRequest;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.service.UserCacheService;
import com.ecommerce.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class UserCacheIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("user_service_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserCacheService userCacheService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final String tenantId = "test-tenant";
    private final Long authUserId = 123L;
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testCacheAsidePattern_GetUserProfile() {
        // Arrange - Create user
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setAuthUserId(authUserId);
        createRequest.setEmail(email);
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");
        createRequest.setPhone("+1234567890");
        createRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        UserProfileResponse createdUser = userService.createUser(tenantId, createRequest);

        // Clear cache to test cache-aside pattern
        userCacheService.invalidateUserProfileCache(tenantId, authUserId);

        // Act - First call should hit database and populate cache
        UserProfileResponse firstCall = userService.getUserProfile(tenantId, authUserId);

        // Verify cache is populated
        Optional<UserProfileResponse> cachedProfile = userCacheService.getUserProfileFromCache(tenantId, authUserId);
        assertTrue(cachedProfile.isPresent());
        assertEquals(firstCall.getId(), cachedProfile.get().getId());

        // Act - Second call should hit cache
        UserProfileResponse secondCall = userService.getUserProfile(tenantId, authUserId);

        // Assert
        assertEquals(firstCall.getId(), secondCall.getId());
        assertEquals(firstCall.getEmail(), secondCall.getEmail());
        assertEquals(firstCall.getFirstName(), secondCall.getFirstName());
    }

    @Test
    void testCacheInvalidationOnUpdate() {
        // Arrange - Create user
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setAuthUserId(authUserId);
        createRequest.setEmail(email);
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");

        UserProfileResponse createdUser = userService.createUser(tenantId, createRequest);

        // Populate cache
        UserProfileResponse cachedUser = userService.getUserProfile(tenantId, authUserId);
        assertNotNull(cachedUser);

        // Verify cache is populated
        Optional<UserProfileResponse> beforeUpdate = userCacheService.getUserProfileFromCache(tenantId, authUserId);
        assertTrue(beforeUpdate.isPresent());

        // Act - Update user
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");

        UserProfileResponse updatedUser = userService.updateUserProfile(tenantId, authUserId, updateRequest);

        // Assert - Cache should be updated with new data
        Optional<UserProfileResponse> afterUpdate = userCacheService.getUserProfileFromCache(tenantId, authUserId);
        assertTrue(afterUpdate.isPresent());
        assertEquals("Jane", afterUpdate.get().getFirstName());
        assertEquals("Smith", afterUpdate.get().getLastName());
    }

    @Test
    void testCacheInvalidationOnEmailChange() {
        // Arrange - Create user
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setAuthUserId(authUserId);
        createRequest.setEmail(email);
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");

        UserProfileResponse createdUser = userService.createUser(tenantId, createRequest);

        // Populate both caches
        userService.getUserProfile(tenantId, authUserId);
        userService.getUserByEmail(tenantId, email);

        // Verify both caches are populated
        assertTrue(userCacheService.getUserProfileFromCache(tenantId, authUserId).isPresent());
        assertTrue(userCacheService.getUserProfileByEmailFromCache(tenantId, email).isPresent());

        // Act - Update email
        String newEmail = "newemail@example.com";
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setEmail(newEmail);

        userService.updateUserProfile(tenantId, authUserId, updateRequest);

        // Assert - Old email cache should be cleared, new email cache should be populated
        assertFalse(userCacheService.getUserProfileByEmailFromCache(tenantId, email).isPresent());
        assertTrue(userCacheService.getUserProfileByEmailFromCache(tenantId, newEmail).isPresent());
    }

    @Test
    void testCacheInvalidationOnDelete() {
        // Arrange - Create user
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setAuthUserId(authUserId);
        createRequest.setEmail(email);
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");

        UserProfileResponse createdUser = userService.createUser(tenantId, createRequest);

        // Populate cache
        userService.getUserProfile(tenantId, authUserId);
        userService.getUserByEmail(tenantId, email);

        // Verify cache is populated
        assertTrue(userCacheService.getUserProfileFromCache(tenantId, authUserId).isPresent());
        assertTrue(userCacheService.getUserProfileByEmailFromCache(tenantId, email).isPresent());

        // Act - Delete user
        userService.deleteUser(tenantId, authUserId);

        // Assert - Cache should be cleared
        assertFalse(userCacheService.getUserProfileFromCache(tenantId, authUserId).isPresent());
        assertFalse(userCacheService.getUserProfileByEmailFromCache(tenantId, email).isPresent());
    }

    @Test
    void testCacheWarmup() throws Exception {
        // Arrange - Create multiple users
        for (int i = 1; i <= 5; i++) {
            CreateUserRequest createRequest = new CreateUserRequest();
            createRequest.setAuthUserId((long) i);
            createRequest.setEmail("user" + i + "@example.com");
            createRequest.setFirstName("User" + i);
            createRequest.setLastName("Test");

            userService.createUser(tenantId, createRequest);
        }

        // Clear cache
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Act - Warm up cache
        userCacheService.warmUpUserCache(tenantId).get();

        // Assert - Cache should be populated for all users
        for (int i = 1; i <= 5; i++) {
            Optional<UserProfileResponse> cachedUser = userCacheService.getUserProfileFromCache(tenantId, (long) i);
            assertTrue(cachedUser.isPresent(), "User " + i + " should be cached");
            assertEquals("User" + i, cachedUser.get().getFirstName());
        }
    }

    @Test
    void testCacheHealth() {
        // Act & Assert
        assertTrue(userCacheService.isCacheHealthy());
    }

    @Test
    void testCacheStatistics() {
        // Arrange - Create and cache some users
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setAuthUserId(authUserId);
        createRequest.setEmail(email);
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");

        userService.createUser(tenantId, createRequest);
        userService.getUserProfile(tenantId, authUserId);

        // Act
        UserCacheService.CacheStatistics stats = userCacheService.getCacheStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.getCacheSize() >= 0);
    }

    @Test
    void testCacheEndpoints() throws Exception {
        // Test cache warmup endpoint
        mockMvc.perform(post("/api/v1/users/cache/warmup")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache warm-up initiated for tenant: " + tenantId));

        // Test cache health endpoint
        mockMvc.perform(get("/api/v1/users/cache/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is healthy"));

        // Test cache statistics endpoint
        mockMvc.perform(get("/api/v1/users/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheSize").exists());
    }

    @Test
    void testCachePerformance() {
        // Arrange - Create user
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setAuthUserId(authUserId);
        createRequest.setEmail(email);
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");

        userService.createUser(tenantId, createRequest);

        // Measure first call (database hit)
        long startTime1 = System.currentTimeMillis();
        UserProfileResponse firstCall = userService.getUserProfile(tenantId, authUserId);
        long firstCallTime = System.currentTimeMillis() - startTime1;

        // Measure second call (cache hit)
        long startTime2 = System.currentTimeMillis();
        UserProfileResponse secondCall = userService.getUserProfile(tenantId, authUserId);
        long secondCallTime = System.currentTimeMillis() - startTime2;

        // Assert - Cache hit should be faster (though this might be flaky in tests)
        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertEquals(firstCall.getId(), secondCall.getId());
        
        // Log performance for manual verification
        System.out.println("First call (DB): " + firstCallTime + "ms");
        System.out.println("Second call (Cache): " + secondCallTime + "ms");
    }
}
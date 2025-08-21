package com.ecommerce.userservice.repository;

import com.ecommerce.shared.security.repository.TenantAwareRepository;
import com.ecommerce.userservice.entity.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity with tenant-aware operations and caching
 */
@Repository
public interface UserRepository extends TenantAwareRepository<User, Long> {

    /**
     * Find user by auth user ID and tenant ID with caching
     */
    @Cacheable(value = "users", key = "#tenantId + ':' + #authUserId")
    Optional<User> findByAuthUserIdAndTenantId(@Param("authUserId") Long authUserId, 
                                              @Param("tenantId") String tenantId);

    /**
     * Find user by email and tenant ID
     */
    @Cacheable(value = "users", key = "#tenantId + ':email:' + #email")
    Optional<User> findByEmailAndTenantId(@Param("email") String email, 
                                         @Param("tenantId") String tenantId);

    /**
     * Check if user exists by email and tenant ID
     */
    boolean existsByEmailAndTenantId(@Param("email") String email, 
                                    @Param("tenantId") String tenantId);

    /**
     * Check if user exists by auth user ID and tenant ID
     */
    boolean existsByAuthUserIdAndTenantId(@Param("authUserId") Long authUserId, 
                                         @Param("tenantId") String tenantId);

    /**
     * Find users by tenant ID with pagination support
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId ORDER BY u.createdAt DESC")
    List<User> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") String tenantId);

    /**
     * Search users by name or email within tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> searchUsersByNameOrEmail(@Param("tenantId") String tenantId, 
                                       @Param("searchTerm") String searchTerm);

    /**
     * Override save to clear cache
     */
    @Override
    @CacheEvict(value = "users", allEntries = true)
    <S extends User> S save(S entity);

    /**
     * Override delete to clear cache
     */
    @Override
    @CacheEvict(value = "users", allEntries = true)
    void delete(User entity);

    /**
     * Override deleteById to clear cache
     */
    @Override
    @CacheEvict(value = "users", allEntries = true)
    void deleteById(Long id);

    /**
     * Clear cache for specific user
     */
    @CacheEvict(value = "users", key = "#tenantId + ':' + #authUserId")
    default void evictUserCache(String tenantId, Long authUserId) {
        // Method to trigger cache eviction
    }

    /**
     * Clear cache for user by email
     */
    @CacheEvict(value = "users", key = "#tenantId + ':email:' + #email")
    default void evictUserCacheByEmail(String tenantId, String email) {
        // Method to trigger cache eviction
    }
}
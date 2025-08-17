package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.UserAuth;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserAuth entity with tenant isolation
 */
@Repository
public interface UserAuthRepository extends JpaRepository<UserAuth, Long>, TenantAwareRepository<UserAuth, Long> {

    /**
     * Find user by username within tenant
     */
    @Query("SELECT u FROM UserAuth u WHERE u.tenantId = :tenantId AND u.username = :username")
    Optional<UserAuth> findByTenantIdAndUsername(@Param("tenantId") String tenantId, 
                                                 @Param("username") String username);

    /**
     * Find user by email within tenant
     */
    @Query("SELECT u FROM UserAuth u WHERE u.tenantId = :tenantId AND u.email = :email")
    Optional<UserAuth> findByTenantIdAndEmail(@Param("tenantId") String tenantId, 
                                              @Param("email") String email);

    /**
     * Find user by username or email within tenant
     */
    @Query("SELECT u FROM UserAuth u WHERE u.tenantId = :tenantId AND (u.username = :usernameOrEmail OR u.email = :usernameOrEmail)")
    Optional<UserAuth> findByTenantIdAndUsernameOrEmail(@Param("tenantId") String tenantId, 
                                                        @Param("usernameOrEmail") String usernameOrEmail);

    /**
     * Check if username exists within tenant
     */
    @Query("SELECT COUNT(u) > 0 FROM UserAuth u WHERE u.tenantId = :tenantId AND u.username = :username")
    boolean existsByTenantIdAndUsername(@Param("tenantId") String tenantId, 
                                        @Param("username") String username);

    /**
     * Check if email exists within tenant
     */
    @Query("SELECT COUNT(u) > 0 FROM UserAuth u WHERE u.tenantId = :tenantId AND u.email = :email")
    boolean existsByTenantIdAndEmail(@Param("tenantId") String tenantId, 
                                     @Param("email") String email);

    /**
     * Update failed login attempts
     */
    @Modifying
    @Query("UPDATE UserAuth u SET u.failedLoginAttempts = :attempts, u.accountLocked = :locked WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, 
                                   @Param("attempts") Integer attempts, 
                                   @Param("locked") Boolean locked);

    /**
     * Reset failed login attempts
     */
    @Modifying
    @Query("UPDATE UserAuth u SET u.failedLoginAttempts = 0, u.accountLocked = false WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") Long userId);

    /**
     * Find active user by tenant and username/email
     */
    @Query("SELECT u FROM UserAuth u WHERE u.tenantId = :tenantId AND (u.username = :usernameOrEmail OR u.email = :usernameOrEmail) AND u.isActive = true AND u.accountLocked = false")
    Optional<UserAuth> findActiveUserByTenantIdAndUsernameOrEmail(@Param("tenantId") String tenantId, 
                                                                  @Param("usernameOrEmail") String usernameOrEmail);
}
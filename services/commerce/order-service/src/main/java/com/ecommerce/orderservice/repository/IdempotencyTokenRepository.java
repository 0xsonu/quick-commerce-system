package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.IdempotencyToken;
import com.ecommerce.orderservice.entity.IdempotencyStatus;
import com.ecommerce.shared.security.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyTokenRepository extends TenantAwareRepository<IdempotencyToken, Long> {

    /**
     * Find an idempotency token by tenant ID and token value
     */
    @Query("SELECT it FROM IdempotencyToken it WHERE it.tenantId = :tenantId AND it.token = :token")
    Optional<IdempotencyToken> findByTenantIdAndToken(@Param("tenantId") String tenantId, @Param("token") String token);

    /**
     * Find an idempotency token by tenant ID, user ID, and request hash for duplicate detection
     */
    @Query("SELECT it FROM IdempotencyToken it WHERE it.tenantId = :tenantId AND it.userId = :userId AND it.requestHash = :requestHash AND it.status = :status")
    Optional<IdempotencyToken> findByTenantIdAndUserIdAndRequestHashAndStatus(
        @Param("tenantId") String tenantId, 
        @Param("userId") Long userId, 
        @Param("requestHash") String requestHash,
        @Param("status") IdempotencyStatus status
    );

    /**
     * Delete expired idempotency tokens
     */
    @Modifying
    @Query("DELETE FROM IdempotencyToken it WHERE it.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Count active tokens for a user within a time window (for rate limiting)
     */
    @Query("SELECT COUNT(it) FROM IdempotencyToken it WHERE it.tenantId = :tenantId AND it.userId = :userId AND it.createdAt > :since AND it.status = 'PROCESSING'")
    long countActiveTokensForUserSince(@Param("tenantId") String tenantId, @Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Find tokens by order ID for debugging/auditing
     */
    @Query("SELECT it FROM IdempotencyToken it WHERE it.tenantId = :tenantId AND it.orderId = :orderId")
    Optional<IdempotencyToken> findByTenantIdAndOrderId(@Param("tenantId") String tenantId, @Param("orderId") Long orderId);
}
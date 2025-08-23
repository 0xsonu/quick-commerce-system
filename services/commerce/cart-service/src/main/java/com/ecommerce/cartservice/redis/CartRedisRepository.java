package com.ecommerce.cartservice.redis;

import com.ecommerce.cartservice.model.Cart;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Redis repository for cart operations
 */
@Repository
public interface CartRedisRepository extends CrudRepository<Cart, String> {

    /**
     * Find cart by tenant and user ID
     */
    default Optional<Cart> findByTenantIdAndUserId(String tenantId, String userId) {
        String cartId = Cart.generateCartId(tenantId, userId);
        return findById(cartId);
    }

    /**
     * Delete cart by tenant and user ID
     */
    default void deleteByTenantIdAndUserId(String tenantId, String userId) {
        String cartId = Cart.generateCartId(tenantId, userId);
        deleteById(cartId);
    }

    /**
     * Check if cart exists by tenant and user ID
     */
    default boolean existsByTenantIdAndUserId(String tenantId, String userId) {
        String cartId = Cart.generateCartId(tenantId, userId);
        return existsById(cartId);
    }
}
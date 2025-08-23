package com.ecommerce.cartservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.cartservice.model.Cart;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * MySQL backup entity for shopping cart recovery
 */
@Entity
@Table(name = "shopping_carts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"}),
       indexes = {
           @Index(name = "idx_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_user_id", columnList = "user_id")
       })
public class ShoppingCartBackup extends BaseEntity {

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @NotNull(message = "Cart data is required")
    @Lob
    @Column(name = "cart_data", nullable = false, columnDefinition = "JSON")
    private String cartData;

    public ShoppingCartBackup() {
        super();
    }

    public ShoppingCartBackup(String tenantId, String userId, Cart cart) {
        super();
        this.setTenantId(tenantId);
        this.userId = userId;
        this.setCartData(cart);
    }

    /**
     * Convert Cart object to JSON string for storage
     */
    public void setCartData(Cart cart) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules(); // For LocalDateTime serialization
            this.cartData = mapper.writeValueAsString(cart);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cart data", e);
        }
    }

    /**
     * Convert JSON string back to Cart object
     */
    public Cart getCartObject() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules(); // For LocalDateTime deserialization
            return mapper.readValue(this.cartData, Cart.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize cart data", e);
        }
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCartData() {
        return cartData;
    }

    public void setCartData(String cartData) {
        this.cartData = cartData;
    }
}
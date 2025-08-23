package com.ecommerce.cartservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shopping cart model for Redis storage with JSON serialization
 */
@RedisHash("cart")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cart implements Serializable {

    @Id
    private String id; // Format: tenant_id:user_id

    @NotBlank(message = "User ID is required")
    @JsonProperty("user_id")
    private String userId;

    @NotBlank(message = "Tenant ID is required")
    @JsonProperty("tenant_id")
    private String tenantId;

    @Valid
    @JsonProperty("items")
    private List<CartItem> items = new ArrayList<>();

    @NotNull(message = "Subtotal is required")
    @Positive(message = "Subtotal must be positive")
    @JsonProperty("subtotal")
    private BigDecimal subtotal = BigDecimal.ZERO;

    @NotNull(message = "Tax is required")
    @JsonProperty("tax")
    private BigDecimal tax = BigDecimal.ZERO;

    @NotNull(message = "Total is required")
    @Positive(message = "Total must be positive")
    @JsonProperty("total")
    private BigDecimal total = BigDecimal.ZERO;

    @NotBlank(message = "Currency is required")
    @JsonProperty("currency")
    private String currency = "USD";

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @TimeToLive(unit = TimeUnit.DAYS)
    private Long ttl = 7L; // 7 days TTL

    public Cart() {
        this.updatedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }

    public Cart(String tenantId, String userId) {
        this();
        this.tenantId = tenantId;
        this.userId = userId;
        this.id = generateCartId(tenantId, userId);
    }

    public static String generateCartId(String tenantId, String userId) {
        return tenantId + ":" + userId;
    }

    public void addItem(CartItem item) {
        // Check if item already exists
        for (CartItem existingItem : items) {
            if (existingItem.getProductId().equals(item.getProductId()) && 
                existingItem.getSku().equals(item.getSku())) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                existingItem.setUpdatedAt(LocalDateTime.now());
                updateTimestamps();
                return;
            }
        }
        // Add new item
        items.add(item);
        updateTimestamps();
    }

    public void removeItem(String productId, String sku) {
        items.removeIf(item -> 
            item.getProductId().equals(productId) && item.getSku().equals(sku));
        updateTimestamps();
    }

    public void updateItemQuantity(String productId, String sku, Integer quantity) {
        for (CartItem item : items) {
            if (item.getProductId().equals(productId) && item.getSku().equals(sku)) {
                if (quantity <= 0) {
                    removeItem(productId, sku);
                } else {
                    item.setQuantity(quantity);
                    item.setUpdatedAt(LocalDateTime.now());
                    updateTimestamps();
                }
                return;
            }
        }
    }

    public void clearItems() {
        items.clear();
        updateTimestamps();
    }

    public int getTotalItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    private void updateTimestamps() {
        this.updatedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
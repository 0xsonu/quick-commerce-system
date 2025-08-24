package com.ecommerce.orderservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.ecommerce.shared.models.TenantAware;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_tokens", indexes = {
    @Index(name = "idx_tenant_token", columnList = "tenant_id, token", unique = true),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
public class IdempotencyToken extends BaseEntity implements TenantAware {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "token", nullable = false, length = 255)
    @NotNull
    private String token;

    @Column(name = "user_id", nullable = false)
    @NotNull
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "request_hash", nullable = false, length = 64)
    @NotNull
    private String requestHash;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private IdempotencyStatus status = IdempotencyStatus.PROCESSING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    @NotNull
    private LocalDateTime expiresAt;

    // Constructors
    public IdempotencyToken() {}

    public IdempotencyToken(String tenantId, String token, Long userId, String requestHash, LocalDateTime expiresAt) {
        this.tenantId = tenantId;
        this.token = token;
        this.userId = userId;
        this.requestHash = requestHash;
        this.expiresAt = expiresAt;
    }

    // Business methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return status == IdempotencyStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == IdempotencyStatus.FAILED;
    }

    public void markCompleted(Long orderId, String responseData) {
        this.orderId = orderId;
        this.responseData = responseData;
        this.status = IdempotencyStatus.COMPLETED;
    }

    public void markFailed(String errorMessage) {
        this.responseData = errorMessage;
        this.status = IdempotencyStatus.FAILED;
    }

    // Getters and Setters
    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
package com.ecommerce.reviewservice.entity;

import com.ecommerce.shared.models.TenantAware;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Document(collection = "review_votes")
@CompoundIndex(name = "unique_user_review_vote", def = "{'tenantId': 1, 'userId': 1, 'reviewId': 1}", unique = true)
@CompoundIndex(name = "review_tenant_idx", def = "{'tenantId': 1, 'reviewId': 1}")
public class ReviewVote implements TenantAware {

    @Id
    private String id;

    @NotBlank
    @Indexed
    private String tenantId;

    @NotNull
    @Indexed
    private Long userId;

    @NotBlank
    @Indexed
    private String reviewId;

    @NotNull
    private Boolean helpful;

    @CreatedDate
    private LocalDateTime createdAt;

    // Constructors
    public ReviewVote() {}

    public ReviewVote(String tenantId, Long userId, String reviewId, Boolean helpful) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.reviewId = reviewId;
        this.helpful = helpful;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public Boolean getHelpful() {
        return helpful;
    }

    public void setHelpful(Boolean helpful) {
        this.helpful = helpful;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
package com.ecommerce.reviewservice.entity;

import com.ecommerce.shared.models.TenantAware;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "reviews")
@CompoundIndex(name = "unique_user_product_review", def = "{'tenantId': 1, 'userId': 1, 'productId': 1}", unique = true)
@CompoundIndex(name = "product_tenant_idx", def = "{'tenantId': 1, 'productId': 1}")
@CompoundIndex(name = "user_tenant_idx", def = "{'tenantId': 1, 'userId': 1}")
public class Review implements TenantAware {

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
    private String productId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 2000)
    private String comment;

    @NotBlank
    private String title;

    @NotNull
    private ReviewStatus status = ReviewStatus.PENDING;

    private String moderationNotes;

    private Long moderatedBy;

    private LocalDateTime moderatedAt;

    @NotNull
    private Boolean verified = false;

    private List<String> imageUrls;

    private Integer helpfulVotes = 0;

    private Integer totalVotes = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructors
    public Review() {}

    public Review(String tenantId, Long userId, String productId, Integer rating, String title, String comment) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.status = ReviewStatus.PENDING;
        this.verified = false;
        this.helpfulVotes = 0;
        this.totalVotes = 0;
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getModerationNotes() {
        return moderationNotes;
    }

    public void setModerationNotes(String moderationNotes) {
        this.moderationNotes = moderationNotes;
    }

    public Long getModeratedBy() {
        return moderatedBy;
    }

    public void setModeratedBy(Long moderatedBy) {
        this.moderatedBy = moderatedBy;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(LocalDateTime moderatedAt) {
        this.moderatedAt = moderatedAt;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public Integer getHelpfulVotes() {
        return helpfulVotes;
    }

    public void setHelpfulVotes(Integer helpfulVotes) {
        this.helpfulVotes = helpfulVotes;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business methods
    public void approve(Long moderatorId, String notes) {
        this.status = ReviewStatus.APPROVED;
        this.moderatedBy = moderatorId;
        this.moderatedAt = LocalDateTime.now();
        this.moderationNotes = notes;
    }

    public void reject(Long moderatorId, String notes) {
        this.status = ReviewStatus.REJECTED;
        this.moderatedBy = moderatorId;
        this.moderatedAt = LocalDateTime.now();
        this.moderationNotes = notes;
    }

    public void flag(String reason) {
        this.status = ReviewStatus.FLAGGED;
        this.moderationNotes = reason;
    }

    public double getHelpfulnessRatio() {
        return totalVotes > 0 ? (double) helpfulVotes / totalVotes : 0.0;
    }
}
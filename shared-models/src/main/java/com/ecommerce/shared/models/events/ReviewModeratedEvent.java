package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Event published when a review is moderated (approved/rejected)
 */
public class ReviewModeratedEvent extends DomainEvent {

    @NotBlank
    private String reviewId;

    @NotNull
    private Long userId;

    @NotBlank
    private String productId;

    @NotBlank
    private String moderationStatus; // APPROVED, REJECTED

    @NotNull
    private Long moderatedBy;

    private String moderationNotes;

    @NotNull
    private Integer rating;

    public ReviewModeratedEvent() {
        super();
    }

    public ReviewModeratedEvent(String tenantId, String reviewId, Long userId, String productId, 
                               String moderationStatus, Long moderatedBy, String moderationNotes, Integer rating) {
        super(tenantId, "REVIEW_MODERATED");
        this.reviewId = reviewId;
        this.userId = userId;
        this.productId = productId;
        this.moderationStatus = moderationStatus;
        this.moderatedBy = moderatedBy;
        this.moderationNotes = moderationNotes;
        this.rating = rating;
    }

    // Getters and Setters
    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
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

    public String getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(String moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public Long getModeratedBy() {
        return moderatedBy;
    }

    public void setModeratedBy(Long moderatedBy) {
        this.moderatedBy = moderatedBy;
    }

    public String getModerationNotes() {
        return moderationNotes;
    }

    public void setModerationNotes(String moderationNotes) {
        this.moderationNotes = moderationNotes;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
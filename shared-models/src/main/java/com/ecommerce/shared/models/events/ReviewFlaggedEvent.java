package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Event published when a review is flagged for moderation
 */
public class ReviewFlaggedEvent extends DomainEvent {

    @NotBlank
    private String reviewId;

    @NotNull
    private Long userId;

    @NotBlank
    private String productId;

    @NotBlank
    private String flagReason;

    private Long flaggedBy;

    @NotNull
    private Integer rating;

    public ReviewFlaggedEvent() {
        super();
    }

    public ReviewFlaggedEvent(String tenantId, String reviewId, Long userId, String productId, 
                             String flagReason, Long flaggedBy, Integer rating) {
        super(tenantId, "REVIEW_FLAGGED");
        this.reviewId = reviewId;
        this.userId = userId;
        this.productId = productId;
        this.flagReason = flagReason;
        this.flaggedBy = flaggedBy;
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

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public Long getFlaggedBy() {
        return flaggedBy;
    }

    public void setFlaggedBy(Long flaggedBy) {
        this.flaggedBy = flaggedBy;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
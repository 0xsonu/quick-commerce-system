package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Event published when a review is deleted
 */
public class ReviewDeletedEvent extends DomainEvent {

    @NotBlank
    private String reviewId;

    @NotNull
    private Long userId;

    @NotBlank
    private String productId;

    @NotNull
    private Integer rating;

    private String deletionReason;

    public ReviewDeletedEvent() {
        super();
    }

    public ReviewDeletedEvent(String tenantId, String reviewId, Long userId, String productId, 
                             Integer rating, String deletionReason) {
        super(tenantId, "REVIEW_DELETED");
        this.reviewId = reviewId;
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.deletionReason = deletionReason;
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public void setDeletionReason(String deletionReason) {
        this.deletionReason = deletionReason;
    }
}
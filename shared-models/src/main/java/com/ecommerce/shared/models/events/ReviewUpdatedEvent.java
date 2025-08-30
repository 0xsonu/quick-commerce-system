package com.ecommerce.shared.models.events;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Event published when a review is updated
 */
public class ReviewUpdatedEvent extends DomainEvent {

    @NotBlank
    private String reviewId;

    @NotNull
    private Long userId;

    @NotBlank
    private String productId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String title;

    private String comment;

    private Boolean verified;

    private Integer previousRating;

    public ReviewUpdatedEvent() {
        super();
    }

    public ReviewUpdatedEvent(String tenantId, String reviewId, Long userId, String productId, 
                             Integer rating, String title, String comment, Boolean verified, Integer previousRating) {
        super(tenantId, "REVIEW_UPDATED");
        this.reviewId = reviewId;
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.verified = verified;
        this.previousRating = previousRating;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Integer getPreviousRating() {
        return previousRating;
    }

    public void setPreviousRating(Integer previousRating) {
        this.previousRating = previousRating;
    }
}
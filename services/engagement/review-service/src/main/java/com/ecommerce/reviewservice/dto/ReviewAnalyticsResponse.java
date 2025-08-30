package com.ecommerce.reviewservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for review analytics data
 */
public class ReviewAnalyticsResponse {

    private String productId;
    private Long totalReviews;
    private Double averageRating;
    private Map<Integer, Long> ratingDistribution; // Rating (1-5) -> Count
    private Long pendingReviews;
    private Long approvedReviews;
    private Long rejectedReviews;
    private Long flaggedReviews;
    private Double moderationRate; // Percentage of reviews that have been moderated
    private LocalDateTime lastReviewDate;
    private LocalDateTime firstReviewDate;

    public ReviewAnalyticsResponse() {}

    public ReviewAnalyticsResponse(String productId, Long totalReviews, Double averageRating,
                                  Map<Integer, Long> ratingDistribution, Long pendingReviews,
                                  Long approvedReviews, Long rejectedReviews, Long flaggedReviews,
                                  Double moderationRate, LocalDateTime lastReviewDate, LocalDateTime firstReviewDate) {
        this.productId = productId;
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
        this.ratingDistribution = ratingDistribution;
        this.pendingReviews = pendingReviews;
        this.approvedReviews = approvedReviews;
        this.rejectedReviews = rejectedReviews;
        this.flaggedReviews = flaggedReviews;
        this.moderationRate = moderationRate;
        this.lastReviewDate = lastReviewDate;
        this.firstReviewDate = firstReviewDate;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Map<Integer, Long> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<Integer, Long> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    public Long getPendingReviews() {
        return pendingReviews;
    }

    public void setPendingReviews(Long pendingReviews) {
        this.pendingReviews = pendingReviews;
    }

    public Long getApprovedReviews() {
        return approvedReviews;
    }

    public void setApprovedReviews(Long approvedReviews) {
        this.approvedReviews = approvedReviews;
    }

    public Long getRejectedReviews() {
        return rejectedReviews;
    }

    public void setRejectedReviews(Long rejectedReviews) {
        this.rejectedReviews = rejectedReviews;
    }

    public Long getFlaggedReviews() {
        return flaggedReviews;
    }

    public void setFlaggedReviews(Long flaggedReviews) {
        this.flaggedReviews = flaggedReviews;
    }

    public Double getModerationRate() {
        return moderationRate;
    }

    public void setModerationRate(Double moderationRate) {
        this.moderationRate = moderationRate;
    }

    public LocalDateTime getLastReviewDate() {
        return lastReviewDate;
    }

    public void setLastReviewDate(LocalDateTime lastReviewDate) {
        this.lastReviewDate = lastReviewDate;
    }

    public LocalDateTime getFirstReviewDate() {
        return firstReviewDate;
    }

    public void setFirstReviewDate(LocalDateTime firstReviewDate) {
        this.firstReviewDate = firstReviewDate;
    }
}
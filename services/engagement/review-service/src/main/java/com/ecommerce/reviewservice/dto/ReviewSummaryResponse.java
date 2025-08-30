package com.ecommerce.reviewservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReviewSummaryResponse {
    private String productId;
    private BigDecimal averageRating;
    private Long totalReviews;
    private Long fiveStarReviews;
    private Long fourStarReviews;
    private Long threeStarReviews;
    private Long twoStarReviews;
    private Long oneStarReviews;
    private BigDecimal fiveStarPercentage;
    private BigDecimal fourStarPercentage;
    private BigDecimal threeStarPercentage;
    private BigDecimal twoStarPercentage;
    private BigDecimal oneStarPercentage;
    private Long verifiedReviews;
    private Long reviewsWithImages;
    private LocalDateTime lastReviewDate;
    private BigDecimal overallHelpfulnessScore;

    public ReviewSummaryResponse() {}

    public ReviewSummaryResponse(String productId, BigDecimal averageRating, Long totalReviews,
                               Long fiveStarReviews, Long fourStarReviews, Long threeStarReviews,
                               Long twoStarReviews, Long oneStarReviews, Long verifiedReviews,
                               Long reviewsWithImages, LocalDateTime lastReviewDate,
                               BigDecimal overallHelpfulnessScore) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.fiveStarReviews = fiveStarReviews;
        this.fourStarReviews = fourStarReviews;
        this.threeStarReviews = threeStarReviews;
        this.twoStarReviews = twoStarReviews;
        this.oneStarReviews = oneStarReviews;
        this.verifiedReviews = verifiedReviews;
        this.reviewsWithImages = reviewsWithImages;
        this.lastReviewDate = lastReviewDate;
        this.overallHelpfulnessScore = overallHelpfulnessScore;
        
        // Calculate percentages
        if (totalReviews > 0) {
            this.fiveStarPercentage = BigDecimal.valueOf(fiveStarReviews * 100.0 / totalReviews);
            this.fourStarPercentage = BigDecimal.valueOf(fourStarReviews * 100.0 / totalReviews);
            this.threeStarPercentage = BigDecimal.valueOf(threeStarReviews * 100.0 / totalReviews);
            this.twoStarPercentage = BigDecimal.valueOf(twoStarReviews * 100.0 / totalReviews);
            this.oneStarPercentage = BigDecimal.valueOf(oneStarReviews * 100.0 / totalReviews);
        } else {
            this.fiveStarPercentage = BigDecimal.ZERO;
            this.fourStarPercentage = BigDecimal.ZERO;
            this.threeStarPercentage = BigDecimal.ZERO;
            this.twoStarPercentage = BigDecimal.ZERO;
            this.oneStarPercentage = BigDecimal.ZERO;
        }
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Long getFiveStarReviews() {
        return fiveStarReviews;
    }

    public void setFiveStarReviews(Long fiveStarReviews) {
        this.fiveStarReviews = fiveStarReviews;
    }

    public Long getFourStarReviews() {
        return fourStarReviews;
    }

    public void setFourStarReviews(Long fourStarReviews) {
        this.fourStarReviews = fourStarReviews;
    }

    public Long getThreeStarReviews() {
        return threeStarReviews;
    }

    public void setThreeStarReviews(Long threeStarReviews) {
        this.threeStarReviews = threeStarReviews;
    }

    public Long getTwoStarReviews() {
        return twoStarReviews;
    }

    public void setTwoStarReviews(Long twoStarReviews) {
        this.twoStarReviews = twoStarReviews;
    }

    public Long getOneStarReviews() {
        return oneStarReviews;
    }

    public void setOneStarReviews(Long oneStarReviews) {
        this.oneStarReviews = oneStarReviews;
    }

    public BigDecimal getFiveStarPercentage() {
        return fiveStarPercentage;
    }

    public void setFiveStarPercentage(BigDecimal fiveStarPercentage) {
        this.fiveStarPercentage = fiveStarPercentage;
    }

    public BigDecimal getFourStarPercentage() {
        return fourStarPercentage;
    }

    public void setFourStarPercentage(BigDecimal fourStarPercentage) {
        this.fourStarPercentage = fourStarPercentage;
    }

    public BigDecimal getThreeStarPercentage() {
        return threeStarPercentage;
    }

    public void setThreeStarPercentage(BigDecimal threeStarPercentage) {
        this.threeStarPercentage = threeStarPercentage;
    }

    public BigDecimal getTwoStarPercentage() {
        return twoStarPercentage;
    }

    public void setTwoStarPercentage(BigDecimal twoStarPercentage) {
        this.twoStarPercentage = twoStarPercentage;
    }

    public BigDecimal getOneStarPercentage() {
        return oneStarPercentage;
    }

    public void setOneStarPercentage(BigDecimal oneStarPercentage) {
        this.oneStarPercentage = oneStarPercentage;
    }

    public Long getVerifiedReviews() {
        return verifiedReviews;
    }

    public void setVerifiedReviews(Long verifiedReviews) {
        this.verifiedReviews = verifiedReviews;
    }

    public Long getReviewsWithImages() {
        return reviewsWithImages;
    }

    public void setReviewsWithImages(Long reviewsWithImages) {
        this.reviewsWithImages = reviewsWithImages;
    }

    public LocalDateTime getLastReviewDate() {
        return lastReviewDate;
    }

    public void setLastReviewDate(LocalDateTime lastReviewDate) {
        this.lastReviewDate = lastReviewDate;
    }

    public BigDecimal getOverallHelpfulnessScore() {
        return overallHelpfulnessScore;
    }

    public void setOverallHelpfulnessScore(BigDecimal overallHelpfulnessScore) {
        this.overallHelpfulnessScore = overallHelpfulnessScore;
    }
}
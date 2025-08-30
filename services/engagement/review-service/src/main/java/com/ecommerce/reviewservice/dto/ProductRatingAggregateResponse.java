package com.ecommerce.reviewservice.dto;

import java.math.BigDecimal;
import java.util.Map;

public class ProductRatingAggregateResponse {
    private String productId;
    private BigDecimal averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution; // rating -> count
    private Long verifiedReviews;
    private Long reviewsWithImages;
    private BigDecimal helpfulnessScore;

    public ProductRatingAggregateResponse() {}

    public ProductRatingAggregateResponse(String productId, BigDecimal averageRating, Long totalReviews,
                                        Map<Integer, Long> ratingDistribution, Long verifiedReviews,
                                        Long reviewsWithImages, BigDecimal helpfulnessScore) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.ratingDistribution = ratingDistribution;
        this.verifiedReviews = verifiedReviews;
        this.reviewsWithImages = reviewsWithImages;
        this.helpfulnessScore = helpfulnessScore;
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

    public Map<Integer, Long> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<Integer, Long> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
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

    public BigDecimal getHelpfulnessScore() {
        return helpfulnessScore;
    }

    public void setHelpfulnessScore(BigDecimal helpfulnessScore) {
        this.helpfulnessScore = helpfulnessScore;
    }
}
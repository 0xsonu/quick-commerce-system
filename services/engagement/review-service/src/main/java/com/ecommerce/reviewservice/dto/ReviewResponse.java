package com.ecommerce.reviewservice.dto;

import com.ecommerce.reviewservice.entity.ReviewStatus;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewResponse {

    private String id;
    private Long userId;
    private String productId;
    private Integer rating;
    private String title;
    private String comment;
    private ReviewStatus status;
    private Boolean verified;
    private List<String> imageUrls;
    private Integer helpfulVotes;
    private Integer totalVotes;
    private Double helpfulnessRatio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ReviewResponse() {}

    public ReviewResponse(String id, Long userId, String productId, Integer rating, String title, 
                         String comment, ReviewStatus status, Boolean verified, List<String> imageUrls,
                         Integer helpfulVotes, Integer totalVotes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.status = status;
        this.verified = verified;
        this.imageUrls = imageUrls;
        this.helpfulVotes = helpfulVotes;
        this.totalVotes = totalVotes;
        this.helpfulnessRatio = totalVotes > 0 ? (double) helpfulVotes / totalVotes : 0.0;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
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

    public Double getHelpfulnessRatio() {
        return helpfulnessRatio;
    }

    public void setHelpfulnessRatio(Double helpfulnessRatio) {
        this.helpfulnessRatio = helpfulnessRatio;
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
}
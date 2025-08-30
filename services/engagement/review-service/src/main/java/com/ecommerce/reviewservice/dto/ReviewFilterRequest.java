package com.ecommerce.reviewservice.dto;

import java.util.List;

public class ReviewFilterRequest {
    private List<Integer> ratings;
    private Boolean verifiedOnly;
    private Boolean withImagesOnly;
    private Integer minHelpfulVotes;
    private String sortBy = "createdAt"; // createdAt, rating, helpfulness
    private String sortDirection = "desc"; // asc, desc

    public ReviewFilterRequest() {}

    public ReviewFilterRequest(List<Integer> ratings, Boolean verifiedOnly, Boolean withImagesOnly,
                             Integer minHelpfulVotes, String sortBy, String sortDirection) {
        this.ratings = ratings;
        this.verifiedOnly = verifiedOnly;
        this.withImagesOnly = withImagesOnly;
        this.minHelpfulVotes = minHelpfulVotes;
        this.sortBy = sortBy != null ? sortBy : "createdAt";
        this.sortDirection = sortDirection != null ? sortDirection : "desc";
    }

    // Getters and Setters
    public List<Integer> getRatings() {
        return ratings;
    }

    public void setRatings(List<Integer> ratings) {
        this.ratings = ratings;
    }

    public Boolean getVerifiedOnly() {
        return verifiedOnly;
    }

    public void setVerifiedOnly(Boolean verifiedOnly) {
        this.verifiedOnly = verifiedOnly;
    }

    public Boolean getWithImagesOnly() {
        return withImagesOnly;
    }

    public void setWithImagesOnly(Boolean withImagesOnly) {
        this.withImagesOnly = withImagesOnly;
    }

    public Integer getMinHelpfulVotes() {
        return minHelpfulVotes;
    }

    public void setMinHelpfulVotes(Integer minHelpfulVotes) {
        this.minHelpfulVotes = minHelpfulVotes;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
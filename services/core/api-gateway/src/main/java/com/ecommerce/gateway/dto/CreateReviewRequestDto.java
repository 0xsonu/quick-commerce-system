package com.ecommerce.gateway.dto;

/**
 * Request DTO for creating a review
 */
public class CreateReviewRequestDto {
    private int rating;
    private String title;
    private String comment;

    public CreateReviewRequestDto() {}

    public CreateReviewRequestDto(int rating, String title, String comment) {
        this.rating = rating;
        this.title = title;
        this.comment = comment;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
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
}
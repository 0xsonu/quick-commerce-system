package com.ecommerce.reviewservice.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class CreateReviewRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;

    @Size(max = 5, message = "Maximum 5 images allowed")
    private List<String> imageUrls;

    // Constructors
    public CreateReviewRequest() {}

    public CreateReviewRequest(String productId, Integer rating, String title, String comment) {
        this.productId = productId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
    }

    // Getters and Setters
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

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
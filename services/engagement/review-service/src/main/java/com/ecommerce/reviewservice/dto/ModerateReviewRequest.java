package com.ecommerce.reviewservice.dto;

import com.ecommerce.reviewservice.entity.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ModerateReviewRequest {

    @NotNull(message = "Status is required")
    private ReviewStatus status;

    @Size(max = 500, message = "Moderation notes must not exceed 500 characters")
    private String moderationNotes;

    // Constructors
    public ModerateReviewRequest() {}

    public ModerateReviewRequest(ReviewStatus status, String moderationNotes) {
        this.status = status;
        this.moderationNotes = moderationNotes;
    }

    // Getters and Setters
    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getModerationNotes() {
        return moderationNotes;
    }

    public void setModerationNotes(String moderationNotes) {
        this.moderationNotes = moderationNotes;
    }
}
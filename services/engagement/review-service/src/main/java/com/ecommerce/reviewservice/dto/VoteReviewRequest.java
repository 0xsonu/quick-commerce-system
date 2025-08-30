package com.ecommerce.reviewservice.dto;

import jakarta.validation.constraints.NotNull;

public class VoteReviewRequest {
    
    @NotNull(message = "Helpful flag is required")
    private Boolean helpful;

    public VoteReviewRequest() {}

    public VoteReviewRequest(Boolean helpful) {
        this.helpful = helpful;
    }

    public Boolean getHelpful() {
        return helpful;
    }

    public void setHelpful(Boolean helpful) {
        this.helpful = helpful;
    }
}
package com.ecommerce.reviewservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewTest {

    private static final String TENANT_ID = "tenant_1";
    private static final Long USER_ID = 1L;
    private static final String PRODUCT_ID = "product_123";

    @Test
    void constructor_Success() {
        // When
        Review review = new Review(TENANT_ID, USER_ID, PRODUCT_ID, 5, "Great Product", "Excellent quality");

        // Then
        assertEquals(TENANT_ID, review.getTenantId());
        assertEquals(USER_ID, review.getUserId());
        assertEquals(PRODUCT_ID, review.getProductId());
        assertEquals(5, review.getRating());
        assertEquals("Great Product", review.getTitle());
        assertEquals("Excellent quality", review.getComment());
        assertEquals(ReviewStatus.PENDING, review.getStatus());
        assertFalse(review.getVerified());
        assertEquals(0, review.getHelpfulVotes());
        assertEquals(0, review.getTotalVotes());
    }

    @Test
    void approve_Success() {
        // Given
        Review review = new Review(TENANT_ID, USER_ID, PRODUCT_ID, 5, "Great Product", "Excellent quality");
        Long moderatorId = 2L;
        String notes = "Good review";

        // When
        review.approve(moderatorId, notes);

        // Then
        assertEquals(ReviewStatus.APPROVED, review.getStatus());
        assertEquals(moderatorId, review.getModeratedBy());
        assertEquals(notes, review.getModerationNotes());
        assertNotNull(review.getModeratedAt());
    }

    @Test
    void reject_Success() {
        // Given
        Review review = new Review(TENANT_ID, USER_ID, PRODUCT_ID, 5, "Great Product", "Excellent quality");
        Long moderatorId = 2L;
        String notes = "Inappropriate content";

        // When
        review.reject(moderatorId, notes);

        // Then
        assertEquals(ReviewStatus.REJECTED, review.getStatus());
        assertEquals(moderatorId, review.getModeratedBy());
        assertEquals(notes, review.getModerationNotes());
        assertNotNull(review.getModeratedAt());
    }

    @Test
    void flag_Success() {
        // Given
        Review review = new Review(TENANT_ID, USER_ID, PRODUCT_ID, 5, "Great Product", "Excellent quality");
        String reason = "Spam content";

        // When
        review.flag(reason);

        // Then
        assertEquals(ReviewStatus.FLAGGED, review.getStatus());
        assertEquals(reason, review.getModerationNotes());
    }

    @Test
    void getHelpfulnessRatio_WithVotes() {
        // Given
        Review review = new Review(TENANT_ID, USER_ID, PRODUCT_ID, 5, "Great Product", "Excellent quality");
        review.setHelpfulVotes(8);
        review.setTotalVotes(10);

        // When
        double ratio = review.getHelpfulnessRatio();

        // Then
        assertEquals(0.8, ratio, 0.001);
    }

    @Test
    void getHelpfulnessRatio_NoVotes() {
        // Given
        Review review = new Review(TENANT_ID, USER_ID, PRODUCT_ID, 5, "Great Product", "Excellent quality");
        review.setHelpfulVotes(0);
        review.setTotalVotes(0);

        // When
        double ratio = review.getHelpfulnessRatio();

        // Then
        assertEquals(0.0, ratio, 0.001);
    }

    @Test
    void settersAndGetters_Success() {
        // Given
        Review review = new Review();
        LocalDateTime now = LocalDateTime.now();

        // When
        review.setId("review_123");
        review.setTenantId(TENANT_ID);
        review.setUserId(USER_ID);
        review.setProductId(PRODUCT_ID);
        review.setRating(4);
        review.setTitle("Good Product");
        review.setComment("Nice quality");
        review.setStatus(ReviewStatus.APPROVED);
        review.setVerified(true);
        review.setHelpfulVotes(5);
        review.setTotalVotes(7);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);

        // Then
        assertEquals("review_123", review.getId());
        assertEquals(TENANT_ID, review.getTenantId());
        assertEquals(USER_ID, review.getUserId());
        assertEquals(PRODUCT_ID, review.getProductId());
        assertEquals(4, review.getRating());
        assertEquals("Good Product", review.getTitle());
        assertEquals("Nice quality", review.getComment());
        assertEquals(ReviewStatus.APPROVED, review.getStatus());
        assertTrue(review.getVerified());
        assertEquals(5, review.getHelpfulVotes());
        assertEquals(7, review.getTotalVotes());
        assertEquals(now, review.getCreatedAt());
        assertEquals(now, review.getUpdatedAt());
    }
}
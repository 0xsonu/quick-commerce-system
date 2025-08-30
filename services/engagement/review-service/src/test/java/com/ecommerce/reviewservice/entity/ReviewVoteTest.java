package com.ecommerce.reviewservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewVoteTest {

    private static final String TENANT_ID = "tenant_1";
    private static final Long USER_ID = 1L;
    private static final String REVIEW_ID = "review_123";

    @Test
    void constructor_WithValidParameters_ShouldCreateReviewVote() {
        // Given & When
        ReviewVote reviewVote = new ReviewVote(TENANT_ID, USER_ID, REVIEW_ID, true);

        // Then
        assertEquals(TENANT_ID, reviewVote.getTenantId());
        assertEquals(USER_ID, reviewVote.getUserId());
        assertEquals(REVIEW_ID, reviewVote.getReviewId());
        assertTrue(reviewVote.getHelpful());
        assertNull(reviewVote.getId());
        assertNull(reviewVote.getCreatedAt());
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyReviewVote() {
        // Given & When
        ReviewVote reviewVote = new ReviewVote();

        // Then
        assertNull(reviewVote.getTenantId());
        assertNull(reviewVote.getUserId());
        assertNull(reviewVote.getReviewId());
        assertNull(reviewVote.getHelpful());
        assertNull(reviewVote.getId());
        assertNull(reviewVote.getCreatedAt());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        // Given
        ReviewVote reviewVote = new ReviewVote();
        String id = "vote_123";
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        reviewVote.setId(id);
        reviewVote.setTenantId(TENANT_ID);
        reviewVote.setUserId(USER_ID);
        reviewVote.setReviewId(REVIEW_ID);
        reviewVote.setHelpful(false);
        reviewVote.setCreatedAt(createdAt);

        // Then
        assertEquals(id, reviewVote.getId());
        assertEquals(TENANT_ID, reviewVote.getTenantId());
        assertEquals(USER_ID, reviewVote.getUserId());
        assertEquals(REVIEW_ID, reviewVote.getReviewId());
        assertFalse(reviewVote.getHelpful());
        assertEquals(createdAt, reviewVote.getCreatedAt());
    }

    @Test
    void tenantAware_ShouldImplementTenantAwareInterface() {
        // Given
        ReviewVote reviewVote = new ReviewVote();

        // When
        reviewVote.setTenantId(TENANT_ID);

        // Then
        assertEquals(TENANT_ID, reviewVote.getTenantId());
        assertTrue(reviewVote instanceof com.ecommerce.shared.models.TenantAware);
    }
}
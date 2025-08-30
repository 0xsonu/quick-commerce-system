package com.ecommerce.reviewservice.repository;

import com.ecommerce.reviewservice.entity.ReviewVote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class ReviewVoteRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ReviewVoteRepository reviewVoteRepository;

    private static final String TENANT_ID = "tenant_1";
    private static final Long USER_ID_1 = 1L;
    private static final Long USER_ID_2 = 2L;
    private static final String REVIEW_ID_1 = "review_123";
    private static final String REVIEW_ID_2 = "review_456";

    @BeforeEach
    void setUp() {
        reviewVoteRepository.deleteAll();
    }

    @Test
    void findByTenantIdAndUserIdAndReviewId_WithExistingVote_ShouldReturnVote() {
        // Given
        ReviewVote vote = new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_1, true);
        reviewVoteRepository.save(vote);

        // When
        Optional<ReviewVote> result = reviewVoteRepository.findByTenantIdAndUserIdAndReviewId(
            TENANT_ID, USER_ID_1, REVIEW_ID_1);

        // Then
        assertTrue(result.isPresent());
        assertEquals(TENANT_ID, result.get().getTenantId());
        assertEquals(USER_ID_1, result.get().getUserId());
        assertEquals(REVIEW_ID_1, result.get().getReviewId());
        assertTrue(result.get().getHelpful());
    }

    @Test
    void findByTenantIdAndUserIdAndReviewId_WithNonExistingVote_ShouldReturnEmpty() {
        // When
        Optional<ReviewVote> result = reviewVoteRepository.findByTenantIdAndUserIdAndReviewId(
            TENANT_ID, USER_ID_1, REVIEW_ID_1);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void existsByTenantIdAndUserIdAndReviewId_WithExistingVote_ShouldReturnTrue() {
        // Given
        ReviewVote vote = new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_1, true);
        reviewVoteRepository.save(vote);

        // When
        boolean exists = reviewVoteRepository.existsByTenantIdAndUserIdAndReviewId(
            TENANT_ID, USER_ID_1, REVIEW_ID_1);

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByTenantIdAndUserIdAndReviewId_WithNonExistingVote_ShouldReturnFalse() {
        // When
        boolean exists = reviewVoteRepository.existsByTenantIdAndUserIdAndReviewId(
            TENANT_ID, USER_ID_1, REVIEW_ID_1);

        // Then
        assertFalse(exists);
    }

    @Test
    void countByTenantIdAndReviewIdAndHelpful_WithHelpfulVotes_ShouldReturnCorrectCount() {
        // Given
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_1, true));
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_2, REVIEW_ID_1, true));
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, 3L, REVIEW_ID_1, false));

        // When
        long helpfulCount = reviewVoteRepository.countByTenantIdAndReviewIdAndHelpful(
            TENANT_ID, REVIEW_ID_1, true);
        long unhelpfulCount = reviewVoteRepository.countByTenantIdAndReviewIdAndHelpful(
            TENANT_ID, REVIEW_ID_1, false);

        // Then
        assertEquals(2L, helpfulCount);
        assertEquals(1L, unhelpfulCount);
    }

    @Test
    void countByTenantIdAndReviewId_WithMultipleVotes_ShouldReturnTotalCount() {
        // Given
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_1, true));
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_2, REVIEW_ID_1, false));
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, 3L, REVIEW_ID_1, true));
        // Different review
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_2, true));

        // When
        long totalCount = reviewVoteRepository.countByTenantIdAndReviewId(TENANT_ID, REVIEW_ID_1);

        // Then
        assertEquals(3L, totalCount);
    }

    @Test
    void deleteByTenantIdAndReviewId_WithExistingVotes_ShouldDeleteAllVotesForReview() {
        // Given
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_1, true));
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_2, REVIEW_ID_1, false));
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, 3L, REVIEW_ID_1, true));
        // Different review - should not be deleted
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_2, true));

        // When
        reviewVoteRepository.deleteByTenantIdAndReviewId(TENANT_ID, REVIEW_ID_1);

        // Then
        long remainingVotes = reviewVoteRepository.countByTenantIdAndReviewId(TENANT_ID, REVIEW_ID_1);
        long otherReviewVotes = reviewVoteRepository.countByTenantIdAndReviewId(TENANT_ID, REVIEW_ID_2);
        
        assertEquals(0L, remainingVotes);
        assertEquals(1L, otherReviewVotes);
    }

    @Test
    void uniqueConstraint_ShouldPreventDuplicateVotes() {
        // Given
        ReviewVote vote1 = new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_1, true);
        reviewVoteRepository.save(vote1);

        // When & Then
        ReviewVote vote2 = new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_1, false);
        
        // This should throw an exception due to unique constraint
        assertThrows(Exception.class, () -> {
            reviewVoteRepository.save(vote2);
        });
    }

    @Test
    void tenantIsolation_ShouldOnlyReturnVotesForCorrectTenant() {
        // Given
        String otherTenantId = "tenant_2";
        reviewVoteRepository.save(new ReviewVote(TENANT_ID, USER_ID_1, REVIEW_ID_1, true));
        reviewVoteRepository.save(new ReviewVote(otherTenantId, USER_ID_1, REVIEW_ID_1, false));

        // When
        long tenant1Count = reviewVoteRepository.countByTenantIdAndReviewId(TENANT_ID, REVIEW_ID_1);
        long tenant2Count = reviewVoteRepository.countByTenantIdAndReviewId(otherTenantId, REVIEW_ID_1);

        // Then
        assertEquals(1L, tenant1Count);
        assertEquals(1L, tenant2Count);
    }
}
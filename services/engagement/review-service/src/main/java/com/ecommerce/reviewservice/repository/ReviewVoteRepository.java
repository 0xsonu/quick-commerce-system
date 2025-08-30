package com.ecommerce.reviewservice.repository;

import com.ecommerce.reviewservice.entity.ReviewVote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewVoteRepository extends MongoRepository<ReviewVote, String> {

    // Find existing vote by user and review
    Optional<ReviewVote> findByTenantIdAndUserIdAndReviewId(String tenantId, Long userId, String reviewId);

    // Check if user has already voted on a review
    boolean existsByTenantIdAndUserIdAndReviewId(String tenantId, Long userId, String reviewId);

    // Count helpful votes for a review
    long countByTenantIdAndReviewIdAndHelpful(String tenantId, String reviewId, Boolean helpful);

    // Count total votes for a review
    long countByTenantIdAndReviewId(String tenantId, String reviewId);

    // Delete all votes for a review (when review is deleted)
    void deleteByTenantIdAndReviewId(String tenantId, String reviewId);
}
package com.ecommerce.reviewservice.repository;

import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    // Find by tenant and product
    Page<Review> findByTenantIdAndProductIdAndStatus(String tenantId, String productId, ReviewStatus status, Pageable pageable);

    // Find by tenant and user
    Page<Review> findByTenantIdAndUserId(String tenantId, Long userId, Pageable pageable);

    // Find by tenant, user and product (for uniqueness check)
    Optional<Review> findByTenantIdAndUserIdAndProductId(String tenantId, Long userId, String productId);

    // Check if review exists for user and product
    boolean existsByTenantIdAndUserIdAndProductId(String tenantId, Long userId, String productId);

    // Find reviews by status for moderation
    Page<Review> findByTenantIdAndStatus(String tenantId, ReviewStatus status, Pageable pageable);

    // Count reviews by product and rating
    @Query("{ 'tenantId': ?0, 'productId': ?1, 'rating': ?2, 'status': 'APPROVED' }")
    long countByTenantIdAndProductIdAndRating(String tenantId, String productId, Integer rating);

    // Get average rating for a product
    @Query(value = "{ 'tenantId': ?0, 'productId': ?1, 'status': 'APPROVED' }", 
           fields = "{ 'rating': 1 }")
    List<Review> findRatingsByTenantIdAndProductId(String tenantId, String productId);

    // Count total approved reviews for a product
    long countByTenantIdAndProductIdAndStatus(String tenantId, String productId, ReviewStatus status);

    // Find reviews with images
    @Query("{ 'tenantId': ?0, 'productId': ?1, 'status': 'APPROVED', 'imageUrls': { $exists: true, $not: { $size: 0 } } }")
    Page<Review> findReviewsWithImagesByTenantIdAndProductId(String tenantId, String productId, Pageable pageable);

    // Find most helpful reviews
    @Query("{ 'tenantId': ?0, 'productId': ?1, 'status': 'APPROVED', 'totalVotes': { $gt: 0 } }")
    Page<Review> findMostHelpfulReviewsByTenantIdAndProductId(String tenantId, String productId, Pageable pageable);

    // Find verified reviews
    Page<Review> findByTenantIdAndProductIdAndStatusAndVerified(String tenantId, String productId, ReviewStatus status, Boolean verified, Pageable pageable);

    // Find reviews by rating range
    @Query("{ 'tenantId': ?0, 'productId': ?1, 'status': 'APPROVED', 'rating': { $gte: ?2, $lte: ?3 } }")
    Page<Review> findByTenantIdAndProductIdAndRatingBetween(String tenantId, String productId, Integer minRating, Integer maxRating, Pageable pageable);

    // Additional methods for analytics and reporting
    
    // Find all reviews for a product (for analytics)
    List<Review> findByTenantIdAndProductId(String tenantId, String productId);

    // Find all reviews for a tenant (for analytics)
    List<Review> findByTenantId(String tenantId);

    // Find reviews moderated in a time period
    List<Review> findByTenantIdAndModeratedAtBetween(String tenantId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    // Count reviews by status
    long countByTenantIdAndStatus(String tenantId, ReviewStatus status);
}
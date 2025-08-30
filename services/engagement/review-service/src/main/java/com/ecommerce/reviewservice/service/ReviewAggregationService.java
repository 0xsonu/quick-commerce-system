package com.ecommerce.reviewservice.service;

import com.ecommerce.reviewservice.dto.*;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.entity.ReviewVote;
import com.ecommerce.reviewservice.exception.ReviewNotFoundException;
import com.ecommerce.reviewservice.exception.UnauthorizedReviewAccessException;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.ecommerce.reviewservice.repository.ReviewVoteRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewAggregationService.class);

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ReviewAggregationService(ReviewRepository reviewRepository, 
                                  ReviewVoteRepository reviewVoteRepository,
                                  MongoTemplate mongoTemplate) {
        this.reviewRepository = reviewRepository;
        this.reviewVoteRepository = reviewVoteRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Transactional(readOnly = true)
    public ProductRatingAggregateResponse getProductRatingAggregate(String productId) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Calculating rating aggregate for product {} in tenant {}", productId, tenantId);

        // Get all approved reviews for the product
        List<Review> reviews = reviewRepository.findByTenantIdAndProductIdAndStatus(
            tenantId, productId, ReviewStatus.APPROVED, Pageable.unpaged()).getContent();

        if (reviews.isEmpty()) {
            return new ProductRatingAggregateResponse(productId, BigDecimal.ZERO, 0L, 
                new HashMap<>(), 0L, 0L, BigDecimal.ZERO);
        }

        // Calculate average rating
        double avgRating = reviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);

        // Calculate rating distribution
        Map<Integer, Long> ratingDistribution = reviews.stream()
            .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        // Ensure all ratings 1-5 are present
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.putIfAbsent(i, 0L);
        }

        // Count verified reviews
        long verifiedReviews = reviews.stream()
            .mapToLong(review -> review.getVerified() ? 1 : 0)
            .sum();

        // Count reviews with images
        long reviewsWithImages = reviews.stream()
            .mapToLong(review -> (review.getImageUrls() != null && !review.getImageUrls().isEmpty()) ? 1 : 0)
            .sum();

        // Calculate overall helpfulness score
        double helpfulnessScore = reviews.stream()
            .mapToDouble(Review::getHelpfulnessRatio)
            .average()
            .orElse(0.0);

        return new ProductRatingAggregateResponse(
            productId,
            BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP),
            (long) reviews.size(),
            ratingDistribution,
            verifiedReviews,
            reviewsWithImages,
            BigDecimal.valueOf(helpfulnessScore).setScale(3, RoundingMode.HALF_UP)
        );
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(String productId) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Generating review summary for product {} in tenant {}", productId, tenantId);

        // Get all approved reviews for the product
        List<Review> reviews = reviewRepository.findByTenantIdAndProductIdAndStatus(
            tenantId, productId, ReviewStatus.APPROVED, Pageable.unpaged()).getContent();

        if (reviews.isEmpty()) {
            return new ReviewSummaryResponse(productId, BigDecimal.ZERO, 0L, 0L, 0L, 0L, 0L, 0L, 
                0L, 0L, null, BigDecimal.ZERO);
        }

        // Calculate average rating
        double avgRating = reviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);

        // Count reviews by rating
        Map<Integer, Long> ratingCounts = reviews.stream()
            .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        long fiveStarReviews = ratingCounts.getOrDefault(5, 0L);
        long fourStarReviews = ratingCounts.getOrDefault(4, 0L);
        long threeStarReviews = ratingCounts.getOrDefault(3, 0L);
        long twoStarReviews = ratingCounts.getOrDefault(2, 0L);
        long oneStarReviews = ratingCounts.getOrDefault(1, 0L);

        // Count verified reviews
        long verifiedReviews = reviews.stream()
            .mapToLong(review -> review.getVerified() ? 1 : 0)
            .sum();

        // Count reviews with images
        long reviewsWithImages = reviews.stream()
            .mapToLong(review -> (review.getImageUrls() != null && !review.getImageUrls().isEmpty()) ? 1 : 0)
            .sum();

        // Find last review date
        LocalDateTime lastReviewDate = reviews.stream()
            .map(Review::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        // Calculate overall helpfulness score
        double helpfulnessScore = reviews.stream()
            .mapToDouble(Review::getHelpfulnessRatio)
            .average()
            .orElse(0.0);

        return new ReviewSummaryResponse(
            productId,
            BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP),
            (long) reviews.size(),
            fiveStarReviews,
            fourStarReviews,
            threeStarReviews,
            twoStarReviews,
            oneStarReviews,
            verifiedReviews,
            reviewsWithImages,
            lastReviewDate,
            BigDecimal.valueOf(helpfulnessScore).setScale(3, RoundingMode.HALF_UP)
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getFilteredReviews(String productId, ReviewFilterRequest filterRequest, 
                                                          int page, int size) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Getting filtered reviews for product {} in tenant {} with filters: {}", 
                   productId, tenantId, filterRequest);

        // Build query criteria
        Criteria criteria = Criteria.where("tenantId").is(tenantId)
            .and("productId").is(productId)
            .and("status").is(ReviewStatus.APPROVED);

        // Apply rating filter
        if (filterRequest.getRatings() != null && !filterRequest.getRatings().isEmpty()) {
            criteria.and("rating").in(filterRequest.getRatings());
        }

        // Apply verified filter
        if (Boolean.TRUE.equals(filterRequest.getVerifiedOnly())) {
            criteria.and("verified").is(true);
        }

        // Apply images filter
        if (Boolean.TRUE.equals(filterRequest.getWithImagesOnly())) {
            criteria.and("imageUrls").exists(true).ne(Collections.emptyList());
        }

        // Apply minimum helpful votes filter
        if (filterRequest.getMinHelpfulVotes() != null && filterRequest.getMinHelpfulVotes() > 0) {
            criteria.and("helpfulVotes").gte(filterRequest.getMinHelpfulVotes());
        }

        // Build sort
        Sort sort = buildSort(filterRequest.getSortBy(), filterRequest.getSortDirection());
        
        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Execute query
        Query query = new Query(criteria).with(pageable);
        List<Review> reviews = mongoTemplate.find(query, Review.class);
        
        // Get total count
        long totalCount = mongoTemplate.count(new Query(criteria), Review.class);

        // Convert to responses
        List<ReviewResponse> reviewResponses = reviews.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        // Calculate pagination info
        int totalPages = (int) Math.ceil((double) totalCount / size);
        boolean isFirst = page == 0;
        boolean isLast = page >= totalPages - 1;

        return new PagedResponse<>(reviewResponses, page, size, totalCount, totalPages, isFirst, isLast);
    }

    public ReviewResponse voteOnReview(String reviewId, VoteReviewRequest voteRequest, Long userId) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("User {} voting on review {} in tenant {}: helpful={}", 
                   userId, reviewId, tenantId, voteRequest.getHelpful());

        // Find the review
        Review review = findReviewByIdAndTenant(reviewId, tenantId);
        
        // Check if user is trying to vote on their own review
        if (review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewAccessException("Users cannot vote on their own reviews");
        }

        // Check if user has already voted
        Optional<ReviewVote> existingVote = reviewVoteRepository.findByTenantIdAndUserIdAndReviewId(
            tenantId, userId, reviewId);

        if (existingVote.isPresent()) {
            // Update existing vote
            ReviewVote vote = existingVote.get();
            Boolean previousVote = vote.getHelpful();
            vote.setHelpful(voteRequest.getHelpful());
            reviewVoteRepository.save(vote);
            
            // Update review vote counts
            updateReviewVoteCounts(review, previousVote, voteRequest.getHelpful());
        } else {
            // Create new vote
            ReviewVote vote = new ReviewVote(tenantId, userId, reviewId, voteRequest.getHelpful());
            reviewVoteRepository.save(vote);
            
            // Update review vote counts
            updateReviewVoteCounts(review, null, voteRequest.getHelpful());
        }

        Review savedReview = reviewRepository.save(review);
        
        logger.info("Vote recorded for review {}: helpful votes={}, total votes={}", 
                   reviewId, savedReview.getHelpfulVotes(), savedReview.getTotalVotes());

        return mapToResponse(savedReview);
    }

    private void updateReviewVoteCounts(Review review, Boolean previousVote, Boolean newVote) {
        // Handle previous vote removal
        if (previousVote != null) {
            if (previousVote) {
                review.setHelpfulVotes(Math.max(0, review.getHelpfulVotes() - 1));
            }
            review.setTotalVotes(Math.max(0, review.getTotalVotes() - 1));
        }

        // Handle new vote addition
        if (newVote) {
            review.setHelpfulVotes(review.getHelpfulVotes() + 1);
        }
        review.setTotalVotes(review.getTotalVotes() + 1);
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;

        return switch (sortBy.toLowerCase()) {
            case "rating" -> Sort.by(direction, "rating");
            case "helpfulness" -> Sort.by(direction, "helpfulVotes").and(Sort.by(Sort.Direction.DESC, "totalVotes"));
            case "createdat", "created_at" -> Sort.by(direction, "createdAt");
            default -> Sort.by(direction, "createdAt");
        };
    }

    private Review findReviewByIdAndTenant(String reviewId, String tenantId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        
        if (reviewOpt.isEmpty()) {
            throw new ReviewNotFoundException("Review not found: " + reviewId);
        }
        
        Review review = reviewOpt.get();
        if (!review.getTenantId().equals(tenantId)) {
            throw new ReviewNotFoundException("Review not found: " + reviewId);
        }
        
        return review;
    }

    private ReviewResponse mapToResponse(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getUserId(),
            review.getProductId(),
            review.getRating(),
            review.getTitle(),
            review.getComment(),
            review.getStatus(),
            review.getVerified(),
            review.getImageUrls(),
            review.getHelpfulVotes(),
            review.getTotalVotes(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
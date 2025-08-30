package com.ecommerce.reviewservice.service;

import com.ecommerce.reviewservice.dto.ModerationReportResponse;
import com.ecommerce.reviewservice.dto.ReviewAnalyticsResponse;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating review analytics and reporting
 */
@Service
@Transactional(readOnly = true)
public class ReviewAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewAnalyticsService.class);

    private final ReviewRepository reviewRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ReviewAnalyticsService(ReviewRepository reviewRepository, MongoTemplate mongoTemplate) {
        this.reviewRepository = reviewRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Generate analytics for a specific product
     */
    public ReviewAnalyticsResponse getProductAnalytics(String productId) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Generating analytics for product {} in tenant {}", productId, tenantId);

        List<Review> reviews = reviewRepository.findByTenantIdAndProductId(tenantId, productId);
        
        if (reviews.isEmpty()) {
            return new ReviewAnalyticsResponse(productId, 0L, 0.0, new HashMap<>(), 
                                             0L, 0L, 0L, 0L, 0.0, null, null);
        }

        // Calculate basic metrics
        long totalReviews = reviews.size();
        double averageRating = reviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);

        // Rating distribution
        Map<Integer, Long> ratingDistribution = reviews.stream()
            .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        // Status counts
        Map<ReviewStatus, Long> statusCounts = reviews.stream()
            .collect(Collectors.groupingBy(Review::getStatus, Collectors.counting()));

        long pendingReviews = statusCounts.getOrDefault(ReviewStatus.PENDING, 0L);
        long approvedReviews = statusCounts.getOrDefault(ReviewStatus.APPROVED, 0L);
        long rejectedReviews = statusCounts.getOrDefault(ReviewStatus.REJECTED, 0L);
        long flaggedReviews = statusCounts.getOrDefault(ReviewStatus.FLAGGED, 0L);

        // Moderation rate
        long moderatedReviews = approvedReviews + rejectedReviews;
        double moderationRate = totalReviews > 0 ? (double) moderatedReviews / totalReviews * 100 : 0.0;

        // Date range
        LocalDateTime firstReviewDate = reviews.stream()
            .map(Review::getCreatedAt)
            .min(LocalDateTime::compareTo)
            .orElse(null);

        LocalDateTime lastReviewDate = reviews.stream()
            .map(Review::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        return new ReviewAnalyticsResponse(
            productId, totalReviews, averageRating, ratingDistribution,
            pendingReviews, approvedReviews, rejectedReviews, flaggedReviews,
            moderationRate, lastReviewDate, firstReviewDate
        );
    }

    /**
     * Generate moderation report for a given time period
     */
    public ModerationReportResponse getModerationReport(LocalDateTime startDate, LocalDateTime endDate) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Generating moderation report for tenant {} from {} to {}", tenantId, startDate, endDate);

        // Find all reviews moderated in the given period
        List<Review> moderatedReviews = reviewRepository.findByTenantIdAndModeratedAtBetween(
            tenantId, startDate, endDate);

        if (moderatedReviews.isEmpty()) {
            return new ModerationReportResponse(0L, 0L, 0L, 0L, 0.0, 0.0, 
                                              new HashMap<>(), new HashMap<>(), 
                                              startDate, endDate, 0.0);
        }

        long totalModerated = moderatedReviews.size();

        // Status counts
        Map<ReviewStatus, Long> statusCounts = moderatedReviews.stream()
            .collect(Collectors.groupingBy(Review::getStatus, Collectors.counting()));

        long approvedCount = statusCounts.getOrDefault(ReviewStatus.APPROVED, 0L);
        long rejectedCount = statusCounts.getOrDefault(ReviewStatus.REJECTED, 0L);
        long flaggedCount = statusCounts.getOrDefault(ReviewStatus.FLAGGED, 0L);

        // Rates
        double approvalRate = totalModerated > 0 ? (double) approvedCount / totalModerated * 100 : 0.0;
        double rejectionRate = totalModerated > 0 ? (double) rejectedCount / totalModerated * 100 : 0.0;

        // Moderator activity
        Map<Long, Long> moderatorActivity = moderatedReviews.stream()
            .filter(review -> review.getModeratedBy() != null)
            .collect(Collectors.groupingBy(Review::getModeratedBy, Collectors.counting()));

        // Flag reasons
        Map<String, Long> flagReasons = moderatedReviews.stream()
            .filter(review -> review.getStatus() == ReviewStatus.FLAGGED || review.getStatus() == ReviewStatus.REJECTED)
            .filter(review -> review.getModerationNotes() != null)
            .collect(Collectors.groupingBy(Review::getModerationNotes, Collectors.counting()));

        // Average moderation time
        double averageModerationTime = moderatedReviews.stream()
            .filter(review -> review.getCreatedAt() != null && review.getModeratedAt() != null)
            .mapToLong(review -> Duration.between(review.getCreatedAt(), review.getModeratedAt()).toHours())
            .average()
            .orElse(0.0);

        return new ModerationReportResponse(
            totalModerated, approvedCount, rejectedCount, flaggedCount,
            approvalRate, rejectionRate, moderatorActivity, flagReasons,
            startDate, endDate, averageModerationTime
        );
    }

    /**
     * Get overall tenant analytics
     */
    public ReviewAnalyticsResponse getTenantAnalytics() {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Generating overall analytics for tenant {}", tenantId);

        List<Review> allReviews = reviewRepository.findByTenantId(tenantId);
        
        if (allReviews.isEmpty()) {
            return new ReviewAnalyticsResponse(null, 0L, 0.0, new HashMap<>(), 
                                             0L, 0L, 0L, 0L, 0.0, null, null);
        }

        // Calculate metrics similar to product analytics but for all reviews
        long totalReviews = allReviews.size();
        double averageRating = allReviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);

        Map<Integer, Long> ratingDistribution = allReviews.stream()
            .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        Map<ReviewStatus, Long> statusCounts = allReviews.stream()
            .collect(Collectors.groupingBy(Review::getStatus, Collectors.counting()));

        long pendingReviews = statusCounts.getOrDefault(ReviewStatus.PENDING, 0L);
        long approvedReviews = statusCounts.getOrDefault(ReviewStatus.APPROVED, 0L);
        long rejectedReviews = statusCounts.getOrDefault(ReviewStatus.REJECTED, 0L);
        long flaggedReviews = statusCounts.getOrDefault(ReviewStatus.FLAGGED, 0L);

        long moderatedReviews = approvedReviews + rejectedReviews;
        double moderationRate = totalReviews > 0 ? (double) moderatedReviews / totalReviews * 100 : 0.0;

        LocalDateTime firstReviewDate = allReviews.stream()
            .map(Review::getCreatedAt)
            .min(LocalDateTime::compareTo)
            .orElse(null);

        LocalDateTime lastReviewDate = allReviews.stream()
            .map(Review::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        return new ReviewAnalyticsResponse(
            null, totalReviews, averageRating, ratingDistribution,
            pendingReviews, approvedReviews, rejectedReviews, flaggedReviews,
            moderationRate, lastReviewDate, firstReviewDate
        );
    }

    /**
     * Get top products by review count
     */
    public Map<String, Long> getTopProductsByReviewCount(int limit) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Getting top {} products by review count for tenant {}", limit, tenantId);

        MatchOperation matchStage = Aggregation.match(Criteria.where("tenantId").is(tenantId));
        GroupOperation groupStage = Aggregation.group("productId").count().as("reviewCount");
        Aggregation aggregation = Aggregation.newAggregation(
            matchStage,
            groupStage,
            Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "reviewCount"),
            Aggregation.limit(limit)
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "reviews", Map.class);
        
        return results.getMappedResults().stream()
            .collect(Collectors.toMap(
                result -> (String) result.get("_id"),
                result -> ((Number) result.get("reviewCount")).longValue(),
                (existing, replacement) -> existing,
                java.util.LinkedHashMap::new
            ));
    }

    /**
     * Get products with highest average ratings
     */
    public Map<String, Double> getTopRatedProducts(int limit) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Getting top {} rated products for tenant {}", limit, tenantId);

        MatchOperation matchStage = Aggregation.match(
            Criteria.where("tenantId").is(tenantId)
                .and("status").is(ReviewStatus.APPROVED)
        );
        GroupOperation groupStage = Aggregation.group("productId")
            .avg("rating").as("averageRating")
            .count().as("reviewCount");
        MatchOperation minReviewsStage = Aggregation.match(Criteria.where("reviewCount").gte(5)); // At least 5 reviews
        
        Aggregation aggregation = Aggregation.newAggregation(
            matchStage,
            groupStage,
            minReviewsStage,
            Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "averageRating"),
            Aggregation.limit(limit)
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "reviews", Map.class);
        
        return results.getMappedResults().stream()
            .collect(Collectors.toMap(
                result -> (String) result.get("_id"),
                result -> ((Number) result.get("averageRating")).doubleValue(),
                (existing, replacement) -> existing,
                java.util.LinkedHashMap::new
            ));
    }
}
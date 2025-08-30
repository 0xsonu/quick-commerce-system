package com.ecommerce.reviewservice.service;

import com.ecommerce.reviewservice.dto.ModerationReportResponse;
import com.ecommerce.reviewservice.dto.ReviewAnalyticsResponse;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewAnalyticsServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ReviewAnalyticsService analyticsService;

    private static final String TENANT_ID = "tenant123";
    private static final String PRODUCT_ID = "product123";

    @BeforeEach
    void setUp() {
        // Mock TenantContext
        try (MockedStatic<TenantContext> mockedTenantContext = mockStatic(TenantContext.class)) {
            mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        }
    }

    @Test
    void getProductAnalytics_WithNoReviews_ShouldReturnEmptyAnalytics() {
        // Given
        when(reviewRepository.findByTenantIdAndProductId(TENANT_ID, PRODUCT_ID))
            .thenReturn(Arrays.asList());

        try (MockedStatic<TenantContext> mockedTenantContext = mockStatic(TenantContext.class)) {
            mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID);

            // When
            ReviewAnalyticsResponse result = analyticsService.getProductAnalytics(PRODUCT_ID);

            // Then
            assertEquals(PRODUCT_ID, result.getProductId());
            assertEquals(0L, result.getTotalReviews());
            assertEquals(0.0, result.getAverageRating());
            assertTrue(result.getRatingDistribution().isEmpty());
            assertEquals(0L, result.getPendingReviews());
            assertEquals(0L, result.getApprovedReviews());
            assertEquals(0L, result.getRejectedReviews());
            assertEquals(0L, result.getFlaggedReviews());
            assertEquals(0.0, result.getModerationRate());
            assertNull(result.getFirstReviewDate());
            assertNull(result.getLastReviewDate());
        }
    }

    @Test
    void getProductAnalytics_WithReviews_ShouldReturnCorrectAnalytics() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlier = now.minusDays(1);

        List<Review> reviews = Arrays.asList(
            createReview("1", 5, ReviewStatus.APPROVED, now),
            createReview("2", 4, ReviewStatus.APPROVED, earlier),
            createReview("3", 3, ReviewStatus.PENDING, now),
            createReview("4", 5, ReviewStatus.REJECTED, now),
            createReview("5", 2, ReviewStatus.FLAGGED, now)
        );

        when(reviewRepository.findByTenantIdAndProductId(TENANT_ID, PRODUCT_ID))
            .thenReturn(reviews);

        try (MockedStatic<TenantContext> mockedTenantContext = mockStatic(TenantContext.class)) {
            mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID);

            // When
            ReviewAnalyticsResponse result = analyticsService.getProductAnalytics(PRODUCT_ID);

            // Then
            assertEquals(PRODUCT_ID, result.getProductId());
            assertEquals(5L, result.getTotalReviews());
            assertEquals(3.8, result.getAverageRating(), 0.01); // (5+4+3+5+2)/5 = 3.8
            
            // Rating distribution
            Map<Integer, Long> distribution = result.getRatingDistribution();
            assertEquals(1L, distribution.get(2));
            assertEquals(1L, distribution.get(3));
            assertEquals(1L, distribution.get(4));
            assertEquals(2L, distribution.get(5));
            
            // Status counts
            assertEquals(1L, result.getPendingReviews());
            assertEquals(2L, result.getApprovedReviews());
            assertEquals(1L, result.getRejectedReviews());
            assertEquals(1L, result.getFlaggedReviews());
            
            // Moderation rate: (2 approved + 1 rejected) / 5 * 100 = 60%
            assertEquals(60.0, result.getModerationRate(), 0.01);
            
            assertEquals(earlier, result.getFirstReviewDate());
            assertEquals(now, result.getLastReviewDate());
        }
    }

    @Test
    void getModerationReport_WithNoReviews_ShouldReturnEmptyReport() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        when(reviewRepository.findByTenantIdAndModeratedAtBetween(TENANT_ID, startDate, endDate))
            .thenReturn(Arrays.asList());

        try (MockedStatic<TenantContext> mockedTenantContext = mockStatic(TenantContext.class)) {
            mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID);

            // When
            ModerationReportResponse result = analyticsService.getModerationReport(startDate, endDate);

            // Then
            assertEquals(0L, result.getTotalReviewsModerated());
            assertEquals(0L, result.getApprovedCount());
            assertEquals(0L, result.getRejectedCount());
            assertEquals(0L, result.getFlaggedCount());
            assertEquals(0.0, result.getApprovalRate());
            assertEquals(0.0, result.getRejectionRate());
            assertTrue(result.getModeratorActivity().isEmpty());
            assertTrue(result.getFlagReasons().isEmpty());
            assertEquals(0.0, result.getAverageModerationTime());
            assertEquals(startDate, result.getReportPeriodStart());
            assertEquals(endDate, result.getReportPeriodEnd());
        }
    }

    @Test
    void getModerationReport_WithReviews_ShouldReturnCorrectReport() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime moderatedAt = LocalDateTime.now().minusDays(1);

        List<Review> moderatedReviews = Arrays.asList(
            createModeratedReview("1", ReviewStatus.APPROVED, 1L, "Good review", moderatedAt),
            createModeratedReview("2", ReviewStatus.APPROVED, 1L, "Another good review", moderatedAt),
            createModeratedReview("3", ReviewStatus.REJECTED, 2L, "Inappropriate content", moderatedAt),
            createModeratedReview("4", ReviewStatus.FLAGGED, 2L, "Spam", moderatedAt)
        );

        when(reviewRepository.findByTenantIdAndModeratedAtBetween(TENANT_ID, startDate, endDate))
            .thenReturn(moderatedReviews);

        try (MockedStatic<TenantContext> mockedTenantContext = mockStatic(TenantContext.class)) {
            mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID);

            // When
            ModerationReportResponse result = analyticsService.getModerationReport(startDate, endDate);

            // Then
            assertEquals(4L, result.getTotalReviewsModerated());
            assertEquals(2L, result.getApprovedCount());
            assertEquals(1L, result.getRejectedCount());
            assertEquals(1L, result.getFlaggedCount());
            assertEquals(50.0, result.getApprovalRate(), 0.01); // 2/4 * 100
            assertEquals(25.0, result.getRejectionRate(), 0.01); // 1/4 * 100
            
            // Moderator activity
            Map<Long, Long> moderatorActivity = result.getModeratorActivity();
            assertEquals(2L, moderatorActivity.get(1L));
            assertEquals(2L, moderatorActivity.get(2L));
            
            // Flag reasons
            Map<String, Long> flagReasons = result.getFlagReasons();
            assertEquals(1L, flagReasons.get("Inappropriate content"));
            assertEquals(1L, flagReasons.get("Spam"));
        }
    }

    @Test
    void getTenantAnalytics_ShouldReturnOverallAnalytics() {
        // Given
        List<Review> allReviews = Arrays.asList(
            createReview("1", 5, ReviewStatus.APPROVED, LocalDateTime.now()),
            createReview("2", 4, ReviewStatus.APPROVED, LocalDateTime.now()),
            createReview("3", 3, ReviewStatus.PENDING, LocalDateTime.now())
        );

        when(reviewRepository.findByTenantId(TENANT_ID)).thenReturn(allReviews);

        try (MockedStatic<TenantContext> mockedTenantContext = mockStatic(TenantContext.class)) {
            mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID);

            // When
            ReviewAnalyticsResponse result = analyticsService.getTenantAnalytics();

            // Then
            assertNull(result.getProductId()); // Should be null for tenant-wide analytics
            assertEquals(3L, result.getTotalReviews());
            assertEquals(4.0, result.getAverageRating(), 0.01); // (5+4+3)/3 = 4.0
            assertEquals(1L, result.getPendingReviews());
            assertEquals(2L, result.getApprovedReviews());
        }
    }

    private Review createReview(String id, int rating, ReviewStatus status, LocalDateTime createdAt) {
        Review review = new Review();
        review.setId(id);
        review.setTenantId(TENANT_ID);
        review.setProductId(PRODUCT_ID);
        review.setUserId(1L);
        review.setRating(rating);
        review.setStatus(status);
        review.setCreatedAt(createdAt);
        review.setTitle("Test Review");
        review.setComment("Test comment");
        return review;
    }

    private Review createModeratedReview(String id, ReviewStatus status, Long moderatedBy, 
                                       String moderationNotes, LocalDateTime moderatedAt) {
        Review review = createReview(id, 5, status, LocalDateTime.now().minusDays(2));
        review.setModeratedBy(moderatedBy);
        review.setModerationNotes(moderationNotes);
        review.setModeratedAt(moderatedAt);
        return review;
    }
}
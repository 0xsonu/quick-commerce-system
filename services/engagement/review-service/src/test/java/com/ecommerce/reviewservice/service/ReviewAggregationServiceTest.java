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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewAggregationServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewVoteRepository reviewVoteRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ReviewAggregationService reviewAggregationService;

    private static final String TENANT_ID = "tenant_1";
    private static final String PRODUCT_ID = "product_123";
    private static final Long USER_ID = 1L;
    private static final String REVIEW_ID = "review_123";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @Test
    void getProductRatingAggregate_WithReviews_ShouldReturnCorrectAggregate() {
        // Given
        List<Review> reviews = createSampleReviews();
        Page<Review> reviewPage = new PageImpl<>(reviews);
        
        when(reviewRepository.findByTenantIdAndProductIdAndStatus(
            eq(TENANT_ID), eq(PRODUCT_ID), eq(ReviewStatus.APPROVED), any(Pageable.class)))
            .thenReturn(reviewPage);

        // When
        ProductRatingAggregateResponse result = reviewAggregationService.getProductRatingAggregate(PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.getProductId());
        assertEquals(BigDecimal.valueOf(3.60).setScale(2), result.getAverageRating());
        assertEquals(5L, result.getTotalReviews());
        
        Map<Integer, Long> expectedDistribution = Map.of(
            1, 1L, 2, 0L, 3, 1L, 4, 1L, 5, 2L
        );
        assertEquals(expectedDistribution, result.getRatingDistribution());
        assertEquals(2L, result.getVerifiedReviews());
        assertEquals(1L, result.getReviewsWithImages());
    }

    @Test
    void getProductRatingAggregate_WithNoReviews_ShouldReturnEmptyAggregate() {
        // Given
        Page<Review> emptyPage = new PageImpl<>(Collections.emptyList());
        
        when(reviewRepository.findByTenantIdAndProductIdAndStatus(
            eq(TENANT_ID), eq(PRODUCT_ID), eq(ReviewStatus.APPROVED), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        ProductRatingAggregateResponse result = reviewAggregationService.getProductRatingAggregate(PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.getProductId());
        assertEquals(BigDecimal.ZERO, result.getAverageRating());
        assertEquals(0L, result.getTotalReviews());
        assertTrue(result.getRatingDistribution().isEmpty());
        assertEquals(0L, result.getVerifiedReviews());
        assertEquals(0L, result.getReviewsWithImages());
        assertEquals(BigDecimal.ZERO, result.getHelpfulnessScore());
    }

    @Test
    void getReviewSummary_WithReviews_ShouldReturnCorrectSummary() {
        // Given
        List<Review> reviews = createSampleReviews();
        Page<Review> reviewPage = new PageImpl<>(reviews);
        
        when(reviewRepository.findByTenantIdAndProductIdAndStatus(
            eq(TENANT_ID), eq(PRODUCT_ID), eq(ReviewStatus.APPROVED), any(Pageable.class)))
            .thenReturn(reviewPage);

        // When
        ReviewSummaryResponse result = reviewAggregationService.getReviewSummary(PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.getProductId());
        assertEquals(BigDecimal.valueOf(3.60).setScale(2), result.getAverageRating());
        assertEquals(5L, result.getTotalReviews());
        assertEquals(2L, result.getFiveStarReviews());
        assertEquals(1L, result.getFourStarReviews());
        assertEquals(1L, result.getThreeStarReviews());
        assertEquals(0L, result.getTwoStarReviews());
        assertEquals(1L, result.getOneStarReviews());
        assertEquals(2L, result.getVerifiedReviews());
        assertEquals(1L, result.getReviewsWithImages());
        assertNotNull(result.getLastReviewDate());
    }

    @Test
    void getFilteredReviews_WithFilters_ShouldApplyFiltersCorrectly() {
        // Given
        ReviewFilterRequest filterRequest = new ReviewFilterRequest();
        filterRequest.setRatings(Arrays.asList(4, 5));
        filterRequest.setVerifiedOnly(true);
        filterRequest.setSortBy("rating");
        filterRequest.setSortDirection("desc");

        List<Review> filteredReviews = createSampleReviews().subList(0, 2);
        
        when(mongoTemplate.find(any(Query.class), eq(Review.class)))
            .thenReturn(filteredReviews);
        when(mongoTemplate.count(any(Query.class), eq(Review.class)))
            .thenReturn(2L);

        // When
        PagedResponse<ReviewResponse> result = reviewAggregationService.getFilteredReviews(
            PRODUCT_ID, filterRequest, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
    }

    @Test
    void voteOnReview_NewVote_ShouldCreateVoteAndUpdateCounts() {
        // Given
        Review review = createSampleReview(REVIEW_ID, 2L, 5, "Great product", true);
        review.setHelpfulVotes(5);
        review.setTotalVotes(8);
        
        VoteReviewRequest voteRequest = new VoteReviewRequest(true);
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewVoteRepository.findByTenantIdAndUserIdAndReviewId(TENANT_ID, USER_ID, REVIEW_ID))
            .thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // When
        ReviewResponse result = reviewAggregationService.voteOnReview(REVIEW_ID, voteRequest, USER_ID);

        // Then
        assertNotNull(result);
        verify(reviewVoteRepository).save(any(ReviewVote.class));
        verify(reviewRepository).save(review);
        assertEquals(6, review.getHelpfulVotes());
        assertEquals(9, review.getTotalVotes());
    }

    @Test
    void voteOnReview_UpdateExistingVote_ShouldUpdateVoteAndCounts() {
        // Given
        Review review = createSampleReview(REVIEW_ID, 2L, 5, "Great product", true);
        review.setHelpfulVotes(5);
        review.setTotalVotes(8);
        
        ReviewVote existingVote = new ReviewVote(TENANT_ID, USER_ID, REVIEW_ID, false);
        VoteReviewRequest voteRequest = new VoteReviewRequest(true);
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewVoteRepository.findByTenantIdAndUserIdAndReviewId(TENANT_ID, USER_ID, REVIEW_ID))
            .thenReturn(Optional.of(existingVote));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // When
        ReviewResponse result = reviewAggregationService.voteOnReview(REVIEW_ID, voteRequest, USER_ID);

        // Then
        assertNotNull(result);
        verify(reviewVoteRepository).save(existingVote);
        verify(reviewRepository).save(review);
        assertTrue(existingVote.getHelpful());
        assertEquals(6, review.getHelpfulVotes()); // +1 for new helpful vote
        assertEquals(8, review.getTotalVotes()); // Same total (changed from unhelpful to helpful)
    }

    @Test
    void voteOnReview_VoteOnOwnReview_ShouldThrowException() {
        // Given
        Review review = createSampleReview(REVIEW_ID, USER_ID, 5, "Great product", true);
        VoteReviewRequest voteRequest = new VoteReviewRequest(true);
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // When & Then
        assertThrows(UnauthorizedReviewAccessException.class, () ->
            reviewAggregationService.voteOnReview(REVIEW_ID, voteRequest, USER_ID));
    }

    @Test
    void voteOnReview_ReviewNotFound_ShouldThrowException() {
        // Given
        VoteReviewRequest voteRequest = new VoteReviewRequest(true);
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ReviewNotFoundException.class, () ->
            reviewAggregationService.voteOnReview(REVIEW_ID, voteRequest, USER_ID));
    }

    @Test
    void voteOnReview_WrongTenant_ShouldThrowException() {
        // Given
        Review review = createSampleReview(REVIEW_ID, 2L, 5, "Great product", true);
        review.setTenantId("different_tenant");
        VoteReviewRequest voteRequest = new VoteReviewRequest(true);
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // When & Then
        assertThrows(ReviewNotFoundException.class, () ->
            reviewAggregationService.voteOnReview(REVIEW_ID, voteRequest, USER_ID));
    }

    private List<Review> createSampleReviews() {
        List<Review> reviews = new ArrayList<>();
        
        // 5-star verified review with images
        Review review1 = createSampleReview("1", 1L, 5, "Excellent", true);
        review1.setImageUrls(Arrays.asList("image1.jpg"));
        review1.setHelpfulVotes(10);
        review1.setTotalVotes(12);
        reviews.add(review1);
        
        // 5-star verified review
        Review review2 = createSampleReview("2", 2L, 5, "Great", true);
        review2.setHelpfulVotes(8);
        review2.setTotalVotes(10);
        reviews.add(review2);
        
        // 4-star unverified review
        Review review3 = createSampleReview("3", 3L, 4, "Good", false);
        review3.setHelpfulVotes(5);
        review3.setTotalVotes(8);
        reviews.add(review3);
        
        // 3-star unverified review
        Review review4 = createSampleReview("4", 4L, 3, "Average", false);
        review4.setHelpfulVotes(2);
        review4.setTotalVotes(5);
        reviews.add(review4);
        
        // 1-star unverified review
        Review review5 = createSampleReview("5", 5L, 1, "Poor", false);
        review5.setHelpfulVotes(1);
        review5.setTotalVotes(3);
        reviews.add(review5);
        
        return reviews;
    }

    private Review createSampleReview(String id, Long userId, Integer rating, String title, Boolean verified) {
        Review review = new Review(TENANT_ID, userId, PRODUCT_ID, rating, title, "Sample comment");
        review.setId(id);
        review.setStatus(ReviewStatus.APPROVED);
        review.setVerified(verified);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return review;
    }
}
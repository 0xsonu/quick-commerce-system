package com.ecommerce.reviewservice.controller;

import com.ecommerce.reviewservice.dto.*;
import com.ecommerce.reviewservice.service.ReviewAggregationService;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewAggregationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.ecommerce.reviewservice.service.ReviewService reviewService;

    @MockBean
    private ReviewAggregationService reviewAggregationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant_1";
    private static final String PRODUCT_ID = "product_123";
    private static final Long USER_ID = 1L;
    private static final String REVIEW_ID = "review_123";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @Test
    void getProductRatingAggregate_ShouldReturnAggregateData() throws Exception {
        // Given
        Map<Integer, Long> ratingDistribution = Map.of(
            1, 1L, 2, 0L, 3, 1L, 4, 2L, 5, 3L
        );
        
        ProductRatingAggregateResponse response = new ProductRatingAggregateResponse(
            PRODUCT_ID,
            BigDecimal.valueOf(4.14),
            7L,
            ratingDistribution,
            3L,
            2L,
            BigDecimal.valueOf(0.75)
        );

        when(reviewAggregationService.getProductRatingAggregate(PRODUCT_ID))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/product/{productId}/aggregate", PRODUCT_ID)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.data.averageRating").value(4.14))
                .andExpect(jsonPath("$.data.totalReviews").value(7))
                .andExpect(jsonPath("$.data.ratingDistribution.5").value(3))
                .andExpect(jsonPath("$.data.ratingDistribution.4").value(2))
                .andExpect(jsonPath("$.data.ratingDistribution.3").value(1))
                .andExpect(jsonPath("$.data.ratingDistribution.2").value(0))
                .andExpect(jsonPath("$.data.ratingDistribution.1").value(1))
                .andExpect(jsonPath("$.data.verifiedReviews").value(3))
                .andExpect(jsonPath("$.data.reviewsWithImages").value(2))
                .andExpect(jsonPath("$.data.helpfulnessScore").value(0.75));
    }

    @Test
    void getReviewSummary_ShouldReturnSummaryData() throws Exception {
        // Given
        ReviewSummaryResponse response = new ReviewSummaryResponse(
            PRODUCT_ID,
            BigDecimal.valueOf(4.20),
            10L,
            4L, 3L, 2L, 1L, 0L,
            5L, 3L,
            LocalDateTime.of(2024, 1, 15, 10, 30),
            BigDecimal.valueOf(0.80)
        );

        when(reviewAggregationService.getReviewSummary(PRODUCT_ID))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/product/{productId}/summary", PRODUCT_ID)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.data.averageRating").value(4.20))
                .andExpect(jsonPath("$.data.totalReviews").value(10))
                .andExpect(jsonPath("$.data.fiveStarReviews").value(4))
                .andExpect(jsonPath("$.data.fourStarReviews").value(3))
                .andExpect(jsonPath("$.data.threeStarReviews").value(2))
                .andExpect(jsonPath("$.data.twoStarReviews").value(1))
                .andExpect(jsonPath("$.data.oneStarReviews").value(0))
                .andExpect(jsonPath("$.data.verifiedReviews").value(5))
                .andExpect(jsonPath("$.data.reviewsWithImages").value(3))
                .andExpect(jsonPath("$.data.overallHelpfulnessScore").value(0.80));
    }

    @Test
    void getFilteredReviews_ShouldReturnFilteredResults() throws Exception {
        // Given
        ReviewFilterRequest filterRequest = new ReviewFilterRequest();
        filterRequest.setRatings(Arrays.asList(4, 5));
        filterRequest.setVerifiedOnly(true);
        filterRequest.setSortBy("rating");
        filterRequest.setSortDirection("desc");

        List<ReviewResponse> reviews = Arrays.asList(
            createSampleReviewResponse("1", 5, "Excellent"),
            createSampleReviewResponse("2", 4, "Good")
        );

        PagedResponse<ReviewResponse> pagedResponse = new PagedResponse<>(
            reviews, 0, 10, 2L, 1, true, true
        );

        when(reviewAggregationService.getFilteredReviews(eq(PRODUCT_ID), any(ReviewFilterRequest.class), eq(0), eq(10)))
            .thenReturn(pagedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/reviews/product/{productId}/filtered", PRODUCT_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filterRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].rating").value(5))
                .andExpect(jsonPath("$.data.content[1].rating").value(4))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void voteOnReview_WithValidRequest_ShouldRecordVote() throws Exception {
        // Given
        VoteReviewRequest voteRequest = new VoteReviewRequest(true);
        ReviewResponse reviewResponse = createSampleReviewResponse(REVIEW_ID, 5, "Great product");

        when(reviewAggregationService.voteOnReview(REVIEW_ID, voteRequest, USER_ID))
            .thenReturn(reviewResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/vote", REVIEW_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(REVIEW_ID))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.message").value("Vote recorded successfully"));
    }

    @Test
    void voteOnReview_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        VoteReviewRequest invalidRequest = new VoteReviewRequest(null); // helpful is required

        // When & Then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/vote", REVIEW_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", USER_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFilteredReviews_WithEmptyFilters_ShouldReturnAllReviews() throws Exception {
        // Given
        ReviewFilterRequest emptyFilterRequest = new ReviewFilterRequest();
        
        List<ReviewResponse> reviews = Arrays.asList(
            createSampleReviewResponse("1", 5, "Excellent"),
            createSampleReviewResponse("2", 3, "Average"),
            createSampleReviewResponse("3", 1, "Poor")
        );

        PagedResponse<ReviewResponse> pagedResponse = new PagedResponse<>(
            reviews, 0, 10, 3L, 1, true, true
        );

        when(reviewAggregationService.getFilteredReviews(eq(PRODUCT_ID), any(ReviewFilterRequest.class), eq(0), eq(10)))
            .thenReturn(pagedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/reviews/product/{productId}/filtered", PRODUCT_ID)
                .header("X-Tenant-ID", TENANT_ID)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyFilterRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    void getProductRatingAggregate_WithNonExistentProduct_ShouldReturnEmptyAggregate() throws Exception {
        // Given
        String nonExistentProductId = "non_existent_product";
        ProductRatingAggregateResponse emptyResponse = new ProductRatingAggregateResponse(
            nonExistentProductId, BigDecimal.ZERO, 0L, new HashMap<>(), 0L, 0L, BigDecimal.ZERO
        );

        when(reviewAggregationService.getProductRatingAggregate(nonExistentProductId))
            .thenReturn(emptyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/product/{productId}/aggregate", nonExistentProductId)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(nonExistentProductId))
                .andExpect(jsonPath("$.data.averageRating").value(0))
                .andExpect(jsonPath("$.data.totalReviews").value(0))
                .andExpect(jsonPath("$.data.verifiedReviews").value(0))
                .andExpect(jsonPath("$.data.reviewsWithImages").value(0));
    }

    private ReviewResponse createSampleReviewResponse(String id, Integer rating, String title) {
        return new ReviewResponse(
            id,
            USER_ID,
            PRODUCT_ID,
            rating,
            title,
            "Sample comment",
            com.ecommerce.reviewservice.entity.ReviewStatus.APPROVED,
            true,
            Collections.emptyList(),
            5,
            8,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
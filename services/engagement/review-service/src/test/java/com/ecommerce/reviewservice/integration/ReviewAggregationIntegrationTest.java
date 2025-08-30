package com.ecommerce.reviewservice.integration;

import com.ecommerce.reviewservice.dto.*;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.entity.ReviewVote;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.ecommerce.reviewservice.repository.ReviewVoteRepository;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
class ReviewAggregationIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewVoteRepository reviewVoteRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant_1";
    private static final String PRODUCT_ID = "product_123";
    private static final Long USER_ID_1 = 1L;
    private static final Long USER_ID_2 = 2L;
    private static final Long USER_ID_3 = 3L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clean up data
        reviewRepository.deleteAll();
        reviewVoteRepository.deleteAll();
        
        // Set tenant context
        TenantContext.setTenantId(TENANT_ID);
        
        // Create sample reviews
        createSampleReviews();
    }

    @Test
    void getProductRatingAggregate_WithRealData_ShouldReturnCorrectAggregate() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/reviews/product/{productId}/aggregate", PRODUCT_ID)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.data.averageRating").value(4.0))
                .andExpect(jsonPath("$.data.totalReviews").value(4))
                .andExpect(jsonPath("$.data.ratingDistribution.5").value(2))
                .andExpect(jsonPath("$.data.ratingDistribution.4").value(1))
                .andExpect(jsonPath("$.data.ratingDistribution.3").value(1))
                .andExpect(jsonPath("$.data.ratingDistribution.2").value(0))
                .andExpect(jsonPath("$.data.ratingDistribution.1").value(0))
                .andExpect(jsonPath("$.data.verifiedReviews").value(2))
                .andExpect(jsonPath("$.data.reviewsWithImages").value(1));
    }

    @Test
    void getReviewSummary_WithRealData_ShouldReturnCorrectSummary() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/reviews/product/{productId}/summary", PRODUCT_ID)
                .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.data.averageRating").value(4.0))
                .andExpect(jsonPath("$.data.totalReviews").value(4))
                .andExpect(jsonPath("$.data.fiveStarReviews").value(2))
                .andExpect(jsonPath("$.data.fourStarReviews").value(1))
                .andExpect(jsonPath("$.data.threeStarReviews").value(1))
                .andExpect(jsonPath("$.data.twoStarReviews").value(0))
                .andExpect(jsonPath("$.data.oneStarReviews").value(0))
                .andExpect(jsonPath("$.data.verifiedReviews").value(2))
                .andExpect(jsonPath("$.data.reviewsWithImages").value(1));
    }

    @Test
    void getFilteredReviews_WithRatingFilter_ShouldReturnFilteredResults() throws Exception {
        // Given
        ReviewFilterRequest filterRequest = new ReviewFilterRequest();
        filterRequest.setRatings(Arrays.asList(5));
        filterRequest.setSortBy("rating");
        filterRequest.setSortDirection("desc");

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
                .andExpect(jsonPath("$.data.content[1].rating").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getFilteredReviews_WithVerifiedOnlyFilter_ShouldReturnOnlyVerifiedReviews() throws Exception {
        // Given
        ReviewFilterRequest filterRequest = new ReviewFilterRequest();
        filterRequest.setVerifiedOnly(true);
        filterRequest.setSortBy("createdAt");
        filterRequest.setSortDirection("desc");

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
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getFilteredReviews_WithImagesOnlyFilter_ShouldReturnOnlyReviewsWithImages() throws Exception {
        // Given
        ReviewFilterRequest filterRequest = new ReviewFilterRequest();
        filterRequest.setWithImagesOnly(true);

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
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void voteOnReview_NewHelpfulVote_ShouldUpdateVoteCounts() throws Exception {
        // Given
        Review review = reviewRepository.findByTenantIdAndProductIdAndStatus(
            TENANT_ID, PRODUCT_ID, ReviewStatus.APPROVED, org.springframework.data.domain.Pageable.unpaged())
            .getContent().get(0);
        
        VoteReviewRequest voteRequest = new VoteReviewRequest(true);
        Long votingUserId = 999L; // Different from review author

        // When & Then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/vote", review.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", votingUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.helpfulVotes").value(6)) // Original 5 + 1
                .andExpect(jsonPath("$.data.totalVotes").value(9)); // Original 8 + 1
    }

    @Test
    void voteOnReview_VoteOnOwnReview_ShouldReturnForbidden() throws Exception {
        // Given
        Review review = reviewRepository.findByTenantIdAndProductIdAndStatus(
            TENANT_ID, PRODUCT_ID, ReviewStatus.APPROVED, org.springframework.data.domain.Pageable.unpaged())
            .getContent().get(0);
        
        VoteReviewRequest voteRequest = new VoteReviewRequest(true);

        // When & Then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/vote", review.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", review.getUserId().toString()) // Same as review author
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void voteOnReview_UpdateExistingVote_ShouldUpdateVoteCounts() throws Exception {
        // Given
        Review review = reviewRepository.findByTenantIdAndProductIdAndStatus(
            TENANT_ID, PRODUCT_ID, ReviewStatus.APPROVED, org.springframework.data.domain.Pageable.unpaged())
            .getContent().get(0);
        
        Long votingUserId = 999L;
        
        // Create existing vote (unhelpful)
        ReviewVote existingVote = new ReviewVote(TENANT_ID, votingUserId, review.getId(), false);
        reviewVoteRepository.save(existingVote);
        
        // Update review counts to reflect existing vote
        review.setTotalVotes(review.getTotalVotes() + 1);
        reviewRepository.save(review);
        
        VoteReviewRequest voteRequest = new VoteReviewRequest(true); // Change to helpful

        // When & Then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/vote", review.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-User-ID", votingUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.helpfulVotes").value(6)) // Original 5 + 1 (changed from unhelpful to helpful)
                .andExpect(jsonPath("$.data.totalVotes").value(9)); // Same total (just changed vote type)
    }

    private void createSampleReviews() {
        // 5-star verified review with images
        Review review1 = new Review(TENANT_ID, USER_ID_1, PRODUCT_ID, 5, "Excellent Product", "Amazing quality!");
        review1.setStatus(ReviewStatus.APPROVED);
        review1.setVerified(true);
        review1.setImageUrls(Arrays.asList("image1.jpg", "image2.jpg"));
        review1.setHelpfulVotes(5);
        review1.setTotalVotes(8);
        review1.setCreatedAt(LocalDateTime.now().minusDays(5));
        reviewRepository.save(review1);

        // 5-star verified review
        Review review2 = new Review(TENANT_ID, USER_ID_2, PRODUCT_ID, 5, "Great Value", "Highly recommend!");
        review2.setStatus(ReviewStatus.APPROVED);
        review2.setVerified(true);
        review2.setHelpfulVotes(8);
        review2.setTotalVotes(10);
        review2.setCreatedAt(LocalDateTime.now().minusDays(3));
        reviewRepository.save(review2);

        // 4-star unverified review
        Review review3 = new Review(TENANT_ID, USER_ID_3, PRODUCT_ID, 4, "Good Product", "Pretty good overall");
        review3.setStatus(ReviewStatus.APPROVED);
        review3.setVerified(false);
        review3.setHelpfulVotes(3);
        review3.setTotalVotes(6);
        review3.setCreatedAt(LocalDateTime.now().minusDays(2));
        reviewRepository.save(review3);

        // 3-star unverified review
        Review review4 = new Review(TENANT_ID, 4L, PRODUCT_ID, 3, "Average", "It's okay");
        review4.setStatus(ReviewStatus.APPROVED);
        review4.setVerified(false);
        review4.setHelpfulVotes(1);
        review4.setTotalVotes(4);
        review4.setCreatedAt(LocalDateTime.now().minusDays(1));
        reviewRepository.save(review4);

        // Pending review (should not be included in aggregations)
        Review pendingReview = new Review(TENANT_ID, 5L, PRODUCT_ID, 1, "Terrible", "Worst product ever");
        pendingReview.setStatus(ReviewStatus.PENDING);
        pendingReview.setVerified(false);
        reviewRepository.save(pendingReview);
    }
}
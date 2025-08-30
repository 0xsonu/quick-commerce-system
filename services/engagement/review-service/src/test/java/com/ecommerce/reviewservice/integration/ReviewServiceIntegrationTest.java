package com.ecommerce.reviewservice.integration;

import com.ecommerce.reviewservice.dto.*;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.exception.DuplicateReviewException;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.ecommerce.reviewservice.service.ReviewService;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ReviewServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    private static final String TENANT_ID = "tenant_1";
    private static final Long USER_ID = 1L;
    private static final String PRODUCT_ID = "product_123";

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID.toString());
    }

    @Test
    void createReview_Success() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");

        // When
        ReviewResponse response = reviewService.createReview(request, USER_ID);

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(PRODUCT_ID, response.getProductId());
        assertEquals(5, response.getRating());
        assertEquals("Great Product", response.getTitle());
        assertEquals("Excellent quality", response.getComment());
        assertEquals(ReviewStatus.PENDING, response.getStatus());
        assertFalse(response.getVerified());

        // Verify in database
        Review savedReview = reviewRepository.findById(response.getId()).orElse(null);
        assertNotNull(savedReview);
        assertEquals(TENANT_ID, savedReview.getTenantId());
    }

    @Test
    void createReview_DuplicateReview_ThrowsException() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        reviewService.createReview(request, USER_ID);

        // When & Then
        assertThrows(DuplicateReviewException.class, () -> {
            reviewService.createReview(request, USER_ID);
        });
    }

    @Test
    void updateReview_Success() {
        // Given
        CreateReviewRequest createRequest = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        ReviewResponse createdReview = reviewService.createReview(createRequest, USER_ID);

        UpdateReviewRequest updateRequest = new UpdateReviewRequest(4, "Updated Title", "Updated comment");

        // When
        ReviewResponse updatedReview = reviewService.updateReview(createdReview.getId(), updateRequest, USER_ID);

        // Then
        assertEquals(4, updatedReview.getRating());
        assertEquals("Updated Title", updatedReview.getTitle());
        assertEquals("Updated comment", updatedReview.getComment());
        assertEquals(ReviewStatus.PENDING, updatedReview.getStatus()); // Should remain pending after update

        // Verify in database
        Review savedReview = reviewRepository.findById(createdReview.getId()).orElse(null);
        assertNotNull(savedReview);
        assertEquals(4, savedReview.getRating());
        assertEquals("Updated Title", savedReview.getTitle());
    }

    @Test
    void getProductReviews_Success() {
        // Given
        CreateReviewRequest request1 = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        CreateReviewRequest request2 = new CreateReviewRequest(PRODUCT_ID, 4, "Good Product", "Good quality");
        
        ReviewResponse review1 = reviewService.createReview(request1, USER_ID);
        ReviewResponse review2 = reviewService.createReview(request2, 2L);

        // Approve reviews for them to appear in product reviews
        reviewService.moderateReview(review1.getId(), new ModerateReviewRequest(ReviewStatus.APPROVED, "Good"), 3L);
        reviewService.moderateReview(review2.getId(), new ModerateReviewRequest(ReviewStatus.APPROVED, "Good"), 3L);

        // When
        PagedResponse<ReviewResponse> response = reviewService.getProductReviews(PRODUCT_ID, 0, 10, "createdAt", "desc");

        // Then
        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        assertEquals(2, response.getContent().size());
        assertTrue(response.getContent().stream().allMatch(r -> r.getStatus() == ReviewStatus.APPROVED));
    }

    @Test
    void moderateReview_Approve_Success() {
        // Given
        CreateReviewRequest createRequest = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        ReviewResponse createdReview = reviewService.createReview(createRequest, USER_ID);

        ModerateReviewRequest moderateRequest = new ModerateReviewRequest(ReviewStatus.APPROVED, "Good review");
        Long moderatorId = 2L;

        // When
        ReviewResponse moderatedReview = reviewService.moderateReview(createdReview.getId(), moderateRequest, moderatorId);

        // Then
        assertEquals(ReviewStatus.APPROVED, moderatedReview.getStatus());

        // Verify in database
        Review savedReview = reviewRepository.findById(createdReview.getId()).orElse(null);
        assertNotNull(savedReview);
        assertEquals(ReviewStatus.APPROVED, savedReview.getStatus());
        assertEquals(moderatorId, savedReview.getModeratedBy());
        assertEquals("Good review", savedReview.getModerationNotes());
        assertNotNull(savedReview.getModeratedAt());
    }

    @Test
    void deleteReview_Success() {
        // Given
        CreateReviewRequest createRequest = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        ReviewResponse createdReview = reviewService.createReview(createRequest, USER_ID);

        // When
        reviewService.deleteReview(createdReview.getId(), USER_ID);

        // Then
        assertFalse(reviewRepository.existsById(createdReview.getId()));
    }

    @Test
    void flagReview_Success() {
        // Given
        CreateReviewRequest createRequest = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        ReviewResponse createdReview = reviewService.createReview(createRequest, USER_ID);

        String reason = "Spam content";

        // When
        reviewService.flagReview(createdReview.getId(), reason);

        // Then
        Review flaggedReview = reviewRepository.findById(createdReview.getId()).orElse(null);
        assertNotNull(flaggedReview);
        assertEquals(ReviewStatus.FLAGGED, flaggedReview.getStatus());
        assertEquals(reason, flaggedReview.getModerationNotes());
    }

    @Test
    void hasUserReviewedProduct_True() {
        // Given
        CreateReviewRequest createRequest = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        reviewService.createReview(createRequest, USER_ID);

        // When
        boolean hasReviewed = reviewService.hasUserReviewedProduct(USER_ID, PRODUCT_ID);

        // Then
        assertTrue(hasReviewed);
    }

    @Test
    void hasUserReviewedProduct_False() {
        // When
        boolean hasReviewed = reviewService.hasUserReviewedProduct(USER_ID, "nonexistent_product");

        // Then
        assertFalse(hasReviewed);
    }

    @Test
    void tenantIsolation_Success() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        ReviewResponse review1 = reviewService.createReview(request, USER_ID);

        // Switch tenant
        TenantContext.setTenantId("tenant_2");
        ReviewResponse review2 = reviewService.createReview(request, USER_ID);

        // When - Switch back to tenant_1
        TenantContext.setTenantId(TENANT_ID);
        PagedResponse<ReviewResponse> tenant1Reviews = reviewService.getUserReviews(USER_ID, 0, 10);

        // Switch to tenant_2
        TenantContext.setTenantId("tenant_2");
        PagedResponse<ReviewResponse> tenant2Reviews = reviewService.getUserReviews(USER_ID, 0, 10);

        // Then
        assertEquals(1, tenant1Reviews.getTotalElements());
        assertEquals(1, tenant2Reviews.getTotalElements());
        assertEquals(review1.getId(), tenant1Reviews.getContent().get(0).getId());
        assertEquals(review2.getId(), tenant2Reviews.getContent().get(0).getId());
    }
}
package com.ecommerce.reviewservice.integration;

import com.ecommerce.reviewservice.dto.CreateReviewRequest;
import com.ecommerce.reviewservice.dto.ModerateReviewRequest;
import com.ecommerce.reviewservice.dto.ReviewResponse;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.ecommerce.reviewservice.service.ReviewEventPublisher;
import com.ecommerce.reviewservice.service.ReviewService;
import com.ecommerce.shared.models.events.ReviewCreatedEvent;
import com.ecommerce.shared.models.events.ReviewFlaggedEvent;
import com.ecommerce.shared.models.events.ReviewModeratedEvent;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class ReviewModerationIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TENANT_ID = "tenant_integration";
    private static final Long USER_ID = 1L;
    private static final Long MODERATOR_ID = 2L;
    private static final String PRODUCT_ID = "product_integration";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID.toString());
        
        // Clean up any existing reviews
        reviewRepository.deleteAll();
        
        // Mock Kafka template
        CompletableFuture<SendResult<String, Object>> mockFuture = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
    }

    @Test
    void completeReviewModerationWorkflow_ShouldPublishCorrectEvents() {
        // Step 1: Create a review
        CreateReviewRequest createRequest = new CreateReviewRequest(
            PRODUCT_ID, 5, "Great Product", "Really loved this product!"
        );
        
        ReviewResponse createdReview = reviewService.createReview(createRequest, USER_ID);
        
        // Verify review was created with PENDING status
        assertNotNull(createdReview);
        assertEquals(ReviewStatus.PENDING, createdReview.getStatus());
        
        // Verify ReviewCreatedEvent was published
        verify(kafkaTemplate).send(eq("review-events"), anyString(), any(ReviewCreatedEvent.class));
        
        // Step 2: Flag the review
        String flagReason = "Suspicious content";
        reviewService.flagReview(createdReview.getId(), flagReason, USER_ID);
        
        // Verify review status changed to FLAGGED
        Review flaggedReview = reviewRepository.findById(createdReview.getId()).orElseThrow();
        assertEquals(ReviewStatus.FLAGGED, flaggedReview.getStatus());
        assertEquals(flagReason, flaggedReview.getModerationNotes());
        
        // Verify ReviewFlaggedEvent was published
        verify(kafkaTemplate).send(eq("review-events"), anyString(), any(ReviewFlaggedEvent.class));
        
        // Step 3: Approve the review after moderation
        ModerateReviewRequest moderateRequest = new ModerateReviewRequest(
            ReviewStatus.APPROVED, "Review is acceptable after investigation"
        );
        
        ReviewResponse moderatedReview = reviewService.moderateReview(
            createdReview.getId(), moderateRequest, MODERATOR_ID
        );
        
        // Verify review was approved
        assertEquals(ReviewStatus.APPROVED, moderatedReview.getStatus());
        
        // Verify ReviewModeratedEvent was published
        verify(kafkaTemplate).send(eq("review-events"), anyString(), any(ReviewModeratedEvent.class));
        
        // Verify final state in database
        Review finalReview = reviewRepository.findById(createdReview.getId()).orElseThrow();
        assertEquals(ReviewStatus.APPROVED, finalReview.getStatus());
        assertEquals(MODERATOR_ID, finalReview.getModeratedBy());
        assertEquals("Review is acceptable after investigation", finalReview.getModerationNotes());
        assertNotNull(finalReview.getModeratedAt());
    }

    @Test
    void rejectReviewWorkflow_ShouldPublishCorrectEvents() {
        // Step 1: Create a review
        CreateReviewRequest createRequest = new CreateReviewRequest(
            PRODUCT_ID, 1, "Terrible Product", "This product is awful and contains inappropriate content!"
        );
        
        ReviewResponse createdReview = reviewService.createReview(createRequest, USER_ID);
        
        // Step 2: Reject the review
        ModerateReviewRequest rejectRequest = new ModerateReviewRequest(
            ReviewStatus.REJECTED, "Contains inappropriate language"
        );
        
        ReviewResponse rejectedReview = reviewService.moderateReview(
            createdReview.getId(), rejectRequest, MODERATOR_ID
        );
        
        // Verify review was rejected
        assertEquals(ReviewStatus.REJECTED, rejectedReview.getStatus());
        
        // Verify events were published
        verify(kafkaTemplate).send(eq("review-events"), anyString(), any(ReviewCreatedEvent.class));
        verify(kafkaTemplate).send(eq("review-events"), anyString(), any(ReviewModeratedEvent.class));
        
        // Verify final state in database
        Review finalReview = reviewRepository.findById(createdReview.getId()).orElseThrow();
        assertEquals(ReviewStatus.REJECTED, finalReview.getStatus());
        assertEquals(MODERATOR_ID, finalReview.getModeratedBy());
        assertEquals("Contains inappropriate language", finalReview.getModerationNotes());
    }

    @Test
    void bulkModerationScenario_ShouldHandleMultipleReviews() {
        // Create multiple reviews
        for (int i = 1; i <= 5; i++) {
            CreateReviewRequest request = new CreateReviewRequest(
                PRODUCT_ID, i, "Review " + i, "Comment for review " + i
            );
            reviewService.createReview(request, USER_ID + i);
        }
        
        // Verify all reviews were created
        assertEquals(5, reviewRepository.count());
        
        // Moderate some reviews
        var reviews = reviewRepository.findAll();
        
        // Approve first 3 reviews
        for (int i = 0; i < 3; i++) {
            ModerateReviewRequest approveRequest = new ModerateReviewRequest(
                ReviewStatus.APPROVED, "Good review"
            );
            reviewService.moderateReview(reviews.get(i).getId(), approveRequest, MODERATOR_ID);
        }
        
        // Reject last 2 reviews
        for (int i = 3; i < 5; i++) {
            ModerateReviewRequest rejectRequest = new ModerateReviewRequest(
                ReviewStatus.REJECTED, "Poor quality review"
            );
            reviewService.moderateReview(reviews.get(i).getId(), rejectRequest, MODERATOR_ID);
        }
        
        // Verify final counts
        assertEquals(3, reviewRepository.countByTenantIdAndStatus(TENANT_ID, ReviewStatus.APPROVED));
        assertEquals(2, reviewRepository.countByTenantIdAndStatus(TENANT_ID, ReviewStatus.REJECTED));
        
        // Verify all events were published (5 created + 5 moderated = 10 total)
        verify(kafkaTemplate, times(10)).send(eq("review-events"), anyString(), any());
    }

    @Test
    void eventPublishingFailure_ShouldNotAffectReviewPersistence() {
        // Given - Kafka publishing fails
        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Kafka is down"));
        
        // When - Create a review (should not fail even if event publishing fails)
        CreateReviewRequest createRequest = new CreateReviewRequest(
            PRODUCT_ID, 5, "Great Product", "Really loved this product!"
        );
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            ReviewResponse createdReview = reviewService.createReview(createRequest, USER_ID);
            assertNotNull(createdReview);
        });
        
        // Verify review was still persisted
        assertEquals(1, reviewRepository.count());
    }
}
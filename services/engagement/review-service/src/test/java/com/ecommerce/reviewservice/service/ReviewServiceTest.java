package com.ecommerce.reviewservice.service;

import com.ecommerce.reviewservice.dto.*;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.exception.DuplicateReviewException;
import com.ecommerce.reviewservice.exception.ReviewNotFoundException;
import com.ecommerce.reviewservice.exception.UnauthorizedReviewAccessException;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewEventPublisher eventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    private static final String TENANT_ID = "tenant_1";
    private static final Long USER_ID = 1L;
    private static final String PRODUCT_ID = "product_123";
    private static final String REVIEW_ID = "review_456";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID.toString());
    }

    @Test
    void createReview_Success() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        
        when(reviewRepository.existsByTenantIdAndUserIdAndProductId(TENANT_ID, USER_ID, PRODUCT_ID))
            .thenReturn(false);
        
        Review savedReview = createSampleReview();
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // When
        ReviewResponse response = reviewService.createReview(request, USER_ID);

        // Then
        assertNotNull(response);
        assertEquals(REVIEW_ID, response.getId());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(PRODUCT_ID, response.getProductId());
        assertEquals(5, response.getRating());
        assertEquals("Great Product", response.getTitle());
        assertEquals(ReviewStatus.PENDING, response.getStatus());
        
        verify(reviewRepository).existsByTenantIdAndUserIdAndProductId(TENANT_ID, USER_ID, PRODUCT_ID);
        verify(reviewRepository).save(any(Review.class));
        verify(eventPublisher).publishReviewCreated(any(Review.class));
    }

    @Test
    void createReview_DuplicateReview_ThrowsException() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        
        when(reviewRepository.existsByTenantIdAndUserIdAndProductId(TENANT_ID, USER_ID, PRODUCT_ID))
            .thenReturn(true);

        // When & Then
        assertThrows(DuplicateReviewException.class, () -> {
            reviewService.createReview(request, USER_ID);
        });
        
        verify(reviewRepository).existsByTenantIdAndUserIdAndProductId(TENANT_ID, USER_ID, PRODUCT_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void updateReview_Success() {
        // Given
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated Title", "Updated comment");
        Review existingReview = createSampleReview();
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(existingReview);

        // When
        ReviewResponse response = reviewService.updateReview(REVIEW_ID, request, USER_ID);

        // Then
        assertNotNull(response);
        assertEquals(4, response.getRating());
        assertEquals("Updated Title", response.getTitle());
        assertEquals("Updated comment", response.getComment());
        
        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository).save(any(Review.class));
        verify(eventPublisher).publishReviewUpdated(any(Review.class), eq(5));
    }

    @Test
    void updateReview_UnauthorizedUser_ThrowsException() {
        // Given
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated Title", "Updated comment");
        Review existingReview = createSampleReview();
        existingReview.setUserId(999L); // Different user
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existingReview));

        // When & Then
        assertThrows(UnauthorizedReviewAccessException.class, () -> {
            reviewService.updateReview(REVIEW_ID, request, USER_ID);
        });
        
        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void updateReview_RejectedReview_ThrowsException() {
        // Given
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated Title", "Updated comment");
        Review existingReview = createSampleReview();
        existingReview.setStatus(ReviewStatus.REJECTED);
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existingReview));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            reviewService.updateReview(REVIEW_ID, request, USER_ID);
        });
        
        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void getReview_Success() {
        // Given
        Review review = createSampleReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // When
        ReviewResponse response = reviewService.getReview(REVIEW_ID);

        // Then
        assertNotNull(response);
        assertEquals(REVIEW_ID, response.getId());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(PRODUCT_ID, response.getProductId());
        
        verify(reviewRepository).findById(REVIEW_ID);
    }

    @Test
    void getReview_NotFound_ThrowsException() {
        // Given
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ReviewNotFoundException.class, () -> {
            reviewService.getReview(REVIEW_ID);
        });
        
        verify(reviewRepository).findById(REVIEW_ID);
    }

    @Test
    void getProductReviews_Success() {
        // Given
        List<Review> reviews = Arrays.asList(createSampleReview(), createSampleReview());
        Page<Review> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 10), 2);
        
        when(reviewRepository.findByTenantIdAndProductIdAndStatus(
            eq(TENANT_ID), eq(PRODUCT_ID), eq(ReviewStatus.APPROVED), any(Pageable.class)))
            .thenReturn(reviewPage);

        // When
        PagedResponse<ReviewResponse> response = reviewService.getProductReviews(PRODUCT_ID, 0, 10, "createdAt", "desc");

        // Then
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(2, response.getTotalElements());
        
        verify(reviewRepository).findByTenantIdAndProductIdAndStatus(
            eq(TENANT_ID), eq(PRODUCT_ID), eq(ReviewStatus.APPROVED), any(Pageable.class));
    }

    @Test
    void getUserReviews_Success() {
        // Given
        List<Review> reviews = Arrays.asList(createSampleReview());
        Page<Review> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 10), 1);
        
        when(reviewRepository.findByTenantIdAndUserId(eq(TENANT_ID), eq(USER_ID), any(Pageable.class)))
            .thenReturn(reviewPage);

        // When
        PagedResponse<ReviewResponse> response = reviewService.getUserReviews(USER_ID, 0, 10);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1, response.getTotalElements());
        
        verify(reviewRepository).findByTenantIdAndUserId(eq(TENANT_ID), eq(USER_ID), any(Pageable.class));
    }

    @Test
    void moderateReview_Approve_Success() {
        // Given
        ModerateReviewRequest request = new ModerateReviewRequest(ReviewStatus.APPROVED, "Good review");
        Review review = createSampleReview();
        Long moderatorId = 2L;
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // When
        ReviewResponse response = reviewService.moderateReview(REVIEW_ID, request, moderatorId);

        // Then
        assertNotNull(response);
        assertEquals(ReviewStatus.APPROVED, response.getStatus());
        
        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository).save(any(Review.class));
        verify(eventPublisher).publishReviewModerated(any(Review.class));
    }

    @Test
    void moderateReview_Reject_Success() {
        // Given
        ModerateReviewRequest request = new ModerateReviewRequest(ReviewStatus.REJECTED, "Inappropriate content");
        Review review = createSampleReview();
        Long moderatorId = 2L;
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // When
        ReviewResponse response = reviewService.moderateReview(REVIEW_ID, request, moderatorId);

        // Then
        assertNotNull(response);
        assertEquals(ReviewStatus.REJECTED, response.getStatus());
        
        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository).save(any(Review.class));
        verify(eventPublisher).publishReviewModerated(any(Review.class));
    }

    @Test
    void deleteReview_Success() {
        // Given
        Review review = createSampleReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // When
        reviewService.deleteReview(REVIEW_ID, USER_ID);

        // Then
        verify(reviewRepository).findById(REVIEW_ID);
        verify(eventPublisher).publishReviewDeleted(any(Review.class), eq("User requested deletion"));
        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_UnauthorizedUser_ThrowsException() {
        // Given
        Review review = createSampleReview();
        review.setUserId(999L); // Different user
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        // When & Then
        assertThrows(UnauthorizedReviewAccessException.class, () -> {
            reviewService.deleteReview(REVIEW_ID, USER_ID);
        });
        
        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void flagReview_Success() {
        // Given
        Review review = createSampleReview();
        String reason = "Spam content";
        
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // When
        reviewService.flagReview(REVIEW_ID, reason);

        // Then
        verify(reviewRepository).findById(REVIEW_ID);
        verify(reviewRepository).save(any(Review.class));
        verify(eventPublisher).publishReviewFlagged(any(Review.class), eq(null));
    }

    @Test
    void hasUserReviewedProduct_True() {
        // Given
        when(reviewRepository.existsByTenantIdAndUserIdAndProductId(TENANT_ID, USER_ID, PRODUCT_ID))
            .thenReturn(true);

        // When
        boolean result = reviewService.hasUserReviewedProduct(USER_ID, PRODUCT_ID);

        // Then
        assertTrue(result);
        verify(reviewRepository).existsByTenantIdAndUserIdAndProductId(TENANT_ID, USER_ID, PRODUCT_ID);
    }

    @Test
    void hasUserReviewedProduct_False() {
        // Given
        when(reviewRepository.existsByTenantIdAndUserIdAndProductId(TENANT_ID, USER_ID, PRODUCT_ID))
            .thenReturn(false);

        // When
        boolean result = reviewService.hasUserReviewedProduct(USER_ID, PRODUCT_ID);

        // Then
        assertFalse(result);
        verify(reviewRepository).existsByTenantIdAndUserIdAndProductId(TENANT_ID, USER_ID, PRODUCT_ID);
    }

    private Review createSampleReview() {
        Review review = new Review(TENANT_ID, USER_ID, PRODUCT_ID, 5, "Great Product", "Excellent quality");
        review.setId(REVIEW_ID);
        review.setStatus(ReviewStatus.PENDING);
        review.setVerified(false);
        review.setHelpfulVotes(0);
        review.setTotalVotes(0);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return review;
    }
}
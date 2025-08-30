package com.ecommerce.reviewservice.controller;

import com.ecommerce.reviewservice.dto.*;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.exception.DuplicateReviewException;
import com.ecommerce.reviewservice.exception.ReviewNotFoundException;
import com.ecommerce.reviewservice.exception.UnauthorizedReviewAccessException;
import com.ecommerce.reviewservice.service.ReviewService;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

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
    void createReview_Success() throws Exception {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        ReviewResponse response = createSampleReviewResponse();
        
        when(reviewService.createReview(any(CreateReviewRequest.class), eq(USER_ID)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/reviews")
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(REVIEW_ID))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.title").value("Great Product"));

        verify(reviewService).createReview(any(CreateReviewRequest.class), eq(USER_ID));
    }

    @Test
    void createReview_DuplicateReview_ReturnsBadRequest() throws Exception {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great Product", "Excellent quality");
        
        when(reviewService.createReview(any(CreateReviewRequest.class), eq(USER_ID)))
            .thenThrow(new DuplicateReviewException("User already has a review for this product"));

        // When & Then
        mockMvc.perform(post("/api/v1/reviews")
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService).createReview(any(CreateReviewRequest.class), eq(USER_ID));
    }

    @Test
    void createReview_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        CreateReviewRequest request = new CreateReviewRequest("", 6, "", ""); // Invalid data

        // When & Then
        mockMvc.perform(post("/api/v1/reviews")
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(CreateReviewRequest.class), any(Long.class));
    }

    @Test
    void updateReview_Success() throws Exception {
        // Given
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated Title", "Updated comment");
        ReviewResponse response = createSampleReviewResponse();
        response.setRating(4);
        response.setTitle("Updated Title");
        
        when(reviewService.updateReview(eq(REVIEW_ID), any(UpdateReviewRequest.class), eq(USER_ID)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/reviews/{reviewId}", REVIEW_ID)
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.title").value("Updated Title"));

        verify(reviewService).updateReview(eq(REVIEW_ID), any(UpdateReviewRequest.class), eq(USER_ID));
    }

    @Test
    void updateReview_UnauthorizedAccess_ReturnsForbidden() throws Exception {
        // Given
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated Title", "Updated comment");
        
        when(reviewService.updateReview(eq(REVIEW_ID), any(UpdateReviewRequest.class), eq(USER_ID)))
            .thenThrow(new UnauthorizedReviewAccessException("User can only update their own reviews"));

        // When & Then
        mockMvc.perform(put("/api/v1/reviews/{reviewId}", REVIEW_ID)
                .header("X-User-ID", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(reviewService).updateReview(eq(REVIEW_ID), any(UpdateReviewRequest.class), eq(USER_ID));
    }

    @Test
    void getReview_Success() throws Exception {
        // Given
        ReviewResponse response = createSampleReviewResponse();
        
        when(reviewService.getReview(REVIEW_ID)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/{reviewId}", REVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(REVIEW_ID))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID));

        verify(reviewService).getReview(REVIEW_ID);
    }

    @Test
    void getReview_NotFound_ReturnsNotFound() throws Exception {
        // Given
        when(reviewService.getReview(REVIEW_ID))
            .thenThrow(new ReviewNotFoundException("Review not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/{reviewId}", REVIEW_ID))
                .andExpect(status().isNotFound());

        verify(reviewService).getReview(REVIEW_ID);
    }

    @Test
    void getProductReviews_Success() throws Exception {
        // Given
        PagedResponse<ReviewResponse> response = new PagedResponse<>(
            Arrays.asList(createSampleReviewResponse()), 0, 10, 1, 1, true, true);
        
        when(reviewService.getProductReviews(eq(PRODUCT_ID), eq(0), eq(10), eq("createdAt"), eq("desc")))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/product/{productId}", PRODUCT_ID)
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(REVIEW_ID))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(reviewService).getProductReviews(eq(PRODUCT_ID), eq(0), eq(10), eq("createdAt"), eq("desc"));
    }

    @Test
    void getUserReviews_Success() throws Exception {
        // Given
        PagedResponse<ReviewResponse> response = new PagedResponse<>(
            Arrays.asList(createSampleReviewResponse()), 0, 10, 1, 1, true, true);
        
        when(reviewService.getUserReviews(eq(USER_ID), eq(0), eq(10)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/user/{userId}", USER_ID)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(reviewService).getUserReviews(eq(USER_ID), eq(0), eq(10));
    }

    @Test
    void moderateReview_Success() throws Exception {
        // Given
        ModerateReviewRequest request = new ModerateReviewRequest(ReviewStatus.APPROVED, "Good review");
        ReviewResponse response = createSampleReviewResponse();
        response.setStatus(ReviewStatus.APPROVED);
        Long moderatorId = 2L;
        
        when(reviewService.moderateReview(eq(REVIEW_ID), any(ModerateReviewRequest.class), eq(moderatorId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/reviews/{reviewId}/moderate", REVIEW_ID)
                .header("X-User-ID", moderatorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(reviewService).moderateReview(eq(REVIEW_ID), any(ModerateReviewRequest.class), eq(moderatorId));
    }

    @Test
    void deleteReview_Success() throws Exception {
        // Given
        doNothing().when(reviewService).deleteReview(REVIEW_ID, USER_ID);

        // When & Then
        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID)
                .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(reviewService).deleteReview(REVIEW_ID, USER_ID);
    }

    @Test
    void flagReview_Success() throws Exception {
        // Given
        String reason = "Spam content";
        doNothing().when(reviewService).flagReview(REVIEW_ID, reason);

        // When & Then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/flag", REVIEW_ID)
                .param("reason", reason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(reviewService).flagReview(REVIEW_ID, reason);
    }

    @Test
    void hasUserReviewedProduct_True() throws Exception {
        // Given
        when(reviewService.hasUserReviewedProduct(USER_ID, PRODUCT_ID)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/user/{userId}/product/{productId}/exists", USER_ID, PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        verify(reviewService).hasUserReviewedProduct(USER_ID, PRODUCT_ID);
    }

    @Test
    void hasUserReviewedProduct_False() throws Exception {
        // Given
        when(reviewService.hasUserReviewedProduct(USER_ID, PRODUCT_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/user/{userId}/product/{productId}/exists", USER_ID, PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));

        verify(reviewService).hasUserReviewedProduct(USER_ID, PRODUCT_ID);
    }

    private ReviewResponse createSampleReviewResponse() {
        return new ReviewResponse(
            REVIEW_ID,
            USER_ID,
            PRODUCT_ID,
            5,
            "Great Product",
            "Excellent quality",
            ReviewStatus.PENDING,
            false,
            Collections.emptyList(),
            0,
            0,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
package com.ecommerce.reviewservice.controller;

import com.ecommerce.reviewservice.dto.*;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.service.ReviewService;
import com.ecommerce.shared.utils.response.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader("X-User-ID") Long userId) {
        
        logger.info("Creating review for user {} and product {}", userId, request.getProductId());
        
        ReviewResponse response = reviewService.createReview(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Review created successfully"));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            @RequestHeader("X-User-ID") Long userId) {
        
        logger.info("Updating review {} for user {}", reviewId, userId);
        
        ReviewResponse response = reviewService.updateReview(reviewId, request, userId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Review updated successfully"));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(@PathVariable String reviewId) {
        
        logger.info("Getting review {}", reviewId);
        
        ReviewResponse response = reviewService.getReview(reviewId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Review retrieved successfully"));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        logger.info("Getting reviews for product {} - page: {}, size: {}", productId, page, size);
        
        PagedResponse<ReviewResponse> response = reviewService.getProductReviews(
            productId, page, size, sortBy, sortDirection);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Product reviews retrieved successfully"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getUserReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("Getting reviews for user {} - page: {}, size: {}", userId, page, size);
        
        PagedResponse<ReviewResponse> response = reviewService.getUserReviews(userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(response, "User reviews retrieved successfully"));
    }

    @GetMapping("/moderation")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getReviewsForModeration(
            @RequestParam(defaultValue = "PENDING") ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("Getting reviews for moderation with status {} - page: {}, size: {}", status, page, size);
        
        PagedResponse<ReviewResponse> response = reviewService.getReviewsForModeration(status, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Reviews for moderation retrieved successfully"));
    }

    @PutMapping("/{reviewId}/moderate")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ModerateReviewRequest request,
            @RequestHeader("X-User-ID") Long moderatorId) {
        
        logger.info("Moderating review {} by moderator {}", reviewId, moderatorId);
        
        ReviewResponse response = reviewService.moderateReview(reviewId, request, moderatorId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Review moderated successfully"));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable String reviewId,
            @RequestHeader("X-User-ID") Long userId) {
        
        logger.info("Deleting review {} for user {}", reviewId, userId);
        
        reviewService.deleteReview(reviewId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
    }

    @PostMapping("/{reviewId}/flag")
    public ResponseEntity<ApiResponse<Void>> flagReview(
            @PathVariable String reviewId,
            @RequestParam String reason,
            @RequestHeader(value = "X-User-ID", required = false) Long flaggedBy) {
        
        logger.info("Flagging review {} for reason: {} by user {}", reviewId, reason, flaggedBy);
        
        reviewService.flagReview(reviewId, reason, flaggedBy);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Review flagged successfully"));
    }

    @GetMapping("/user/{userId}/product/{productId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> hasUserReviewedProduct(
            @PathVariable Long userId,
            @PathVariable String productId) {
        
        logger.info("Checking if user {} has reviewed product {}", userId, productId);
        
        boolean hasReviewed = reviewService.hasUserReviewedProduct(userId, productId);
        
        return ResponseEntity.ok(ApiResponse.success(hasReviewed, "Review existence check completed"));
    }
}
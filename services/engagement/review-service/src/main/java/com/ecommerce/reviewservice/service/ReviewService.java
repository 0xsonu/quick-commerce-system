package com.ecommerce.reviewservice.service;

import com.ecommerce.reviewservice.dto.*;
import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.reviewservice.exception.DuplicateReviewException;
import com.ecommerce.reviewservice.exception.ReviewNotFoundException;
import com.ecommerce.reviewservice.exception.UnauthorizedReviewAccessException;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ReviewResponse createReview(CreateReviewRequest request, Long userId) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Creating review for user {} and product {} in tenant {}", userId, request.getProductId(), tenantId);

        // Check if user already has a review for this product
        if (reviewRepository.existsByTenantIdAndUserIdAndProductId(tenantId, userId, request.getProductId())) {
            throw new DuplicateReviewException("User already has a review for this product");
        }

        Review review = new Review(tenantId, userId, request.getProductId(), 
                                 request.getRating(), request.getTitle(), request.getComment());
        review.setImageUrls(request.getImageUrls());

        Review savedReview = reviewRepository.save(review);
        
        logger.info("Review created with ID: {}", savedReview.getId());
        
        return mapToResponse(savedReview);
    }

    public ReviewResponse updateReview(String reviewId, UpdateReviewRequest request, Long userId) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Updating review {} for user {} in tenant {}", reviewId, userId, tenantId);

        Review review = findReviewByIdAndTenant(reviewId, tenantId);
        
        // Check if user owns this review
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewAccessException("User can only update their own reviews");
        }

        // Only allow updates if review is pending or approved
        if (review.getStatus() == ReviewStatus.REJECTED) {
            throw new IllegalStateException("Cannot update rejected reviews");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setImageUrls(request.getImageUrls());
        
        // Reset status to pending if it was approved (needs re-moderation)
        if (review.getStatus() == ReviewStatus.APPROVED) {
            review.setStatus(ReviewStatus.PENDING);
        }

        Review savedReview = reviewRepository.save(review);
        
        logger.info("Review updated: {}", savedReview.getId());
        
        return mapToResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(String reviewId) {
        String tenantId = TenantContext.getTenantId();
        
        Review review = findReviewByIdAndTenant(reviewId, tenantId);
        return mapToResponse(review);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(String productId, int page, int size, String sortBy, String sortDirection) {
        String tenantId = TenantContext.getTenantId();
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Review> reviewPage = reviewRepository.findByTenantIdAndProductIdAndStatus(
            tenantId, productId, ReviewStatus.APPROVED, pageable);
        
        return mapToPagedResponse(reviewPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getUserReviews(Long userId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByTenantIdAndUserId(tenantId, userId, pageable);
        
        return mapToPagedResponse(reviewPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getReviewsForModeration(ReviewStatus status, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        
        return mapToPagedResponse(reviewPage);
    }

    public ReviewResponse moderateReview(String reviewId, ModerateReviewRequest request, Long moderatorId) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Moderating review {} by moderator {} in tenant {}", reviewId, moderatorId, tenantId);

        Review review = findReviewByIdAndTenant(reviewId, tenantId);
        
        if (request.getStatus() == ReviewStatus.APPROVED) {
            review.approve(moderatorId, request.getModerationNotes());
        } else if (request.getStatus() == ReviewStatus.REJECTED) {
            review.reject(moderatorId, request.getModerationNotes());
        } else {
            throw new IllegalArgumentException("Invalid moderation status: " + request.getStatus());
        }

        Review savedReview = reviewRepository.save(review);
        
        logger.info("Review {} moderated with status: {}", reviewId, request.getStatus());
        
        return mapToResponse(savedReview);
    }

    public void deleteReview(String reviewId, Long userId) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Deleting review {} for user {} in tenant {}", reviewId, userId, tenantId);

        Review review = findReviewByIdAndTenant(reviewId, tenantId);
        
        // Check if user owns this review
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewAccessException("User can only delete their own reviews");
        }

        reviewRepository.delete(review);
        
        logger.info("Review deleted: {}", reviewId);
    }

    public void flagReview(String reviewId, String reason) {
        String tenantId = TenantContext.getTenantId();
        
        logger.info("Flagging review {} in tenant {} for reason: {}", reviewId, tenantId, reason);

        Review review = findReviewByIdAndTenant(reviewId, tenantId);
        review.flag(reason);
        
        reviewRepository.save(review);
        
        logger.info("Review flagged: {}", reviewId);
    }

    @Transactional(readOnly = true)
    public boolean hasUserReviewedProduct(Long userId, String productId) {
        String tenantId = TenantContext.getTenantId();
        return reviewRepository.existsByTenantIdAndUserIdAndProductId(tenantId, userId, productId);
    }

    private Review findReviewByIdAndTenant(String reviewId, String tenantId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        
        if (reviewOpt.isEmpty()) {
            throw new ReviewNotFoundException("Review not found: " + reviewId);
        }
        
        Review review = reviewOpt.get();
        if (!review.getTenantId().equals(tenantId)) {
            throw new ReviewNotFoundException("Review not found: " + reviewId);
        }
        
        return review;
    }

    private ReviewResponse mapToResponse(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getUserId(),
            review.getProductId(),
            review.getRating(),
            review.getTitle(),
            review.getComment(),
            review.getStatus(),
            review.getVerified(),
            review.getImageUrls(),
            review.getHelpfulVotes(),
            review.getTotalVotes(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }

    private PagedResponse<ReviewResponse> mapToPagedResponse(Page<Review> reviewPage) {
        List<ReviewResponse> content = reviewPage.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return new PagedResponse<>(
            content,
            reviewPage.getNumber(),
            reviewPage.getSize(),
            reviewPage.getTotalElements(),
            reviewPage.getTotalPages(),
            reviewPage.isFirst(),
            reviewPage.isLast()
        );
    }
}
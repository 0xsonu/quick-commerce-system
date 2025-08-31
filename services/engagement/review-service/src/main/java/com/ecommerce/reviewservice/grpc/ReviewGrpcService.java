package com.ecommerce.reviewservice.grpc;

import com.ecommerce.reviewservice.dto.ReviewResponse;
import com.ecommerce.reviewservice.dto.ProductRatingAggregateResponse;
import com.ecommerce.reviewservice.service.ReviewService;
import com.ecommerce.reviewservice.service.ReviewAggregationService;
import com.ecommerce.reviewservice.proto.ReviewServiceGrpc;
import com.ecommerce.reviewservice.proto.ReviewServiceProtos.*;
import com.ecommerce.shared.grpc.TenantContextInterceptor;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Map;

@GrpcService(interceptors = {TenantContextInterceptor.class})
public class ReviewGrpcService extends ReviewServiceGrpc.ReviewServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ReviewGrpcService.class);

    private final ReviewService reviewService;
    private final ReviewAggregationService aggregationService;

    @Autowired
    public ReviewGrpcService(ReviewService reviewService, ReviewAggregationService aggregationService) {
        this.reviewService = reviewService;
        this.aggregationService = aggregationService;
    }

    @Override
    public void getReview(com.ecommerce.reviewservice.proto.ReviewServiceProtos.GetReviewRequest request, 
                         StreamObserver<GetReviewResponse> responseObserver) {
        try {
            logger.debug("gRPC GetReview request for review: {} in tenant: {}", 
                        request.getReviewId(), TenantContext.getTenantId());

            ReviewResponse review = reviewService.getReviewById(request.getReviewId());
            
            GetReviewResponse response = GetReviewResponse.newBuilder()
                .setReview(convertToProtoReview(review))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting review: {}", request.getReviewId(), e);
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("Review not found: " + request.getReviewId())
                .asRuntimeException());
        }
    }

    @Override
    public void getProductReviews(GetProductReviewsRequest request, 
                                 StreamObserver<GetProductReviewsResponse> responseObserver) {
        try {
            logger.debug("gRPC GetProductReviews request for product: {} in tenant: {}", 
                        request.getProductId(), TenantContext.getTenantId());

            Pageable pageable = PageRequest.of(
                request.getPageRequest().getPage(),
                request.getPageRequest().getSize()
            );

            Page<ReviewResponse> reviewsPage = reviewService.getProductReviews(
                request.getProductId(), 
                request.getMinRating() > 0 ? request.getMinRating() : null,
                request.getMaxRating() > 0 ? request.getMaxRating() : null,
                pageable
            );
            
            GetProductReviewsResponse.Builder responseBuilder = GetProductReviewsResponse.newBuilder();
            
            // Add reviews
            for (ReviewResponse review : reviewsPage.getContent()) {
                responseBuilder.addReviews(convertToProtoReview(review));
            }
            
            // Add page response
            responseBuilder.setPageResponse(CommonProtos.PageResponse.newBuilder()
                .setPage(reviewsPage.getNumber())
                .setSize(reviewsPage.getSize())
                .setTotalElements(reviewsPage.getTotalElements())
                .setTotalPages(reviewsPage.getTotalPages())
                .build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting product reviews: {}", request.getProductId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void getUserReviews(GetUserReviewsRequest request, 
                              StreamObserver<GetUserReviewsResponse> responseObserver) {
        try {
            logger.debug("gRPC GetUserReviews request for user: {} in tenant: {}", 
                        request.getUserId(), TenantContext.getTenantId());

            Pageable pageable = PageRequest.of(
                request.getPageRequest().getPage(),
                request.getPageRequest().getSize()
            );

            Page<ReviewResponse> reviewsPage = reviewService.getUserReviews(request.getUserId(), pageable);
            
            GetUserReviewsResponse.Builder responseBuilder = GetUserReviewsResponse.newBuilder();
            
            // Add reviews
            for (ReviewResponse review : reviewsPage.getContent()) {
                responseBuilder.addReviews(convertToProtoReview(review));
            }
            
            // Add page response
            responseBuilder.setPageResponse(CommonProtos.PageResponse.newBuilder()
                .setPage(reviewsPage.getNumber())
                .setSize(reviewsPage.getSize())
                .setTotalElements(reviewsPage.getTotalElements())
                .setTotalPages(reviewsPage.getTotalPages())
                .build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting user reviews: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void getProductRatingAggregate(GetProductRatingAggregateRequest request, 
                                         StreamObserver<GetProductRatingAggregateResponse> responseObserver) {
        try {
            logger.debug("gRPC GetProductRatingAggregate request for product: {} in tenant: {}", 
                        request.getProductId(), TenantContext.getTenantId());

            ProductRatingAggregateResponse aggregate = aggregationService.getProductRatingAggregate(request.getProductId());
            
            GetProductRatingAggregateResponse.Builder responseBuilder = GetProductRatingAggregateResponse.newBuilder()
                .setProductId(aggregate.getProductId())
                .setAverageRating(aggregate.getAverageRating())
                .setTotalReviews(aggregate.getTotalReviews());

            // Add rating counts
            for (Map.Entry<Integer, Long> entry : aggregate.getRatingCounts().entrySet()) {
                responseBuilder.addRatingCounts(RatingCount.newBuilder()
                    .setRating(entry.getKey())
                    .setCount(entry.getValue())
                    .build());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting product rating aggregate: {}", request.getProductId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void hasUserReviewedProduct(HasUserReviewedProductRequest request, 
                                      StreamObserver<HasUserReviewedProductResponse> responseObserver) {
        try {
            logger.debug("gRPC HasUserReviewedProduct request for user: {} product: {} in tenant: {}", 
                        request.getUserId(), request.getProductId(), TenantContext.getTenantId());

            ReviewResponse existingReview = reviewService.getUserProductReview(request.getUserId(), request.getProductId());
            
            HasUserReviewedProductResponse response = HasUserReviewedProductResponse.newBuilder()
                .setHasReviewed(existingReview != null)
                .setReviewId(existingReview != null ? existingReview.getId() : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error checking user review: user {} product {}", request.getUserId(), request.getProductId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    private Review convertToProtoReview(ReviewResponse review) {
        return Review.newBuilder()
            .setId(review.getId())
            .setProductId(review.getProductId())
            .setUserId(review.getUserId())
            .setUserName(review.getUserName() != null ? review.getUserName() : "")
            .setRating(review.getRating())
            .setTitle(review.getTitle() != null ? review.getTitle() : "")
            .setComment(review.getComment() != null ? review.getComment() : "")
            .setStatus(review.getStatus().name())
            .setVerifiedPurchase(review.isVerifiedPurchase())
            .setHelpfulVotes(review.getHelpfulVotes())
            .setTotalVotes(review.getTotalVotes())
            .setCreatedAt(review.getCreatedAt().getEpochSecond())
            .setUpdatedAt(review.getUpdatedAt().getEpochSecond())
            .build();
    }
}
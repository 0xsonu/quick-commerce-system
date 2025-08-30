package com.ecommerce.reviewservice.repository;

import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class ReviewRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ReviewRepository reviewRepository;

    private static final String TENANT_ID = "tenant_1";
    private static final Long USER_ID = 1L;
    private static final String PRODUCT_ID = "product_123";

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
    }

    @Test
    void save_Success() {
        // Given
        Review review = createSampleReview();

        // When
        Review savedReview = reviewRepository.save(review);

        // Then
        assertNotNull(savedReview.getId());
        assertEquals(TENANT_ID, savedReview.getTenantId());
        assertEquals(USER_ID, savedReview.getUserId());
        assertEquals(PRODUCT_ID, savedReview.getProductId());
        assertEquals(5, savedReview.getRating());
        assertEquals("Great Product", savedReview.getTitle());
    }

    @Test
    void findByTenantIdAndProductIdAndStatus_Success() {
        // Given
        Review review1 = createSampleReview();
        review1.setStatus(ReviewStatus.APPROVED);
        Review review2 = createSampleReview();
        review2.setUserId(2L);
        review2.setStatus(ReviewStatus.APPROVED);
        Review review3 = createSampleReview();
        review3.setUserId(3L);
        review3.setStatus(ReviewStatus.PENDING);

        reviewRepository.saveAll(Arrays.asList(review1, review2, review3));

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByTenantIdAndProductIdAndStatus(
            TENANT_ID, PRODUCT_ID, ReviewStatus.APPROVED, pageable);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(r -> r.getStatus() == ReviewStatus.APPROVED));
    }

    @Test
    void findByTenantIdAndUserId_Success() {
        // Given
        Review review1 = createSampleReview();
        Review review2 = createSampleReview();
        review2.setProductId("product_456");
        Review review3 = createSampleReview();
        review3.setUserId(2L);

        reviewRepository.saveAll(Arrays.asList(review1, review2, review3));

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID, pageable);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(r -> r.getUserId().equals(USER_ID)));
    }

    @Test
    void findByTenantIdAndUserIdAndProductId_Success() {
        // Given
        Review review = createSampleReview();
        reviewRepository.save(review);

        // When
        Optional<Review> result = reviewRepository.findByTenantIdAndUserIdAndProductId(
            TENANT_ID, USER_ID, PRODUCT_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(USER_ID, result.get().getUserId());
        assertEquals(PRODUCT_ID, result.get().getProductId());
    }

    @Test
    void existsByTenantIdAndUserIdAndProductId_True() {
        // Given
        Review review = createSampleReview();
        reviewRepository.save(review);

        // When
        boolean exists = reviewRepository.existsByTenantIdAndUserIdAndProductId(
            TENANT_ID, USER_ID, PRODUCT_ID);

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByTenantIdAndUserIdAndProductId_False() {
        // When
        boolean exists = reviewRepository.existsByTenantIdAndUserIdAndProductId(
            TENANT_ID, USER_ID, "nonexistent_product");

        // Then
        assertFalse(exists);
    }

    @Test
    void findByTenantIdAndStatus_Success() {
        // Given
        Review review1 = createSampleReview();
        review1.setStatus(ReviewStatus.PENDING);
        Review review2 = createSampleReview();
        review2.setUserId(2L);
        review2.setStatus(ReviewStatus.PENDING);
        Review review3 = createSampleReview();
        review3.setUserId(3L);
        review3.setStatus(ReviewStatus.APPROVED);

        reviewRepository.saveAll(Arrays.asList(review1, review2, review3));

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByTenantIdAndStatus(
            TENANT_ID, ReviewStatus.PENDING, pageable);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(r -> r.getStatus() == ReviewStatus.PENDING));
    }

    @Test
    void countByTenantIdAndProductIdAndStatus_Success() {
        // Given
        Review review1 = createSampleReview();
        review1.setStatus(ReviewStatus.APPROVED);
        Review review2 = createSampleReview();
        review2.setUserId(2L);
        review2.setStatus(ReviewStatus.APPROVED);
        Review review3 = createSampleReview();
        review3.setUserId(3L);
        review3.setStatus(ReviewStatus.PENDING);

        reviewRepository.saveAll(Arrays.asList(review1, review2, review3));

        // When
        long count = reviewRepository.countByTenantIdAndProductIdAndStatus(
            TENANT_ID, PRODUCT_ID, ReviewStatus.APPROVED);

        // Then
        assertEquals(2, count);
    }

    @Test
    void findRatingsByTenantIdAndProductId_Success() {
        // Given
        Review review1 = createSampleReview();
        review1.setRating(5);
        review1.setStatus(ReviewStatus.APPROVED);
        Review review2 = createSampleReview();
        review2.setUserId(2L);
        review2.setRating(4);
        review2.setStatus(ReviewStatus.APPROVED);
        Review review3 = createSampleReview();
        review3.setUserId(3L);
        review3.setRating(3);
        review3.setStatus(ReviewStatus.PENDING); // Should not be included

        reviewRepository.saveAll(Arrays.asList(review1, review2, review3));

        // When
        List<Review> ratings = reviewRepository.findRatingsByTenantIdAndProductId(TENANT_ID, PRODUCT_ID);

        // Then
        assertEquals(2, ratings.size());
        // Note: The query only returns rating field, so we can only check the count and rating values
        assertTrue(ratings.stream().allMatch(r -> r.getRating() != null));
        assertTrue(ratings.stream().anyMatch(r -> r.getRating() == 5));
        assertTrue(ratings.stream().anyMatch(r -> r.getRating() == 4));
    }

    @Test
    void findByTenantIdAndProductIdAndStatusAndVerified_Success() {
        // Given
        Review review1 = createSampleReview();
        review1.setStatus(ReviewStatus.APPROVED);
        review1.setVerified(true);
        Review review2 = createSampleReview();
        review2.setUserId(2L);
        review2.setStatus(ReviewStatus.APPROVED);
        review2.setVerified(false);

        reviewRepository.saveAll(Arrays.asList(review1, review2));

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> result = reviewRepository.findByTenantIdAndProductIdAndStatusAndVerified(
            TENANT_ID, PRODUCT_ID, ReviewStatus.APPROVED, true, pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getVerified());
    }

    @Test
    void uniqueConstraint_PreventsDuplicateReviews() {
        // Given
        Review review1 = createSampleReview();
        Review review2 = createSampleReview(); // Same tenant, user, and product

        reviewRepository.save(review1);

        // When & Then
        assertThrows(Exception.class, () -> {
            reviewRepository.save(review2);
        });
    }

    @Test
    void tenantIsolation_OnlyReturnsTenantData() {
        // Given
        Review review1 = createSampleReview();
        review1.setTenantId("tenant_1");
        Review review2 = createSampleReview();
        review2.setTenantId("tenant_2");
        review2.setUserId(2L);

        reviewRepository.saveAll(Arrays.asList(review1, review2));

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Review> tenant1Reviews = reviewRepository.findByTenantIdAndProductIdAndStatus(
            "tenant_1", PRODUCT_ID, ReviewStatus.PENDING, pageable);
        Page<Review> tenant2Reviews = reviewRepository.findByTenantIdAndProductIdAndStatus(
            "tenant_2", PRODUCT_ID, ReviewStatus.PENDING, pageable);

        // Then
        assertEquals(1, tenant1Reviews.getTotalElements());
        assertEquals(1, tenant2Reviews.getTotalElements());
        assertEquals("tenant_1", tenant1Reviews.getContent().get(0).getTenantId());
        assertEquals("tenant_2", tenant2Reviews.getContent().get(0).getTenantId());
    }

    private Review createSampleReview() {
        Review review = new Review(TENANT_ID, USER_ID, PRODUCT_ID, 5, "Great Product", "Excellent quality");
        review.setStatus(ReviewStatus.PENDING);
        review.setVerified(false);
        review.setHelpfulVotes(0);
        review.setTotalVotes(0);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return review;
    }
}
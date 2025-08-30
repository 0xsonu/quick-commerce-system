package com.ecommerce.reviewservice.controller;

import com.ecommerce.reviewservice.dto.ModerationReportResponse;
import com.ecommerce.reviewservice.dto.ReviewAnalyticsResponse;
import com.ecommerce.reviewservice.service.ReviewAnalyticsService;
import com.ecommerce.shared.utils.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller for review analytics and reporting endpoints
 */
@RestController
@RequestMapping("/api/v1/reviews/analytics")
public class ReviewAnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewAnalyticsController.class);

    private final ReviewAnalyticsService analyticsService;

    @Autowired
    public ReviewAnalyticsController(ReviewAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<ReviewAnalyticsResponse>> getProductAnalytics(
            @PathVariable String productId) {
        
        logger.info("Getting analytics for product {}", productId);
        
        ReviewAnalyticsResponse analytics = analyticsService.getProductAnalytics(productId);
        
        return ResponseEntity.ok(ApiResponse.success(analytics, "Product analytics retrieved successfully"));
    }

    @GetMapping("/tenant")
    public ResponseEntity<ApiResponse<ReviewAnalyticsResponse>> getTenantAnalytics() {
        
        logger.info("Getting overall tenant analytics");
        
        ReviewAnalyticsResponse analytics = analyticsService.getTenantAnalytics();
        
        return ResponseEntity.ok(ApiResponse.success(analytics, "Tenant analytics retrieved successfully"));
    }

    @GetMapping("/moderation/report")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> getModerationReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("Getting moderation report from {} to {}", startDate, endDate);
        
        ModerationReportResponse report = analyticsService.getModerationReport(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(report, "Moderation report generated successfully"));
    }

    @GetMapping("/top-products/by-reviews")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getTopProductsByReviewCount(
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting top {} products by review count", limit);
        
        Map<String, Long> topProducts = analyticsService.getTopProductsByReviewCount(limit);
        
        return ResponseEntity.ok(ApiResponse.success(topProducts, "Top products by review count retrieved successfully"));
    }

    @GetMapping("/top-products/by-rating")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getTopRatedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting top {} rated products", limit);
        
        Map<String, Double> topProducts = analyticsService.getTopRatedProducts(limit);
        
        return ResponseEntity.ok(ApiResponse.success(topProducts, "Top rated products retrieved successfully"));
    }
}
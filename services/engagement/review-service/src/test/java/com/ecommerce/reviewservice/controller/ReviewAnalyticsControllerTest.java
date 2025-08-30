package com.ecommerce.reviewservice.controller;

import com.ecommerce.reviewservice.dto.ModerationReportResponse;
import com.ecommerce.reviewservice.dto.ReviewAnalyticsResponse;
import com.ecommerce.reviewservice.service.ReviewAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewAnalyticsController.class)
class ReviewAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewAnalyticsService analyticsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getProductAnalytics_ShouldReturnAnalytics() throws Exception {
        // Given
        String productId = "product123";
        ReviewAnalyticsResponse analytics = new ReviewAnalyticsResponse();
        analytics.setProductId(productId);
        analytics.setTotalReviews(10L);
        analytics.setAverageRating(4.5);

        when(analyticsService.getProductAnalytics(productId)).thenReturn(analytics);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/analytics/product/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product analytics retrieved successfully"))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.totalReviews").value(10))
                .andExpect(jsonPath("$.data.averageRating").value(4.5));
    }

    @Test
    void getTenantAnalytics_ShouldReturnAnalytics() throws Exception {
        // Given
        ReviewAnalyticsResponse analytics = new ReviewAnalyticsResponse();
        analytics.setTotalReviews(100L);
        analytics.setAverageRating(4.2);

        when(analyticsService.getTenantAnalytics()).thenReturn(analytics);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/analytics/tenant")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tenant analytics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalReviews").value(100))
                .andExpect(jsonPath("$.data.averageRating").value(4.2));
    }

    @Test
    void getModerationReport_ShouldReturnReport() throws Exception {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59);
        
        ModerationReportResponse report = new ModerationReportResponse();
        report.setTotalReviewsModerated(50L);
        report.setApprovedCount(40L);
        report.setRejectedCount(10L);
        report.setApprovalRate(80.0);

        when(analyticsService.getModerationReport(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(report);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/analytics/moderation/report")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-01-31T23:59:00")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Moderation report generated successfully"))
                .andExpect(jsonPath("$.data.totalReviewsModerated").value(50))
                .andExpect(jsonPath("$.data.approvedCount").value(40))
                .andExpect(jsonPath("$.data.rejectedCount").value(10))
                .andExpect(jsonPath("$.data.approvalRate").value(80.0));
    }

    @Test
    void getTopProductsByReviewCount_ShouldReturnTopProducts() throws Exception {
        // Given
        Map<String, Long> topProducts = new HashMap<>();
        topProducts.put("product1", 100L);
        topProducts.put("product2", 80L);
        topProducts.put("product3", 60L);

        when(analyticsService.getTopProductsByReviewCount(10)).thenReturn(topProducts);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/analytics/top-products/by-reviews")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Top products by review count retrieved successfully"))
                .andExpect(jsonPath("$.data.product1").value(100))
                .andExpect(jsonPath("$.data.product2").value(80))
                .andExpect(jsonPath("$.data.product3").value(60));
    }

    @Test
    void getTopRatedProducts_ShouldReturnTopProducts() throws Exception {
        // Given
        Map<String, Double> topProducts = new HashMap<>();
        topProducts.put("product1", 4.8);
        topProducts.put("product2", 4.7);
        topProducts.put("product3", 4.6);

        when(analyticsService.getTopRatedProducts(10)).thenReturn(topProducts);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/analytics/top-products/by-rating")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Top rated products retrieved successfully"))
                .andExpect(jsonPath("$.data.product1").value(4.8))
                .andExpect(jsonPath("$.data.product2").value(4.7))
                .andExpect(jsonPath("$.data.product3").value(4.6));
    }

    @Test
    void getTopProductsByReviewCount_WithDefaultLimit_ShouldUseDefaultValue() throws Exception {
        // Given
        Map<String, Long> topProducts = new HashMap<>();
        topProducts.put("product1", 100L);

        when(analyticsService.getTopProductsByReviewCount(10)).thenReturn(topProducts);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/analytics/top-products/by-reviews")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getTopRatedProducts_WithDefaultLimit_ShouldUseDefaultValue() throws Exception {
        // Given
        Map<String, Double> topProducts = new HashMap<>();
        topProducts.put("product1", 4.8);

        when(analyticsService.getTopRatedProducts(10)).thenReturn(topProducts);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/analytics/top-products/by-rating")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
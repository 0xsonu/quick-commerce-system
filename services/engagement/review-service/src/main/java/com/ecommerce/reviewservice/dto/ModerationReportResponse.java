package com.ecommerce.reviewservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for moderation reporting data
 */
public class ModerationReportResponse {

    private Long totalReviewsModerated;
    private Long approvedCount;
    private Long rejectedCount;
    private Long flaggedCount;
    private Double approvalRate; // Percentage of reviews approved
    private Double rejectionRate; // Percentage of reviews rejected
    private Map<Long, Long> moderatorActivity; // Moderator ID -> Number of reviews moderated
    private Map<String, Long> flagReasons; // Flag reason -> Count
    private LocalDateTime reportPeriodStart;
    private LocalDateTime reportPeriodEnd;
    private Double averageModerationTime; // Average time in hours from creation to moderation

    public ModerationReportResponse() {}

    public ModerationReportResponse(Long totalReviewsModerated, Long approvedCount, Long rejectedCount,
                                   Long flaggedCount, Double approvalRate, Double rejectionRate,
                                   Map<Long, Long> moderatorActivity, Map<String, Long> flagReasons,
                                   LocalDateTime reportPeriodStart, LocalDateTime reportPeriodEnd,
                                   Double averageModerationTime) {
        this.totalReviewsModerated = totalReviewsModerated;
        this.approvedCount = approvedCount;
        this.rejectedCount = rejectedCount;
        this.flaggedCount = flaggedCount;
        this.approvalRate = approvalRate;
        this.rejectionRate = rejectionRate;
        this.moderatorActivity = moderatorActivity;
        this.flagReasons = flagReasons;
        this.reportPeriodStart = reportPeriodStart;
        this.reportPeriodEnd = reportPeriodEnd;
        this.averageModerationTime = averageModerationTime;
    }

    // Getters and Setters
    public Long getTotalReviewsModerated() {
        return totalReviewsModerated;
    }

    public void setTotalReviewsModerated(Long totalReviewsModerated) {
        this.totalReviewsModerated = totalReviewsModerated;
    }

    public Long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public Long getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(Long rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public Long getFlaggedCount() {
        return flaggedCount;
    }

    public void setFlaggedCount(Long flaggedCount) {
        this.flaggedCount = flaggedCount;
    }

    public Double getApprovalRate() {
        return approvalRate;
    }

    public void setApprovalRate(Double approvalRate) {
        this.approvalRate = approvalRate;
    }

    public Double getRejectionRate() {
        return rejectionRate;
    }

    public void setRejectionRate(Double rejectionRate) {
        this.rejectionRate = rejectionRate;
    }

    public Map<Long, Long> getModeratorActivity() {
        return moderatorActivity;
    }

    public void setModeratorActivity(Map<Long, Long> moderatorActivity) {
        this.moderatorActivity = moderatorActivity;
    }

    public Map<String, Long> getFlagReasons() {
        return flagReasons;
    }

    public void setFlagReasons(Map<String, Long> flagReasons) {
        this.flagReasons = flagReasons;
    }

    public LocalDateTime getReportPeriodStart() {
        return reportPeriodStart;
    }

    public void setReportPeriodStart(LocalDateTime reportPeriodStart) {
        this.reportPeriodStart = reportPeriodStart;
    }

    public LocalDateTime getReportPeriodEnd() {
        return reportPeriodEnd;
    }

    public void setReportPeriodEnd(LocalDateTime reportPeriodEnd) {
        this.reportPeriodEnd = reportPeriodEnd;
    }

    public Double getAverageModerationTime() {
        return averageModerationTime;
    }

    public void setAverageModerationTime(Double averageModerationTime) {
        this.averageModerationTime = averageModerationTime;
    }
}
package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.entity.NotificationChannel;

import java.time.LocalDateTime;

public class NotificationABTestResponse {

    private Long id;
    private String tenantId;
    private String testName;
    private String templateKey;
    private NotificationChannel channel;
    private Long controlVersionId;
    private Long variantVersionId;
    private Integer trafficSplitPercentage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private String description;
    private String successMetric;
    private String createdBy;
    private String stoppedBy;
    private LocalDateTime stoppedAt;
    private String stopReason;
    private Long controlSentCount;
    private Long variantSentCount;
    private Long controlSuccessCount;
    private Long variantSuccessCount;
    private LocalDateTime lastStatsUpdate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated fields
    private Double controlSuccessRate;
    private Double variantSuccessRate;
    private Double improvementPercentage;
    private Boolean isStatisticallySignificant;

    // Constructors
    public NotificationABTestResponse() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public Long getControlVersionId() {
        return controlVersionId;
    }

    public void setControlVersionId(Long controlVersionId) {
        this.controlVersionId = controlVersionId;
    }

    public Long getVariantVersionId() {
        return variantVersionId;
    }

    public void setVariantVersionId(Long variantVersionId) {
        this.variantVersionId = variantVersionId;
    }

    public Integer getTrafficSplitPercentage() {
        return trafficSplitPercentage;
    }

    public void setTrafficSplitPercentage(Integer trafficSplitPercentage) {
        this.trafficSplitPercentage = trafficSplitPercentage;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuccessMetric() {
        return successMetric;
    }

    public void setSuccessMetric(String successMetric) {
        this.successMetric = successMetric;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getStoppedBy() {
        return stoppedBy;
    }

    public void setStoppedBy(String stoppedBy) {
        this.stoppedBy = stoppedBy;
    }

    public LocalDateTime getStoppedAt() {
        return stoppedAt;
    }

    public void setStoppedAt(LocalDateTime stoppedAt) {
        this.stoppedAt = stoppedAt;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public Long getControlSentCount() {
        return controlSentCount;
    }

    public void setControlSentCount(Long controlSentCount) {
        this.controlSentCount = controlSentCount;
    }

    public Long getVariantSentCount() {
        return variantSentCount;
    }

    public void setVariantSentCount(Long variantSentCount) {
        this.variantSentCount = variantSentCount;
    }

    public Long getControlSuccessCount() {
        return controlSuccessCount;
    }

    public void setControlSuccessCount(Long controlSuccessCount) {
        this.controlSuccessCount = controlSuccessCount;
    }

    public Long getVariantSuccessCount() {
        return variantSuccessCount;
    }

    public void setVariantSuccessCount(Long variantSuccessCount) {
        this.variantSuccessCount = variantSuccessCount;
    }

    public LocalDateTime getLastStatsUpdate() {
        return lastStatsUpdate;
    }

    public void setLastStatsUpdate(LocalDateTime lastStatsUpdate) {
        this.lastStatsUpdate = lastStatsUpdate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getControlSuccessRate() {
        return controlSuccessRate;
    }

    public void setControlSuccessRate(Double controlSuccessRate) {
        this.controlSuccessRate = controlSuccessRate;
    }

    public Double getVariantSuccessRate() {
        return variantSuccessRate;
    }

    public void setVariantSuccessRate(Double variantSuccessRate) {
        this.variantSuccessRate = variantSuccessRate;
    }

    public Double getImprovementPercentage() {
        return improvementPercentage;
    }

    public void setImprovementPercentage(Double improvementPercentage) {
        this.improvementPercentage = improvementPercentage;
    }

    public Boolean getIsStatisticallySignificant() {
        return isStatisticallySignificant;
    }

    public void setIsStatisticallySignificant(Boolean isStatisticallySignificant) {
        this.isStatisticallySignificant = isStatisticallySignificant;
    }
}
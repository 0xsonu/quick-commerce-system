package com.ecommerce.notificationservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.ecommerce.shared.models.TenantAware;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_ab_tests",
       indexes = {
           @Index(name = "idx_tenant_template_test", columnList = "tenant_id, template_key, test_name"),
           @Index(name = "idx_tenant_active_tests", columnList = "tenant_id, is_active"),
           @Index(name = "idx_test_period", columnList = "start_date, end_date")
       })
public class NotificationABTest extends BaseEntity implements TenantAware {

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotBlank
    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @NotBlank
    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @NotNull
    @Column(name = "control_version_id", nullable = false)
    private Long controlVersionId;

    @NotNull
    @Column(name = "variant_version_id", nullable = false)
    private Long variantVersionId;

    @NotNull
    @Column(name = "traffic_split_percentage", nullable = false)
    private Integer trafficSplitPercentage; // Percentage for variant (0-100)

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "success_metric", length = 50)
    private String successMetric; // OPEN_RATE, CLICK_RATE, CONVERSION_RATE

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "stopped_by", length = 100)
    private String stoppedBy;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @Column(name = "stop_reason", length = 255)
    private String stopReason;

    // Statistics fields (updated periodically)
    @Column(name = "control_sent_count", nullable = false)
    private Long controlSentCount = 0L;

    @Column(name = "variant_sent_count", nullable = false)
    private Long variantSentCount = 0L;

    @Column(name = "control_success_count", nullable = false)
    private Long controlSuccessCount = 0L;

    @Column(name = "variant_success_count", nullable = false)
    private Long variantSuccessCount = 0L;

    @Column(name = "last_stats_update")
    private LocalDateTime lastStatsUpdate;

    // Constructors
    public NotificationABTest() {}

    public NotificationABTest(String tenantId, String testName, String templateKey, 
                             NotificationChannel channel, Long controlVersionId, 
                             Long variantVersionId, Integer trafficSplitPercentage,
                             LocalDateTime startDate, String createdBy) {
        this.tenantId = tenantId;
        this.testName = testName;
        this.templateKey = templateKey;
        this.channel = channel;
        this.controlVersionId = controlVersionId;
        this.variantVersionId = variantVersionId;
        this.trafficSplitPercentage = trafficSplitPercentage;
        this.startDate = startDate;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
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
}
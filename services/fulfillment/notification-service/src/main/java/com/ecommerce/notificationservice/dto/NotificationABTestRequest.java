package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class NotificationABTestRequest {

    @NotBlank(message = "Test name is required")
    @Size(max = 100, message = "Test name must not exceed 100 characters")
    private String testName;

    @NotBlank(message = "Template key is required")
    @Size(max = 100, message = "Template key must not exceed 100 characters")
    private String templateKey;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotNull(message = "Control version ID is required")
    private Long controlVersionId;

    @NotNull(message = "Variant version ID is required")
    private Long variantVersionId;

    @NotNull(message = "Traffic split percentage is required")
    @Min(value = 1, message = "Traffic split percentage must be at least 1")
    @Max(value = 99, message = "Traffic split percentage must be at most 99")
    private Integer trafficSplitPercentage;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Pattern(regexp = "OPEN_RATE|CLICK_RATE|CONVERSION_RATE", 
             message = "Success metric must be one of: OPEN_RATE, CLICK_RATE, CONVERSION_RATE")
    private String successMetric;

    // Constructors
    public NotificationABTestRequest() {}

    // Getters and Setters
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
}
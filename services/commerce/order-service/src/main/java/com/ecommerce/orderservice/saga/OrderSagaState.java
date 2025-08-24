package com.ecommerce.orderservice.saga;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class OrderSagaState {
    private Long orderId;
    private String tenantId;
    private SagaStatus status;
    private SagaStep currentStep;
    private Map<String, Object> sagaData;
    private LocalDateTime startedAt;
    private LocalDateTime lastUpdatedAt;
    private LocalDateTime timeoutAt;
    private String errorMessage;
    private int retryCount;
    private static final int MAX_RETRIES = 3;

    public OrderSagaState(Long orderId, String tenantId) {
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.status = SagaStatus.STARTED;
        this.currentStep = SagaStep.USER_VALIDATION;
        this.sagaData = new HashMap<>();
        this.startedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        this.timeoutAt = LocalDateTime.now().plusMinutes(30); // 30 minute timeout
        this.retryCount = 0;
    }

    // Business methods
    public void moveToNextStep() {
        if (currentStep != SagaStep.COMPLETED) {
            this.currentStep = currentStep.getNextStep();
            this.lastUpdatedAt = LocalDateTime.now();
        }
    }

    public void moveToPreviousStep() {
        if (currentStep != SagaStep.USER_VALIDATION) {
            this.currentStep = currentStep.getPreviousStep();
            this.lastUpdatedAt = LocalDateTime.now();
        }
    }

    public void markCompleted() {
        this.status = SagaStatus.COMPLETED;
        this.currentStep = SagaStep.COMPLETED;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = SagaStatus.FAILED;
        this.errorMessage = errorMessage;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void startCompensation() {
        this.status = SagaStatus.COMPENSATING;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void markCompensated() {
        this.status = SagaStatus.COMPENSATED;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public boolean isTimedOut() {
        return LocalDateTime.now().isAfter(timeoutAt);
    }

    public boolean canRetry() {
        return retryCount < MAX_RETRIES;
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void putSagaData(String key, Object value) {
        this.sagaData.put(key, value);
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public Object getSagaData(String key) {
        return this.sagaData.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSagaData(String key, Class<T> type) {
        Object value = this.sagaData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public SagaStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(SagaStep currentStep) {
        this.currentStep = currentStep;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getSagaData() {
        return sagaData;
    }

    public void setSagaData(Map<String, Object> sagaData) {
        this.sagaData = sagaData;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public LocalDateTime getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(LocalDateTime timeoutAt) {
        this.timeoutAt = timeoutAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
package com.ecommerce.orderservice.saga;

public enum SagaStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED;

    public boolean isActive() {
        return this == STARTED || this == IN_PROGRESS || this == COMPENSATING;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == COMPENSATED || this == FAILED;
    }

    public boolean canCompensate() {
        return this == IN_PROGRESS || this == FAILED;
    }
}
package com.ecommerce.orderservice.saga;

public enum SagaStep {
    USER_VALIDATION,
    INVENTORY_RESERVATION,
    PAYMENT_PROCESSING,
    ORDER_CONFIRMATION,
    COMPLETED;

    public SagaStep getNextStep() {
        return switch (this) {
            case USER_VALIDATION -> INVENTORY_RESERVATION;
            case INVENTORY_RESERVATION -> PAYMENT_PROCESSING;
            case PAYMENT_PROCESSING -> ORDER_CONFIRMATION;
            case ORDER_CONFIRMATION -> COMPLETED;
            case COMPLETED -> throw new IllegalStateException("Saga is already completed");
        };
    }

    public SagaStep getPreviousStep() {
        return switch (this) {
            case USER_VALIDATION -> throw new IllegalStateException("No previous step for USER_VALIDATION");
            case INVENTORY_RESERVATION -> USER_VALIDATION;
            case PAYMENT_PROCESSING -> INVENTORY_RESERVATION;
            case ORDER_CONFIRMATION -> PAYMENT_PROCESSING;
            case COMPLETED -> ORDER_CONFIRMATION;
        };
    }

    public boolean isCompensatable() {
        return this != USER_VALIDATION && this != COMPLETED;
    }
}
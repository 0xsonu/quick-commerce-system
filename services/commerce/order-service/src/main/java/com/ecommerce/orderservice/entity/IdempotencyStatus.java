package com.ecommerce.orderservice.entity;

public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
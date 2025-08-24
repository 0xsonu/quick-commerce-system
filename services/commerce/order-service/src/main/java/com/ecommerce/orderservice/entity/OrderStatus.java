package com.ecommerce.orderservice.entity;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        return getAllowedTransitions().contains(newStatus);
    }

    public Set<OrderStatus> getAllowedTransitions() {
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(PROCESSING, CANCELLED);
            case PROCESSING -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }

    public boolean isFinalState() {
        return getAllowedTransitions().isEmpty();
    }

    public boolean isActive() {
        return this != CANCELLED && this != DELIVERED;
    }
}
package com.ecommerce.shippingservice.entity;

public enum ShipmentStatus {
    CREATED("Created"),
    PICKED_UP("Picked Up"),
    IN_TRANSIT("In Transit"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    EXCEPTION("Exception"),
    RETURNED("Returned");

    private final String displayName;

    ShipmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED;
    }

    public boolean isActive() {
        return this == IN_TRANSIT || this == OUT_FOR_DELIVERY || this == PICKED_UP;
    }

    public boolean hasException() {
        return this == EXCEPTION;
    }
}
package com.ecommerce.notificationservice.entity;

public enum NotificationType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    INVENTORY_LOW,
    ACCOUNT_CREATED,
    PASSWORD_RESET,
    PROMOTIONAL,
    NEWSLETTER
}
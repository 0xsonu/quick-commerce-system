package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateOrderStatusRequest {

    @NotNull(message = "New status is required")
    private OrderStatus newStatus;

    private String reason;

    // Constructors
    public UpdateOrderStatusRequest() {}

    public UpdateOrderStatusRequest(OrderStatus newStatus, String reason) {
        this.newStatus = newStatus;
        this.reason = reason;
    }

    // Getters and Setters
    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
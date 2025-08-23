package com.ecommerce.inventoryservice.dto;

import com.ecommerce.shared.models.events.InventoryReservedEvent;
import com.ecommerce.shared.models.events.InventoryReservationFailedEvent;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationResponse {

    private String reservationId;
    private String orderId;
    private ReservationStatus status;
    private List<InventoryReservedEvent.ReservedItemData> reservedItems;
    private List<InventoryReservationFailedEvent.FailedItemData> failedItems;
    private String failureReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    public ReservationResponse() {
        this.createdAt = LocalDateTime.now();
    }

    public static ReservationResponse success(String reservationId, 
                                            List<InventoryReservedEvent.ReservedItemData> reservedItems) {
        ReservationResponse response = new ReservationResponse();
        response.reservationId = reservationId;
        response.status = ReservationStatus.SUCCESS;
        response.reservedItems = reservedItems;
        response.expiresAt = LocalDateTime.now().plusMinutes(30); // Default TTL
        return response;
    }

    public static ReservationResponse failed(String reservationId, 
                                           List<InventoryReservationFailedEvent.FailedItemData> failedItems) {
        ReservationResponse response = new ReservationResponse();
        response.reservationId = reservationId;
        response.status = ReservationStatus.FAILED;
        response.failedItems = failedItems;
        response.failureReason = "Insufficient inventory for some items";
        return response;
    }

    public static ReservationResponse failed(String reservationId, String reason) {
        ReservationResponse response = new ReservationResponse();
        response.reservationId = reservationId;
        response.status = ReservationStatus.FAILED;
        response.failureReason = reason;
        return response;
    }

    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public List<InventoryReservedEvent.ReservedItemData> getReservedItems() {
        return reservedItems;
    }

    public void setReservedItems(List<InventoryReservedEvent.ReservedItemData> reservedItems) {
        this.reservedItems = reservedItems;
    }

    public List<InventoryReservationFailedEvent.FailedItemData> getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(List<InventoryReservationFailedEvent.FailedItemData> failedItems) {
        this.failedItems = failedItems;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public enum ReservationStatus {
        SUCCESS,
        FAILED,
        EXPIRED
    }
}
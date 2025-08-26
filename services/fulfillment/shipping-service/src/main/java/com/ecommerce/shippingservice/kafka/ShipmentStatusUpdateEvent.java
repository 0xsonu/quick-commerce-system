package com.ecommerce.shippingservice.kafka;

import com.ecommerce.shared.models.events.DomainEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Event published when shipment status is updated
 */
public class ShipmentStatusUpdateEvent extends DomainEvent {

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Shipment number is required")
    private String shipmentNumber;

    private String trackingNumber;

    @NotBlank(message = "Carrier name is required")
    private String carrierName;

    private String previousStatus;

    @NotBlank(message = "New status is required")
    private String newStatus;

    @NotNull(message = "Status change time is required")
    private LocalDateTime statusChangeTime;

    public ShipmentStatusUpdateEvent() {
        super();
        setEventType("SHIPMENT_STATUS_UPDATED");
    }

    public ShipmentStatusUpdateEvent(String tenantId, Long shipmentId, Long orderId, 
                                   String shipmentNumber, String trackingNumber, String carrierName,
                                   String previousStatus, String newStatus, LocalDateTime statusChangeTime) {
        super(tenantId, "SHIPMENT_STATUS_UPDATED");
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.shipmentNumber = shipmentNumber;
        this.trackingNumber = trackingNumber;
        this.carrierName = carrierName;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.statusChangeTime = statusChangeTime;
    }

    // Getters and Setters
    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public void setShipmentNumber(String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getStatusChangeTime() {
        return statusChangeTime;
    }

    public void setStatusChangeTime(LocalDateTime statusChangeTime) {
        this.statusChangeTime = statusChangeTime;
    }
}
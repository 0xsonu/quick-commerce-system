package com.ecommerce.shippingservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_tracking", indexes = {
    @Index(name = "idx_shipment_id", columnList = "shipment_id"),
    @Index(name = "idx_event_time", columnList = "event_time"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_carrier_event_id", columnList = "carrier_event_id")
})
public class ShipmentTracking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    @JsonBackReference
    private Shipment shipment;

    @NotBlank
    @Column(name = "status", nullable = false, length = 100)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", length = 255)
    private String location;

    @NotNull
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "carrier_event_id", length = 255)
    private String carrierEventId;

    // Constructors
    public ShipmentTracking() {}

    public ShipmentTracking(String status, String description, String location, LocalDateTime eventTime) {
        this.status = status;
        this.description = description;
        this.location = location;
        this.eventTime = eventTime;
    }

    public ShipmentTracking(String status, String description, String location, LocalDateTime eventTime, String carrierEventId) {
        this(status, description, location, eventTime);
        this.carrierEventId = carrierEventId;
    }

    // Business methods
    public boolean isDeliveryEvent() {
        return "DELIVERED".equalsIgnoreCase(status) || 
               (description != null && description.toLowerCase().contains("delivered"));
    }

    public boolean isExceptionEvent() {
        return "EXCEPTION".equalsIgnoreCase(status) || 
               (description != null && (description.toLowerCase().contains("exception") || 
                                      description.toLowerCase().contains("failed") ||
                                      description.toLowerCase().contains("delayed")));
    }

    // Getters and Setters
    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getCarrierEventId() {
        return carrierEventId;
    }

    public void setCarrierEventId(String carrierEventId) {
        this.carrierEventId = carrierEventId;
    }
}
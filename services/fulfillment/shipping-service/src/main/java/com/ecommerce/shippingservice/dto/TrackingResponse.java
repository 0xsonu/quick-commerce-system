package com.ecommerce.shippingservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TrackingResponse {

    private String trackingNumber;
    private String status;
    private String statusDescription;
    private String currentLocation;
    private LocalDateTime lastUpdated;
    private LocalDate estimatedDeliveryDate;
    private List<TrackingEventResponse> events;

    // Constructors
    public TrackingResponse() {}

    public TrackingResponse(String trackingNumber, String status, String statusDescription,
                           String currentLocation, LocalDateTime lastUpdated,
                           LocalDate estimatedDeliveryDate, List<TrackingEventResponse> events) {
        this.trackingNumber = trackingNumber;
        this.status = status;
        this.statusDescription = statusDescription;
        this.currentLocation = currentLocation;
        this.lastUpdated = lastUpdated;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.events = events;
    }

    // Getters and Setters
    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public List<TrackingEventResponse> getEvents() {
        return events;
    }

    public void setEvents(List<TrackingEventResponse> events) {
        this.events = events;
    }
}
package com.ecommerce.shippingservice.dto;

import java.time.LocalDateTime;

public class TrackingEventResponse {

    private String status;
    private String description;
    private String location;
    private LocalDateTime eventTime;

    // Constructors
    public TrackingEventResponse() {}

    public TrackingEventResponse(String status, String description, String location, LocalDateTime eventTime) {
        this.status = status;
        this.description = description;
        this.location = location;
        this.eventTime = eventTime;
    }

    // Getters and Setters
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
}
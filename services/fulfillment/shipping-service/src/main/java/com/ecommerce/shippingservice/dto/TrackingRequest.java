package com.ecommerce.shippingservice.dto;

import jakarta.validation.constraints.NotBlank;

public class TrackingRequest {

    @NotBlank
    private String trackingNumber;

    private String carrierName; // Optional - if not provided, will try to detect

    // Constructors
    public TrackingRequest() {}

    public TrackingRequest(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public TrackingRequest(String trackingNumber, String carrierName) {
        this.trackingNumber = trackingNumber;
        this.carrierName = carrierName;
    }

    // Getters and Setters
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
}
package com.ecommerce.shippingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ShippingRateRequest {

    @NotBlank
    private String originAddress;

    @NotBlank
    private String destinationAddress;

    @NotNull
    @Positive
    private BigDecimal weightKg;

    private String dimensions; // JSON string for {length, width, height}

    private String serviceType; // Optional - if not provided, return all available services

    // Constructors
    public ShippingRateRequest() {}

    public ShippingRateRequest(String originAddress, String destinationAddress, BigDecimal weightKg) {
        this.originAddress = originAddress;
        this.destinationAddress = destinationAddress;
        this.weightKg = weightKg;
    }

    // Getters and Setters
    public String getOriginAddress() {
        return originAddress;
    }

    public void setOriginAddress(String originAddress) {
        this.originAddress = originAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
}
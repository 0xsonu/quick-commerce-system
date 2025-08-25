package com.ecommerce.shippingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public class CreateShipmentRequest {

    @NotNull
    private Long orderId;

    @NotBlank
    private String carrierName;

    @NotBlank
    private String serviceType;

    @NotBlank
    private String shippingAddress; // JSON string

    @NotNull
    @Positive
    private BigDecimal weightKg;

    private String dimensions; // JSON string for {length, width, height}

    @NotNull
    private List<ShipmentItemRequest> items;

    // Constructors
    public CreateShipmentRequest() {}

    public CreateShipmentRequest(Long orderId, String carrierName, String serviceType, 
                                String shippingAddress, BigDecimal weightKg, List<ShipmentItemRequest> items) {
        this.orderId = orderId;
        this.carrierName = carrierName;
        this.serviceType = serviceType;
        this.shippingAddress = shippingAddress;
        this.weightKg = weightKg;
        this.items = items;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
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

    public List<ShipmentItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ShipmentItemRequest> items) {
        this.items = items;
    }
}
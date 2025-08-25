package com.ecommerce.shippingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateShipmentResponse {

    private boolean success;
    private String trackingNumber;
    private String carrierShipmentId;
    private BigDecimal shippingCost;
    private String currency;
    private LocalDate estimatedDeliveryDate;
    private String errorMessage;

    // Constructors
    public CreateShipmentResponse() {}

    public CreateShipmentResponse(boolean success, String trackingNumber, String carrierShipmentId,
                                 BigDecimal shippingCost, String currency, LocalDate estimatedDeliveryDate,
                                 String errorMessage) {
        this.success = success;
        this.trackingNumber = trackingNumber;
        this.carrierShipmentId = carrierShipmentId;
        this.shippingCost = shippingCost;
        this.currency = currency;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.errorMessage = errorMessage;
    }

    // Static factory methods
    public static CreateShipmentResponse success(String trackingNumber, String carrierShipmentId,
                                               BigDecimal shippingCost, String currency,
                                               LocalDate estimatedDeliveryDate) {
        return new CreateShipmentResponse(true, trackingNumber, carrierShipmentId, 
                                        shippingCost, currency, estimatedDeliveryDate, null);
    }

    public static CreateShipmentResponse failure(String errorMessage) {
        return new CreateShipmentResponse(false, null, null, null, null, null, errorMessage);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrierShipmentId() {
        return carrierShipmentId;
    }

    public void setCarrierShipmentId(String carrierShipmentId) {
        this.carrierShipmentId = carrierShipmentId;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
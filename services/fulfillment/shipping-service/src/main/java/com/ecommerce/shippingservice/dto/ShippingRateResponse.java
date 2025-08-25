package com.ecommerce.shippingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ShippingRateResponse {

    private String carrierName;
    private String serviceCode;
    private String serviceName;
    private BigDecimal rate;
    private String currency;
    private Integer transitDays;
    private LocalDate estimatedDeliveryDate;

    // Constructors
    public ShippingRateResponse() {}

    public ShippingRateResponse(String carrierName, String serviceCode, String serviceName, 
                               BigDecimal rate, String currency, Integer transitDays, 
                               LocalDate estimatedDeliveryDate) {
        this.carrierName = carrierName;
        this.serviceCode = serviceCode;
        this.serviceName = serviceName;
        this.rate = rate;
        this.currency = currency;
        this.transitDays = transitDays;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    // Getters and Setters
    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getTransitDays() {
        return transitDays;
    }

    public void setTransitDays(Integer transitDays) {
        this.transitDays = transitDays;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }
}
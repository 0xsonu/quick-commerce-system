package com.ecommerce.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateOrderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<CreateOrderItemRequest> items;

    @NotNull(message = "Billing address is required")
    @Valid
    private AddressDto billingAddress;

    @NotNull(message = "Shipping address is required")
    @Valid
    private AddressDto shippingAddress;

    private String currency = "USD";

    // Constructors
    public CreateOrderRequest() {}

    public CreateOrderRequest(Long userId, List<CreateOrderItemRequest> items, 
                             AddressDto billingAddress, AddressDto shippingAddress) {
        this.userId = userId;
        this.items = items;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }

    public AddressDto getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(AddressDto billingAddress) {
        this.billingAddress = billingAddress;
    }

    public AddressDto getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(AddressDto shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
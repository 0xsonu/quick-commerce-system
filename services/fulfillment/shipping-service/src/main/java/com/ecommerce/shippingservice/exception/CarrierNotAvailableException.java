package com.ecommerce.shippingservice.exception;

public class CarrierNotAvailableException extends RuntimeException {

    public CarrierNotAvailableException(String message) {
        super(message);
    }

    public CarrierNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
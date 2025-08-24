package com.ecommerce.orderservice.exception;

public class DuplicateOrderException extends RuntimeException {
    
    private final Long existingOrderId;
    
    public DuplicateOrderException(String message, Long existingOrderId) {
        super(message);
        this.existingOrderId = existingOrderId;
    }
    
    public DuplicateOrderException(String message, Long existingOrderId, Throwable cause) {
        super(message, cause);
        this.existingOrderId = existingOrderId;
    }
    
    public Long getExistingOrderId() {
        return existingOrderId;
    }
}
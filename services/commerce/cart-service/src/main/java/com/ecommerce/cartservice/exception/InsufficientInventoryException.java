package com.ecommerce.cartservice.exception;

/**
 * Exception thrown when there is insufficient inventory for a product
 */
public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(String message) {
        super(message);
    }

    public InsufficientInventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
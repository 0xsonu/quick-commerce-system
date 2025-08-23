package com.ecommerce.cartservice.exception;

/**
 * Exception thrown when a product is not available for purchase
 */
public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException(String message) {
        super(message);
    }

    public ProductNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
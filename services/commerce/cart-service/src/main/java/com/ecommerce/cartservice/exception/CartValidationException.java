package com.ecommerce.cartservice.exception;

/**
 * Exception thrown when cart validation fails
 */
public class CartValidationException extends RuntimeException {

    public CartValidationException(String message) {
        super(message);
    }

    public CartValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
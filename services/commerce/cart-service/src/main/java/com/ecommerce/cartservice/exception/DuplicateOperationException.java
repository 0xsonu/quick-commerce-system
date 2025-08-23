package com.ecommerce.cartservice.exception;

/**
 * Exception thrown when a duplicate operation is detected
 */
public class DuplicateOperationException extends RuntimeException {

    public DuplicateOperationException(String message) {
        super(message);
    }

    public DuplicateOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
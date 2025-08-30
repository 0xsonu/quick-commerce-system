package com.ecommerce.reviewservice.exception;

public class UnauthorizedReviewAccessException extends RuntimeException {
    
    public UnauthorizedReviewAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedReviewAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
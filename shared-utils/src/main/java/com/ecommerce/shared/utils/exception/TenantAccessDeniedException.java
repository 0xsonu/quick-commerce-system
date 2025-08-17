package com.ecommerce.shared.utils.exception;

/**
 * Exception thrown when tenant access is denied
 */
public class TenantAccessDeniedException extends RuntimeException {
    
    public TenantAccessDeniedException(String message) {
        super(message);
    }

    public TenantAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
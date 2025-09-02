package com.ecommerce.gateway.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility for handling gRPC exceptions and mapping them to appropriate HTTP responses
 */
@Component
public class GrpcExceptionHandler {

    private static final Map<Status.Code, HttpStatus> STATUS_MAPPING = Map.of(
        Status.Code.OK, HttpStatus.OK,
        Status.Code.NOT_FOUND, HttpStatus.NOT_FOUND,
        Status.Code.PERMISSION_DENIED, HttpStatus.FORBIDDEN,
        Status.Code.INVALID_ARGUMENT, HttpStatus.BAD_REQUEST,
        Status.Code.UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
        Status.Code.DEADLINE_EXCEEDED, HttpStatus.REQUEST_TIMEOUT,
        Status.Code.RESOURCE_EXHAUSTED, HttpStatus.TOO_MANY_REQUESTS,
        Status.Code.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR,
        Status.Code.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED
    );

    public HttpStatus mapToHttpStatus(Status.Code grpcCode) {
        return STATUS_MAPPING.getOrDefault(grpcCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public RuntimeException handleGrpcException(StatusRuntimeException e, String operation) {
        Status.Code code = e.getStatus().getCode();
        String message = e.getStatus().getDescription();

        return switch (code) {
            case NOT_FOUND -> new ResourceNotFoundException(
                String.format("%s failed: Resource not found - %s", operation, message));
            case PERMISSION_DENIED -> new AccessDeniedException(
                String.format("%s failed: Access denied - %s", operation, message));
            case INVALID_ARGUMENT -> new ValidationException(
                String.format("%s failed: Invalid argument - %s", operation, message));
            case UNAVAILABLE -> new ServiceUnavailableException(
                String.format("%s failed: Service unavailable - %s", operation, message));
            case DEADLINE_EXCEEDED -> new TimeoutException(
                String.format("%s failed: Request timeout - %s", operation, message));
            case RESOURCE_EXHAUSTED -> new RateLimitException(
                String.format("%s failed: Rate limit exceeded - %s", operation, message));
            case UNAUTHENTICATED -> new AuthenticationException(
                String.format("%s failed: Authentication required - %s", operation, message));
            default -> new ServiceCommunicationException(
                String.format("%s failed: %s - %s", operation, code, message), e);
        };
    }

    // Custom exception classes
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }

    public static class TimeoutException extends RuntimeException {
        public TimeoutException(String message) {
            super(message);
        }
    }

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    public static class ServiceCommunicationException extends RuntimeException {
        public ServiceCommunicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
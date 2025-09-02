package com.ecommerce.cartservice.client.grpc;

import com.ecommerce.cartservice.dto.InventoryCheckResponse;
import com.ecommerce.cartservice.exception.InsufficientInventoryException;
import com.ecommerce.inventoryservice.proto.InventoryServiceGrpc;
import com.ecommerce.inventoryservice.proto.InventoryServiceProtos.*;
import com.ecommerce.shared.grpc.GrpcContextUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * gRPC client for Inventory Service
 */
@Component
public class InventoryServiceGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceGrpcClient.class);

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub;

    /**
     * Check inventory availability for a product
     */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "checkAvailabilityFallback")
    @Retry(name = "inventory-service")
    public InventoryCheckResponse checkAvailability(String productId, int requestedQuantity) {
        try {
            logger.debug("Checking inventory availability via gRPC: productId={}, quantity={}", 
                        productId, requestedQuantity);

            // Create request with tenant context
            CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setProductId(productId)
                .setRequestedQuantity(requestedQuantity)
                .build();

            // Make gRPC call with context and timeout
            CheckAvailabilityResponse response = GrpcContextUtils.withCurrentContext(inventoryServiceStub)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .checkAvailability(request);

            // Convert to DTO
            InventoryCheckResponse checkResponse = new InventoryCheckResponse();
            checkResponse.setProductId(response.getProductId());
            checkResponse.setAvailable(response.getIsAvailable());
            checkResponse.setAvailableQuantity(response.getAvailableQuantity());
            checkResponse.setRequestedQuantity(requestedQuantity);

            logger.debug("Inventory availability check completed via gRPC: productId={}, available={}, stock={}", 
                        productId, response.getIsAvailable(), response.getAvailableQuantity());

            return checkResponse;

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error checking inventory availability: productId={}, status={}, description={}", 
                        productId, e.getStatus().getCode(), e.getStatus().getDescription());
            throw mapGrpcException(e, "Inventory availability check failed for product: " + productId);
            
        } catch (Exception e) {
            logger.error("Unexpected error checking inventory availability via gRPC: productId={}", productId, e);
            throw new InsufficientInventoryException("Inventory check failed due to unexpected error for product: " + productId, e);
        }
    }

    /**
     * Map gRPC exceptions to appropriate business exceptions
     */
    private RuntimeException mapGrpcException(StatusRuntimeException e, String operation) {
        Status.Code code = e.getStatus().getCode();
        String message = e.getStatus().getDescription();
        
        return switch (code) {
            case NOT_FOUND -> new InsufficientInventoryException(operation + " - Product not found: " + message);
            case PERMISSION_DENIED -> new InsufficientInventoryException(operation + " - Access denied: " + message);
            case INVALID_ARGUMENT -> new IllegalArgumentException(operation + " - Invalid argument: " + message);
            case UNAVAILABLE -> new InsufficientInventoryException(operation + " - Service unavailable: " + message);
            case DEADLINE_EXCEEDED -> new InsufficientInventoryException(operation + " - Request timeout: " + message);
            case RESOURCE_EXHAUSTED -> new InsufficientInventoryException(operation + " - Rate limit exceeded: " + message);
            default -> new InsufficientInventoryException(operation + " - " + code + ": " + message, e);
        };
    }

    /**
     * Fallback method for checkAvailability when circuit breaker is open
     */
    public InventoryCheckResponse checkAvailabilityFallback(String productId, int requestedQuantity, Exception ex) {
        logger.warn("Inventory availability check fallback triggered for product: {}, error: {}", productId, ex.getMessage());
        throw new InsufficientInventoryException("Inventory service is currently unavailable. Please try again later.", ex);
    }

    /**
     * Reserve inventory for a product
     */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveInventoryFallback")
    @Retry(name = "inventory-service")
    public boolean reserveInventory(String productId, int quantity, String reservationId) {
        try {
            logger.debug("Reserving inventory via gRPC: productId={}, quantity={}, reservationId={}", 
                        productId, quantity, reservationId);

            // Create request with tenant context
            ReserveInventoryRequest request = ReserveInventoryRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setProductId(productId)
                .setQuantity(quantity)
                .setReservationId(reservationId)
                .setTtlSeconds(3600) // 1 hour TTL
                .build();

            // Make gRPC call with context and timeout
            ReserveInventoryResponse response = GrpcContextUtils.withCurrentContext(inventoryServiceStub)
                .withDeadlineAfter(10, TimeUnit.SECONDS) // Longer timeout for reservation operations
                .reserveInventory(request);

            logger.debug("Inventory reservation completed via gRPC: productId={}, success={}, reservationId={}", 
                        productId, response.getSuccess(), reservationId);

            if (!response.getSuccess() && !response.getErrorMessage().isEmpty()) {
                logger.warn("Inventory reservation failed: productId={}, error={}", 
                           productId, response.getErrorMessage());
                throw new InsufficientInventoryException("Inventory reservation failed: " + response.getErrorMessage());
            }

            return response.getSuccess();

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error reserving inventory: productId={}, reservationId={}, status={}, description={}", 
                        productId, reservationId, e.getStatus().getCode(), e.getStatus().getDescription());
            throw mapGrpcException(e, "Inventory reservation failed for product: " + productId);
            
        } catch (Exception e) {
            logger.error("Unexpected error reserving inventory via gRPC: productId={}, reservationId={}", 
                        productId, reservationId, e);
            throw new InsufficientInventoryException("Inventory reservation failed due to unexpected error for product: " + productId, e);
        }
    }

    /**
     * Fallback method for reserveInventory when circuit breaker is open
     */
    public boolean reserveInventoryFallback(String productId, int quantity, String reservationId, Exception ex) {
        logger.warn("Inventory reservation fallback triggered for product: {}, error: {}", productId, ex.getMessage());
        throw new InsufficientInventoryException("Inventory service is currently unavailable. Please try again later.", ex);
    }

    /**
     * Release inventory reservation
     */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "releaseInventoryFallback")
    @Retry(name = "inventory-service")
    public boolean releaseInventory(String reservationId) {
        try {
            logger.debug("Releasing inventory reservation via gRPC: reservationId={}", reservationId);

            // Create request with tenant context
            ReleaseInventoryRequest request = ReleaseInventoryRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setReservationId(reservationId)
                .build();

            // Make gRPC call with context and timeout
            ReleaseInventoryResponse response = GrpcContextUtils.withCurrentContext(inventoryServiceStub)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .releaseInventory(request);

            logger.debug("Inventory release completed via gRPC: reservationId={}, success={}", 
                        reservationId, response.getSuccess());

            if (!response.getSuccess() && !response.getErrorMessage().isEmpty()) {
                logger.warn("Inventory release failed: reservationId={}, error={}", 
                           reservationId, response.getErrorMessage());
                // Don't throw exception for release failures as they're often non-critical
            }

            return response.getSuccess();

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error releasing inventory: reservationId={}, status={}, description={}", 
                        reservationId, e.getStatus().getCode(), e.getStatus().getDescription());
            // Don't throw exception for release failures - log and return false
            return false;
            
        } catch (Exception e) {
            logger.error("Unexpected error releasing inventory via gRPC: reservationId={}", reservationId, e);
            return false;
        }
    }

    /**
     * Fallback method for releaseInventory when circuit breaker is open
     */
    public boolean releaseInventoryFallback(String reservationId, Exception ex) {
        logger.warn("Inventory release fallback triggered for reservation: {}, error: {}", reservationId, ex.getMessage());
        // For release operations, we return false instead of throwing exception
        // as release failures are often non-critical
        return false;
    }

    /**
     * Get current stock level for a product
     */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "getStockLevelFallback")
    @Retry(name = "inventory-service")
    public InventoryCheckResponse getStockLevel(String productId) {
        try {
            logger.debug("Getting stock level via gRPC: productId={}", productId);

            // Create request with tenant context
            GetStockLevelRequest request = GetStockLevelRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setProductId(productId)
                .build();

            // Make gRPC call with context and timeout
            GetStockLevelResponse response = GrpcContextUtils.withCurrentContext(inventoryServiceStub)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getStockLevel(request);

            // Convert to DTO
            InventoryCheckResponse stockResponse = new InventoryCheckResponse();
            stockResponse.setProductId(response.getProductId());
            stockResponse.setAvailable(response.getAvailableQuantity() > 0);
            stockResponse.setAvailableQuantity(response.getAvailableQuantity());
            stockResponse.setRequestedQuantity(0); // Not applicable for stock level check

            logger.debug("Stock level retrieved via gRPC: productId={}, available={}, reserved={}", 
                        productId, response.getAvailableQuantity(), response.getReservedQuantity());

            return stockResponse;

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error getting stock level: productId={}, status={}, description={}", 
                        productId, e.getStatus().getCode(), e.getStatus().getDescription());
            throw mapGrpcException(e, "Stock level check failed for product: " + productId);
            
        } catch (Exception e) {
            logger.error("Unexpected error getting stock level via gRPC: productId={}", productId, e);
            throw new InsufficientInventoryException("Stock level check failed due to unexpected error for product: " + productId, e);
        }
    }

    /**
     * Fallback method for getStockLevel when circuit breaker is open
     */
    public InventoryCheckResponse getStockLevelFallback(String productId, Exception ex) {
        logger.warn("Stock level check fallback triggered for product: {}, error: {}", productId, ex.getMessage());
        throw new InsufficientInventoryException("Inventory service is currently unavailable. Please try again later.", ex);
    }
}
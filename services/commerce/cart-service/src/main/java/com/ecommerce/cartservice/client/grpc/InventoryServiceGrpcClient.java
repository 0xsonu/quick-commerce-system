package com.ecommerce.cartservice.client.grpc;

import com.ecommerce.cartservice.dto.InventoryCheckResponse;
import com.ecommerce.inventoryservice.proto.InventoryServiceGrpc;
import com.ecommerce.inventoryservice.proto.InventoryServiceProtos.*;
import com.ecommerce.shared.grpc.GrpcContextUtils;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

            // Make gRPC call with context
            CheckAvailabilityResponse response = GrpcContextUtils.withCurrentContext(inventoryServiceStub)
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
            logger.error("gRPC error checking inventory availability: productId={}, error={}", 
                        productId, e.getStatus(), e);
            
            // Return unavailable response on error
            InventoryCheckResponse errorResponse = new InventoryCheckResponse();
            errorResponse.setProductId(productId);
            errorResponse.setAvailable(false);
            errorResponse.setAvailableQuantity(0);
            errorResponse.setRequestedQuantity(requestedQuantity);
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Unexpected error checking inventory availability via gRPC: productId={}", productId, e);
            
            // Return unavailable response on error
            InventoryCheckResponse errorResponse = new InventoryCheckResponse();
            errorResponse.setProductId(productId);
            errorResponse.setAvailable(false);
            errorResponse.setAvailableQuantity(0);
            errorResponse.setRequestedQuantity(requestedQuantity);
            return errorResponse;
        }
    }

    /**
     * Reserve inventory for a product
     */
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

            // Make gRPC call with context
            ReserveInventoryResponse response = GrpcContextUtils.withCurrentContext(inventoryServiceStub)
                .reserveInventory(request);

            logger.debug("Inventory reservation completed via gRPC: productId={}, success={}, reservationId={}", 
                        productId, response.getSuccess(), reservationId);

            if (!response.getSuccess() && !response.getErrorMessage().isEmpty()) {
                logger.warn("Inventory reservation failed: productId={}, error={}", 
                           productId, response.getErrorMessage());
            }

            return response.getSuccess();

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error reserving inventory: productId={}, reservationId={}, error={}", 
                        productId, reservationId, e.getStatus(), e);
            return false;
            
        } catch (Exception e) {
            logger.error("Unexpected error reserving inventory via gRPC: productId={}, reservationId={}", 
                        productId, reservationId, e);
            return false;
        }
    }

    /**
     * Release inventory reservation
     */
    public boolean releaseInventory(String reservationId) {
        try {
            logger.debug("Releasing inventory reservation via gRPC: reservationId={}", reservationId);

            // Create request with tenant context
            ReleaseInventoryRequest request = ReleaseInventoryRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setReservationId(reservationId)
                .build();

            // Make gRPC call with context
            ReleaseInventoryResponse response = GrpcContextUtils.withCurrentContext(inventoryServiceStub)
                .releaseInventory(request);

            logger.debug("Inventory release completed via gRPC: reservationId={}, success={}", 
                        reservationId, response.getSuccess());

            if (!response.getSuccess() && !response.getErrorMessage().isEmpty()) {
                logger.warn("Inventory release failed: reservationId={}, error={}", 
                           reservationId, response.getErrorMessage());
            }

            return response.getSuccess();

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error releasing inventory: reservationId={}, error={}", 
                        reservationId, e.getStatus(), e);
            return false;
            
        } catch (Exception e) {
            logger.error("Unexpected error releasing inventory via gRPC: reservationId={}", reservationId, e);
            return false;
        }
    }

    /**
     * Get current stock level for a product
     */
    public InventoryCheckResponse getStockLevel(String productId) {
        try {
            logger.debug("Getting stock level via gRPC: productId={}", productId);

            // Create request with tenant context
            GetStockLevelRequest request = GetStockLevelRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setProductId(productId)
                .build();

            // Make gRPC call with context
            GetStockLevelResponse response = GrpcContextUtils.withCurrentContext(inventoryServiceStub)
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
            logger.error("gRPC error getting stock level: productId={}, error={}", 
                        productId, e.getStatus(), e);
            
            // Return zero stock response on error
            InventoryCheckResponse errorResponse = new InventoryCheckResponse();
            errorResponse.setProductId(productId);
            errorResponse.setAvailable(false);
            errorResponse.setAvailableQuantity(0);
            errorResponse.setRequestedQuantity(0);
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Unexpected error getting stock level via gRPC: productId={}", productId, e);
            
            // Return zero stock response on error
            InventoryCheckResponse errorResponse = new InventoryCheckResponse();
            errorResponse.setProductId(productId);
            errorResponse.setAvailable(false);
            errorResponse.setAvailableQuantity(0);
            errorResponse.setRequestedQuantity(0);
            return errorResponse;
        }
    }
}
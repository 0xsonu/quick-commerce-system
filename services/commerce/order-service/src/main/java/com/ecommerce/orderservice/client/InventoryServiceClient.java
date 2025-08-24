package com.ecommerce.orderservice.client;

import com.ecommerce.inventoryservice.proto.InventoryServiceGrpc;
import com.ecommerce.inventoryservice.proto.InventoryServiceProtos.*;
import com.ecommerce.shared.proto.CommonProtos.TenantContext;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Service
public class InventoryServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceClient.class);
    
    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub;

    public CheckAvailabilityResponse checkAvailability(TenantContext context, String productId, int quantity) {
        try {
            CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                    .setContext(context)
                    .setProductId(productId)
                    .setRequestedQuantity(quantity)
                    .build();

            return inventoryServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .checkAvailability(request);
        } catch (Exception e) {
            logger.error("Failed to check availability for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Inventory service unavailable", e);
        }
    }

    public ReserveInventoryResponse reserveInventory(TenantContext context, String productId, 
                                                   int quantity, String reservationId, long ttlSeconds) {
        try {
            ReserveInventoryRequest request = ReserveInventoryRequest.newBuilder()
                    .setContext(context)
                    .setProductId(productId)
                    .setQuantity(quantity)
                    .setReservationId(reservationId)
                    .setTtlSeconds(ttlSeconds)
                    .build();

            return inventoryServiceStub
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .reserveInventory(request);
        } catch (Exception e) {
            logger.error("Failed to reserve inventory for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Inventory reservation failed", e);
        }
    }

    public ReleaseInventoryResponse releaseInventory(TenantContext context, String reservationId) {
        try {
            ReleaseInventoryRequest request = ReleaseInventoryRequest.newBuilder()
                    .setContext(context)
                    .setReservationId(reservationId)
                    .build();

            return inventoryServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .releaseInventory(request);
        } catch (Exception e) {
            logger.error("Failed to release inventory reservation {}: {}", reservationId, e.getMessage());
            throw new RuntimeException("Inventory release failed", e);
        }
    }
}
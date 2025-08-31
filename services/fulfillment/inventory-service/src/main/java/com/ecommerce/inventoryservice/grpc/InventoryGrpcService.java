package com.ecommerce.inventoryservice.grpc;

import com.ecommerce.inventoryservice.dto.InventoryItemResponse;
import com.ecommerce.inventoryservice.dto.ReservationResponse;
import com.ecommerce.inventoryservice.dto.CreateReservationRequest;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.inventoryservice.service.InventoryReservationService;
import com.ecommerce.inventoryservice.proto.InventoryServiceGrpc;
import com.ecommerce.inventoryservice.proto.InventoryServiceProtos.*;
import com.ecommerce.shared.grpc.TenantContextInterceptor;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService(interceptors = {TenantContextInterceptor.class})
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(InventoryGrpcService.class);

    private final InventoryService inventoryService;
    private final InventoryReservationService reservationService;

    @Autowired
    public InventoryGrpcService(InventoryService inventoryService, 
                               InventoryReservationService reservationService) {
        this.inventoryService = inventoryService;
        this.reservationService = reservationService;
    }

    @Override
    public void checkAvailability(CheckAvailabilityRequest request, StreamObserver<CheckAvailabilityResponse> responseObserver) {
        try {
            logger.debug("gRPC CheckAvailability request for product: {} quantity: {} in tenant: {}", 
                        request.getProductId(), request.getRequestedQuantity(), TenantContext.getTenantId());

            InventoryItemResponse inventory = inventoryService.getInventoryByProductId(request.getProductId());
            
            boolean isAvailable = inventory != null && 
                                inventory.getAvailableQuantity() >= request.getRequestedQuantity();
            int availableQuantity = inventory != null ? inventory.getAvailableQuantity() : 0;

            CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setIsAvailable(isAvailable)
                .setAvailableQuantity(availableQuantity)
                .setProductId(request.getProductId())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error checking availability for product: {}", request.getProductId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void reserveInventory(ReserveInventoryRequest request, StreamObserver<ReserveInventoryResponse> responseObserver) {
        try {
            logger.info("gRPC ReserveInventory request for product: {} quantity: {} reservation: {} in tenant: {}", 
                       request.getProductId(), request.getQuantity(), request.getReservationId(), TenantContext.getTenantId());

            CreateReservationRequest reservationRequest = new CreateReservationRequest();
            reservationRequest.setProductId(request.getProductId());
            reservationRequest.setQuantity(request.getQuantity());
            reservationRequest.setReservationId(request.getReservationId());
            reservationRequest.setTtlSeconds(request.getTtlSeconds());

            ReservationResponse reservation = reservationService.createReservation(reservationRequest);
            
            ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                .setSuccess(reservation != null)
                .setReservationId(request.getReservationId())
                .setErrorMessage(reservation == null ? "Failed to create reservation" : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error reserving inventory for product: {} reservation: {}", 
                        request.getProductId(), request.getReservationId(), e);
            
            ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                .setSuccess(false)
                .setReservationId(request.getReservationId())
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void releaseInventory(ReleaseInventoryRequest request, StreamObserver<ReleaseInventoryResponse> responseObserver) {
        try {
            logger.info("gRPC ReleaseInventory request for reservation: {} in tenant: {}", 
                       request.getReservationId(), TenantContext.getTenantId());

            reservationService.releaseReservation(request.getReservationId());
            
            ReleaseInventoryResponse response = ReleaseInventoryResponse.newBuilder()
                .setSuccess(true)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error releasing inventory reservation: {}", request.getReservationId(), e);
            
            ReleaseInventoryResponse response = ReleaseInventoryResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getStockLevel(GetStockLevelRequest request, StreamObserver<GetStockLevelResponse> responseObserver) {
        try {
            logger.debug("gRPC GetStockLevel request for product: {} in tenant: {}", 
                        request.getProductId(), TenantContext.getTenantId());

            InventoryItemResponse inventory = inventoryService.getInventoryByProductId(request.getProductId());
            
            if (inventory != null) {
                GetStockLevelResponse response = GetStockLevelResponse.newBuilder()
                    .setProductId(request.getProductId())
                    .setAvailableQuantity(inventory.getAvailableQuantity())
                    .setReservedQuantity(inventory.getReservedQuantity())
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Inventory not found for product: " + request.getProductId())
                    .asRuntimeException());
            }

        } catch (Exception e) {
            logger.error("Error getting stock level for product: {}", request.getProductId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }
}
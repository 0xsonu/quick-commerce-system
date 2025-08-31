package com.ecommerce.shippingservice.grpc;

import com.ecommerce.shippingservice.dto.ShipmentResponse;
import com.ecommerce.shippingservice.dto.CreateShipmentRequest;
import com.ecommerce.shippingservice.dto.UpdateShipmentStatusRequest;
import com.ecommerce.shippingservice.dto.TrackingResponse;
import com.ecommerce.shippingservice.service.ShipmentService;
import com.ecommerce.shippingservice.service.ShipmentTrackingService;
import com.ecommerce.shippingservice.proto.ShippingServiceGrpc;
import com.ecommerce.shippingservice.proto.ShippingServiceProtos.*;
import com.ecommerce.shared.grpc.TenantContextInterceptor;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@GrpcService(interceptors = {TenantContextInterceptor.class})
public class ShippingGrpcService extends ShippingServiceGrpc.ShippingServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ShippingGrpcService.class);

    private final ShipmentService shipmentService;
    private final ShipmentTrackingService trackingService;

    @Autowired
    public ShippingGrpcService(ShipmentService shipmentService, ShipmentTrackingService trackingService) {
        this.shipmentService = shipmentService;
        this.trackingService = trackingService;
    }

    @Override
    public void createShipment(com.ecommerce.shippingservice.proto.ShippingServiceProtos.CreateShipmentRequest request, 
                              StreamObserver<CreateShipmentResponse> responseObserver) {
        try {
            logger.info("gRPC CreateShipment request for order: {} carrier: {} in tenant: {}", 
                       request.getOrderId(), request.getCarrier(), TenantContext.getTenantId());

            CreateShipmentRequest serviceRequest = new CreateShipmentRequest();
            serviceRequest.setOrderId(request.getOrderId());
            serviceRequest.setCarrier(request.getCarrier());
            serviceRequest.setServiceType(request.getServiceType());
            
            // Convert shipping address
            if (request.hasShippingAddress()) {
                com.ecommerce.shippingservice.dto.Address address = new com.ecommerce.shippingservice.dto.Address();
                address.setStreetAddress(request.getShippingAddress().getStreetAddress());
                address.setCity(request.getShippingAddress().getCity());
                address.setState(request.getShippingAddress().getState());
                address.setPostalCode(request.getShippingAddress().getPostalCode());
                address.setCountry(request.getShippingAddress().getCountry());
                serviceRequest.setShippingAddress(address);
            }

            // Convert shipment items
            for (ShipmentItem protoItem : request.getItemsList()) {
                com.ecommerce.shippingservice.dto.ShipmentItem item = new com.ecommerce.shippingservice.dto.ShipmentItem();
                item.setProductId(protoItem.getProductId());
                item.setSku(protoItem.getSku());
                item.setProductName(protoItem.getProductName());
                item.setQuantity(protoItem.getQuantity());
                serviceRequest.getItems().add(item);
            }

            ShipmentResponse shipment = shipmentService.createShipment(serviceRequest);
            
            CreateShipmentResponse response = CreateShipmentResponse.newBuilder()
                .setSuccess(shipment != null)
                .setShipmentId(shipment != null ? shipment.getId() : 0)
                .setTrackingNumber(shipment != null ? shipment.getTrackingNumber() : "")
                .setErrorMessage(shipment == null ? "Failed to create shipment" : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error creating shipment for order: {}", request.getOrderId(), e);
            
            CreateShipmentResponse response = CreateShipmentResponse.newBuilder()
                .setSuccess(false)
                .setShipmentId(0)
                .setTrackingNumber("")
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getShipment(com.ecommerce.shippingservice.proto.ShippingServiceProtos.GetShipmentRequest request, 
                           StreamObserver<GetShipmentResponse> responseObserver) {
        try {
            logger.debug("gRPC GetShipment request for shipment: {} in tenant: {}", 
                        request.getShipmentId(), TenantContext.getTenantId());

            ShipmentResponse shipment = shipmentService.getShipmentById(request.getShipmentId());
            
            GetShipmentResponse response = GetShipmentResponse.newBuilder()
                .setShipment(convertToProtoShipment(shipment))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting shipment: {}", request.getShipmentId(), e);
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("Shipment not found: " + request.getShipmentId())
                .asRuntimeException());
        }
    }

    @Override
    public void getShipmentsByOrder(GetShipmentsByOrderRequest request, 
                                   StreamObserver<GetShipmentsByOrderResponse> responseObserver) {
        try {
            logger.debug("gRPC GetShipmentsByOrder request for order: {} in tenant: {}", 
                        request.getOrderId(), TenantContext.getTenantId());

            List<ShipmentResponse> shipments = shipmentService.getShipmentsByOrderId(request.getOrderId());
            
            GetShipmentsByOrderResponse.Builder responseBuilder = GetShipmentsByOrderResponse.newBuilder();
            for (ShipmentResponse shipment : shipments) {
                responseBuilder.addShipments(convertToProtoShipment(shipment));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting shipments for order: {}", request.getOrderId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void updateShipmentStatus(com.ecommerce.shippingservice.proto.ShippingServiceProtos.UpdateShipmentStatusRequest request, 
                                    StreamObserver<UpdateShipmentStatusResponse> responseObserver) {
        try {
            logger.info("gRPC UpdateShipmentStatus request for shipment: {} status: {} in tenant: {}", 
                       request.getShipmentId(), request.getStatus(), TenantContext.getTenantId());

            UpdateShipmentStatusRequest serviceRequest = new UpdateShipmentStatusRequest();
            serviceRequest.setShipmentId(request.getShipmentId());
            serviceRequest.setStatus(request.getStatus());
            serviceRequest.setLocation(request.getLocation());
            serviceRequest.setNotes(request.getNotes());

            shipmentService.updateShipmentStatus(serviceRequest);
            
            UpdateShipmentStatusResponse response = UpdateShipmentStatusResponse.newBuilder()
                .setSuccess(true)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error updating shipment status: {}", request.getShipmentId(), e);
            
            UpdateShipmentStatusResponse response = UpdateShipmentStatusResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void trackShipment(TrackShipmentRequest request, StreamObserver<TrackShipmentResponse> responseObserver) {
        try {
            logger.debug("gRPC TrackShipment request for tracking: {} in tenant: {}", 
                        request.getTrackingNumber(), TenantContext.getTenantId());

            TrackingResponse tracking = trackingService.trackShipment(request.getTrackingNumber());
            
            TrackShipmentResponse.Builder responseBuilder = TrackShipmentResponse.newBuilder()
                .setSuccess(tracking != null);

            if (tracking != null) {
                responseBuilder.setShipment(convertToProtoShipment(tracking.getShipment()));
                
                // Add tracking events
                for (com.ecommerce.shippingservice.dto.TrackingEvent event : tracking.getTrackingEvents()) {
                    responseBuilder.addTrackingEvents(TrackingEvent.newBuilder()
                        .setStatus(event.getStatus())
                        .setLocation(event.getLocation())
                        .setDescription(event.getDescription())
                        .setTimestamp(event.getTimestamp().getEpochSecond())
                        .build());
                }
            } else {
                responseBuilder.setErrorMessage("Tracking information not found");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error tracking shipment: {}", request.getTrackingNumber(), e);
            
            TrackShipmentResponse response = TrackShipmentResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private Shipment convertToProtoShipment(ShipmentResponse shipment) {
        Shipment.Builder shipmentBuilder = Shipment.newBuilder()
            .setId(shipment.getId())
            .setOrderId(shipment.getOrderId())
            .setTrackingNumber(shipment.getTrackingNumber())
            .setCarrier(shipment.getCarrier())
            .setServiceType(shipment.getServiceType())
            .setStatus(shipment.getStatus().name())
            .setCreatedAt(shipment.getCreatedAt().getEpochSecond())
            .setUpdatedAt(shipment.getUpdatedAt().getEpochSecond());

        // Add shipping address
        if (shipment.getShippingAddress() != null) {
            shipmentBuilder.setShippingAddress(CommonProtos.Address.newBuilder()
                .setStreetAddress(shipment.getShippingAddress().getStreetAddress())
                .setCity(shipment.getShippingAddress().getCity())
                .setState(shipment.getShippingAddress().getState())
                .setPostalCode(shipment.getShippingAddress().getPostalCode())
                .setCountry(shipment.getShippingAddress().getCountry())
                .build());
        }

        // Add shipment items
        if (shipment.getItems() != null) {
            for (com.ecommerce.shippingservice.dto.ShipmentItem item : shipment.getItems()) {
                shipmentBuilder.addItems(ShipmentItem.newBuilder()
                    .setProductId(item.getProductId())
                    .setSku(item.getSku())
                    .setProductName(item.getProductName())
                    .setQuantity(item.getQuantity())
                    .build());
            }
        }

        // Add estimated delivery if available
        if (shipment.getEstimatedDelivery() != null) {
            shipmentBuilder.setEstimatedDelivery(shipment.getEstimatedDelivery().getEpochSecond());
        }

        return shipmentBuilder.build();
    }
}
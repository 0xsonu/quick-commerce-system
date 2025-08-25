package com.ecommerce.shippingservice.controller;

import com.ecommerce.shared.utils.response.ApiResponse;
import com.ecommerce.shippingservice.dto.*;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.service.ShipmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentController.class);

    private final ShipmentService shipmentService;

    @Autowired
    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * Create a new shipment
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {
        
        logger.info("Creating shipment for order: {}", request.getOrderId());
        
        try {
            ShipmentResponse shipment = shipmentService.createShipment(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(shipment, "Shipment created successfully"));
        } catch (Exception e) {
            logger.error("Error creating shipment", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to create shipment: " + e.getMessage()));
        }
    }

    /**
     * Get shipment by ID
     */
    @GetMapping("/{shipmentId}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipment(@PathVariable Long shipmentId) {
        logger.info("Getting shipment: {}", shipmentId);
        
        try {
            ShipmentResponse shipment = shipmentService.getShipment(shipmentId);
            return ResponseEntity.ok(ApiResponse.success(shipment));
        } catch (Exception e) {
            logger.error("Error getting shipment: {}", shipmentId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Shipment not found: " + e.getMessage()));
        }
    }

    /**
     * Get shipment by tracking number
     */
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentByTrackingNumber(
            @PathVariable String trackingNumber) {
        
        logger.info("Getting shipment by tracking number: {}", trackingNumber);
        
        try {
            ShipmentResponse shipment = shipmentService.getShipmentByTrackingNumber(trackingNumber);
            return ResponseEntity.ok(ApiResponse.success(shipment));
        } catch (Exception e) {
            logger.error("Error getting shipment by tracking number: {}", trackingNumber, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Shipment not found: " + e.getMessage()));
        }
    }

    /**
     * Get shipments by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getShipmentsByOrderId(@PathVariable Long orderId) {
        logger.info("Getting shipments for order: {}", orderId);
        
        try {
            List<ShipmentResponse> shipments = shipmentService.getShipmentsByOrderId(orderId);
            return ResponseEntity.ok(ApiResponse.success(shipments));
        } catch (Exception e) {
            logger.error("Error getting shipments for order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get shipments: " + e.getMessage()));
        }
    }

    /**
     * Get shipments with pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShipmentResponse>>> getShipments(Pageable pageable) {
        logger.info("Getting shipments with pagination: {}", pageable);
        
        try {
            Page<ShipmentResponse> shipments = shipmentService.getShipments(pageable);
            return ResponseEntity.ok(ApiResponse.success(shipments));
        } catch (Exception e) {
            logger.error("Error getting shipments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get shipments: " + e.getMessage()));
        }
    }

    /**
     * Update shipment status
     */
    @PutMapping("/{shipmentId}/status")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateShipmentStatus(
            @PathVariable Long shipmentId,
            @RequestParam ShipmentStatus status,
            @RequestParam(required = false) String reason) {
        
        logger.info("Updating shipment {} status to: {}", shipmentId, status);
        
        try {
            ShipmentResponse shipment = shipmentService.updateShipmentStatus(shipmentId, status, reason);
            return ResponseEntity.ok(ApiResponse.success(shipment, "Shipment status updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating shipment status: {}", shipmentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to update shipment status: " + e.getMessage()));
        }
    }

    /**
     * Track shipment using carrier API
     */
    @GetMapping("/{trackingNumber}/track")
    public ResponseEntity<ApiResponse<TrackingResponse>> trackShipment(
            @PathVariable String trackingNumber,
            @RequestParam(required = false) String carrierName) {
        
        logger.info("Tracking shipment: {} with carrier: {}", trackingNumber, carrierName);
        
        try {
            TrackingResponse tracking = shipmentService.trackShipment(trackingNumber, carrierName);
            return ResponseEntity.ok(ApiResponse.success(tracking));
        } catch (Exception e) {
            logger.error("Error tracking shipment: {}", trackingNumber, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to track shipment: " + e.getMessage()));
        }
    }

    /**
     * Update tracking information from carrier
     */
    @PostMapping("/{shipmentId}/update-tracking")
    public ResponseEntity<ApiResponse<String>> updateTrackingFromCarrier(@PathVariable Long shipmentId) {
        logger.info("Updating tracking from carrier for shipment: {}", shipmentId);
        
        try {
            shipmentService.updateTrackingFromCarrier(shipmentId);
            return ResponseEntity.ok(ApiResponse.success("Tracking updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating tracking from carrier: {}", shipmentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to update tracking: " + e.getMessage()));
        }
    }

    /**
     * Cancel shipment
     */
    @PostMapping("/{shipmentId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelShipment(
            @PathVariable Long shipmentId,
            @RequestParam(required = false) String reason) {
        
        logger.info("Cancelling shipment: {} with reason: {}", shipmentId, reason);
        
        try {
            boolean cancelled = shipmentService.cancelShipment(shipmentId, reason);
            if (cancelled) {
                return ResponseEntity.ok(ApiResponse.success("Shipment cancelled successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to cancel shipment"));
            }
        } catch (Exception e) {
            logger.error("Error cancelling shipment: {}", shipmentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to cancel shipment: " + e.getMessage()));
        }
    }
}
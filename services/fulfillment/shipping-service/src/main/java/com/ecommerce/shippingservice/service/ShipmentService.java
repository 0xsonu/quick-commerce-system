package com.ecommerce.shippingservice.service;

import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shippingservice.carrier.CarrierService;
import com.ecommerce.shippingservice.carrier.CarrierServiceFactory;
import com.ecommerce.shippingservice.carrier.ResilientCarrierService;
import com.ecommerce.shippingservice.carrier.impl.ResilientFedExCarrierService;
import com.ecommerce.shippingservice.carrier.impl.ResilientUPSCarrierService;
import com.ecommerce.shippingservice.dto.*;
import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentItem;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.entity.ShipmentTracking;
import com.ecommerce.shippingservice.exception.CarrierNotAvailableException;
import com.ecommerce.shippingservice.exception.ShipmentNotFoundException;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ShipmentService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final CarrierServiceFactory carrierServiceFactory;
    private final ShipmentTrackingService trackingService;
    private final ResilientCarrierService resilientCarrierService;

    @Autowired
    public ShipmentService(ShipmentRepository shipmentRepository,
                          CarrierServiceFactory carrierServiceFactory,
                          ShipmentTrackingService trackingService,
                          ResilientCarrierService resilientCarrierService) {
        this.shipmentRepository = shipmentRepository;
        this.carrierServiceFactory = carrierServiceFactory;
        this.trackingService = trackingService;
        this.resilientCarrierService = resilientCarrierService;
    }

    /**
     * Create a new shipment
     */
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        String tenantId = TenantContext.getTenantId();
        logger.info("Creating shipment for order {} with carrier {} for tenant {}", 
                   request.getOrderId(), request.getCarrierName(), tenantId);

        // Get carrier service
        CarrierService carrierService = carrierServiceFactory.getCarrierService(request.getCarrierName())
            .orElseThrow(() -> new CarrierNotAvailableException("Carrier not supported: " + request.getCarrierName()));

        if (!carrierService.isAvailable(tenantId)) {
            throw new CarrierNotAvailableException("Carrier not configured for tenant: " + request.getCarrierName());
        }

        // Create shipment entity
        Shipment shipment = new Shipment();
        shipment.setTenantId(tenantId);
        shipment.setOrderId(request.getOrderId());
        shipment.setShipmentNumber(generateShipmentNumber());
        shipment.setCarrierName(request.getCarrierName());
        shipment.setCarrierService(request.getServiceType());
        shipment.setShippingAddress(request.getShippingAddress());
        shipment.setWeightKg(request.getWeightKg());
        shipment.setDimensionsCm(request.getDimensions());
        shipment.setStatus(ShipmentStatus.CREATED);

        // Add items
        for (ShipmentItemRequest itemRequest : request.getItems()) {
            ShipmentItem item = new ShipmentItem();
            item.setOrderItemId(itemRequest.getOrderItemId());
            item.setProductId(itemRequest.getProductId());
            item.setSku(itemRequest.getSku());
            item.setProductName(itemRequest.getProductName());
            item.setQuantity(itemRequest.getQuantity());
            item.setWeightKg(itemRequest.getWeightKg());
            shipment.addItem(item);
        }

        // Save shipment
        shipment = shipmentRepository.save(shipment);

        // Create shipment with carrier using resilience patterns
        try {
            CreateShipmentResponse carrierResponse = createShipmentWithResilience(carrierService, tenantId, request);
            
            if (carrierResponse.isSuccess()) {
                shipment.setTrackingNumber(carrierResponse.getTrackingNumber());
                shipment.setShippingCost(carrierResponse.getShippingCost());
                shipment.setCurrency(carrierResponse.getCurrency());
                shipment.setEstimatedDeliveryDate(carrierResponse.getEstimatedDeliveryDate());
                
                // Add initial tracking event
                ShipmentTracking initialTracking = new ShipmentTracking(
                    "CREATED",
                    "Shipment created",
                    "Origin facility",
                    LocalDateTime.now()
                );
                shipment.addTrackingEvent(initialTracking);
                
                shipment = shipmentRepository.save(shipment);
                
                logger.info("Shipment created successfully with tracking number: {}", carrierResponse.getTrackingNumber());
            } else {
                logger.error("Failed to create shipment with carrier: {}", carrierResponse.getErrorMessage());
                throw new RuntimeException("Failed to create shipment with carrier: " + carrierResponse.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("Error creating shipment with carrier", e);
            throw new RuntimeException("Failed to create shipment with carrier", e);
        }

        return convertToResponse(shipment);
    }

    /**
     * Get shipment by ID
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));
        
        return convertToResponse(shipment);
    }

    /**
     * Get shipment by tracking number
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber));
        
        return convertToResponse(shipment);
    }

    /**
     * Get shipments by order ID
     */
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsByOrderId(Long orderId) {
        List<Shipment> shipments = shipmentRepository.findByOrderId(orderId);
        return shipments.stream()
            .map(this::convertToResponse)
            .toList();
    }

    /**
     * Get shipments with pagination
     */
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getShipments(Pageable pageable) {
        Page<Shipment> shipments = shipmentRepository.findAll(pageable);
        return shipments.map(this::convertToResponse);
    }

    /**
     * Update shipment status
     */
    public ShipmentResponse updateShipmentStatus(Long shipmentId, ShipmentStatus newStatus, String reason) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));

        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.updateStatus(newStatus);

        // Add tracking event for status change
        ShipmentTracking statusChangeEvent = new ShipmentTracking(
            newStatus.name(),
            reason != null ? reason : "Status updated to " + newStatus.getDisplayName(),
            null,
            LocalDateTime.now()
        );
        shipment.addTrackingEvent(statusChangeEvent);

        shipment = shipmentRepository.save(shipment);

        logger.info("Shipment {} status updated from {} to {}", shipmentId, oldStatus, newStatus);

        return convertToResponse(shipment);
    }

    /**
     * Track shipment using carrier API
     */
    public TrackingResponse trackShipment(String trackingNumber, String carrierName) {
        String tenantId = TenantContext.getTenantId();
        
        // If carrier name not provided, try to find it from our records
        final String finalCarrierName;
        if (carrierName == null) {
            Optional<Shipment> shipment = shipmentRepository.findByTrackingNumber(trackingNumber);
            if (shipment.isPresent()) {
                finalCarrierName = shipment.get().getCarrierName();
            } else {
                throw new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber);
            }
        } else {
            finalCarrierName = carrierName;
        }

        CarrierService carrierService = carrierServiceFactory.getCarrierService(finalCarrierName)
            .orElseThrow(() -> new CarrierNotAvailableException("Carrier not supported: " + finalCarrierName));

        TrackingRequest trackingRequest = new TrackingRequest(trackingNumber, finalCarrierName);
        return trackShipmentWithResilience(carrierService, tenantId, trackingRequest);
    }

    /**
     * Update tracking information from carrier
     */
    public void updateTrackingFromCarrier(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));

        if (shipment.getTrackingNumber() == null) {
            logger.warn("Cannot update tracking for shipment {} - no tracking number", shipmentId);
            return;
        }

        try {
            TrackingResponse trackingResponse = trackShipment(shipment.getTrackingNumber(), shipment.getCarrierName());
            trackingService.updateTrackingFromCarrierResponse(shipment, trackingResponse);
        } catch (Exception e) {
            logger.error("Failed to update tracking for shipment {}", shipmentId, e);
        }
    }

    /**
     * Cancel shipment
     */
    public boolean cancelShipment(Long shipmentId, String reason) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));

        if (shipment.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot cancel shipment in terminal status: " + shipment.getStatus());
        }

        String tenantId = TenantContext.getTenantId();
        CarrierService carrierService = carrierServiceFactory.getCarrierService(shipment.getCarrierName())
            .orElseThrow(() -> new CarrierNotAvailableException("Carrier not supported: " + shipment.getCarrierName()));

        boolean cancelled = false;
        if (shipment.getTrackingNumber() != null) {
            cancelled = cancelShipmentWithResilience(carrierService, tenantId, shipment.getTrackingNumber());
        }

        if (cancelled || shipment.getTrackingNumber() == null) {
            updateShipmentStatus(shipmentId, ShipmentStatus.RETURNED, reason);
            return true;
        }

        return false;
    }

    private String generateShipmentNumber() {
        return "SH" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ShipmentResponse convertToResponse(Shipment shipment) {
        ShipmentResponse response = new ShipmentResponse();
        response.setId(shipment.getId());
        response.setOrderId(shipment.getOrderId());
        response.setShipmentNumber(shipment.getShipmentNumber());
        response.setCarrierName(shipment.getCarrierName());
        response.setCarrierService(shipment.getCarrierService());
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setStatus(shipment.getStatus());
        response.setShippingAddress(shipment.getShippingAddress());
        response.setEstimatedDeliveryDate(shipment.getEstimatedDeliveryDate());
        response.setActualDeliveryDate(shipment.getActualDeliveryDate());
        response.setWeightKg(shipment.getWeightKg());
        response.setDimensionsCm(shipment.getDimensionsCm());
        response.setShippingCost(shipment.getShippingCost());
        response.setCurrency(shipment.getCurrency());
        response.setShippedAt(shipment.getShippedAt());
        response.setDeliveredAt(shipment.getDeliveredAt());
        response.setCreatedAt(shipment.getCreatedAt());
        response.setUpdatedAt(shipment.getUpdatedAt());

        // Convert items
        if (shipment.getItems() != null) {
            List<ShipmentItemResponse> itemResponses = shipment.getItems().stream()
                .map(item -> new ShipmentItemResponse(
                    item.getId(),
                    item.getOrderItemId(),
                    item.getProductId(),
                    item.getSku(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getWeightKg()
                ))
                .toList();
            response.setItems(itemResponses);
        }

        // Convert tracking events
        if (shipment.getTrackingEvents() != null) {
            List<TrackingEventResponse> eventResponses = shipment.getTrackingEvents().stream()
                .map(event -> new TrackingEventResponse(
                    event.getStatus(),
                    event.getDescription(),
                    event.getLocation(),
                    event.getEventTime()
                ))
                .toList();
            response.setTrackingEvents(eventResponses);
        }

        return response;
    }

    /**
     * Create shipment with resilience patterns
     */
    private CreateShipmentResponse createShipmentWithResilience(CarrierService carrierService, String tenantId, CreateShipmentRequest request) {
        return resilientCarrierService.createShipmentWithResilience(carrierService, tenantId, request);
    }

    /**
     * Track shipment with resilience patterns
     */
    private TrackingResponse trackShipmentWithResilience(CarrierService carrierService, String tenantId, TrackingRequest request) {
        return resilientCarrierService.trackShipmentWithResilience(carrierService, tenantId, request);
    }

    /**
     * Cancel shipment with resilience patterns
     */
    private boolean cancelShipmentWithResilience(CarrierService carrierService, String tenantId, String trackingNumber) {
        return resilientCarrierService.cancelShipmentWithResilience(carrierService, tenantId, trackingNumber);
    }

    /**
     * Get shipping rates with resilience patterns
     */
    public List<ShippingRateResponse> getShippingRatesWithResilience(String carrierName, ShippingRateRequest request) {
        String tenantId = TenantContext.getTenantId();
        
        CarrierService carrierService = carrierServiceFactory.getCarrierService(carrierName)
            .orElseThrow(() -> new CarrierNotAvailableException("Carrier not supported: " + carrierName));

        return resilientCarrierService.getShippingRatesWithResilience(carrierService, tenantId, request);
    }

    /**
     * Validate address with resilience patterns
     */
    public boolean validateAddressWithResilience(String carrierName, String address) {
        String tenantId = TenantContext.getTenantId();
        
        CarrierService carrierService = carrierServiceFactory.getCarrierService(carrierName)
            .orElseThrow(() -> new CarrierNotAvailableException("Carrier not supported: " + carrierName));

        return resilientCarrierService.validateAddressWithResilience(carrierService, tenantId, address);
    }
}
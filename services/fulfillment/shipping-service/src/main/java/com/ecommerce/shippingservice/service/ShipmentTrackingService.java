package com.ecommerce.shippingservice.service;

import com.ecommerce.shippingservice.dto.TrackingEventResponse;
import com.ecommerce.shippingservice.dto.TrackingResponse;
import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.entity.ShipmentTracking;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import com.ecommerce.shippingservice.repository.ShipmentTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ShipmentTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentTrackingService.class);

    private final ShipmentTrackingRepository trackingRepository;
    private final ShipmentRepository shipmentRepository;

    @Autowired
    public ShipmentTrackingService(ShipmentTrackingRepository trackingRepository,
                                  ShipmentRepository shipmentRepository) {
        this.trackingRepository = trackingRepository;
        this.shipmentRepository = shipmentRepository;
    }

    /**
     * Add tracking event to shipment
     */
    public void addTrackingEvent(Long shipmentId, String status, String description, String location, LocalDateTime eventTime) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new RuntimeException("Shipment not found: " + shipmentId));

        ShipmentTracking tracking = new ShipmentTracking(status, description, location, eventTime);
        tracking.setShipment(shipment);
        
        trackingRepository.save(tracking);
        
        // Update shipment status if needed
        updateShipmentStatusFromTracking(shipment, tracking);
        
        logger.info("Added tracking event for shipment {}: {} - {}", shipmentId, status, description);
    }

    /**
     * Update tracking information from carrier response
     */
    public void updateTrackingFromCarrierResponse(Shipment shipment, TrackingResponse trackingResponse) {
        logger.info("Updating tracking for shipment {} from carrier response", shipment.getId());

        // Update shipment status based on carrier response
        ShipmentStatus newStatus = mapCarrierStatusToShipmentStatus(trackingResponse.getStatus());
        if (newStatus != shipment.getStatus()) {
            shipment.updateStatus(newStatus);
            shipmentRepository.save(shipment);
        }

        // Add new tracking events
        if (trackingResponse.getEvents() != null) {
            for (TrackingEventResponse event : trackingResponse.getEvents()) {
                // Check if we already have this event
                if (!trackingRepository.existsByCarrierEventId(generateCarrierEventId(event))) {
                    ShipmentTracking tracking = new ShipmentTracking(
                        event.getStatus(),
                        event.getDescription(),
                        event.getLocation(),
                        event.getEventTime(),
                        generateCarrierEventId(event)
                    );
                    tracking.setShipment(shipment);
                    trackingRepository.save(tracking);
                }
            }
        }
    }

    /**
     * Get tracking events for a shipment
     */
    @Transactional(readOnly = true)
    public List<ShipmentTracking> getTrackingEvents(Long shipmentId) {
        return trackingRepository.findByShipmentIdOrderByEventTimeDesc(shipmentId);
    }

    /**
     * Get latest tracking events for a shipment
     */
    @Transactional(readOnly = true)
    public List<ShipmentTracking> getLatestTrackingEvents(Long shipmentId, int limit) {
        List<ShipmentTracking> events = trackingRepository.findLatestTrackingEvents(shipmentId);
        return events.size() > limit ? events.subList(0, limit) : events;
    }

    /**
     * Get tracking events since a specific time
     */
    @Transactional(readOnly = true)
    public List<ShipmentTracking> getTrackingEventsSince(Long shipmentId, LocalDateTime since) {
        return trackingRepository.findTrackingEventsSince(shipmentId, since);
    }

    private void updateShipmentStatusFromTracking(Shipment shipment, ShipmentTracking tracking) {
        ShipmentStatus newStatus = mapTrackingStatusToShipmentStatus(tracking.getStatus());
        
        if (newStatus != null && newStatus != shipment.getStatus()) {
            shipment.updateStatus(newStatus);
            shipmentRepository.save(shipment);
            logger.info("Updated shipment {} status to {} based on tracking event", shipment.getId(), newStatus);
        }
    }

    private ShipmentStatus mapTrackingStatusToShipmentStatus(String trackingStatus) {
        if (trackingStatus == null) {
            return null;
        }

        return switch (trackingStatus.toUpperCase()) {
            case "PICKED_UP" -> ShipmentStatus.PICKED_UP;
            case "IN_TRANSIT" -> ShipmentStatus.IN_TRANSIT;
            case "OUT_FOR_DELIVERY" -> ShipmentStatus.OUT_FOR_DELIVERY;
            case "DELIVERED" -> ShipmentStatus.DELIVERED;
            case "EXCEPTION" -> ShipmentStatus.EXCEPTION;
            case "RETURNED" -> ShipmentStatus.RETURNED;
            default -> null;
        };
    }

    private ShipmentStatus mapCarrierStatusToShipmentStatus(String carrierStatus) {
        if (carrierStatus == null) {
            return ShipmentStatus.CREATED;
        }

        return switch (carrierStatus.toUpperCase()) {
            case "PICKED_UP" -> ShipmentStatus.PICKED_UP;
            case "IN_TRANSIT" -> ShipmentStatus.IN_TRANSIT;
            case "OUT_FOR_DELIVERY" -> ShipmentStatus.OUT_FOR_DELIVERY;
            case "DELIVERED" -> ShipmentStatus.DELIVERED;
            case "EXCEPTION" -> ShipmentStatus.EXCEPTION;
            case "RETURNED" -> ShipmentStatus.RETURNED;
            default -> ShipmentStatus.IN_TRANSIT;
        };
    }

    private String generateCarrierEventId(TrackingEventResponse event) {
        return event.getStatus() + "_" + event.getEventTime().toString() + "_" + 
               (event.getLocation() != null ? event.getLocation().hashCode() : 0);
    }
}
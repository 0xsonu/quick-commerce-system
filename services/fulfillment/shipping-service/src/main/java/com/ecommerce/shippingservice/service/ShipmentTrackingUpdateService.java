package com.ecommerce.shippingservice.service;

import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for scheduled tracking updates from carriers
 */
@Service
public class ShipmentTrackingUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentTrackingUpdateService.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentOrchestrationService orchestrationService;

    @Value("${shipping.tracking-update-batch-size:50}")
    private int batchSize;

    @Value("${shipping.tracking-update-enabled:true}")
    private boolean trackingUpdateEnabled;

    @Autowired
    public ShipmentTrackingUpdateService(ShipmentRepository shipmentRepository,
                                       ShipmentOrchestrationService orchestrationService) {
        this.shipmentRepository = shipmentRepository;
        this.orchestrationService = orchestrationService;
    }

    /**
     * Scheduled task to update tracking information for active shipments
     * Runs every hour by default (configured in application.yml)
     */
    @Scheduled(fixedRateString = "${shipping.tracking-update-interval:3600000}")
    public void updateActiveShipmentTracking() {
        if (!trackingUpdateEnabled) {
            logger.debug("Tracking update is disabled");
            return;
        }

        logger.info("Starting scheduled tracking update for active shipments");

        try {
            // Get active shipments that need tracking updates
            List<ShipmentStatus> activeStatuses = List.of(
                ShipmentStatus.PICKED_UP,
                ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.OUT_FOR_DELIVERY
            );

            int page = 0;
            Page<Shipment> shipmentsPage;
            int totalUpdated = 0;

            do {
                Pageable pageable = PageRequest.of(page, batchSize);
                shipmentsPage = shipmentRepository.findByStatusInAndTrackingNumberIsNotNull(
                    activeStatuses, pageable);

                if (!shipmentsPage.isEmpty()) {
                    // Process batch asynchronously
                    CompletableFuture<Integer> batchResult = processTrackingUpdateBatch(
                        shipmentsPage.getContent());
                    
                    try {
                        totalUpdated += batchResult.get();
                    } catch (Exception e) {
                        logger.error("Error processing tracking update batch {}: {}", page, e.getMessage(), e);
                    }
                }

                page++;
            } while (shipmentsPage.hasNext());

            logger.info("Completed scheduled tracking update. Updated {} shipments", totalUpdated);

        } catch (Exception e) {
            logger.error("Error during scheduled tracking update: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a batch of shipments for tracking updates asynchronously
     */
    @Async
    public CompletableFuture<Integer> processTrackingUpdateBatch(List<Shipment> shipments) {
        logger.debug("Processing tracking update batch of {} shipments", shipments.size());

        int updated = 0;
        for (Shipment shipment : shipments) {
            try {
                orchestrationService.processTrackingUpdate(shipment.getId());
                updated++;
                
                // Small delay to avoid overwhelming carrier APIs
                Thread.sleep(100);
                
            } catch (Exception e) {
                logger.warn("Failed to update tracking for shipment {}: {}", 
                           shipment.getId(), e.getMessage());
            }
        }

        logger.debug("Completed tracking update batch. Updated {} out of {} shipments", 
                    updated, shipments.size());
        
        return CompletableFuture.completedFuture(updated);
    }

    /**
     * Update tracking for a specific shipment
     */
    public void updateShipmentTracking(Long shipmentId) {
        try {
            logger.info("Manually updating tracking for shipment {}", shipmentId);
            orchestrationService.processTrackingUpdate(shipmentId);
        } catch (Exception e) {
            logger.error("Failed to update tracking for shipment {}: {}", shipmentId, e.getMessage(), e);
            throw new RuntimeException("Failed to update shipment tracking", e);
        }
    }

    /**
     * Update tracking for all shipments of a specific order
     */
    public void updateOrderShipmentTracking(Long orderId) {
        try {
            logger.info("Updating tracking for all shipments of order {}", orderId);
            
            List<Shipment> shipments = shipmentRepository.findByOrderId(orderId);
            for (Shipment shipment : shipments) {
                if (shipment.getTrackingNumber() != null && !shipment.getStatus().isTerminal()) {
                    orchestrationService.processTrackingUpdate(shipment.getId());
                }
            }
            
            logger.info("Completed tracking update for {} shipments of order {}", 
                       shipments.size(), orderId);
            
        } catch (Exception e) {
            logger.error("Failed to update tracking for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to update order shipment tracking", e);
        }
    }
}
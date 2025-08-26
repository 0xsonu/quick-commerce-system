package com.ecommerce.shippingservice.service;

import com.ecommerce.shared.models.events.OrderConfirmedEvent;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shippingservice.dto.CreateShipmentRequest;
import com.ecommerce.shippingservice.dto.ShipmentItemRequest;
import com.ecommerce.shippingservice.dto.ShipmentResponse;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.kafka.ShipmentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service that orchestrates shipment creation and status updates
 * Handles order events and coordinates with carrier services
 */
@Service
@Transactional
public class ShipmentOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentOrchestrationService.class);

    private final ShipmentService shipmentService;
    private final ShipmentEventPublisher eventPublisher;

    @Value("${shipping.default-carrier:fedex}")
    private String defaultCarrier;

    @Value("${shipping.default-service:GROUND}")
    private String defaultService;

    @Value("${shipping.auto-ship-on-creation:true}")
    private boolean autoShipOnCreation;

    @Autowired
    public ShipmentOrchestrationService(ShipmentService shipmentService,
                                      ShipmentEventPublisher eventPublisher) {
        this.shipmentService = shipmentService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Process OrderConfirmedEvent to create shipment automatically
     */
    public void processOrderConfirmed(OrderConfirmedEvent event) {
        try {
            logger.info("Processing OrderConfirmedEvent for order {} in tenant {}", 
                       event.getOrderId(), event.getTenantId());

            // Set tenant context
            TenantContext.setTenantId(event.getTenantId());
            TenantContext.setCorrelationId(event.getCorrelationId());

            // Create shipment request from order event
            CreateShipmentRequest shipmentRequest = buildShipmentRequest(event);

            // Create the shipment
            ShipmentResponse shipment = shipmentService.createShipment(shipmentRequest);

            logger.info("Created shipment {} for order {} with tracking number {}", 
                       shipment.getId(), event.getOrderId(), shipment.getTrackingNumber());

            // If auto-ship is enabled, immediately mark as in transit and publish event
            if (autoShipOnCreation && shipment.getTrackingNumber() != null) {
                processShipmentCreated(shipment);
            }

        } catch (Exception e) {
            logger.error("Failed to process OrderConfirmedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create shipment for order: " + event.getOrderId(), e);
        }
    }

    /**
     * Process newly created shipment - update status and publish events
     */
    public void processShipmentCreated(ShipmentResponse shipment) {
        try {
            logger.info("Processing shipment creation for shipment {}", shipment.getId());

            // Update shipment status to IN_TRANSIT (simulating carrier pickup)
            ShipmentResponse updatedShipment = shipmentService.updateShipmentStatus(
                shipment.getId(), 
                ShipmentStatus.IN_TRANSIT, 
                "Shipment picked up by carrier"
            );

            // Publish OrderShippedEvent
            eventPublisher.publishOrderShippedEvent(convertToEntity(updatedShipment));

            logger.info("Published OrderShippedEvent for shipment {} with tracking number {}", 
                       updatedShipment.getId(), updatedShipment.getTrackingNumber());

        } catch (Exception e) {
            logger.error("Failed to process shipment creation for shipment {}: {}", 
                        shipment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Process shipment status update and publish appropriate events
     */
    public void processShipmentStatusUpdate(Long shipmentId, ShipmentStatus oldStatus, ShipmentStatus newStatus) {
        try {
            logger.info("Processing shipment status update for shipment {} from {} to {}", 
                       shipmentId, oldStatus, newStatus);

            ShipmentResponse shipment = shipmentService.getShipment(shipmentId);

            // Publish status update event
            eventPublisher.publishShipmentStatusUpdateEvent(
                convertToEntity(shipment), 
                oldStatus.name(), 
                newStatus.name()
            );

            // Publish specific events based on status
            switch (newStatus) {
                case IN_TRANSIT:
                    if (oldStatus == ShipmentStatus.CREATED || oldStatus == ShipmentStatus.PICKED_UP) {
                        eventPublisher.publishOrderShippedEvent(convertToEntity(shipment));
                    }
                    break;
                case DELIVERED:
                    eventPublisher.publishOrderDeliveredEvent(convertToEntity(shipment));
                    break;
            }

            logger.info("Published events for shipment status update: {} -> {}", oldStatus, newStatus);

        } catch (Exception e) {
            logger.error("Failed to process shipment status update for shipment {}: {}", 
                        shipmentId, e.getMessage(), e);
        }
    }

    /**
     * Process tracking updates from carrier and publish events if status changed
     */
    public void processTrackingUpdate(Long shipmentId) {
        try {
            logger.info("Processing tracking update for shipment {}", shipmentId);

            ShipmentResponse shipmentBefore = shipmentService.getShipment(shipmentId);
            ShipmentStatus oldStatus = shipmentBefore.getStatus();

            // Update tracking from carrier
            shipmentService.updateTrackingFromCarrier(shipmentId);

            // Get updated shipment
            ShipmentResponse shipmentAfter = shipmentService.getShipment(shipmentId);
            ShipmentStatus newStatus = shipmentAfter.getStatus();

            // If status changed, process the update
            if (oldStatus != newStatus) {
                processShipmentStatusUpdate(shipmentId, oldStatus, newStatus);
            }

        } catch (Exception e) {
            logger.error("Failed to process tracking update for shipment {}: {}", 
                        shipmentId, e.getMessage(), e);
        }
    }

    /**
     * Build CreateShipmentRequest from OrderConfirmedEvent
     */
    private CreateShipmentRequest buildShipmentRequest(OrderConfirmedEvent event) {
        CreateShipmentRequest request = new CreateShipmentRequest();
        
        request.setOrderId(Long.parseLong(event.getOrderId()));
        request.setCarrierName(defaultCarrier);
        request.setServiceType(defaultService);
        
        // Convert order items to shipment items
        List<ShipmentItemRequest> shipmentItems = event.getItems().stream()
            .map(this::convertToShipmentItem)
            .toList();
        request.setItems(shipmentItems);

        // Set default shipping address (in real implementation, this would come from order service)
        request.setShippingAddress(buildDefaultShippingAddress());
        
        // Calculate estimated weight based on items (simplified calculation)
        BigDecimal totalWeight = BigDecimal.valueOf(shipmentItems.size() * 0.5); // 0.5kg per item
        request.setWeightKg(totalWeight);
        
        // Set default dimensions
        request.setDimensions("{\"length\": 30, \"width\": 20, \"height\": 10}");

        return request;
    }

    /**
     * Convert OrderItemData to ShipmentItemRequest
     */
    private ShipmentItemRequest convertToShipmentItem(OrderConfirmedEvent.OrderItemData orderItem) {
        ShipmentItemRequest shipmentItem = new ShipmentItemRequest();
        
        // Generate a mock order item ID (in real implementation, this would come from order service)
        shipmentItem.setOrderItemId(System.currentTimeMillis());
        shipmentItem.setProductId(orderItem.getProductId());
        shipmentItem.setSku(orderItem.getSku());
        shipmentItem.setProductName("Product " + orderItem.getProductId()); // Simplified
        shipmentItem.setQuantity(orderItem.getQuantity());
        shipmentItem.setWeightKg(BigDecimal.valueOf(0.5)); // Default weight per item
        
        return shipmentItem;
    }

    /**
     * Build default shipping address (in real implementation, this would come from order service)
     */
    private String buildDefaultShippingAddress() {
        return """
            {
                "name": "Customer Name",
                "street": "123 Main St",
                "city": "Anytown",
                "state": "CA",
                "postalCode": "12345",
                "country": "US",
                "phone": "555-1234"
            }
            """;
    }

    /**
     * Convert ShipmentResponse to Shipment entity (simplified conversion)
     * In a real implementation, you might want to fetch the full entity from repository
     */
    private com.ecommerce.shippingservice.entity.Shipment convertToEntity(ShipmentResponse response) {
        // This is a simplified conversion - in practice you might want to fetch from repository
        com.ecommerce.shippingservice.entity.Shipment shipment = new com.ecommerce.shippingservice.entity.Shipment();
        shipment.setId(response.getId());
        shipment.setTenantId(TenantContext.getTenantId());
        shipment.setOrderId(response.getOrderId());
        shipment.setShipmentNumber(response.getShipmentNumber());
        shipment.setCarrierName(response.getCarrierName());
        shipment.setTrackingNumber(response.getTrackingNumber());
        shipment.setStatus(response.getStatus());
        shipment.setEstimatedDeliveryDate(response.getEstimatedDeliveryDate());
        shipment.setDeliveredAt(response.getDeliveredAt());
        
        // Convert items
        if (response.getItems() != null) {
            response.getItems().forEach(itemResponse -> {
                com.ecommerce.shippingservice.entity.ShipmentItem item = 
                    new com.ecommerce.shippingservice.entity.ShipmentItem();
                item.setId(itemResponse.getId());
                item.setProductId(itemResponse.getProductId());
                item.setSku(itemResponse.getSku());
                item.setQuantity(itemResponse.getQuantity());
                shipment.addItem(item);
            });
        }
        
        return shipment;
    }
}
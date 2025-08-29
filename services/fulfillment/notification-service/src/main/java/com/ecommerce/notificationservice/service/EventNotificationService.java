package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.shared.models.events.*;
import com.ecommerce.shared.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for processing domain events and converting them to notifications
 */
@Service
public class EventNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EventNotificationService.class);

    private final NotificationBatchService notificationBatchService;
    private final NotificationTemplateSelectionService templateSelectionService;

    @Autowired
    public EventNotificationService(NotificationBatchService notificationBatchService,
                                  NotificationTemplateSelectionService templateSelectionService) {
        this.notificationBatchService = notificationBatchService;
        this.templateSelectionService = templateSelectionService;
    }

    /**
     * Process order created event and trigger notifications
     */
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        logger.info("Processing order created event: orderId={}, userId={}", 
                   event.getOrderId(), event.getUserId());

        Map<String, Object> templateVariables = createOrderTemplateVariables(event);
        
        // Create notification requests for different channels
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_CREATED,
                NotificationChannel.EMAIL,
                templateVariables,
                event.getCorrelationId()
            ),
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_CREATED,
                NotificationChannel.SMS,
                templateVariables,
                event.getCorrelationId()
            )
        );

        // Submit for batched processing
        notificationBatchService.submitNotifications(requests);
    }

    /**
     * Process order confirmed event and trigger notifications
     */
    public void processOrderConfirmedEvent(OrderConfirmedEvent event) {
        logger.info("Processing order confirmed event: orderId={}, userId={}", 
                   event.getOrderId(), event.getUserId());

        Map<String, Object> templateVariables = createOrderConfirmedTemplateVariables(event);
        
        // Create notification requests for different channels
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_CONFIRMED,
                NotificationChannel.EMAIL,
                templateVariables,
                event.getCorrelationId()
            ),
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_CONFIRMED,
                NotificationChannel.SMS,
                templateVariables,
                event.getCorrelationId()
            )
        );

        // Submit for batched processing
        notificationBatchService.submitNotifications(requests);
    }

    /**
     * Process order shipped event and trigger notifications
     */
    public void processOrderShippedEvent(OrderShippedEvent event) {
        logger.info("Processing order shipped event: orderId={}, userId={}", 
                   event.getOrderId(), event.getUserId());

        Map<String, Object> templateVariables = createOrderShippedTemplateVariables(event);
        
        // Create notification requests for different channels
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_SHIPPED,
                NotificationChannel.EMAIL,
                templateVariables,
                event.getCorrelationId()
            ),
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_SHIPPED,
                NotificationChannel.SMS,
                templateVariables,
                event.getCorrelationId()
            )
        );

        // Submit for batched processing
        notificationBatchService.submitNotifications(requests);
    }

    /**
     * Process order delivered event and trigger notifications
     */
    public void processOrderDeliveredEvent(OrderDeliveredEvent event) {
        logger.info("Processing order delivered event: orderId={}, userId={}", 
                   event.getOrderId(), event.getUserId());

        Map<String, Object> templateVariables = createOrderDeliveredTemplateVariables(event);
        
        // Create notification requests for different channels
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_DELIVERED,
                NotificationChannel.EMAIL,
                templateVariables,
                event.getCorrelationId()
            ),
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_DELIVERED,
                NotificationChannel.SMS,
                templateVariables,
                event.getCorrelationId()
            )
        );

        // Submit for batched processing
        notificationBatchService.submitNotifications(requests);
    }

    /**
     * Process order cancelled event and trigger notifications
     */
    public void processOrderCancelledEvent(OrderCancelledEvent event) {
        logger.info("Processing order cancelled event: orderId={}, userId={}", 
                   event.getOrderId(), event.getUserId());

        Map<String, Object> templateVariables = createOrderCancelledTemplateVariables(event);
        
        // Create notification requests for different channels
        List<NotificationRequest> requests = List.of(
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_CANCELLED,
                NotificationChannel.EMAIL,
                templateVariables,
                event.getCorrelationId()
            ),
            createNotificationRequest(
                Long.parseLong(event.getUserId()),
                NotificationType.ORDER_CANCELLED,
                NotificationChannel.SMS,
                templateVariables,
                event.getCorrelationId()
            )
        );

        // Submit for batched processing
        notificationBatchService.submitNotifications(requests);
    }

    /**
     * Process inventory low stock event and trigger admin notifications
     */
    public void processInventoryLowStockEvent(InventoryReservationFailedEvent event) {
        logger.info("Processing inventory low stock event: orderId={}", event.getOrderId());

        Map<String, Object> templateVariables = createInventoryLowStockTemplateVariables(event);
        
        // Create notification request for admin users (this would typically query for admin users)
        // For now, we'll create a generic admin notification
        NotificationRequest request = createAdminNotificationRequest(
            NotificationType.INVENTORY_LOW,
            NotificationChannel.EMAIL,
            templateVariables,
            event.getCorrelationId()
        );

        // Submit for batched processing
        notificationBatchService.submitNotifications(List.of(request));
    }

    /**
     * Create notification request with template selection
     */
    private NotificationRequest createNotificationRequest(Long userId, 
                                                        NotificationType notificationType,
                                                        NotificationChannel channel,
                                                        Map<String, Object> templateVariables,
                                                        String correlationId) {
        
        String templateKey = templateSelectionService.selectTemplate(
            TenantContext.getTenantId(), notificationType, channel);
        
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setNotificationType(notificationType);
        request.setChannel(channel);
        request.setTemplateKey(templateKey);
        request.setTemplateVariables(templateVariables);
        request.setMetadata(Map.of("correlationId", correlationId));
        
        return request;
    }

    /**
     * Create admin notification request
     */
    private NotificationRequest createAdminNotificationRequest(NotificationType notificationType,
                                                             NotificationChannel channel,
                                                             Map<String, Object> templateVariables,
                                                             String correlationId) {
        
        String templateKey = templateSelectionService.selectTemplate(
            TenantContext.getTenantId(), notificationType, channel);
        
        NotificationRequest request = new NotificationRequest();
        request.setUserId(0L); // Admin user ID - this should be configurable
        request.setNotificationType(notificationType);
        request.setChannel(channel);
        request.setTemplateKey(templateKey);
        request.setTemplateVariables(templateVariables);
        request.setRecipient("admin@ecommerce.com"); // This should be configurable
        request.setMetadata(Map.of("correlationId", correlationId));
        
        return request;
    }

    /**
     * Create template variables for order events
     */
    private Map<String, Object> createOrderTemplateVariables(OrderCreatedEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("userId", event.getUserId());
        variables.put("totalAmount", event.getTotalAmount());
        variables.put("status", event.getStatus());
        variables.put("itemCount", event.getItems().size());
        variables.put("items", event.getItems());
        return variables;
    }

    /**
     * Create template variables for order confirmed events
     */
    private Map<String, Object> createOrderConfirmedTemplateVariables(OrderConfirmedEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("userId", event.getUserId());
        variables.put("totalAmount", event.getTotalAmount());
        variables.put("paymentId", event.getPaymentId());
        variables.put("itemCount", event.getItems().size());
        variables.put("items", event.getItems());
        return variables;
    }

    /**
     * Create template variables for order shipped events
     */
    private Map<String, Object> createOrderShippedTemplateVariables(OrderShippedEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("userId", event.getUserId());
        variables.put("trackingNumber", event.getTrackingNumber());
        variables.put("carrier", event.getCarrier());
        variables.put("estimatedDeliveryDate", event.getEstimatedDeliveryDate());
        variables.put("itemCount", event.getItems().size());
        variables.put("items", event.getItems());
        return variables;
    }

    /**
     * Create template variables for order delivered events
     */
    private Map<String, Object> createOrderDeliveredTemplateVariables(OrderDeliveredEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("userId", event.getUserId());
        variables.put("itemCount", event.getItems().size());
        variables.put("items", event.getItems());
        return variables;
    }

    /**
     * Create template variables for order cancelled events
     */
    private Map<String, Object> createOrderCancelledTemplateVariables(OrderCancelledEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("userId", event.getUserId());
        variables.put("reason", event.getReason());
        variables.put("itemCount", event.getItems().size());
        variables.put("items", event.getItems());
        return variables;
    }

    /**
     * Create template variables for inventory low stock events
     */
    private Map<String, Object> createInventoryLowStockTemplateVariables(InventoryReservationFailedEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("failedItems", event.getFailedItems());
        variables.put("reason", event.getReason());
        variables.put("itemCount", event.getFailedItems().size());
        return variables;
    }
}
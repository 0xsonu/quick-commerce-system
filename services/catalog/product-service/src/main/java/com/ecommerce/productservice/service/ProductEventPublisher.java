package com.ecommerce.productservice.service;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.shared.models.events.ProductCreatedEvent;
import com.ecommerce.shared.models.events.ProductDeletedEvent;
import com.ecommerce.shared.models.events.ProductUpdatedEvent;
import com.ecommerce.shared.utils.CorrelationIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for publishing product events to Kafka
 */
@Service
public class ProductEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ProductEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.product-events:product-events}")
    private String productEventsTopic;

    @Autowired
    public ProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a ProductCreatedEvent when a new product is created
     */
    public void publishProductCreatedEvent(Product product) {
        try {
            String correlationId = getOrGenerateCorrelationId();
            
            ProductCreatedEvent event = new ProductCreatedEvent(
                product.getTenantId(),
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getSubcategory(),
                product.getBrand(),
                product.getSku(),
                product.getPrice() != null ? product.getPrice().getAmount() : null,
                product.getPrice() != null ? product.getPrice().getCurrency() : null,
                product.getAttributes(),
                product.getStatus() != null ? product.getStatus().name() : null,
                correlationId
            );

            publishEvent(product.getId(), event, "ProductCreated");
            
        } catch (Exception e) {
            logger.error("Failed to publish ProductCreatedEvent for product ID: {}", product.getId(), e);
            // Don't throw exception to avoid breaking the main flow
        }
    }

    /**
     * Publishes a ProductUpdatedEvent when a product is updated
     */
    public void publishProductUpdatedEvent(Product product, Map<String, Object> previousValues, 
                                         Map<String, Object> updatedFields) {
        try {
            String correlationId = getOrGenerateCorrelationId();
            
            ProductUpdatedEvent event = new ProductUpdatedEvent(
                product.getTenantId(),
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getSubcategory(),
                product.getBrand(),
                product.getSku(),
                product.getPrice() != null ? product.getPrice().getAmount() : null,
                product.getPrice() != null ? product.getPrice().getCurrency() : null,
                product.getAttributes(),
                product.getStatus() != null ? product.getStatus().name() : null,
                correlationId,
                previousValues,
                updatedFields
            );

            publishEvent(product.getId(), event, "ProductUpdated");
            
        } catch (Exception e) {
            logger.error("Failed to publish ProductUpdatedEvent for product ID: {}", product.getId(), e);
            // Don't throw exception to avoid breaking the main flow
        }
    }

    /**
     * Publishes a ProductDeletedEvent when a product is deleted
     */
    public void publishProductDeletedEvent(Product product, String deletionReason) {
        try {
            String correlationId = getOrGenerateCorrelationId();
            
            ProductDeletedEvent event = new ProductDeletedEvent(
                product.getTenantId(),
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getSubcategory(),
                product.getBrand(),
                product.getSku(),
                product.getPrice() != null ? product.getPrice().getAmount() : null,
                product.getPrice() != null ? product.getPrice().getCurrency() : null,
                product.getAttributes(),
                product.getStatus() != null ? product.getStatus().name() : null,
                correlationId,
                deletionReason
            );

            publishEvent(product.getId(), event, "ProductDeleted");
            
        } catch (Exception e) {
            logger.error("Failed to publish ProductDeletedEvent for product ID: {}", product.getId(), e);
            // Don't throw exception to avoid breaking the main flow
        }
    }

    /**
     * Generic method to publish events to Kafka
     */
    private void publishEvent(String productId, Object event, String eventType) {
        // Use product ID as the partition key to ensure ordering for the same product
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(productEventsTopic, productId, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Successfully published {} event for product ID: {} to topic: {} with offset: {}",
                    eventType, productId, productEventsTopic, result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to publish {} event for product ID: {} to topic: {}",
                    eventType, productId, productEventsTopic, ex);
            }
        });
    }

    /**
     * Gets correlation ID from MDC or generates a new one
     */
    private String getOrGenerateCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = CorrelationIdGenerator.generate();
        }
        return correlationId;
    }
}
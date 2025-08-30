package com.ecommerce.reviewservice.service;

import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.shared.models.events.*;
import com.ecommerce.shared.utils.CorrelationIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for publishing review-related events to Kafka
 */
@Service
public class ReviewEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ReviewEventPublisher.class);
    
    private static final String REVIEW_EVENTS_TOPIC = "review-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public ReviewEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a ReviewCreatedEvent when a new review is created
     */
    public void publishReviewCreated(Review review) {
        try {
            ReviewCreatedEvent event = new ReviewCreatedEvent(
                review.getTenantId(),
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getVerified()
            );
            
            event.setCorrelationId(CorrelationIdGenerator.generate());
            
            publishEvent(event, "ReviewCreated");
            
        } catch (Exception e) {
            logger.error("Failed to publish ReviewCreatedEvent for review {}: {}", review.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes a ReviewUpdatedEvent when a review is updated
     */
    public void publishReviewUpdated(Review review, Integer previousRating) {
        try {
            ReviewUpdatedEvent event = new ReviewUpdatedEvent(
                review.getTenantId(),
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getVerified(),
                previousRating
            );
            
            event.setCorrelationId(CorrelationIdGenerator.generate());
            
            publishEvent(event, "ReviewUpdated");
            
        } catch (Exception e) {
            logger.error("Failed to publish ReviewUpdatedEvent for review {}: {}", review.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes a ReviewModeratedEvent when a review is approved or rejected
     */
    public void publishReviewModerated(Review review) {
        try {
            ReviewModeratedEvent event = new ReviewModeratedEvent(
                review.getTenantId(),
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getStatus().name(),
                review.getModeratedBy(),
                review.getModerationNotes(),
                review.getRating()
            );
            
            event.setCorrelationId(CorrelationIdGenerator.generate());
            
            publishEvent(event, "ReviewModerated");
            
        } catch (Exception e) {
            logger.error("Failed to publish ReviewModeratedEvent for review {}: {}", review.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes a ReviewFlaggedEvent when a review is flagged
     */
    public void publishReviewFlagged(Review review, Long flaggedBy) {
        try {
            ReviewFlaggedEvent event = new ReviewFlaggedEvent(
                review.getTenantId(),
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getModerationNotes(), // Contains flag reason
                flaggedBy,
                review.getRating()
            );
            
            event.setCorrelationId(CorrelationIdGenerator.generate());
            
            publishEvent(event, "ReviewFlagged");
            
        } catch (Exception e) {
            logger.error("Failed to publish ReviewFlaggedEvent for review {}: {}", review.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes a ReviewDeletedEvent when a review is deleted
     */
    public void publishReviewDeleted(Review review, String deletionReason) {
        try {
            ReviewDeletedEvent event = new ReviewDeletedEvent(
                review.getTenantId(),
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getRating(),
                deletionReason
            );
            
            event.setCorrelationId(CorrelationIdGenerator.generate());
            
            publishEvent(event, "ReviewDeleted");
            
        } catch (Exception e) {
            logger.error("Failed to publish ReviewDeletedEvent for review {}: {}", review.getId(), e.getMessage(), e);
        }
    }

    /**
     * Generic method to publish events to Kafka
     */
    private void publishEvent(DomainEvent event, String eventType) {
        String key = event.getTenantId() + ":" + event.getEventId();
        
        logger.info("Publishing {} event with ID {} for tenant {}", 
                   eventType, event.getEventId(), event.getTenantId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(REVIEW_EVENTS_TOPIC, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Successfully published {} event with ID {} to topic {} at offset {}",
                           eventType, event.getEventId(), REVIEW_EVENTS_TOPIC, result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to publish {} event with ID {}: {}", 
                           eventType, event.getEventId(), ex.getMessage(), ex);
            }
        });
    }
}
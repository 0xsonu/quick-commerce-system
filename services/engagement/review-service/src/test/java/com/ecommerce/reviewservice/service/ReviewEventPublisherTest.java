package com.ecommerce.reviewservice.service;

import com.ecommerce.reviewservice.entity.Review;
import com.ecommerce.reviewservice.entity.ReviewStatus;
import com.ecommerce.shared.models.events.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ReviewEventPublisher eventPublisher;

    private Review testReview;
    private CompletableFuture<SendResult<String, Object>> mockFuture;

    @BeforeEach
    void setUp() {
        testReview = new Review();
        testReview.setId("review123");
        testReview.setTenantId("tenant123");
        testReview.setUserId(1L);
        testReview.setProductId("product123");
        testReview.setRating(5);
        testReview.setTitle("Great product");
        testReview.setComment("Really loved it");
        testReview.setVerified(true);
        testReview.setStatus(ReviewStatus.APPROVED);
        testReview.setCreatedAt(LocalDateTime.now());

        mockFuture = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
    }

    @Test
    void publishReviewCreated_ShouldPublishCorrectEvent() {
        // When
        eventPublisher.publishReviewCreated(testReview);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ReviewCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewCreatedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals("review-events", topicCaptor.getValue());
        assertTrue(keyCaptor.getValue().startsWith("tenant123:"));

        ReviewCreatedEvent event = eventCaptor.getValue();
        assertEquals("review123", event.getReviewId());
        assertEquals("tenant123", event.getTenantId());
        assertEquals(1L, event.getUserId());
        assertEquals("product123", event.getProductId());
        assertEquals(5, event.getRating());
        assertEquals("Great product", event.getTitle());
        assertEquals("Really loved it", event.getComment());
        assertTrue(event.getVerified());
        assertEquals("REVIEW_CREATED", event.getEventType());
        assertNotNull(event.getCorrelationId());
    }

    @Test
    void publishReviewUpdated_ShouldPublishCorrectEvent() {
        // Given
        Integer previousRating = 4;

        // When
        eventPublisher.publishReviewUpdated(testReview, previousRating);

        // Then
        ArgumentCaptor<ReviewUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewUpdatedEvent.class);
        verify(kafkaTemplate).send(eq("review-events"), anyString(), eventCaptor.capture());

        ReviewUpdatedEvent event = eventCaptor.getValue();
        assertEquals("review123", event.getReviewId());
        assertEquals("tenant123", event.getTenantId());
        assertEquals(1L, event.getUserId());
        assertEquals("product123", event.getProductId());
        assertEquals(5, event.getRating());
        assertEquals(4, event.getPreviousRating());
        assertEquals("REVIEW_UPDATED", event.getEventType());
    }

    @Test
    void publishReviewModerated_ShouldPublishCorrectEvent() {
        // Given
        testReview.setModeratedBy(2L);
        testReview.setModerationNotes("Approved after review");

        // When
        eventPublisher.publishReviewModerated(testReview);

        // Then
        ArgumentCaptor<ReviewModeratedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewModeratedEvent.class);
        verify(kafkaTemplate).send(eq("review-events"), anyString(), eventCaptor.capture());

        ReviewModeratedEvent event = eventCaptor.getValue();
        assertEquals("review123", event.getReviewId());
        assertEquals("tenant123", event.getTenantId());
        assertEquals(1L, event.getUserId());
        assertEquals("product123", event.getProductId());
        assertEquals("APPROVED", event.getModerationStatus());
        assertEquals(2L, event.getModeratedBy());
        assertEquals("Approved after review", event.getModerationNotes());
        assertEquals(5, event.getRating());
        assertEquals("REVIEW_MODERATED", event.getEventType());
    }

    @Test
    void publishReviewFlagged_ShouldPublishCorrectEvent() {
        // Given
        testReview.setStatus(ReviewStatus.FLAGGED);
        testReview.setModerationNotes("Inappropriate content");
        Long flaggedBy = 3L;

        // When
        eventPublisher.publishReviewFlagged(testReview, flaggedBy);

        // Then
        ArgumentCaptor<ReviewFlaggedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewFlaggedEvent.class);
        verify(kafkaTemplate).send(eq("review-events"), anyString(), eventCaptor.capture());

        ReviewFlaggedEvent event = eventCaptor.getValue();
        assertEquals("review123", event.getReviewId());
        assertEquals("tenant123", event.getTenantId());
        assertEquals(1L, event.getUserId());
        assertEquals("product123", event.getProductId());
        assertEquals("Inappropriate content", event.getFlagReason());
        assertEquals(3L, event.getFlaggedBy());
        assertEquals(5, event.getRating());
        assertEquals("REVIEW_FLAGGED", event.getEventType());
    }

    @Test
    void publishReviewDeleted_ShouldPublishCorrectEvent() {
        // Given
        String deletionReason = "User requested deletion";

        // When
        eventPublisher.publishReviewDeleted(testReview, deletionReason);

        // Then
        ArgumentCaptor<ReviewDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewDeletedEvent.class);
        verify(kafkaTemplate).send(eq("review-events"), anyString(), eventCaptor.capture());

        ReviewDeletedEvent event = eventCaptor.getValue();
        assertEquals("review123", event.getReviewId());
        assertEquals("tenant123", event.getTenantId());
        assertEquals(1L, event.getUserId());
        assertEquals("product123", event.getProductId());
        assertEquals(5, event.getRating());
        assertEquals("User requested deletion", event.getDeletionReason());
        assertEquals("REVIEW_DELETED", event.getEventType());
    }

    @Test
    void publishEvent_ShouldHandleKafkaException() {
        // Given
        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Kafka error"));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> eventPublisher.publishReviewCreated(testReview));
    }
}
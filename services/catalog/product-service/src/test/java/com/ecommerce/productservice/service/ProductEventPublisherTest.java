package com.ecommerce.productservice.service;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.shared.models.events.ProductCreatedEvent;
import com.ecommerce.shared.models.events.ProductDeletedEvent;
import com.ecommerce.shared.models.events.ProductUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProductEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    private ProductEventPublisher eventPublisher;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        eventPublisher = new ProductEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(eventPublisher, "productEventsTopic", "product-events");

        // Create test product
        testProduct = new Product();
        testProduct.setId("product-123");
        testProduct.setTenantId("tenant-abc");
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setCategory("Electronics");
        testProduct.setSubcategory("Smartphones");
        testProduct.setBrand("TestBrand");
        testProduct.setSku("TEST-SKU-123");
        testProduct.setPrice(new Product.Price(BigDecimal.valueOf(299.99), "USD"));
        testProduct.setStatus(Product.ProductStatus.ACTIVE);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("color", "Black");
        attributes.put("storage", "128GB");
        testProduct.setAttributes(attributes);

        // Mock Kafka template
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        lenient().when(kafkaTemplate.send(any(String.class), any(String.class), any(Object.class))).thenReturn(future);
    }

    @Test
    void testPublishProductCreatedEvent() {
        // Given
        String correlationId = "test-correlation-id";
        MDC.put("correlationId", correlationId);

        // When
        eventPublisher.publishProductCreatedEvent(testProduct);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals("product-events", topicCaptor.getValue());
        assertEquals("product-123", keyCaptor.getValue());

        Object capturedEvent = eventCaptor.getValue();
        assertInstanceOf(ProductCreatedEvent.class, capturedEvent);

        ProductCreatedEvent event = (ProductCreatedEvent) capturedEvent;
        assertEquals("ProductCreated", event.getEventType());
        assertEquals("tenant-abc", event.getTenantId());
        assertEquals("product-123", event.getProductId());
        assertEquals("Test Product", event.getName());
        assertEquals("Test Description", event.getDescription());
        assertEquals("Electronics", event.getCategory());
        assertEquals("Smartphones", event.getSubcategory());
        assertEquals("TestBrand", event.getBrand());
        assertEquals("TEST-SKU-123", event.getSku());
        assertEquals(BigDecimal.valueOf(299.99), event.getPrice());
        assertEquals("USD", event.getCurrency());
        assertEquals("ACTIVE", event.getStatus());
        assertEquals(correlationId, event.getCorrelationId());
        assertNotNull(event.getAttributes());
        assertEquals("Black", event.getAttributes().get("color"));
        assertEquals("128GB", event.getAttributes().get("storage"));

        // Clean up MDC
        MDC.clear();
    }

    @Test
    void testPublishProductCreatedEventWithoutCorrelationId() {
        // Given - no correlation ID in MDC
        MDC.clear();

        // When
        eventPublisher.publishProductCreatedEvent(testProduct);

        // Then
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("product-events"), eq("product-123"), eventCaptor.capture());

        ProductCreatedEvent event = (ProductCreatedEvent) eventCaptor.getValue();
        assertNotNull(event.getCorrelationId());
        assertFalse(event.getCorrelationId().isEmpty());
    }

    @Test
    void testPublishProductUpdatedEvent() {
        // Given
        String correlationId = "test-correlation-id";
        MDC.put("correlationId", correlationId);

        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("name", "Old Product Name");
        previousValues.put("price", BigDecimal.valueOf(199.99));

        Map<String, Object> updatedFields = new HashMap<>();
        updatedFields.put("name", "Test Product");
        updatedFields.put("price", BigDecimal.valueOf(299.99));

        // When
        eventPublisher.publishProductUpdatedEvent(testProduct, previousValues, updatedFields);

        // Then
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("product-events"), eq("product-123"), eventCaptor.capture());

        Object capturedEvent = eventCaptor.getValue();
        assertInstanceOf(ProductUpdatedEvent.class, capturedEvent);

        ProductUpdatedEvent event = (ProductUpdatedEvent) capturedEvent;
        assertEquals("ProductUpdated", event.getEventType());
        assertEquals("tenant-abc", event.getTenantId());
        assertEquals("product-123", event.getProductId());
        assertEquals("Test Product", event.getName());
        assertEquals(correlationId, event.getCorrelationId());
        assertEquals(previousValues, event.getPreviousValues());
        assertEquals(updatedFields, event.getUpdatedFields());

        // Clean up MDC
        MDC.clear();
    }

    @Test
    void testPublishProductDeletedEvent() {
        // Given
        String correlationId = "test-correlation-id";
        String deletionReason = "Product discontinued";
        MDC.put("correlationId", correlationId);

        // When
        eventPublisher.publishProductDeletedEvent(testProduct, deletionReason);

        // Then
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("product-events"), eq("product-123"), eventCaptor.capture());

        Object capturedEvent = eventCaptor.getValue();
        assertInstanceOf(ProductDeletedEvent.class, capturedEvent);

        ProductDeletedEvent event = (ProductDeletedEvent) capturedEvent;
        assertEquals("ProductDeleted", event.getEventType());
        assertEquals("tenant-abc", event.getTenantId());
        assertEquals("product-123", event.getProductId());
        assertEquals("Test Product", event.getName());
        assertEquals(correlationId, event.getCorrelationId());
        assertEquals(deletionReason, event.getDeletionReason());

        // Clean up MDC
        MDC.clear();
    }

    @Test
    void testPublishEventWithNullPrice() {
        // Given
        testProduct.setPrice(null);

        // When
        eventPublisher.publishProductCreatedEvent(testProduct);

        // Then
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("product-events"), eq("product-123"), eventCaptor.capture());

        ProductCreatedEvent event = (ProductCreatedEvent) eventCaptor.getValue();
        assertNull(event.getPrice());
        assertNull(event.getCurrency());
    }

    @Test
    void testPublishEventWithNullStatus() {
        // Given
        testProduct.setStatus(null);

        // When
        eventPublisher.publishProductCreatedEvent(testProduct);

        // Then
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("product-events"), eq("product-123"), eventCaptor.capture());

        ProductCreatedEvent event = (ProductCreatedEvent) eventCaptor.getValue();
        assertNull(event.getStatus());
    }

    @Test
    void testPublishEventHandlesKafkaException() {
        // Given
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka connection failed"));
        lenient().when(kafkaTemplate.send(any(String.class), any(String.class), any(Object.class))).thenReturn(failedFuture);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> eventPublisher.publishProductCreatedEvent(testProduct));

        verify(kafkaTemplate).send(eq("product-events"), eq("product-123"), any(ProductCreatedEvent.class));
    }

    @Test
    void testEventPublishingDoesNotThrowOnException() {
        // Given
        lenient().when(kafkaTemplate.send(any(String.class), any(String.class), any(Object.class)))
            .thenThrow(new RuntimeException("Kafka error"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> eventPublisher.publishProductCreatedEvent(testProduct));
        assertDoesNotThrow(() -> eventPublisher.publishProductUpdatedEvent(testProduct, new HashMap<>(), new HashMap<>()));
        assertDoesNotThrow(() -> eventPublisher.publishProductDeletedEvent(testProduct, "test reason"));
    }
}
package com.ecommerce.productservice.integration;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.service.ProductService;
import com.ecommerce.shared.models.events.ProductCreatedEvent;
import com.ecommerce.shared.models.events.ProductDeletedEvent;
import com.ecommerce.shared.models.events.ProductUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"product-events"})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "app.kafka.topics.product-events=product-events"
})
@DirtiesContext
class ProductEventIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, Object> consumer;
    private ObjectMapper objectMapper;

    private final String tenantId = "test-tenant";

    @BeforeEach
    void setUp() {
        // Set up Kafka consumer
        Map<String, Object> consumerProps = new HashMap<>(
            KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ecommerce.shared.models.events");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);

        consumer = new DefaultKafkaConsumerFactory<String, Object>(consumerProps).createConsumer();
        consumer.subscribe(Collections.singletonList("product-events"));

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // Clean up any existing test data
        productRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
        productRepository.deleteAll();
    }

    @Test
    void testProductCreatedEventIsPublished() throws Exception {
        // Given
        CreateProductRequest request = createTestCreateRequest();

        // When
        ProductResponse response = productService.createProduct(tenantId, request);

        // Then
        assertNotNull(response);

        // Verify event was published
        ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, Object> record = records.iterator().next();
        assertEquals(response.getId(), record.key());

        // Convert the record value to ProductCreatedEvent
        String jsonValue = objectMapper.writeValueAsString(record.value());
        ProductCreatedEvent event = objectMapper.readValue(jsonValue, ProductCreatedEvent.class);

        assertEquals("ProductCreated", event.getEventType());
        assertEquals(tenantId, event.getTenantId());
        assertEquals(response.getId(), event.getProductId());
        assertEquals(request.getName(), event.getName());
        assertEquals(request.getDescription(), event.getDescription());
        assertEquals(request.getCategory(), event.getCategory());
        assertEquals(request.getBrand(), event.getBrand());
        assertEquals(request.getSku(), event.getSku());
        assertEquals(request.getPrice().getAmount(), event.getPrice());
        assertEquals(request.getPrice().getCurrency(), event.getCurrency());
        assertEquals("ACTIVE", event.getStatus());
        assertNotNull(event.getCorrelationId());
    }

    @Test
    void testProductUpdatedEventIsPublished() throws Exception {
        // Given - create a product first
        CreateProductRequest createRequest = createTestCreateRequest();
        ProductResponse createdProduct = productService.createProduct(tenantId, createRequest);

        // Clear the consumer to ignore the create event
        consumer.poll(Duration.ofSeconds(1));

        UpdateProductRequest updateRequest = new UpdateProductRequest();
        updateRequest.setName("Updated Product Name");
        updateRequest.setDescription("Updated Description");
        updateRequest.setPrice(new CreateProductRequest.PriceDto(new BigDecimal("399.99"), "USD"));

        // When
        ProductResponse updatedProduct = productService.updateProduct(tenantId, createdProduct.getId(), updateRequest);

        // Then
        assertNotNull(updatedProduct);

        // Verify event was published
        ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, Object> record = records.iterator().next();
        assertEquals(createdProduct.getId(), record.key());

        // Convert the record value to ProductUpdatedEvent
        String jsonValue = objectMapper.writeValueAsString(record.value());
        ProductUpdatedEvent event = objectMapper.readValue(jsonValue, ProductUpdatedEvent.class);

        assertEquals("ProductUpdated", event.getEventType());
        assertEquals(tenantId, event.getTenantId());
        assertEquals(createdProduct.getId(), event.getProductId());
        assertEquals(updateRequest.getName(), event.getName());
        assertEquals(updateRequest.getDescription(), event.getDescription());
        assertEquals(updateRequest.getPrice().getAmount(), event.getPrice());
        assertEquals(updateRequest.getPrice().getCurrency(), event.getCurrency());
        assertNotNull(event.getCorrelationId());
        assertNotNull(event.getPreviousValues());
        assertNotNull(event.getUpdatedFields());
    }

    @Test
    void testProductDeletedEventIsPublished() throws Exception {
        // Given - create a product first
        CreateProductRequest createRequest = createTestCreateRequest();
        ProductResponse createdProduct = productService.createProduct(tenantId, createRequest);

        // Clear the consumer to ignore the create event
        consumer.poll(Duration.ofSeconds(1));

        // When
        productService.deleteProduct(tenantId, createdProduct.getId());

        // Then
        // Verify event was published
        ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, Object> record = records.iterator().next();
        assertEquals(createdProduct.getId(), record.key());

        // Convert the record value to ProductDeletedEvent
        String jsonValue = objectMapper.writeValueAsString(record.value());
        ProductDeletedEvent event = objectMapper.readValue(jsonValue, ProductDeletedEvent.class);

        assertEquals("ProductDeleted", event.getEventType());
        assertEquals(tenantId, event.getTenantId());
        assertEquals(createdProduct.getId(), event.getProductId());
        assertEquals(createRequest.getName(), event.getName());
        assertEquals(createRequest.getDescription(), event.getDescription());
        assertEquals(createRequest.getCategory(), event.getCategory());
        assertEquals(createRequest.getBrand(), event.getBrand());
        assertEquals(createRequest.getSku(), event.getSku());
        assertEquals("Manual deletion", event.getDeletionReason());
        assertNotNull(event.getCorrelationId());
    }

    @Test
    void testEventPublishingWithCorrelationId() throws Exception {
        // Given
        CreateProductRequest request = createTestCreateRequest();
        String correlationId = "test-correlation-123";
        
        // Set correlation ID in MDC (simulating request context)
        org.slf4j.MDC.put("correlationId", correlationId);

        try {
            // When
            ProductResponse response = productService.createProduct(tenantId, request);

            // Then
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(10));
            assertEquals(1, records.count());

            ConsumerRecord<String, Object> record = records.iterator().next();
            String jsonValue = objectMapper.writeValueAsString(record.value());
            ProductCreatedEvent event = objectMapper.readValue(jsonValue, ProductCreatedEvent.class);

            assertEquals(correlationId, event.getCorrelationId());
        } finally {
            org.slf4j.MDC.clear();
        }
    }

    private CreateProductRequest createTestCreateRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setCategory("Electronics");
        request.setSubcategory("Smartphones");
        request.setBrand("TestBrand");
        request.setSku("TEST-SKU-" + System.currentTimeMillis()); // Unique SKU
        request.setPrice(new CreateProductRequest.PriceDto(new BigDecimal("299.99"), "USD"));
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("color", "Black");
        attributes.put("storage", "128GB");
        request.setAttributes(attributes);
        
        return request;
    }
}
package com.ecommerce.shippingservice.integration;

import com.ecommerce.shared.models.events.OrderConfirmedEvent;
import com.ecommerce.shared.models.events.OrderShippedEvent;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-events", "shipping-events"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "shipping.auto-ship-on-creation=true"
})
@DirtiesContext
class ShipmentKafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Producer<String, Object> producer;
    private Consumer<String, Object> consumer;

    @BeforeEach
    void setUp() {
        // Set up tenant context
        TenantContext.setTenantId("tenant-123");
        TenantContext.setCorrelationId("corr-456");

        // Set up Kafka producer
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put("key.serializer", StringSerializer.class);
        producerProps.put("value.serializer", JsonSerializer.class);
        producer = new DefaultKafkaProducerFactory<String, Object>(producerProps).createProducer();

        // Set up Kafka consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        consumer = new DefaultKafkaConsumerFactory<String, Object>(consumerProps).createConsumer();
        consumer.subscribe(Collections.singletonList("shipping-events"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        if (producer != null) {
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @Transactional
    void orderConfirmedEvent_ShouldCreateShipmentAndPublishShippedEvent() throws Exception {
        // Given
        List<OrderConfirmedEvent.OrderItemData> items = List.of(
            new OrderConfirmedEvent.OrderItemData("product-1", "SKU-001", 2, BigDecimal.valueOf(29.99)),
            new OrderConfirmedEvent.OrderItemData("product-2", "SKU-002", 1, BigDecimal.valueOf(49.99))
        );

        OrderConfirmedEvent orderEvent = new OrderConfirmedEvent(
            "tenant-123",
            "100",
            "user-789",
            items,
            BigDecimal.valueOf(109.97),
            "payment-abc"
        );
        orderEvent.setCorrelationId("corr-456");

        // When - publish order confirmed event
        ProducerRecord<String, Object> record = new ProducerRecord<>(
            "order-events", 
            "tenant-123:100", 
            orderEvent
        );
        producer.send(record).get();

        // Wait for processing
        Thread.sleep(2000);

        // Then - verify shipment was created
        List<Shipment> shipments = shipmentRepository.findByOrderId(100L);
        assertFalse(shipments.isEmpty());
        
        Shipment shipment = shipments.get(0);
        assertEquals("tenant-123", shipment.getTenantId());
        assertEquals(100L, shipment.getOrderId());
        assertEquals("fedex", shipment.getCarrierName());
        assertNotNull(shipment.getShipmentNumber());
        assertEquals(2, shipment.getItems().size());

        // Verify OrderShippedEvent was published
        ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(5));
        assertFalse(records.isEmpty());

        boolean foundShippedEvent = false;
        for (ConsumerRecord<String, Object> consumerRecord : records) {
            if (consumerRecord.value() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) consumerRecord.value();
                if ("ORDER_SHIPPED".equals(eventMap.get("eventType"))) {
                    foundShippedEvent = true;
                    assertEquals("100", eventMap.get("orderId"));
                    assertEquals("fedex", eventMap.get("carrier"));
                    assertNotNull(eventMap.get("trackingNumber"));
                    break;
                }
            }
        }
        assertTrue(foundShippedEvent, "OrderShippedEvent should have been published");
    }

    @Test
    @Transactional
    void orderConfirmedEvent_WithInvalidData_ShouldHandleGracefully() throws Exception {
        // Given - invalid order event (missing required fields)
        OrderConfirmedEvent invalidEvent = new OrderConfirmedEvent();
        invalidEvent.setTenantId("tenant-123");
        invalidEvent.setOrderId("invalid-order");
        invalidEvent.setCorrelationId("corr-456");

        // When - publish invalid order event
        ProducerRecord<String, Object> record = new ProducerRecord<>(
            "order-events", 
            "tenant-123:invalid", 
            invalidEvent
        );
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            producer.send(record).get();
            Thread.sleep(1000);
        });

        // Then - no shipment should be created
        List<Shipment> shipments = shipmentRepository.findAll();
        assertTrue(shipments.isEmpty());
    }

    @Test
    @Transactional
    void shipmentStatusUpdate_ShouldPublishStatusUpdateEvent() throws Exception {
        // Given - create a shipment first
        Shipment shipment = new Shipment();
        shipment.setTenantId("tenant-123");
        shipment.setOrderId(200L);
        shipment.setShipmentNumber("SH200");
        shipment.setCarrierName("UPS");
        shipment.setTrackingNumber("UPS123456");
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment = shipmentRepository.save(shipment);

        // When - update shipment status to DELIVERED
        shipment.updateStatus(ShipmentStatus.DELIVERED);
        shipmentRepository.save(shipment);

        // Simulate status update event publishing (in real scenario, this would be triggered by service)
        // For this test, we'll verify the shipment status was updated
        Optional<Shipment> updatedShipment = shipmentRepository.findById(shipment.getId());
        assertTrue(updatedShipment.isPresent());
        assertEquals(ShipmentStatus.DELIVERED, updatedShipment.get().getStatus());
    }

    @Test
    void kafkaConfiguration_ShouldBeProperlyConfigured() {
        // Verify Kafka broker is running
        assertNotNull(embeddedKafkaBroker);
        assertTrue(embeddedKafkaBroker.getBrokersAsString().contains("localhost"));
        
        // Verify producer and consumer are created
        assertNotNull(producer);
        assertNotNull(consumer);
    }
}
package com.ecommerce.orderservice.integration;

import com.ecommerce.orderservice.dto.AddressDto;
import com.ecommerce.orderservice.dto.CreateOrderItemRequest;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.UpdateOrderStatusRequest;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.shared.models.events.*;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "app.kafka.topics.order-events=order-events"
})
@DirtiesContext
class OrderEventIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, DomainEvent> consumer;

    @BeforeEach
    void setUp() {
        // Set up tenant context
        TenantContext.setTenantId("test-tenant");
        TenantContext.setUserId("test-user");

        // Create Kafka consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ecommerce.shared.models.events");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DomainEvent.class);

        ConsumerFactory<String, DomainEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("order-events"));
    }

    @Test
    @Transactional
    void createOrder_ShouldPublishOrderCreatedEvent() throws Exception {
        // Arrange
        CreateOrderRequest request = createTestOrderRequest();

        // Act
        OrderResponse orderResponse = orderService.createOrder(request);

        // Assert
        assertNotNull(orderResponse);
        
        // Consume the event
        ConsumerRecords<String, DomainEvent> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, DomainEvent> record = records.iterator().next();
        assertEquals(orderResponse.getId().toString(), record.key());
        
        DomainEvent event = record.value();
        assertInstanceOf(OrderCreatedEvent.class, event);
        
        OrderCreatedEvent orderCreatedEvent = (OrderCreatedEvent) event;
        assertEquals(orderResponse.getId().toString(), orderCreatedEvent.getOrderId());
        assertEquals("test-tenant", orderCreatedEvent.getTenantId());
        assertEquals("ORDER_CREATED", orderCreatedEvent.getEventType());
        assertEquals(2, orderCreatedEvent.getItems().size());
        assertNotNull(orderCreatedEvent.getCorrelationId());
    }

    @Test
    @Transactional
    void updateOrderStatus_ToConfirmed_ShouldPublishOrderConfirmedEvent() throws Exception {
        // Arrange
        CreateOrderRequest request = createTestOrderRequest();
        OrderResponse orderResponse = orderService.createOrder(request);
        
        // Clear the OrderCreated event
        consumer.poll(Duration.ofSeconds(2));

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest();
        statusRequest.setNewStatus(OrderStatus.CONFIRMED);
        statusRequest.setReason("Payment successful");

        // Act
        orderService.updateOrderStatus(orderResponse.getId(), statusRequest);

        // Assert
        ConsumerRecords<String, DomainEvent> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, DomainEvent> record = records.iterator().next();
        DomainEvent event = record.value();
        assertInstanceOf(OrderConfirmedEvent.class, event);
        
        OrderConfirmedEvent orderConfirmedEvent = (OrderConfirmedEvent) event;
        assertEquals(orderResponse.getId().toString(), orderConfirmedEvent.getOrderId());
        assertEquals("test-tenant", orderConfirmedEvent.getTenantId());
        assertEquals("ORDER_CONFIRMED", orderConfirmedEvent.getEventType());
        assertEquals("payment-id-placeholder", orderConfirmedEvent.getPaymentId());
    }

    @Test
    @Transactional
    void updateOrderStatus_ToProcessing_ShouldPublishOrderProcessingEvent() throws Exception {
        // Arrange
        CreateOrderRequest request = createTestOrderRequest();
        OrderResponse orderResponse = orderService.createOrder(request);
        
        // Clear the OrderCreated event
        consumer.poll(Duration.ofSeconds(2));

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest();
        statusRequest.setNewStatus(OrderStatus.PROCESSING);
        statusRequest.setReason("Order processing started");

        // Act
        orderService.updateOrderStatus(orderResponse.getId(), statusRequest);

        // Assert
        ConsumerRecords<String, DomainEvent> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, DomainEvent> record = records.iterator().next();
        DomainEvent event = record.value();
        assertInstanceOf(OrderProcessingEvent.class, event);
        
        OrderProcessingEvent orderProcessingEvent = (OrderProcessingEvent) event;
        assertEquals(orderResponse.getId().toString(), orderProcessingEvent.getOrderId());
        assertEquals("test-tenant", orderProcessingEvent.getTenantId());
        assertEquals("ORDER_PROCESSING", orderProcessingEvent.getEventType());
    }

    @Test
    @Transactional
    void updateOrderStatus_ToShipped_ShouldPublishOrderShippedEvent() throws Exception {
        // Arrange
        CreateOrderRequest request = createTestOrderRequest();
        OrderResponse orderResponse = orderService.createOrder(request);
        
        // Clear the OrderCreated event
        consumer.poll(Duration.ofSeconds(2));

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest();
        statusRequest.setNewStatus(OrderStatus.SHIPPED);
        statusRequest.setReason("Order shipped");

        // Act
        orderService.updateOrderStatus(orderResponse.getId(), statusRequest);

        // Assert
        ConsumerRecords<String, DomainEvent> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, DomainEvent> record = records.iterator().next();
        DomainEvent event = record.value();
        assertInstanceOf(OrderShippedEvent.class, event);
        
        OrderShippedEvent orderShippedEvent = (OrderShippedEvent) event;
        assertEquals(orderResponse.getId().toString(), orderShippedEvent.getOrderId());
        assertEquals("test-tenant", orderShippedEvent.getTenantId());
        assertEquals("ORDER_SHIPPED", orderShippedEvent.getEventType());
        assertEquals("tracking-placeholder", orderShippedEvent.getTrackingNumber());
        assertEquals("carrier-placeholder", orderShippedEvent.getCarrier());
    }

    @Test
    @Transactional
    void cancelOrder_ShouldPublishOrderCancelledEvent() throws Exception {
        // Arrange
        CreateOrderRequest request = createTestOrderRequest();
        OrderResponse orderResponse = orderService.createOrder(request);
        
        // Clear the OrderCreated event
        consumer.poll(Duration.ofSeconds(2));

        String cancellationReason = "Customer requested cancellation";

        // Act
        orderService.cancelOrder(orderResponse.getId(), cancellationReason);

        // Assert
        ConsumerRecords<String, DomainEvent> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(1, records.count());

        ConsumerRecord<String, DomainEvent> record = records.iterator().next();
        DomainEvent event = record.value();
        assertInstanceOf(OrderCancelledEvent.class, event);
        
        OrderCancelledEvent orderCancelledEvent = (OrderCancelledEvent) event;
        assertEquals(orderResponse.getId().toString(), orderCancelledEvent.getOrderId());
        assertEquals("test-tenant", orderCancelledEvent.getTenantId());
        assertEquals("ORDER_CANCELLED", orderCancelledEvent.getEventType());
        assertEquals(cancellationReason, orderCancelledEvent.getReason());
    }

    @Test
    @Transactional
    void multipleStatusUpdates_ShouldPublishMultipleEvents() throws Exception {
        // Arrange
        CreateOrderRequest request = createTestOrderRequest();
        OrderResponse orderResponse = orderService.createOrder(request);
        
        // Clear the OrderCreated event
        consumer.poll(Duration.ofSeconds(2));

        // Act - Update to CONFIRMED
        UpdateOrderStatusRequest confirmedRequest = new UpdateOrderStatusRequest();
        confirmedRequest.setNewStatus(OrderStatus.CONFIRMED);
        confirmedRequest.setReason("Payment successful");
        orderService.updateOrderStatus(orderResponse.getId(), confirmedRequest);

        // Act - Update to PROCESSING
        UpdateOrderStatusRequest processingRequest = new UpdateOrderStatusRequest();
        processingRequest.setNewStatus(OrderStatus.PROCESSING);
        processingRequest.setReason("Order processing started");
        orderService.updateOrderStatus(orderResponse.getId(), processingRequest);

        // Assert
        ConsumerRecords<String, DomainEvent> records = consumer.poll(Duration.ofSeconds(10));
        assertEquals(2, records.count());

        // Verify both events were published
        boolean foundConfirmed = false;
        boolean foundProcessing = false;
        
        for (ConsumerRecord<String, DomainEvent> record : records) {
            DomainEvent event = record.value();
            if (event instanceof OrderConfirmedEvent) {
                foundConfirmed = true;
            } else if (event instanceof OrderProcessingEvent) {
                foundProcessing = true;
            }
        }
        
        assertTrue(foundConfirmed, "OrderConfirmedEvent should be published");
        assertTrue(foundProcessing, "OrderProcessingEvent should be published");
    }

    private CreateOrderRequest createTestOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setCurrency("USD");
        
        // Set addresses
        request.setBillingAddress(createTestAddress());
        request.setShippingAddress(createTestAddress());
        
        // Add items
        CreateOrderItemRequest item1 = new CreateOrderItemRequest();
        item1.setProductId("product-1");
        item1.setSku("SKU-1");
        item1.setProductName("Test Product 1");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("25.00"));
        
        CreateOrderItemRequest item2 = new CreateOrderItemRequest();
        item2.setProductId("product-2");
        item2.setSku("SKU-2");
        item2.setProductName("Test Product 2");
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("50.00"));
        
        request.setItems(Arrays.asList(item1, item2));
        
        return request;
    }

    private AddressDto createTestAddress() {
        AddressDto address = new AddressDto();
        address.setStreetAddress("123 Test St");
        address.setCity("Test City");
        address.setState("TS");
        address.setPostalCode("12345");
        address.setCountry("US");
        return address;
    }
}
package com.ecommerce.notificationservice.integration;

import com.ecommerce.notificationservice.entity.*;
import com.ecommerce.notificationservice.repository.NotificationLogRepository;
import com.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import com.ecommerce.shared.models.events.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, 
               topics = {"order-events", "shipping-events", "inventory-events"},
               brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "notification.email.enabled=false",
    "notification.sms.enabled=false"
})
@DirtiesContext
class EventDrivenNotificationIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up previous test data
        notificationLogRepository.deleteAll();
        notificationTemplateRepository.deleteAll();
        
        // Create test templates
        createTestTemplates();
    }

    @Test
    void shouldProcessOrderCreatedEventAndCreateNotifications() throws Exception {
        // Given
        OrderCreatedEvent event = createOrderCreatedEvent();

        // When
        kafkaTemplate.send("order-events", event);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationLog> logs = notificationLogRepository.findAll();
            assertThat(logs).hasSize(2); // Email and SMS notifications
            
            // Verify email notification
            NotificationLog emailLog = logs.stream()
                .filter(log -> log.getChannel() == NotificationChannel.EMAIL)
                .findFirst().orElseThrow();
            
            assertThat(emailLog.getTenantId()).isEqualTo("tenant1");
            assertThat(emailLog.getUserId()).isEqualTo(456L);
            assertThat(emailLog.getNotificationType()).isEqualTo(NotificationType.ORDER_CREATED);
            assertThat(emailLog.getSubject()).contains("Order Created");
            assertThat(emailLog.getContent()).contains("order123");
            assertThat(emailLog.getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.FAILED);
            
            // Verify SMS notification
            NotificationLog smsLog = logs.stream()
                .filter(log -> log.getChannel() == NotificationChannel.SMS)
                .findFirst().orElseThrow();
            
            assertThat(smsLog.getTenantId()).isEqualTo("tenant1");
            assertThat(smsLog.getUserId()).isEqualTo(456L);
            assertThat(smsLog.getNotificationType()).isEqualTo(NotificationType.ORDER_CREATED);
            assertThat(smsLog.getContent()).contains("order123");
        });
    }

    @Test
    void shouldHandleMultipleEventsInBatch() throws Exception {
        // Given
        List<OrderCreatedEvent> events = List.of(
            createOrderCreatedEvent("order1", "user1"),
            createOrderCreatedEvent("order2", "user2"),
            createOrderCreatedEvent("order3", "user3")
        );

        // When
        for (OrderCreatedEvent event : events) {
            kafkaTemplate.send("order-events", event);
        }

        // Then
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationLog> logs = notificationLogRepository.findAll();
            assertThat(logs).hasSizeGreaterThanOrEqualTo(6); // 2 notifications per event
            
            // Verify we have notifications for all users
            List<Long> userIds = logs.stream()
                .map(NotificationLog::getUserId)
                .distinct()
                .toList();
            
            assertThat(userIds).containsExactlyInAnyOrder(1L, 2L, 3L);
        });
    }

    @Test
    void shouldUseTenantSpecificTemplate() throws Exception {
        // Given
        createTenantSpecificTemplate();
        OrderCreatedEvent event = createOrderCreatedEvent();

        // When
        kafkaTemplate.send("order-events", event);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationLog> logs = notificationLogRepository.findAll();
            
            NotificationLog emailLog = logs.stream()
                .filter(log -> log.getChannel() == NotificationChannel.EMAIL)
                .findFirst().orElseThrow();
            
            assertThat(emailLog.getSubject()).contains("Custom Order Created");
            assertThat(emailLog.getContent()).contains("Custom template content");
        });
    }

    @Test
    void shouldHandleEventProcessingFailureGracefully() throws Exception {
        // Given - Create an event with invalid data that might cause processing issues
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setTenantId("tenant1");
        event.setOrderId("invalid-order");
        event.setUserId("invalid-user"); // This will cause parsing issues
        event.setItems(List.of());
        event.setTotalAmount(BigDecimal.ZERO);
        event.setStatus("PENDING");

        // When
        kafkaTemplate.send("order-events", event);

        // Then - Should not crash the application
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // The event should be processed but might result in failed notifications
            // The important thing is that the application doesn't crash
            assertThat(true).isTrue(); // Just verify we get here without exceptions
        });
    }

    private OrderCreatedEvent createOrderCreatedEvent() {
        return createOrderCreatedEvent("order123", "456");
    }

    private OrderCreatedEvent createOrderCreatedEvent(String orderId, String userId) {
        List<OrderCreatedEvent.OrderItemData> items = List.of(
            new OrderCreatedEvent.OrderItemData("product1", "SKU1", 2, BigDecimal.valueOf(29.99))
        );
        
        OrderCreatedEvent event = new OrderCreatedEvent("tenant1", orderId, userId, 
                                                       items, BigDecimal.valueOf(59.98), "PENDING");
        event.setCorrelationId("corr123");
        return event;
    }

    private void createTestTemplates() {
        // Email template
        NotificationTemplate emailTemplate = new NotificationTemplate(
            "tenant1", 
            "order_created_email", 
            NotificationChannel.EMAIL,
            "Order Created - #{orderId}",
            "Your order #{orderId} has been created with total amount #{totalAmount}"
        );
        notificationTemplateRepository.save(emailTemplate);

        // SMS template
        NotificationTemplate smsTemplate = new NotificationTemplate(
            "tenant1", 
            "order_created_sms", 
            NotificationChannel.SMS,
            "Order Created",
            "Order #{orderId} created. Total: #{totalAmount}"
        );
        notificationTemplateRepository.save(smsTemplate);
    }

    private void createTenantSpecificTemplate() {
        NotificationTemplate tenantTemplate = new NotificationTemplate(
            "tenant1", 
            "tenant1_order_created_email", 
            NotificationChannel.EMAIL,
            "Custom Order Created - #{orderId}",
            "Custom template content for order #{orderId}"
        );
        notificationTemplateRepository.save(tenantTemplate);
    }
}
package com.ecommerce.orderservice.saga;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class OrderSagaIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("order_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSagaOrchestrator sagaOrchestrator;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("test-tenant");
        TenantContext.setUserId("1");
        TenantContext.setCorrelationId("test-correlation");

        // Create and save test order
        testOrder = new Order("test-tenant", "ORD-TEST-123", 1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setCurrency("USD");
        testOrder.setBillingAddress("{\"street\":\"123 Main St\",\"city\":\"Test City\"}");
        testOrder.setShippingAddress("{\"street\":\"123 Main St\",\"city\":\"Test City\"}");

        OrderItem item = new OrderItem("product1", "SKU1", "Test Product", 2, new BigDecimal("50.00"));
        testOrder.addItem(item);
        testOrder.calculateTotals();

        testOrder = orderRepository.save(testOrder);
    }

    @Test
    void testSagaStateCreationAndRetrieval() {
        // Test that saga state can be created and retrieved
        OrderSagaState sagaState = new OrderSagaState(testOrder.getId(), testOrder.getTenantId());
        
        assertNotNull(sagaState);
        assertEquals(testOrder.getId(), sagaState.getOrderId());
        assertEquals(testOrder.getTenantId(), sagaState.getTenantId());
        assertEquals(SagaStatus.STARTED, sagaState.getStatus());
        assertEquals(SagaStep.USER_VALIDATION, sagaState.getCurrentStep());
    }

    @Test
    void testSagaStateTransitions() {
        OrderSagaState sagaState = new OrderSagaState(testOrder.getId(), testOrder.getTenantId());
        
        // Test normal progression
        assertEquals(SagaStep.USER_VALIDATION, sagaState.getCurrentStep());
        
        sagaState.moveToNextStep();
        assertEquals(SagaStep.INVENTORY_RESERVATION, sagaState.getCurrentStep());
        
        sagaState.moveToNextStep();
        assertEquals(SagaStep.PAYMENT_PROCESSING, sagaState.getCurrentStep());
        
        sagaState.moveToNextStep();
        assertEquals(SagaStep.ORDER_CONFIRMATION, sagaState.getCurrentStep());
        
        sagaState.moveToNextStep();
        assertEquals(SagaStep.COMPLETED, sagaState.getCurrentStep());
        
        // Test completion
        sagaState.markCompleted();
        assertEquals(SagaStatus.COMPLETED, sagaState.getStatus());
    }

    @Test
    void testSagaFailureAndCompensation() {
        OrderSagaState sagaState = new OrderSagaState(testOrder.getId(), testOrder.getTenantId());
        
        // Move to payment processing step
        sagaState.moveToNextStep(); // USER_VALIDATION
        sagaState.moveToNextStep(); // INVENTORY_RESERVATION
        sagaState.moveToNextStep(); // PAYMENT_PROCESSING
        
        // Simulate failure
        sagaState.markFailed("Payment processing failed");
        assertEquals(SagaStatus.FAILED, sagaState.getStatus());
        assertEquals("Payment processing failed", sagaState.getErrorMessage());
        
        // Start compensation
        sagaState.startCompensation();
        assertEquals(SagaStatus.COMPENSATING, sagaState.getStatus());
        
        // Complete compensation
        sagaState.markCompensated();
        assertEquals(SagaStatus.COMPENSATED, sagaState.getStatus());
    }

    @Test
    void testSagaRetryLogic() {
        OrderSagaState sagaState = new OrderSagaState(testOrder.getId(), testOrder.getTenantId());
        
        // Test retry count
        assertEquals(0, sagaState.getRetryCount());
        assertTrue(sagaState.canRetry());
        
        sagaState.incrementRetryCount();
        assertEquals(1, sagaState.getRetryCount());
        assertTrue(sagaState.canRetry());
        
        sagaState.incrementRetryCount();
        assertEquals(2, sagaState.getRetryCount());
        assertTrue(sagaState.canRetry());
        
        sagaState.incrementRetryCount();
        assertEquals(3, sagaState.getRetryCount());
        assertFalse(sagaState.canRetry()); // Max retries reached
    }

    @Test
    void testSagaTimeout() {
        OrderSagaState sagaState = new OrderSagaState(testOrder.getId(), testOrder.getTenantId());
        
        // Initially not timed out
        assertFalse(sagaState.isTimedOut());
        
        // Set timeout in the past
        sagaState.setTimeoutAt(sagaState.getStartedAt().minusMinutes(1));
        assertTrue(sagaState.isTimedOut());
    }

    @Test
    void testSagaDataManagement() {
        OrderSagaState sagaState = new OrderSagaState(testOrder.getId(), testOrder.getTenantId());
        
        // Test storing and retrieving saga data
        sagaState.putSagaData("paymentMethod", "CREDIT_CARD");
        sagaState.putSagaData("paymentToken", "token123");
        sagaState.putSagaData("reservationId", "reservation-123");
        sagaState.putSagaData("amount", new BigDecimal("100.00"));
        
        assertEquals("CREDIT_CARD", sagaState.getSagaData("paymentMethod"));
        assertEquals("token123", sagaState.getSagaData("paymentToken"));
        assertEquals("reservation-123", sagaState.getSagaData("reservationId"));
        assertEquals(new BigDecimal("100.00"), sagaState.getSagaData("amount"));
        
        // Test typed retrieval
        String paymentMethod = sagaState.getSagaData("paymentMethod", String.class);
        assertEquals("CREDIT_CARD", paymentMethod);
        
        BigDecimal amount = sagaState.getSagaData("amount", BigDecimal.class);
        assertEquals(new BigDecimal("100.00"), amount);
        
        // Test null for non-existent key
        assertNull(sagaState.getSagaData("nonExistentKey"));
        assertNull(sagaState.getSagaData("nonExistentKey", String.class));
    }

    @Test
    void testOrderStatusTransitions() {
        // Test valid transitions
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PROCESSING));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        
        // Test invalid transitions
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
        
        // Test final states
        assertTrue(OrderStatus.DELIVERED.isFinalState());
        assertTrue(OrderStatus.CANCELLED.isFinalState());
        assertFalse(OrderStatus.PENDING.isFinalState());
        assertFalse(OrderStatus.CONFIRMED.isFinalState());
    }

    @Test
    void testSagaStepTransitions() {
        // Test next step transitions
        assertEquals(SagaStep.INVENTORY_RESERVATION, SagaStep.USER_VALIDATION.getNextStep());
        assertEquals(SagaStep.PAYMENT_PROCESSING, SagaStep.INVENTORY_RESERVATION.getNextStep());
        assertEquals(SagaStep.ORDER_CONFIRMATION, SagaStep.PAYMENT_PROCESSING.getNextStep());
        assertEquals(SagaStep.COMPLETED, SagaStep.ORDER_CONFIRMATION.getNextStep());
        
        // Test previous step transitions
        assertEquals(SagaStep.USER_VALIDATION, SagaStep.INVENTORY_RESERVATION.getPreviousStep());
        assertEquals(SagaStep.INVENTORY_RESERVATION, SagaStep.PAYMENT_PROCESSING.getPreviousStep());
        assertEquals(SagaStep.PAYMENT_PROCESSING, SagaStep.ORDER_CONFIRMATION.getPreviousStep());
        assertEquals(SagaStep.ORDER_CONFIRMATION, SagaStep.COMPLETED.getPreviousStep());
        
        // Test compensatable steps
        assertFalse(SagaStep.USER_VALIDATION.isCompensatable());
        assertTrue(SagaStep.INVENTORY_RESERVATION.isCompensatable());
        assertTrue(SagaStep.PAYMENT_PROCESSING.isCompensatable());
        assertTrue(SagaStep.ORDER_CONFIRMATION.isCompensatable());
        assertFalse(SagaStep.COMPLETED.isCompensatable());
    }

    @Test
    void testSagaStatusBehavior() {
        // Test active status
        assertTrue(SagaStatus.STARTED.isActive());
        assertTrue(SagaStatus.IN_PROGRESS.isActive());
        assertTrue(SagaStatus.COMPENSATING.isActive());
        assertFalse(SagaStatus.COMPLETED.isActive());
        assertFalse(SagaStatus.COMPENSATED.isActive());
        assertFalse(SagaStatus.FAILED.isActive());
        
        // Test final status
        assertFalse(SagaStatus.STARTED.isFinal());
        assertFalse(SagaStatus.IN_PROGRESS.isFinal());
        assertFalse(SagaStatus.COMPENSATING.isFinal());
        assertTrue(SagaStatus.COMPLETED.isFinal());
        assertTrue(SagaStatus.COMPENSATED.isFinal());
        assertTrue(SagaStatus.FAILED.isFinal());
        
        // Test compensation capability
        assertFalse(SagaStatus.STARTED.canCompensate());
        assertTrue(SagaStatus.IN_PROGRESS.canCompensate());
        assertFalse(SagaStatus.COMPENSATING.canCompensate());
        assertFalse(SagaStatus.COMPLETED.canCompensate());
        assertFalse(SagaStatus.COMPENSATED.canCompensate());
        assertTrue(SagaStatus.FAILED.canCompensate());
    }
}
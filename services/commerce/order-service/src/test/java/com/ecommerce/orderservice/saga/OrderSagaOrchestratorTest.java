package com.ecommerce.orderservice.saga;

import com.ecommerce.inventoryservice.proto.InventoryServiceGrpc;
import com.ecommerce.inventoryservice.proto.InventoryServiceProtos.*;
import com.ecommerce.orderservice.client.InventoryServiceClient;
import com.ecommerce.orderservice.client.PaymentServiceClient;
import com.ecommerce.orderservice.client.UserServiceClient;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.paymentservice.proto.PaymentServiceGrpc;
import com.ecommerce.paymentservice.proto.PaymentServiceProtos.*;
import com.ecommerce.shared.proto.CommonProtos.Money;
import com.ecommerce.userservice.proto.UserServiceGrpc;
import com.ecommerce.userservice.proto.UserServiceProtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private OrderRepository orderRepository;

    private OrderSagaOrchestrator sagaOrchestrator;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        sagaOrchestrator = new OrderSagaOrchestrator(
            userServiceClient,
            inventoryServiceClient,
            paymentServiceClient,
            orderRepository
        );

        // Create test order
        testOrder = new Order("tenant1", "ORD-123", 1L);
        testOrder.setId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setCurrency("USD");

        OrderItem item = new OrderItem("product1", "SKU1", "Test Product", 2, new BigDecimal("50.00"));
        testOrder.addItem(item);
        testOrder.calculateTotals();
    }

    @Test
    void testSuccessfulSagaExecution() throws Exception {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Mock successful user validation
        ValidateUserResponse userValidation = ValidateUserResponse.newBuilder()
            .setIsValid(true)
            .setIsActive(true)
            .build();
        when(userServiceClient.validateUser(any(), eq(1L))).thenReturn(userValidation);

        // Mock successful inventory reservation
        ReserveInventoryResponse inventoryReservation = ReserveInventoryResponse.newBuilder()
            .setSuccess(true)
            .setReservationId("reservation-123")
            .build();
        when(inventoryServiceClient.reserveInventory(any(), eq("product1"), eq(2), anyString(), anyLong()))
            .thenReturn(inventoryReservation);

        // Mock successful payment processing
        ProcessPaymentResponse paymentResponse = ProcessPaymentResponse.newBuilder()
            .setSuccess(true)
            .setPaymentId("payment-123")
            .setTransactionId("txn-123")
            .setStatus("COMPLETED")
            .build();
        when(paymentServiceClient.processPayment(any(), eq(1L), any(Money.class), anyString(), anyString(), anyString()))
            .thenReturn(paymentResponse);

        // Mock order save for confirmation
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        CompletableFuture<Boolean> result = sagaOrchestrator.processOrder(1L, "CREDIT_CARD", "token123");

        // Assert
        assertTrue(result.get());
        
        OrderSagaState sagaState = sagaOrchestrator.getSagaState(1L);
        assertNotNull(sagaState);
        assertEquals(SagaStatus.COMPLETED, sagaState.getStatus());
        assertEquals(SagaStep.COMPLETED, sagaState.getCurrentStep());

        // Verify all services were called
        verify(userServiceClient).validateUser(any(), eq(1L));
        verify(inventoryServiceClient).reserveInventory(any(), eq("product1"), eq(2), anyString(), anyLong());
        verify(paymentServiceClient).processPayment(any(), eq(1L), any(Money.class), anyString(), anyString(), anyString());
        verify(orderRepository, atLeast(1)).save(any(Order.class));
    }

    @Test
    void testSagaFailureWithCompensation() throws Exception {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Mock successful user validation
        ValidateUserResponse userValidation = ValidateUserResponse.newBuilder()
            .setIsValid(true)
            .setIsActive(true)
            .build();
        when(userServiceClient.validateUser(any(), eq(1L))).thenReturn(userValidation);

        // Mock successful inventory reservation
        ReserveInventoryResponse inventoryReservation = ReserveInventoryResponse.newBuilder()
            .setSuccess(true)
            .setReservationId("reservation-123")
            .build();
        when(inventoryServiceClient.reserveInventory(any(), eq("product1"), eq(2), anyString(), anyLong()))
            .thenReturn(inventoryReservation);

        // Mock failed payment processing
        ProcessPaymentResponse paymentResponse = ProcessPaymentResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage("Payment declined")
            .build();
        when(paymentServiceClient.processPayment(any(), eq(1L), any(Money.class), anyString(), anyString(), anyString()))
            .thenReturn(paymentResponse);

        // Mock successful inventory release (compensation)
        ReleaseInventoryResponse releaseResponse = ReleaseInventoryResponse.newBuilder()
            .setSuccess(true)
            .build();
        when(inventoryServiceClient.releaseInventory(any(), anyString()))
            .thenReturn(releaseResponse);

        // Mock order save for cancellation
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        CompletableFuture<Boolean> result = sagaOrchestrator.processOrder(1L, "CREDIT_CARD", "token123");

        // Assert
        assertFalse(result.get());
        
        OrderSagaState sagaState = sagaOrchestrator.getSagaState(1L);
        assertNotNull(sagaState);
        assertEquals(SagaStatus.COMPENSATED, sagaState.getStatus());

        // Verify compensation was executed
        verify(inventoryServiceClient).releaseInventory(any(), anyString());
        verify(orderRepository, atLeast(1)).save(any(Order.class));
    }

    @Test
    void testUserValidationFailure() throws Exception {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Mock failed user validation
        ValidateUserResponse userValidation = ValidateUserResponse.newBuilder()
            .setIsValid(false)
            .setIsActive(false)
            .build();
        when(userServiceClient.validateUser(any(), eq(1L))).thenReturn(userValidation);

        // Mock order save for cancellation
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        CompletableFuture<Boolean> result = sagaOrchestrator.processOrder(1L, "CREDIT_CARD", "token123");

        // Assert
        assertFalse(result.get());
        
        OrderSagaState sagaState = sagaOrchestrator.getSagaState(1L);
        assertNotNull(sagaState);
        assertEquals(SagaStatus.COMPENSATED, sagaState.getStatus());

        // Verify only user validation was called (accounting for retries)
        verify(userServiceClient, atLeastOnce()).validateUser(any(), eq(1L));
        verifyNoInteractions(inventoryServiceClient);
        verifyNoInteractions(paymentServiceClient);
    }

    @Test
    void testInventoryReservationFailure() throws Exception {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Mock successful user validation
        ValidateUserResponse userValidation = ValidateUserResponse.newBuilder()
            .setIsValid(true)
            .setIsActive(true)
            .build();
        when(userServiceClient.validateUser(any(), eq(1L))).thenReturn(userValidation);

        // Mock failed inventory reservation
        ReserveInventoryResponse inventoryReservation = ReserveInventoryResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage("Insufficient inventory")
            .build();
        when(inventoryServiceClient.reserveInventory(any(), eq("product1"), eq(2), anyString(), anyLong()))
            .thenReturn(inventoryReservation);

        // Mock order save for cancellation
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        CompletableFuture<Boolean> result = sagaOrchestrator.processOrder(1L, "CREDIT_CARD", "token123");

        // Assert
        assertFalse(result.get());
        
        OrderSagaState sagaState = sagaOrchestrator.getSagaState(1L);
        assertNotNull(sagaState);
        assertEquals(SagaStatus.COMPENSATED, sagaState.getStatus());

        // Verify services were called appropriately (accounting for retries)
        verify(userServiceClient, atLeastOnce()).validateUser(any(), eq(1L));
        verify(inventoryServiceClient, atLeastOnce()).reserveInventory(any(), eq("product1"), eq(2), anyString(), anyLong());
        verifyNoInteractions(paymentServiceClient);
    }

    @Test
    void testSagaRetryMechanism() throws Exception {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Mock user validation that fails first time, succeeds second time
        ValidateUserResponse failedValidation = ValidateUserResponse.newBuilder()
            .setIsValid(false)
            .setIsActive(false)
            .build();
        ValidateUserResponse successValidation = ValidateUserResponse.newBuilder()
            .setIsValid(true)
            .setIsActive(true)
            .build();
        
        when(userServiceClient.validateUser(any(), eq(1L)))
            .thenReturn(failedValidation)
            .thenReturn(failedValidation)
            .thenReturn(successValidation);

        // Mock successful inventory reservation
        ReserveInventoryResponse inventoryReservation = ReserveInventoryResponse.newBuilder()
            .setSuccess(true)
            .setReservationId("reservation-123")
            .build();
        when(inventoryServiceClient.reserveInventory(any(), eq("product1"), eq(2), anyString(), anyLong()))
            .thenReturn(inventoryReservation);

        // Mock successful payment processing
        ProcessPaymentResponse paymentResponse = ProcessPaymentResponse.newBuilder()
            .setSuccess(true)
            .setPaymentId("payment-123")
            .setTransactionId("txn-123")
            .setStatus("COMPLETED")
            .build();
        when(paymentServiceClient.processPayment(any(), eq(1L), any(Money.class), anyString(), anyString(), anyString()))
            .thenReturn(paymentResponse);

        // Mock order save
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        CompletableFuture<Boolean> result = sagaOrchestrator.processOrder(1L, "CREDIT_CARD", "token123");

        // Assert
        assertTrue(result.get());
        
        OrderSagaState sagaState = sagaOrchestrator.getSagaState(1L);
        assertNotNull(sagaState);
        assertEquals(SagaStatus.COMPLETED, sagaState.getStatus());

        // Verify retry happened
        verify(userServiceClient, times(3)).validateUser(any(), eq(1L));
    }

    @Test
    void testSagaTimeout() {
        // Arrange
        OrderSagaState sagaState = new OrderSagaState(1L, "tenant1");
        sagaState.setStatus(SagaStatus.IN_PROGRESS); // Set to active status
        sagaState.setTimeoutAt(sagaState.getStartedAt().minusMinutes(1)); // Set timeout in the past
        
        // Mock order repository for compensation
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        
        // Simulate adding the saga state
        sagaOrchestrator.getSagaStates().put(1L, sagaState);

        // Act
        sagaOrchestrator.handleTimeouts();

        // Assert - after timeout and compensation, status should be COMPENSATED
        assertEquals(SagaStatus.COMPENSATED, sagaState.getStatus());
        assertEquals("Saga timeout", sagaState.getErrorMessage());
    }

    @Test
    void testSagaStateManagement() {
        // Test saga state creation and transitions
        OrderSagaState sagaState = new OrderSagaState(1L, "tenant1");
        
        assertEquals(SagaStatus.STARTED, sagaState.getStatus());
        assertEquals(SagaStep.USER_VALIDATION, sagaState.getCurrentStep());
        
        sagaState.moveToNextStep();
        assertEquals(SagaStep.INVENTORY_RESERVATION, sagaState.getCurrentStep());
        
        sagaState.moveToNextStep();
        assertEquals(SagaStep.PAYMENT_PROCESSING, sagaState.getCurrentStep());
        
        sagaState.moveToNextStep();
        assertEquals(SagaStep.ORDER_CONFIRMATION, sagaState.getCurrentStep());
        
        sagaState.moveToNextStep();
        assertEquals(SagaStep.COMPLETED, sagaState.getCurrentStep());
        
        sagaState.markCompleted();
        assertEquals(SagaStatus.COMPLETED, sagaState.getStatus());
    }

    @Test
    void testSagaDataStorage() {
        OrderSagaState sagaState = new OrderSagaState(1L, "tenant1");
        
        sagaState.putSagaData("paymentMethod", "CREDIT_CARD");
        sagaState.putSagaData("paymentToken", "token123");
        sagaState.putSagaData("reservationId", "reservation-123");
        
        assertEquals("CREDIT_CARD", sagaState.getSagaData("paymentMethod"));
        assertEquals("token123", sagaState.getSagaData("paymentToken"));
        assertEquals("reservation-123", sagaState.getSagaData("reservationId"));
        
        String paymentMethod = sagaState.getSagaData("paymentMethod", String.class);
        assertEquals("CREDIT_CARD", paymentMethod);
    }
}
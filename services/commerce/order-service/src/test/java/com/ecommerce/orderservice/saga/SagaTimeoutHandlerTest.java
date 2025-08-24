package com.ecommerce.orderservice.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaTimeoutHandlerTest {

    @Mock
    private OrderSagaOrchestrator sagaOrchestrator;

    private SagaTimeoutHandler timeoutHandler;

    @BeforeEach
    void setUp() {
        timeoutHandler = new SagaTimeoutHandler(sagaOrchestrator);
    }

    @Test
    void testHandleTimeouts() {
        // Act
        timeoutHandler.handleTimeouts();

        // Assert
        verify(sagaOrchestrator).handleTimeouts();
    }

    @Test
    void testHandleTimeoutsWithException() {
        // Arrange
        doThrow(new RuntimeException("Test exception")).when(sagaOrchestrator).handleTimeouts();

        // Act & Assert - should not throw exception
        timeoutHandler.handleTimeouts();

        // Verify the method was called despite the exception
        verify(sagaOrchestrator).handleTimeouts();
    }

    @Test
    void testCleanupCompletedSagas() {
        // Act
        timeoutHandler.cleanupCompletedSagas();

        // Assert
        verify(sagaOrchestrator).cleanupCompletedSagas();
    }

    @Test
    void testCleanupCompletedSagasWithException() {
        // Arrange
        doThrow(new RuntimeException("Test exception")).when(sagaOrchestrator).cleanupCompletedSagas();

        // Act & Assert - should not throw exception
        timeoutHandler.cleanupCompletedSagas();

        // Verify the method was called despite the exception
        verify(sagaOrchestrator).cleanupCompletedSagas();
    }
}
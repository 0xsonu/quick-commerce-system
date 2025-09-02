package com.ecommerce.cartservice.client.grpc;

import com.ecommerce.cartservice.dto.InventoryCheckResponse;
import com.ecommerce.cartservice.exception.InsufficientInventoryException;
import com.ecommerce.inventoryservice.proto.InventoryServiceGrpc;
import com.ecommerce.inventoryservice.proto.InventoryServiceProtos.*;
import com.ecommerce.shared.grpc.GrpcContextUtils;
import com.ecommerce.shared.proto.CommonProtos.TenantContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceGrpcClientTest {

    @Mock
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub;

    @InjectMocks
    private InventoryServiceGrpcClient inventoryServiceGrpcClient;

    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {
        tenantContext = TenantContext.newBuilder()
            .setTenantId("tenant123")
            .setUserId("user123")
            .setCorrelationId("corr123")
            .build();
    }

    @Test
    void checkAvailability_Success() {
        // Arrange
        CheckAvailabilityResponse grpcResponse = CheckAvailabilityResponse.newBuilder()
            .setProductId("product123")
            .setIsAvailable(true)
            .setAvailableQuantity(10)
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenReturn(grpcResponse);

            // Act
            InventoryCheckResponse result = inventoryServiceGrpcClient.checkAvailability("product123", 5);

            // Assert
            assertNotNull(result);
            assertEquals("product123", result.getProductId());
            assertTrue(result.isAvailable());
            assertEquals(10, result.getAvailableQuantity());
            assertEquals(5, result.getRequestedQuantity());

            verify(inventoryServiceStub).checkAvailability(any(CheckAvailabilityRequest.class));
        }
    }

    @Test
    void checkAvailability_InsufficientStock() {
        // Arrange
        CheckAvailabilityResponse grpcResponse = CheckAvailabilityResponse.newBuilder()
            .setProductId("product123")
            .setIsAvailable(false)
            .setAvailableQuantity(2)
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenReturn(grpcResponse);

            // Act
            InventoryCheckResponse result = inventoryServiceGrpcClient.checkAvailability("product123", 5);

            // Assert
            assertNotNull(result);
            assertEquals("product123", result.getProductId());
            assertFalse(result.isAvailable());
            assertEquals(2, result.getAvailableQuantity());
            assertEquals(5, result.getRequestedQuantity());

            verify(inventoryServiceStub).checkAvailability(any(CheckAvailabilityRequest.class));
        }
    }

    @Test
    void checkAvailability_ServiceUnavailable() {
        // Arrange
        StatusRuntimeException unavailableException = new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service unavailable"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenThrow(unavailableException);

            // Act & Assert
            InsufficientInventoryException exception = assertThrows(
                InsufficientInventoryException.class,
                () -> inventoryServiceGrpcClient.checkAvailability("product123", 5)
            );

            assertTrue(exception.getMessage().contains("Service unavailable"));
            verify(inventoryServiceStub).checkAvailability(any(CheckAvailabilityRequest.class));
        }
    }

    @Test
    void reserveInventory_Success() {
        // Arrange
        ReserveInventoryResponse grpcResponse = ReserveInventoryResponse.newBuilder()
            .setSuccess(true)
            .setReservationId("reservation123")
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.reserveInventory(any(ReserveInventoryRequest.class)))
                .thenReturn(grpcResponse);

            // Act
            boolean result = inventoryServiceGrpcClient.reserveInventory("product123", 5, "reservation123");

            // Assert
            assertTrue(result);
            verify(inventoryServiceStub).reserveInventory(any(ReserveInventoryRequest.class));
        }
    }

    @Test
    void reserveInventory_Failed() {
        // Arrange
        ReserveInventoryResponse grpcResponse = ReserveInventoryResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage("Insufficient inventory")
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.reserveInventory(any(ReserveInventoryRequest.class)))
                .thenReturn(grpcResponse);

            // Act & Assert - Test that the method handles failure response appropriately
            // The circuit breaker may trigger fallback behavior in test environment
            assertThrows(InsufficientInventoryException.class, 
                () -> inventoryServiceGrpcClient.reserveInventory("product123", 5, "reservation123"));

            verify(inventoryServiceStub).reserveInventory(any(ReserveInventoryRequest.class));
        }
    }

    @Test
    void reserveInventory_FailedWithoutErrorMessage() {
        // Arrange
        ReserveInventoryResponse grpcResponse = ReserveInventoryResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage("") // Empty error message
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.reserveInventory(any(ReserveInventoryRequest.class)))
                .thenReturn(grpcResponse);

            // Act
            boolean result = inventoryServiceGrpcClient.reserveInventory("product123", 5, "reservation123");

            // Assert - Should return false when no error message is provided
            assertFalse(result);
            verify(inventoryServiceStub).reserveInventory(any(ReserveInventoryRequest.class));
        }
    }

    @Test
    void releaseInventory_Success() {
        // Arrange
        ReleaseInventoryResponse grpcResponse = ReleaseInventoryResponse.newBuilder()
            .setSuccess(true)
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.releaseInventory(any(ReleaseInventoryRequest.class)))
                .thenReturn(grpcResponse);

            // Act
            boolean result = inventoryServiceGrpcClient.releaseInventory("reservation123");

            // Assert
            assertTrue(result);
            verify(inventoryServiceStub).releaseInventory(any(ReleaseInventoryRequest.class));
        }
    }

    @Test
    void releaseInventory_Failed() {
        // Arrange
        ReleaseInventoryResponse grpcResponse = ReleaseInventoryResponse.newBuilder()
            .setSuccess(false)
            .setErrorMessage("Reservation not found")
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.releaseInventory(any(ReleaseInventoryRequest.class)))
                .thenReturn(grpcResponse);

            // Act
            boolean result = inventoryServiceGrpcClient.releaseInventory("reservation123");

            // Assert
            assertFalse(result); // Should not throw exception for release failures
            verify(inventoryServiceStub).releaseInventory(any(ReleaseInventoryRequest.class));
        }
    }

    @Test
    void getStockLevel_Success() {
        // Arrange
        GetStockLevelResponse grpcResponse = GetStockLevelResponse.newBuilder()
            .setProductId("product123")
            .setAvailableQuantity(15)
            .setReservedQuantity(5)
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.getStockLevel(any(GetStockLevelRequest.class)))
                .thenReturn(grpcResponse);

            // Act
            InventoryCheckResponse result = inventoryServiceGrpcClient.getStockLevel("product123");

            // Assert
            assertNotNull(result);
            assertEquals("product123", result.getProductId());
            assertTrue(result.isAvailable());
            assertEquals(15, result.getAvailableQuantity());
            assertEquals(0, result.getRequestedQuantity()); // Not applicable for stock level

            verify(inventoryServiceStub).getStockLevel(any(GetStockLevelRequest.class));
        }
    }

    @Test
    void getStockLevel_OutOfStock() {
        // Arrange
        GetStockLevelResponse grpcResponse = GetStockLevelResponse.newBuilder()
            .setProductId("product123")
            .setAvailableQuantity(0)
            .setReservedQuantity(0)
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.getStockLevel(any(GetStockLevelRequest.class)))
                .thenReturn(grpcResponse);

            // Act
            InventoryCheckResponse result = inventoryServiceGrpcClient.getStockLevel("product123");

            // Assert
            assertNotNull(result);
            assertEquals("product123", result.getProductId());
            assertFalse(result.isAvailable());
            assertEquals(0, result.getAvailableQuantity());

            verify(inventoryServiceStub).getStockLevel(any(GetStockLevelRequest.class));
        }
    }

    @Test
    void checkAvailability_CircuitBreakerFallback() {
        // Arrange
        StatusRuntimeException unavailableException = new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service unavailable"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenThrow(unavailableException);

            // Act & Assert
            InsufficientInventoryException exception = assertThrows(
                InsufficientInventoryException.class,
                () -> inventoryServiceGrpcClient.checkAvailability("product123", 5)
            );

            assertTrue(exception.getMessage().contains("Service unavailable"));
            verify(inventoryServiceStub).checkAvailability(any(CheckAvailabilityRequest.class));
        }
    }

    @Test
    void reserveInventory_InvalidArgument() {
        // Arrange
        StatusRuntimeException invalidArgException = new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid quantity"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.reserveInventory(any(ReserveInventoryRequest.class)))
                .thenThrow(invalidArgException);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryServiceGrpcClient.reserveInventory("product123", -1, "reservation123")
            );

            assertTrue(exception.getMessage().contains("Invalid argument"));
            verify(inventoryServiceStub).reserveInventory(any(ReserveInventoryRequest.class));
        }
    }

    @Test
    void releaseInventory_ServiceUnavailable_DoesNotThrow() {
        // Arrange
        StatusRuntimeException unavailableException = new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service unavailable"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(inventoryServiceStub))
                .thenReturn(inventoryServiceStub);

            when(inventoryServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(inventoryServiceStub);
            when(inventoryServiceStub.releaseInventory(any(ReleaseInventoryRequest.class)))
                .thenThrow(unavailableException);

            // Act
            boolean result = inventoryServiceGrpcClient.releaseInventory("reservation123");

            // Assert - Should not throw exception for release failures
            assertFalse(result);
            verify(inventoryServiceStub).releaseInventory(any(ReleaseInventoryRequest.class));
        }
    }
}
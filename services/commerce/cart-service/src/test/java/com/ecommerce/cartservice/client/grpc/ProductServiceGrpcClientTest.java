package com.ecommerce.cartservice.client.grpc;

import com.ecommerce.cartservice.dto.ProductValidationResponse;
import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.productservice.proto.ProductServiceGrpc;
import com.ecommerce.productservice.proto.ProductServiceProtos.*;
import com.ecommerce.shared.grpc.GrpcContextUtils;
import com.ecommerce.shared.proto.CommonProtos.TenantContext;
import com.ecommerce.shared.proto.CommonProtos.Money;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceGrpcClientTest {

    @Mock
    private ProductServiceGrpc.ProductServiceBlockingStub productServiceStub;

    @InjectMocks
    private ProductServiceGrpcClient productServiceGrpcClient;

    private ValidateProductResponse validGrpcResponse;
    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {
        tenantContext = TenantContext.newBuilder()
            .setTenantId("tenant123")
            .setUserId("user123")
            .setCorrelationId("corr123")
            .build();

        Money price = Money.newBuilder()
            .setAmountCents(2999) // $29.99
            .setCurrency("USD")
            .build();

        validGrpcResponse = ValidateProductResponse.newBuilder()
            .setProductId("product123")
            .setSku("SKU123")
            .setName("Test Product")
            .setIsValid(true)
            .setIsActive(true)
            .setPrice(price)
            .build();
    }

    @Test
    void validateProduct_Success() {
        // Arrange
        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.validateProduct(any(ValidateProductRequest.class)))
                .thenReturn(validGrpcResponse);

            // Act
            ProductValidationResponse result = productServiceGrpcClient.validateProduct("product123", "SKU123");

            // Assert
            assertNotNull(result);
            assertEquals("product123", result.getProductId());
            assertEquals("SKU123", result.getSku());
            assertEquals("Test Product", result.getName());
            assertTrue(result.isValid());
            assertTrue(result.isActive());
            assertEquals(new BigDecimal("29.99"), result.getPrice());
            assertEquals("USD", result.getCurrency());

            verify(productServiceStub).validateProduct(any(ValidateProductRequest.class));
        }
    }

    @Test
    void validateProduct_NotFound() {
        // Arrange
        StatusRuntimeException notFoundException = new StatusRuntimeException(Status.NOT_FOUND.withDescription("Product not found"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.validateProduct(any(ValidateProductRequest.class)))
                .thenThrow(notFoundException);

            // Act & Assert
            ProductNotAvailableException exception = assertThrows(
                ProductNotAvailableException.class,
                () -> productServiceGrpcClient.validateProduct("product123", "SKU123")
            );

            assertTrue(exception.getMessage().contains("Product not found"));
            verify(productServiceStub).validateProduct(any(ValidateProductRequest.class));
        }
    }

    @Test
    void validateProduct_ServiceUnavailable() {
        // Arrange
        StatusRuntimeException unavailableException = new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service unavailable"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.validateProduct(any(ValidateProductRequest.class)))
                .thenThrow(unavailableException);

            // Act & Assert
            ProductNotAvailableException exception = assertThrows(
                ProductNotAvailableException.class,
                () -> productServiceGrpcClient.validateProduct("product123", "SKU123")
            );

            assertTrue(exception.getMessage().contains("Service unavailable"));
            verify(productServiceStub).validateProduct(any(ValidateProductRequest.class));
        }
    }

    @Test
    void validateProduct_DeadlineExceeded() {
        // Arrange
        StatusRuntimeException timeoutException = new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Request timeout"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.validateProduct(any(ValidateProductRequest.class)))
                .thenThrow(timeoutException);

            // Act & Assert
            ProductNotAvailableException exception = assertThrows(
                ProductNotAvailableException.class,
                () -> productServiceGrpcClient.validateProduct("product123", "SKU123")
            );

            assertTrue(exception.getMessage().contains("Request timeout"));
            verify(productServiceStub).validateProduct(any(ValidateProductRequest.class));
        }
    }

    @Test
    void validateProduct_UnexpectedError() {
        // Arrange
        RuntimeException unexpectedException = new RuntimeException("Unexpected error");

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.validateProduct(any(ValidateProductRequest.class)))
                .thenThrow(unexpectedException);

            // Act & Assert
            ProductNotAvailableException exception = assertThrows(
                ProductNotAvailableException.class,
                () -> productServiceGrpcClient.validateProduct("product123", "SKU123")
            );

            assertTrue(exception.getMessage().contains("unexpected error"));
            verify(productServiceStub).validateProduct(any(ValidateProductRequest.class));
        }
    }

    @Test
    void getProduct_Success() {
        // Arrange
        Product product = Product.newBuilder()
            .setId("product123")
            .setSku("SKU123")
            .setName("Test Product")
            .setIsActive(true)
            .setPrice(Money.newBuilder().setAmountCents(2999).setCurrency("USD"))
            .build();

        GetProductResponse getProductResponse = GetProductResponse.newBuilder()
            .setProduct(product)
            .build();

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.getProduct(any(GetProductRequest.class)))
                .thenReturn(getProductResponse);

            // Act
            ProductValidationResponse result = productServiceGrpcClient.getProduct("product123");

            // Assert
            assertNotNull(result);
            assertEquals("product123", result.getProductId());
            assertEquals("SKU123", result.getSku());
            assertEquals("Test Product", result.getName());
            assertTrue(result.isValid());
            assertTrue(result.isActive());
            assertEquals(new BigDecimal("29.99"), result.getPrice());

            verify(productServiceStub).getProduct(any(GetProductRequest.class));
        }
    }

    @Test
    void validateProduct_CircuitBreakerFallback() {
        // Arrange
        StatusRuntimeException unavailableException = new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service unavailable"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.validateProduct(any(ValidateProductRequest.class)))
                .thenThrow(unavailableException);

            // Act & Assert
            ProductNotAvailableException exception = assertThrows(
                ProductNotAvailableException.class,
                () -> productServiceGrpcClient.validateProduct("product123", "SKU123")
            );

            assertTrue(exception.getMessage().contains("Service unavailable"));
            verify(productServiceStub).validateProduct(any(ValidateProductRequest.class));
        }
    }

    @Test
    void validateProduct_InvalidArgument() {
        // Arrange
        StatusRuntimeException invalidArgException = new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid product ID"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.validateProduct(any(ValidateProductRequest.class)))
                .thenThrow(invalidArgException);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productServiceGrpcClient.validateProduct("", "SKU123")
            );

            assertTrue(exception.getMessage().contains("Invalid argument"));
            verify(productServiceStub).validateProduct(any(ValidateProductRequest.class));
        }
    }

    @Test
    void validateProduct_ResourceExhausted() {
        // Arrange
        StatusRuntimeException rateLimitException = new StatusRuntimeException(Status.RESOURCE_EXHAUSTED.withDescription("Rate limit exceeded"));

        try (MockedStatic<GrpcContextUtils> mockedStatic = mockStatic(GrpcContextUtils.class)) {
            mockedStatic.when(GrpcContextUtils::createTenantContext).thenReturn(tenantContext);
            mockedStatic.when(() -> GrpcContextUtils.withCurrentContext(productServiceStub))
                .thenReturn(productServiceStub);

            when(productServiceStub.withDeadlineAfter(anyLong(), any()))
                .thenReturn(productServiceStub);
            when(productServiceStub.validateProduct(any(ValidateProductRequest.class)))
                .thenThrow(rateLimitException);

            // Act & Assert
            ProductNotAvailableException exception = assertThrows(
                ProductNotAvailableException.class,
                () -> productServiceGrpcClient.validateProduct("product123", "SKU123")
            );

            assertTrue(exception.getMessage().contains("Rate limit exceeded"));
            verify(productServiceStub).validateProduct(any(ValidateProductRequest.class));
        }
    }
}
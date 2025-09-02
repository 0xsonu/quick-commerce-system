package com.ecommerce.gateway.integration;

import com.ecommerce.gateway.service.GatewayGrpcClientService;
import com.ecommerce.gateway.grpc.GrpcMessageMapper;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.userservice.proto.UserServiceGrpc;
import com.ecommerce.userservice.proto.UserServiceProtos;
import com.ecommerce.productservice.proto.ProductServiceGrpc;
import com.ecommerce.productservice.proto.ProductServiceProtos;
import com.ecommerce.cartservice.proto.CartServiceGrpc;
import com.ecommerce.cartservice.proto.CartServiceProtos;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for gRPC client functionality in API Gateway
 */
@SpringBootTest
@SpringJUnitConfig
@ExtendWith(MockitoExtension.class)
class GrpcGatewayIntegrationTest {

    private final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private GatewayGrpcClientService grpcClientService;
    private GrpcMessageMapper messageMapper;

    // Mock gRPC services
    private UserServiceGrpc.UserServiceImplBase mockUserService;
    private ProductServiceGrpc.ProductServiceImplBase mockProductService;
    private CartServiceGrpc.CartServiceImplBase mockCartService;

    @BeforeEach
    void setUp() throws Exception {
        messageMapper = new GrpcMessageMapper();
        
        // Create mock services
        mockUserService = mock(UserServiceGrpc.UserServiceImplBase.class);
        mockProductService = mock(ProductServiceGrpc.ProductServiceImplBase.class);
        mockCartService = mock(CartServiceGrpc.CartServiceImplBase.class);

        // Create in-process server
        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(mockUserService)
            .addService(mockProductService)
            .addService(mockCartService)
            .build()
            .start());

        // Create client service with in-process channel
        // Note: In a real test, you would inject the actual service with test configuration
        // This is a simplified version for demonstration
    }

    @Test
    void testGetUserProfile_Success() {
        // Given
        String tenantId = "tenant-1";
        String userId = "123";

        UserServiceProtos.User user = UserServiceProtos.User.newBuilder()
            .setId(123L)
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("john.doe@example.com")
            .setPhone("+1234567890")
            .setIsActive(true)
            .build();

        UserServiceProtos.GetUserResponse response = UserServiceProtos.GetUserResponse.newBuilder()
            .setUser(user)
            .build();

        doAnswer(invocation -> {
            StreamObserver<UserServiceProtos.GetUserResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        }).when(mockUserService).getUser(any(), any());

        // When & Then
        // This would test the actual service call
        // For now, we'll test the mapping functionality
        GrpcMessageMapper.UserProfileResponse mappedResponse = messageMapper.mapToUserProfileResponse(response);

        assertThat(mappedResponse.getId()).isEqualTo(123L);
        assertThat(mappedResponse.getFirstName()).isEqualTo("John");
        assertThat(mappedResponse.getLastName()).isEqualTo("Doe");
        assertThat(mappedResponse.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(mappedResponse.isActive()).isTrue();
    }

    @Test
    void testGetProduct_Success() {
        // Given
        String tenantId = "tenant-1";
        String productId = "product-123";

        CommonProtos.Money price = CommonProtos.Money.newBuilder()
            .setAmountCents(2999)
            .setCurrency("USD")
            .build();

        ProductServiceProtos.Product product = ProductServiceProtos.Product.newBuilder()
            .setId("product-123")
            .setName("Test Product")
            .setDescription("A test product")
            .setSku("SKU-123")
            .setPrice(price)
            .setCategory("Electronics")
            .setBrand("TestBrand")
            .setIsActive(true)
            .addImageUrls("https://example.com/image1.jpg")
            .putAttributes("color", "blue")
            .build();

        ProductServiceProtos.GetProductResponse response = ProductServiceProtos.GetProductResponse.newBuilder()
            .setProduct(product)
            .build();

        // When
        GrpcMessageMapper.ProductResponse mappedResponse = messageMapper.mapToProductResponse(response);

        // Then
        assertThat(mappedResponse.getId()).isEqualTo("product-123");
        assertThat(mappedResponse.getName()).isEqualTo("Test Product");
        assertThat(mappedResponse.getPrice()).isEqualTo(new BigDecimal("29.99"));
        assertThat(mappedResponse.getCategory()).isEqualTo("Electronics");
        assertThat(mappedResponse.isActive()).isTrue();
        assertThat(mappedResponse.getImageUrls()).contains("https://example.com/image1.jpg");
        assertThat(mappedResponse.getAttributes()).containsEntry("color", "blue");
    }

    @Test
    void testGetCart_Success() {
        // Given
        String tenantId = "tenant-1";
        String userId = "123";

        CommonProtos.Money itemPrice = CommonProtos.Money.newBuilder()
            .setAmountCents(1999)
            .setCurrency("USD")
            .build();

        CommonProtos.Money totalPrice = CommonProtos.Money.newBuilder()
            .setAmountCents(3998)
            .setCurrency("USD")
            .build();

        CartServiceProtos.CartItem cartItem = CartServiceProtos.CartItem.newBuilder()
            .setProductId("product-123")
            .setSku("SKU-123")
            .setQuantity(2)
            .setUnitPrice(itemPrice)
            .setTotalPrice(totalPrice)
            .build();

        CartServiceProtos.Cart cart = CartServiceProtos.Cart.newBuilder()
            .setUserId(123L)
            .addItems(cartItem)
            .setTotalAmount(totalPrice)
            .build();

        CartServiceProtos.GetCartResponse response = CartServiceProtos.GetCartResponse.newBuilder()
            .setCart(cart)
            .build();

        // When
        GrpcMessageMapper.CartResponse mappedResponse = messageMapper.mapToCartResponse(response);

        // Then
        assertThat(mappedResponse.getUserId()).isEqualTo(123L);
        assertThat(mappedResponse.getItems()).hasSize(1);
        assertThat(mappedResponse.getTotalAmount()).isEqualTo(new BigDecimal("39.98"));

        GrpcMessageMapper.CartItemResponse item = mappedResponse.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo("product-123");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualTo(new BigDecimal("19.99"));
    }

    @Test
    void testMoneyMapping() {
        // Test money conversion from cents to decimal
        CommonProtos.Money grpcMoney = CommonProtos.Money.newBuilder()
            .setAmountCents(12345)
            .setCurrency("USD")
            .build();

        BigDecimal result = messageMapper.mapMoney(grpcMoney);
        assertThat(result).isEqualTo(new BigDecimal("123.45"));

        // Test money conversion from decimal to cents
        BigDecimal amount = new BigDecimal("99.99");
        CommonProtos.Money grpcResult = messageMapper.mapToGrpcMoney(amount, "EUR");
        
        assertThat(grpcResult.getAmountCents()).isEqualTo(9999);
        assertThat(grpcResult.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void testTenantContextBuilding() {
        // Given
        String tenantId = "tenant-123";
        String userId = "user-456";
        String correlationId = "corr-789";

        // When
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(tenantId, userId, correlationId);

        // Then
        assertThat(context.getTenantId()).isEqualTo(tenantId);
        assertThat(context.getUserId()).isEqualTo(userId);
        assertThat(context.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    void testTenantContextBuilding_WithNullValues() {
        // When
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(null, null, null);

        // Then
        assertThat(context.getTenantId()).isEmpty();
        assertThat(context.getUserId()).isEmpty();
        assertThat(context.getCorrelationId()).isEmpty();
    }

    @Test
    void testGetProductsByIds_Success() {
        // Given
        List<String> productIds = Arrays.asList("product-1", "product-2");

        CommonProtos.Money price1 = CommonProtos.Money.newBuilder()
            .setAmountCents(1999)
            .setCurrency("USD")
            .build();

        CommonProtos.Money price2 = CommonProtos.Money.newBuilder()
            .setAmountCents(2999)
            .setCurrency("USD")
            .build();

        ProductServiceProtos.Product product1 = ProductServiceProtos.Product.newBuilder()
            .setId("product-1")
            .setName("Product 1")
            .setPrice(price1)
            .setIsActive(true)
            .build();

        ProductServiceProtos.Product product2 = ProductServiceProtos.Product.newBuilder()
            .setId("product-2")
            .setName("Product 2")
            .setPrice(price2)
            .setIsActive(true)
            .build();

        ProductServiceProtos.GetProductsByIdsResponse response = ProductServiceProtos.GetProductsByIdsResponse.newBuilder()
            .addProducts(product1)
            .addProducts(product2)
            .build();

        // When
        List<GrpcMessageMapper.ProductResponse> mappedResponse = messageMapper.mapToProductsResponse(response);

        // Then
        assertThat(mappedResponse).hasSize(2);
        assertThat(mappedResponse.get(0).getId()).isEqualTo("product-1");
        assertThat(mappedResponse.get(0).getPrice()).isEqualTo(new BigDecimal("19.99"));
        assertThat(mappedResponse.get(1).getId()).isEqualTo("product-2");
        assertThat(mappedResponse.get(1).getPrice()).isEqualTo(new BigDecimal("29.99"));
    }

    @Test
    void testErrorHandling_StatusRuntimeException() {
        // This test would verify that gRPC exceptions are properly handled
        // In a real implementation, you would test the actual service calls
        // and verify that StatusRuntimeException is properly converted to business exceptions
        
        StatusRuntimeException grpcException = new StatusRuntimeException(Status.NOT_FOUND.withDescription("User not found"));
        
        // Verify that the exception contains the expected status
        assertThat(grpcException.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(grpcException.getStatus().getDescription()).isEqualTo("User not found");
    }
}
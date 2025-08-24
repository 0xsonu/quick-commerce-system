package com.ecommerce.orderservice.grpc;

import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.UpdateOrderStatusRequest;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.proto.OrderServiceGrpc;
import com.ecommerce.orderservice.proto.OrderServiceProtos.*;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderGrpcServiceTest {

    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private OrderService orderService;

    private OrderServiceGrpc.OrderServiceBlockingStub blockingStub;
    private OrderGrpcService grpcService;

    @BeforeEach
    void setUp() throws Exception {
        TenantContext.setTenantId("tenant1");
        TenantContext.setUserId("user1");

        grpcService = new OrderGrpcService(orderService);

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(grpcService)
            .build()
            .start());

        blockingStub = OrderServiceGrpc.newBlockingStub(
            grpcCleanup.register(InProcessChannelBuilder
                .forName(serverName)
                .directExecutor()
                .build()));
    }

    @Test
    void testGetOrder_Success() {
        // Given
        OrderResponse mockOrder = createMockOrderResponse();
        when(orderService.getOrder(1L)).thenReturn(mockOrder);

        // When
        GetOrderRequest request = GetOrderRequest.newBuilder()
            .setOrderId(1L)
            .build();
        GetOrderResponse response = blockingStub.getOrder(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getOrder());
        assertEquals(1L, response.getOrder().getId());
        assertEquals("ORD-123", response.getOrder().getOrderNumber());
        assertEquals(1L, response.getOrder().getUserId());
        assertEquals("PENDING", response.getOrder().getStatus());
        verify(orderService).getOrder(1L);
    }

    @Test
    void testGetOrder_NotFound() {
        // Given
        when(orderService.getOrder(999L)).thenThrow(new OrderNotFoundException("Order not found"));

        // When & Then
        GetOrderRequest request = GetOrderRequest.newBuilder()
            .setOrderId(999L)
            .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            blockingStub.getOrder(request);
        });

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
        assertTrue(exception.getMessage().contains("Order not found"));
    }

    @Test
    void testValidateOrder_Success() {
        // Given
        OrderResponse mockOrder = createMockOrderResponse();
        when(orderService.getOrder(1L)).thenReturn(mockOrder);

        // When
        ValidateOrderRequest request = ValidateOrderRequest.newBuilder()
            .setOrderId(1L)
            .build();
        ValidateOrderResponse response = blockingStub.validateOrder(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getIsValid());
        assertEquals("PENDING", response.getStatus());
        assertEquals(1L, response.getUserId());
        assertEquals(10000L, response.getTotalAmount().getAmountCents()); // $100.00
        assertEquals("USD", response.getTotalAmount().getCurrency());
    }

    @Test
    void testValidateOrder_NotFound() {
        // Given
        when(orderService.getOrder(999L)).thenThrow(new OrderNotFoundException("Order not found"));

        // When
        ValidateOrderRequest request = ValidateOrderRequest.newBuilder()
            .setOrderId(999L)
            .build();
        ValidateOrderResponse response = blockingStub.validateOrder(request);

        // Then
        assertNotNull(response);
        assertFalse(response.getIsValid());
        assertEquals("NOT_FOUND", response.getStatus());
    }

    @Test
    void testUpdateOrderStatus_Success() {
        // Given
        OrderResponse mockOrder = createMockOrderResponse();
        mockOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderService.updateOrderStatus(eq(1L), any(UpdateOrderStatusRequest.class)))
            .thenReturn(mockOrder);

        // When
        com.ecommerce.orderservice.proto.OrderServiceProtos.UpdateOrderStatusRequest request = 
            com.ecommerce.orderservice.proto.OrderServiceProtos.UpdateOrderStatusRequest.newBuilder()
                .setOrderId(1L)
                .setNewStatus("CONFIRMED")
                .setReason("Payment received")
                .build();
        UpdateOrderStatusResponse response = blockingStub.updateOrderStatus(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertTrue(response.getErrorMessage().isEmpty());
        verify(orderService).updateOrderStatus(eq(1L), any(UpdateOrderStatusRequest.class));
    }

    @Test
    void testUpdateOrderStatus_InvalidTransition() {
        // Given
        when(orderService.updateOrderStatus(eq(1L), any(UpdateOrderStatusRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid status transition"));

        // When
        com.ecommerce.orderservice.proto.OrderServiceProtos.UpdateOrderStatusRequest request = 
            com.ecommerce.orderservice.proto.OrderServiceProtos.UpdateOrderStatusRequest.newBuilder()
                .setOrderId(1L)
                .setNewStatus("DELIVERED")
                .setReason("Invalid transition")
                .build();
        UpdateOrderStatusResponse response = blockingStub.updateOrderStatus(request);

        // Then
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Invalid status transition", response.getErrorMessage());
    }

    @Test
    void testGetOrdersByUser_Success() {
        // Given
        List<OrderResponse> orders = Arrays.asList(createMockOrderResponse());
        Page<OrderResponse> orderPage = new PageImpl<>(orders, Pageable.ofSize(10), 1);
        when(orderService.getOrdersByUser(eq(1L), any(Pageable.class))).thenReturn(orderPage);

        // When
        GetOrdersByUserRequest request = GetOrdersByUserRequest.newBuilder()
            .setUserId(1L)
            .setPageRequest(CommonProtos.PageRequest.newBuilder()
                .setPage(0)
                .setSize(10)
                .build())
            .build();
        GetOrdersByUserResponse response = blockingStub.getOrdersByUser(request);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getOrdersCount());
        assertEquals("ORD-123", response.getOrders(0).getOrderNumber());
        
        CommonProtos.PageResponse pageResponse = response.getPageResponse();
        assertEquals(0, pageResponse.getPage());
        assertEquals(10, pageResponse.getSize());
        assertEquals(1L, pageResponse.getTotalElements());
        assertEquals(1, pageResponse.getTotalPages());
    }

    private OrderResponse createMockOrderResponse() {
        OrderResponse order = new OrderResponse();
        order.setId(1L);
        order.setOrderNumber("ORD-123");
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(new BigDecimal("90.00"));
        order.setTaxAmount(new BigDecimal("7.20"));
        order.setShippingAmount(new BigDecimal("2.80"));
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setCurrency("USD");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }
}
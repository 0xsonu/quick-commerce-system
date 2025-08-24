package com.ecommerce.orderservice.grpc;

import com.ecommerce.orderservice.dto.OrderResponse;

import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OrderValidationException;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.proto.OrderServiceGrpc;
import com.ecommerce.orderservice.proto.OrderServiceProtos.*;
import com.ecommerce.shared.grpc.TenantContextInterceptor;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.ZoneOffset;

@GrpcService(interceptors = {TenantContextInterceptor.class})
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(OrderGrpcService.class);

    private final OrderService orderService;

    @Autowired
    public OrderGrpcService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<GetOrderResponse> responseObserver) {
        try {
            logger.debug("gRPC GetOrder request for order: {} in tenant: {}", 
                        request.getOrderId(), TenantContext.getTenantId());

            OrderResponse order = orderService.getOrder(request.getOrderId());
            
            GetOrderResponse response = GetOrderResponse.newBuilder()
                .setOrder(convertToProtoOrder(order))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (OrderNotFoundException e) {
            logger.warn("Order not found: {}", request.getOrderId());
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("Order not found: " + request.getOrderId())
                .asRuntimeException());
        } catch (Exception e) {
            logger.error("Error getting order: {}", request.getOrderId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void validateOrder(ValidateOrderRequest request, StreamObserver<ValidateOrderResponse> responseObserver) {
        try {
            logger.debug("gRPC ValidateOrder request for order: {} in tenant: {}", 
                        request.getOrderId(), TenantContext.getTenantId());

            OrderResponse order = orderService.getOrder(request.getOrderId());
            
            ValidateOrderResponse response = ValidateOrderResponse.newBuilder()
                .setIsValid(order.getStatus().isActive())
                .setStatus(order.getStatus().name())
                .setTotalAmount(convertToProtoMoney(order.getTotalAmount(), order.getCurrency()))
                .setUserId(order.getUserId())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (OrderNotFoundException e) {
            logger.warn("Order not found for validation: {}", request.getOrderId());
            ValidateOrderResponse response = ValidateOrderResponse.newBuilder()
                .setIsValid(false)
                .setStatus("NOT_FOUND")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error validating order: {}", request.getOrderId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void updateOrderStatus(com.ecommerce.orderservice.proto.OrderServiceProtos.UpdateOrderStatusRequest request, 
                                 StreamObserver<UpdateOrderStatusResponse> responseObserver) {
        try {
            logger.info("gRPC UpdateOrderStatus request for order: {} to status: {} in tenant: {}", 
                       request.getOrderId(), request.getNewStatus(), TenantContext.getTenantId());

            OrderStatus newStatus = OrderStatus.valueOf(request.getNewStatus());
            com.ecommerce.orderservice.dto.UpdateOrderStatusRequest serviceRequest = 
                new com.ecommerce.orderservice.dto.UpdateOrderStatusRequest(newStatus, request.getReason());
            
            orderService.updateOrderStatus(request.getOrderId(), serviceRequest);
            
            UpdateOrderStatusResponse response = UpdateOrderStatusResponse.newBuilder()
                .setSuccess(true)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (OrderNotFoundException e) {
            logger.warn("Order not found for status update: {}", request.getOrderId());
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("Order not found: " + request.getOrderId())
                .asRuntimeException());
        } catch (OrderValidationException | IllegalArgumentException e) {
            logger.warn("Invalid status transition for order: {}", request.getOrderId(), e);
            UpdateOrderStatusResponse response = UpdateOrderStatusResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error updating order status: {}", request.getOrderId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void getOrdersByUser(GetOrdersByUserRequest request, StreamObserver<GetOrdersByUserResponse> responseObserver) {
        try {
            logger.debug("gRPC GetOrdersByUser request for user: {} in tenant: {}", 
                        request.getUserId(), TenantContext.getTenantId());

            Pageable pageable = PageRequest.of(
                request.getPageRequest().getPage(),
                request.getPageRequest().getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
            );

            Page<OrderResponse> ordersPage = orderService.getOrdersByUser(request.getUserId(), pageable);
            
            GetOrdersByUserResponse.Builder responseBuilder = GetOrdersByUserResponse.newBuilder();
            
            // Add orders
            for (OrderResponse order : ordersPage.getContent()) {
                responseBuilder.addOrders(convertToProtoOrder(order));
            }
            
            // Add page response
            responseBuilder.setPageResponse(CommonProtos.PageResponse.newBuilder()
                .setPage(ordersPage.getNumber())
                .setSize(ordersPage.getSize())
                .setTotalElements(ordersPage.getTotalElements())
                .setTotalPages(ordersPage.getTotalPages())
                .build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting orders by user: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    private Order convertToProtoOrder(OrderResponse order) {
        Order.Builder orderBuilder = Order.newBuilder()
            .setId(order.getId())
            .setOrderNumber(order.getOrderNumber())
            .setUserId(order.getUserId())
            .setStatus(order.getStatus().name())
            .setSubtotal(convertToProtoMoney(order.getSubtotal(), order.getCurrency()))
            .setTaxAmount(convertToProtoMoney(order.getTaxAmount(), order.getCurrency()))
            .setShippingAmount(convertToProtoMoney(order.getShippingAmount(), order.getCurrency()))
            .setTotalAmount(convertToProtoMoney(order.getTotalAmount(), order.getCurrency()))
            .setCreatedAt(order.getCreatedAt().atZone(ZoneOffset.UTC).toEpochSecond())
            .setUpdatedAt(order.getUpdatedAt().atZone(ZoneOffset.UTC).toEpochSecond());

        // Add billing address
        if (order.getBillingAddress() != null) {
            orderBuilder.setBillingAddress(CommonProtos.Address.newBuilder()
                .setStreetAddress(order.getBillingAddress().getStreetAddress())
                .setCity(order.getBillingAddress().getCity())
                .setState(order.getBillingAddress().getState())
                .setPostalCode(order.getBillingAddress().getPostalCode())
                .setCountry(order.getBillingAddress().getCountry())
                .build());
        }

        // Add shipping address
        if (order.getShippingAddress() != null) {
            orderBuilder.setShippingAddress(CommonProtos.Address.newBuilder()
                .setStreetAddress(order.getShippingAddress().getStreetAddress())
                .setCity(order.getShippingAddress().getCity())
                .setState(order.getShippingAddress().getState())
                .setPostalCode(order.getShippingAddress().getPostalCode())
                .setCountry(order.getShippingAddress().getCountry())
                .build());
        }

        // Add order items
        if (order.getItems() != null) {
            for (com.ecommerce.orderservice.dto.OrderItemResponse item : order.getItems()) {
                orderBuilder.addItems(OrderItem.newBuilder()
                    .setId(item.getId())
                    .setProductId(item.getProductId())
                    .setSku(item.getSku())
                    .setProductName(item.getProductName())
                    .setQuantity(item.getQuantity())
                    .setUnitPrice(convertToProtoMoney(item.getUnitPrice(), order.getCurrency()))
                    .setTotalPrice(convertToProtoMoney(item.getTotalPrice(), order.getCurrency()))
                    .build());
            }
        }

        return orderBuilder.build();
    }

    private CommonProtos.Money convertToProtoMoney(BigDecimal amount, String currency) {
        return CommonProtos.Money.newBuilder()
            .setAmountCents(amount.multiply(new BigDecimal("100")).longValue())
            .setCurrency(currency)
            .build();
    }
}
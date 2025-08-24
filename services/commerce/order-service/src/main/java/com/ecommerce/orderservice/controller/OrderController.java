package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.shared.utils.response.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        
        logger.info("Creating order for user: {} in tenant: {}", userId, tenantId);

        OrderResponse order = orderService.createOrder(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(order, "Order created successfully"));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long orderId) {
        
        logger.debug("Getting order: {} for user: {} in tenant: {}", orderId, userId, tenantId);

        OrderResponse order = orderService.getOrder(orderId);
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable String orderNumber) {
        
        logger.debug("Getting order by number: {} for user: {} in tenant: {}", 
                    orderNumber, userId, tenantId);

        OrderResponse order = orderService.getOrderByNumber(orderNumber);
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String requestingUserId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        logger.debug("Getting orders for user: {} in tenant: {}", userId, tenantId);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<OrderResponse> orders = orderService.getOrdersByUser(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable OrderStatus status) {
        
        logger.debug("Getting orders with status: {} in tenant: {}", status, tenantId);

        List<OrderResponse> orders = orderService.getOrdersByStatus(status);
        
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        
        logger.info("Updating order status: {} to {} by user: {}", 
                   orderId, request.getNewStatus(), userId);

        OrderResponse order = orderService.updateOrderStatus(orderId, request);
        
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated successfully"));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {
        
        logger.info("Cancelling order: {} by user: {} with reason: {}", orderId, userId, reason);

        orderService.cancelOrder(orderId, reason);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Order cancelled successfully"));
    }

    @GetMapping("/{orderId}/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateOrder(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long orderId) {
        
        logger.debug("Validating order: {} for user: {} in tenant: {}", orderId, userId, tenantId);

        boolean isValid = orderService.validateOrder(orderId);
        
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }
}
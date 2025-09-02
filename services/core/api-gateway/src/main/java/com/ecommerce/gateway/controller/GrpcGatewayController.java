package com.ecommerce.gateway.controller;

import com.ecommerce.gateway.dto.AuthRequest;
import com.ecommerce.gateway.dto.CreateReviewRequestDto;
import com.ecommerce.gateway.dto.TokenValidationRequest;
import com.ecommerce.gateway.grpc.GrpcExceptionHandler;
import com.ecommerce.gateway.grpc.GrpcMessageMapper;
import com.ecommerce.gateway.service.GatewayGrpcClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that demonstrates gRPC client usage in API Gateway
 * Provides REST endpoints that internally use gRPC for service communication
 */
@RestController
@RequestMapping("/api/v1/grpc")
public class GrpcGatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GrpcGatewayController.class);

    private final GatewayGrpcClientService grpcClientService;
    private final GrpcExceptionHandler exceptionHandler;

    public GrpcGatewayController(GatewayGrpcClientService grpcClientService, 
                                GrpcExceptionHandler exceptionHandler) {
        this.grpcClientService = grpcClientService;
        this.exceptionHandler = exceptionHandler;
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<GrpcMessageMapper.UserProfileResponse> getUserProfile(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String userId) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Get user profile for tenantId: {}, userId: {}", tenantId, userId);
            GrpcMessageMapper.UserProfileResponse profile = grpcClientService.getUserProfile(tenantId, userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Failed to get user profile", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @GetMapping("/users/{userId}/addresses")
    public ResponseEntity<List<GrpcMessageMapper.UserAddressResponse>> getUserAddresses(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String userId) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Get user addresses for tenantId: {}, userId: {}", tenantId, userId);
            List<GrpcMessageMapper.UserAddressResponse> addresses = grpcClientService.getUserAddresses(tenantId, userId);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            logger.error("Failed to get user addresses", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<GrpcMessageMapper.ProductResponse> getProduct(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @PathVariable String productId) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Get product for tenantId: {}, productId: {}", tenantId, productId);
            GrpcMessageMapper.ProductResponse product = grpcClientService.getProduct(tenantId, userId, productId);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            logger.error("Failed to get product", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @PostMapping("/products/batch")
    public ResponseEntity<List<GrpcMessageMapper.ProductResponse>> getProductsByIds(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @RequestBody List<String> productIds) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Get products by IDs for tenantId: {}, count: {}", tenantId, productIds.size());
            List<GrpcMessageMapper.ProductResponse> products = grpcClientService.getProductsByIds(tenantId, userId, productIds);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Failed to get products by IDs", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @GetMapping("/users/{userId}/cart")
    public ResponseEntity<GrpcMessageMapper.CartResponse> getCart(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String userId) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Get cart for tenantId: {}, userId: {}", tenantId, userId);
            GrpcMessageMapper.CartResponse cart = grpcClientService.getCart(tenantId, userId);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            logger.error("Failed to get cart", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @PostMapping("/users/{userId}/cart/items")
    public ResponseEntity<GrpcMessageMapper.CartResponse> addCartItem(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String userId,
            @RequestBody GatewayGrpcClientService.AddCartItemRequest request) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Add cart item for tenantId: {}, userId: {}, productId: {}", 
                tenantId, userId, request.getProductId());
            GrpcMessageMapper.CartResponse cart = grpcClientService.addCartItem(tenantId, userId, request);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            logger.error("Failed to add cart item", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<GrpcMessageMapper.OrderResponse> getOrder(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long orderId) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Get order for tenantId: {}, userId: {}, orderId: {}", tenantId, userId, orderId);
            GrpcMessageMapper.OrderResponse order = grpcClientService.getOrder(tenantId, userId, orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            logger.error("Failed to get order", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @GetMapping("/inventory/{productId}/availability")
    public ResponseEntity<GatewayGrpcClientService.InventoryAvailabilityResponse> checkInventoryAvailability(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId,
            @RequestParam int quantity) {
        
        setCorrelationId();
        setMDCContext(tenantId, null);
        
        try {
            logger.info("REST request: Check inventory availability for tenantId: {}, productId: {}, quantity: {}", 
                tenantId, productId, quantity);
            GatewayGrpcClientService.InventoryAvailabilityResponse availability = 
                grpcClientService.checkInventoryAvailability(tenantId, productId, quantity);
            return ResponseEntity.ok(availability);
        } catch (Exception e) {
            logger.error("Failed to check inventory availability", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @PostMapping("/payments")
    public ResponseEntity<GatewayGrpcClientService.PaymentResponse> processPayment(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestBody GatewayGrpcClientService.ProcessPaymentRequest request) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Process payment for tenantId: {}, userId: {}, orderId: {}", 
                tenantId, userId, request.getOrderId());
            GatewayGrpcClientService.PaymentResponse payment = grpcClientService.processPayment(tenantId, userId, request);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            logger.error("Failed to process payment", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    @PostMapping("/shipments")
    public ResponseEntity<GatewayGrpcClientService.ShippingResponse> createShipment(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestBody GatewayGrpcClientService.CreateShipmentRequest request) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Create shipment for tenantId: {}, userId: {}, orderId: {}", 
                tenantId, userId, request.getOrderId());
            GatewayGrpcClientService.ShippingResponse shipment = grpcClientService.createShipment(tenantId, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
        } catch (Exception e) {
            logger.error("Failed to create shipment", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    // Auth Service Endpoints - Note: Authentication is typically handled by the gateway itself

    @PostMapping("/auth/validate")
    public ResponseEntity<GatewayGrpcClientService.TokenValidationResponse> validateToken(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody TokenValidationRequest request) {
        
        setCorrelationId();
        setMDCContext(tenantId, null);
        
        try {
            logger.info("REST request: Validate token for tenantId: {}", tenantId);
            GatewayGrpcClientService.TokenValidationResponse validation = grpcClientService.validateToken(tenantId, request.getToken());
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            logger.error("Failed to validate token", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    // Notification Service Endpoints
    @PostMapping("/users/{userId}/notifications")
    public ResponseEntity<GatewayGrpcClientService.NotificationResponse> sendNotification(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String userId,
            @RequestBody GatewayGrpcClientService.SendNotificationRequest request) {
        
        setCorrelationId();
        setMDCContext(tenantId, userId);
        
        try {
            logger.info("REST request: Send notification for tenantId: {}, userId: {}, templateId: {}", 
                tenantId, userId, request.getTemplateId());
            GatewayGrpcClientService.NotificationResponse notification = grpcClientService.sendNotification(tenantId, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            logger.error("Failed to send notification", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    // Note: Get user notifications is not implemented as it's not in the current proto definition

    // Review Service Endpoints
    // Note: Review creation is not available in the current proto definition

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<GatewayGrpcClientService.ReviewResponse>> getProductReviews(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String productId) {
        
        setCorrelationId();
        setMDCContext(tenantId, null);
        
        try {
            logger.info("REST request: Get reviews for tenantId: {}, productId: {}", tenantId, productId);
            List<GatewayGrpcClientService.ReviewResponse> reviews = grpcClientService.getProductReviews(tenantId, productId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            logger.error("Failed to get product reviews", e);
            return handleException(e);
        } finally {
            clearMDCContext();
        }
    }

    private void setCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
    }

    private void setMDCContext(String tenantId, String userId) {
        if (tenantId != null) {
            MDC.put("tenantId", tenantId);
        }
        if (userId != null) {
            MDC.put("userId", userId);
        }
    }

    private void clearMDCContext() {
        MDC.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> handleException(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        if (e instanceof GrpcExceptionHandler.ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (e instanceof GrpcExceptionHandler.AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
        } else if (e instanceof GrpcExceptionHandler.ValidationException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof GrpcExceptionHandler.ServiceUnavailableException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (e instanceof GrpcExceptionHandler.TimeoutException) {
            status = HttpStatus.REQUEST_TIMEOUT;
        } else if (e instanceof GrpcExceptionHandler.RateLimitException) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else if (e instanceof GrpcExceptionHandler.AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
        }
        
        return (ResponseEntity<T>) ResponseEntity.status(status)
            .body(new ErrorResponse(e.getMessage(), status.value()));
    }

    public static class ErrorResponse {
        private String message;
        private int status;

        public ErrorResponse(String message, int status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() { return message; }
        public int getStatus() { return status; }
    }
}
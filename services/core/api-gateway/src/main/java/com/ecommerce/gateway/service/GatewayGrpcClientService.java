package com.ecommerce.gateway.service;

import com.ecommerce.gateway.grpc.GrpcExceptionHandler;
import com.ecommerce.gateway.grpc.GrpcMessageMapper;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.userservice.proto.UserServiceGrpc;
import com.ecommerce.userservice.proto.UserServiceProtos;
import com.ecommerce.productservice.proto.ProductServiceGrpc;
import com.ecommerce.productservice.proto.ProductServiceProtos;
import com.ecommerce.cartservice.proto.CartServiceGrpc;
import com.ecommerce.cartservice.proto.CartServiceProtos;
import com.ecommerce.orderservice.proto.OrderServiceGrpc;
import com.ecommerce.orderservice.proto.OrderServiceProtos;
import com.ecommerce.paymentservice.proto.PaymentServiceGrpc;
import com.ecommerce.paymentservice.proto.PaymentServiceProtos;
import com.ecommerce.inventoryservice.proto.InventoryServiceGrpc;
import com.ecommerce.inventoryservice.proto.InventoryServiceProtos;
import com.ecommerce.shippingservice.proto.ShippingServiceGrpc;
import com.ecommerce.shippingservice.proto.ShippingServiceProtos;
import com.ecommerce.authservice.proto.AuthServiceGrpc;
import com.ecommerce.authservice.proto.AuthServiceProtos;
import com.ecommerce.notificationservice.proto.NotificationServiceGrpc;
import com.ecommerce.notificationservice.proto.NotificationServiceProtos;
import com.ecommerce.reviewservice.proto.ReviewServiceGrpc;
import com.ecommerce.reviewservice.proto.ReviewServiceProtos;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service that provides gRPC client functionality for the API Gateway
 * Handles REST-to-gRPC protocol translation and error handling
 */
@Service
public class GatewayGrpcClientService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayGrpcClientService.class);

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @GrpcClient("product-service")
    private ProductServiceGrpc.ProductServiceBlockingStub productServiceStub;

    @GrpcClient("cart-service")
    private CartServiceGrpc.CartServiceBlockingStub cartServiceStub;

    @GrpcClient("order-service")
    private OrderServiceGrpc.OrderServiceBlockingStub orderServiceStub;

    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentServiceStub;

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceStub;

    @GrpcClient("shipping-service")
    private ShippingServiceGrpc.ShippingServiceBlockingStub shippingServiceStub;

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    @GrpcClient("notification-service")
    private NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceStub;

    @GrpcClient("review-service")
    private ReviewServiceGrpc.ReviewServiceBlockingStub reviewServiceStub;

    private final GrpcMessageMapper messageMapper;
    private final GrpcExceptionHandler exceptionHandler;

    public GatewayGrpcClientService(GrpcMessageMapper messageMapper, GrpcExceptionHandler exceptionHandler) {
        this.messageMapper = messageMapper;
        this.exceptionHandler = exceptionHandler;
    }

    // User Service Methods
    @CircuitBreaker(name = "user-service")
    @Retry(name = "user-service")
    public GrpcMessageMapper.UserProfileResponse getUserProfile(String tenantId, String userId) {
        logger.debug("Getting user profile for tenantId: {}, userId: {}", tenantId, userId);
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        UserServiceProtos.GetUserRequest request = UserServiceProtos.GetUserRequest.newBuilder()
            .setContext(context)
            .setUserId(Long.parseLong(userId))
            .build();

        try {
            UserServiceProtos.GetUserResponse response = userServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getUser(request);
            
            logger.debug("Successfully retrieved user profile for userId: {}", userId);
            return messageMapper.mapToUserProfileResponse(response);
        } catch (StatusRuntimeException e) {
            logger.error("Failed to get user profile for userId: {}", userId, e);
            throw exceptionHandler.handleGrpcException(e, "Get user profile");
        }
    }

    @CircuitBreaker(name = "user-service")
    @Retry(name = "user-service")
    public List<GrpcMessageMapper.UserAddressResponse> getUserAddresses(String tenantId, String userId) {
        logger.debug("Getting user addresses for tenantId: {}, userId: {}", tenantId, userId);
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        UserServiceProtos.GetUserAddressesRequest request = UserServiceProtos.GetUserAddressesRequest.newBuilder()
            .setContext(context)
            .setUserId(Long.parseLong(userId))
            .build();

        try {
            UserServiceProtos.GetUserAddressesResponse response = userServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getUserAddresses(request);
            
            logger.debug("Successfully retrieved {} addresses for userId: {}", 
                response.getAddressesCount(), userId);
            return messageMapper.mapToUserAddressesResponse(response);
        } catch (StatusRuntimeException e) {
            logger.error("Failed to get user addresses for userId: {}", userId, e);
            throw exceptionHandler.handleGrpcException(e, "Get user addresses");
        }
    }

    // Product Service Methods
    @CircuitBreaker(name = "product-service")
    @Retry(name = "product-service")
    public GrpcMessageMapper.ProductResponse getProduct(String tenantId, String userId, String productId) {
        logger.debug("Getting product for tenantId: {}, productId: {}", tenantId, productId);
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        ProductServiceProtos.GetProductRequest request = ProductServiceProtos.GetProductRequest.newBuilder()
            .setContext(context)
            .setProductId(productId)
            .build();

        try {
            ProductServiceProtos.GetProductResponse response = productServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getProduct(request);
            
            logger.debug("Successfully retrieved product: {}", productId);
            return messageMapper.mapToProductResponse(response);
        } catch (StatusRuntimeException e) {
            logger.error("Failed to get product: {}", productId, e);
            throw exceptionHandler.handleGrpcException(e, "Get product");
        }
    }

    @CircuitBreaker(name = "product-service")
    @Retry(name = "product-service")
    public List<GrpcMessageMapper.ProductResponse> getProductsByIds(String tenantId, String userId, List<String> productIds) {
        logger.debug("Getting products for tenantId: {}, productIds: {}", tenantId, productIds);
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        ProductServiceProtos.GetProductsByIdsRequest request = ProductServiceProtos.GetProductsByIdsRequest.newBuilder()
            .setContext(context)
            .addAllProductIds(productIds)
            .build();

        try {
            ProductServiceProtos.GetProductsByIdsResponse response = productServiceStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .getProductsByIds(request);
            
            logger.debug("Successfully retrieved {} products", response.getProductsCount());
            return messageMapper.mapToProductsResponse(response);
        } catch (StatusRuntimeException e) {
            logger.error("Failed to get products by IDs: {}", productIds, e);
            throw exceptionHandler.handleGrpcException(e, "Get products by IDs");
        }
    }

    // Cart Service Methods
    @CircuitBreaker(name = "cart-service")
    @Retry(name = "cart-service")
    public GrpcMessageMapper.CartResponse getCart(String tenantId, String userId) {
        logger.debug("Getting cart for tenantId: {}, userId: {}", tenantId, userId);
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        CartServiceProtos.GetCartRequest request = CartServiceProtos.GetCartRequest.newBuilder()
            .setContext(context)
            .setUserId(Long.parseLong(userId))
            .build();

        try {
            CartServiceProtos.GetCartResponse response = cartServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getCart(request);
            
            logger.debug("Successfully retrieved cart for userId: {}", userId);
            return messageMapper.mapToCartResponse(response);
        } catch (StatusRuntimeException e) {
            logger.error("Failed to get cart for userId: {}", userId, e);
            throw exceptionHandler.handleGrpcException(e, "Get cart");
        }
    }

    @CircuitBreaker(name = "cart-service")
    @Retry(name = "cart-service")
    public GrpcMessageMapper.CartResponse addCartItem(String tenantId, String userId, AddCartItemRequest request) {
        logger.debug("Adding item to cart for tenantId: {}, userId: {}, productId: {}", 
            tenantId, userId, request.getProductId());
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        CartServiceProtos.AddToCartRequest grpcRequest = CartServiceProtos.AddToCartRequest.newBuilder()
            .setContext(context)
            .setUserId(Long.parseLong(userId))
            .setProductId(request.getProductId())
            .setSku(request.getSku())
            .setQuantity(request.getQuantity())
            .build();

        try {
            CartServiceProtos.AddToCartResponse response = cartServiceStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .addToCart(grpcRequest);
            
            logger.debug("Successfully added item to cart for userId: {}", userId);
            return messageMapper.mapToCartResponse(response.getCart());
        } catch (StatusRuntimeException e) {
            logger.error("Failed to add item to cart for userId: {}", userId, e);
            throw exceptionHandler.handleGrpcException(e, "Add cart item");
        }
    }

    // Order Service Methods
    @CircuitBreaker(name = "order-service")
    @Retry(name = "order-service")
    public GrpcMessageMapper.OrderResponse getOrder(String tenantId, String userId, Long orderId) {
        logger.debug("Getting order for tenantId: {}, userId: {}, orderId: {}", tenantId, userId, orderId);
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        OrderServiceProtos.GetOrderRequest request = OrderServiceProtos.GetOrderRequest.newBuilder()
            .setContext(context)
            .setOrderId(orderId)
            .build();

        try {
            OrderServiceProtos.GetOrderResponse response = orderServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getOrder(request);
            
            logger.debug("Successfully retrieved order: {}", response.getOrder().getId());
            return messageMapper.mapToOrderResponse(response.getOrder());
        } catch (StatusRuntimeException e) {
            logger.error("Failed to get order: {}", orderId, e);
            throw exceptionHandler.handleGrpcException(e, "Get order");
        }
    }

    // Inventory Service Methods
    @CircuitBreaker(name = "inventory-service")
    @Retry(name = "inventory-service")
    public InventoryAvailabilityResponse checkInventoryAvailability(String tenantId, String productId, int quantity) {
        logger.debug("Checking inventory availability for tenantId: {}, productId: {}, quantity: {}", 
            tenantId, productId, quantity);
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, null, MDC.get("correlationId"));

        InventoryServiceProtos.CheckAvailabilityRequest request = InventoryServiceProtos.CheckAvailabilityRequest.newBuilder()
            .setContext(context)
            .setProductId(productId)
            .setRequestedQuantity(quantity)
            .build();

        try {
            InventoryServiceProtos.CheckAvailabilityResponse response = inventoryServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .checkAvailability(request);
            
            logger.debug("Successfully checked inventory availability for productId: {}", productId);
            return InventoryAvailabilityResponse.builder()
                .available(response.getIsAvailable())
                .productId(response.getProductId())
                .availableQuantity(response.getAvailableQuantity())
                .build();
        } catch (StatusRuntimeException e) {
            logger.error("Failed to check inventory availability for productId: {}", productId, e);
            throw exceptionHandler.handleGrpcException(e, "Check inventory availability");
        }
    }

    // Payment Service Methods
    @CircuitBreaker(name = "payment-service")
    @Retry(name = "payment-service")
    public PaymentResponse processPayment(String tenantId, String userId, ProcessPaymentRequest request) {
        logger.debug("Processing payment for tenantId: {}, userId: {}, orderId: {}", 
            tenantId, userId, request.getOrderId());
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        PaymentServiceProtos.ProcessPaymentRequest grpcRequest = PaymentServiceProtos.ProcessPaymentRequest.newBuilder()
            .setContext(context)
            .setOrderId(Long.parseLong(request.getOrderId()))
            .setAmount(messageMapper.mapToGrpcMoney(request.getAmount(), request.getCurrency()))
            .setPaymentMethod(request.getPaymentMethod())
            .setPaymentToken(request.getPaymentToken())
            .setIdempotencyKey(request.getIdempotencyKey())
            .build();

        try {
            PaymentServiceProtos.ProcessPaymentResponse response = paymentServiceStub
                .withDeadlineAfter(30, TimeUnit.SECONDS)
                .processPayment(grpcRequest);
            
            logger.debug("Successfully processed payment: {}", response.getPaymentId());
            return PaymentResponse.builder()
                .id(response.getPaymentId())
                .orderId(request.getOrderId())
                .status(response.getStatus())
                .transactionId(response.getTransactionId())
                .build();
        } catch (StatusRuntimeException e) {
            logger.error("Failed to process payment for orderId: {}", request.getOrderId(), e);
            throw exceptionHandler.handleGrpcException(e, "Process payment");
        }
    }

    // Shipping Service Methods
    @CircuitBreaker(name = "shipping-service")
    @Retry(name = "shipping-service")
    public ShippingResponse createShipment(String tenantId, String userId, CreateShipmentRequest request) {
        logger.debug("Creating shipment for tenantId: {}, userId: {}, orderId: {}", 
            tenantId, userId, request.getOrderId());
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        ShippingServiceProtos.CreateShipmentRequest grpcRequest = ShippingServiceProtos.CreateShipmentRequest.newBuilder()
            .setContext(context)
            .setOrderId(Long.parseLong(request.getOrderId()))
            .setCarrier(request.getCarrier())
            .setServiceType(request.getServiceType())
            .build();

        try {
            ShippingServiceProtos.CreateShipmentResponse response = shippingServiceStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .createShipment(grpcRequest);
            
            logger.debug("Successfully created shipment: {}", response.getShipmentId());
            return ShippingResponse.builder()
                .id(String.valueOf(response.getShipmentId()))
                .orderId(request.getOrderId())
                .trackingNumber(response.getTrackingNumber())
                .build();
        } catch (StatusRuntimeException e) {
            logger.error("Failed to create shipment for orderId: {}", request.getOrderId(), e);
            throw exceptionHandler.handleGrpcException(e, "Create shipment");
        }
    }

    // Auth Service Methods
    @CircuitBreaker(name = "auth-service")
    @Retry(name = "auth-service")
    public TokenValidationResponse validateToken(String tenantId, String token) {
        logger.debug("Validating token for tenantId: {}", tenantId);

        AuthServiceProtos.ValidateTokenRequest request = AuthServiceProtos.ValidateTokenRequest.newBuilder()
            .setToken(token)
            .build();

        try {
            AuthServiceProtos.ValidateTokenResponse response = authServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .validateToken(request);
            
            logger.debug("Successfully validated token");
            return TokenValidationResponse.builder()
                .valid(response.getIsValid())
                .userId(response.getUserId())
                .expiresAt(String.valueOf(response.getExpiresAt()))
                .build();
        } catch (StatusRuntimeException e) {
            logger.error("Failed to validate token", e);
            throw exceptionHandler.handleGrpcException(e, "Validate token");
        }
    }

    // Notification Service Methods
    @CircuitBreaker(name = "notification-service")
    @Retry(name = "notification-service")
    public NotificationResponse sendNotification(String tenantId, String userId, SendNotificationRequest request) {
        logger.debug("Sending notification for tenantId: {}, userId: {}, templateId: {}", 
            tenantId, userId, request.getTemplateId());
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, userId, MDC.get("correlationId"));

        NotificationServiceProtos.SendNotificationRequest grpcRequest = NotificationServiceProtos.SendNotificationRequest.newBuilder()
            .setContext(context)
            .setUserId(Long.parseLong(userId))
            .setTemplateId(request.getTemplateId())
            .setChannel(request.getChannel())
            .setPriority(request.getPriority())
            .putAllTemplateData(request.getTemplateData())
            .setIdempotencyKey(request.getIdempotencyKey())
            .build();

        try {
            NotificationServiceProtos.SendNotificationResponse response = notificationServiceStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .sendNotification(grpcRequest);
            
            logger.debug("Successfully sent notification: {}", response.getNotificationId());
            return NotificationResponse.builder()
                .id(response.getNotificationId())
                .success(response.getSuccess())
                .errorMessage(response.getErrorMessage())
                .build();
        } catch (StatusRuntimeException e) {
            logger.error("Failed to send notification", e);
            throw exceptionHandler.handleGrpcException(e, "Send notification");
        }
    }

    // Review Service Methods
    @CircuitBreaker(name = "review-service")
    @Retry(name = "review-service")
    public List<ReviewResponse> getProductReviews(String tenantId, String productId) {
        logger.debug("Getting reviews for tenantId: {}, productId: {}", tenantId, productId);
        
        CommonProtos.TenantContext context = messageMapper.buildTenantContext(
            tenantId, null, MDC.get("correlationId"));

        ReviewServiceProtos.GetProductReviewsRequest request = ReviewServiceProtos.GetProductReviewsRequest.newBuilder()
            .setContext(context)
            .setProductId(productId)
            .build();

        try {
            ReviewServiceProtos.GetProductReviewsResponse response = reviewServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getProductReviews(request);
            
            logger.debug("Successfully retrieved {} reviews for productId: {}", 
                response.getReviewsCount(), productId);
            return response.getReviewsList().stream()
                .map(this::mapToReviewResponse)
                .collect(java.util.stream.Collectors.toList());
        } catch (StatusRuntimeException e) {
            logger.error("Failed to get product reviews for productId: {}", productId, e);
            throw exceptionHandler.handleGrpcException(e, "Get product reviews");
        }
    }

    private ReviewResponse mapToReviewResponse(ReviewServiceProtos.Review review) {
        return ReviewResponse.builder()
            .id(review.getId())
            .userId(String.valueOf(review.getUserId()))
            .productId(review.getProductId())
            .rating(review.getRating())
            .title(review.getTitle())
            .comment(review.getComment())
            .createdAt(String.valueOf(review.getCreatedAt()))
            .build();
    }

    // Request/Response DTOs (these would typically be in a separate package)
    public static class AddCartItemRequest {
        private String productId;
        private String sku;
        private int quantity;

        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class ProcessPaymentRequest {
        private String orderId;
        private java.math.BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String paymentToken;
        private String idempotencyKey;

        // Getters and setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getPaymentToken() { return paymentToken; }
        public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    }

    public static class CreateShipmentRequest {
        private String orderId;
        private String carrier;
        private String serviceType;

        // Getters and setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCarrier() { return carrier; }
        public void setCarrier(String carrier) { this.carrier = carrier; }
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    }

    public static class InventoryAvailabilityResponse {
        private boolean available;
        private String productId;
        private int availableQuantity;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private InventoryAvailabilityResponse response = new InventoryAvailabilityResponse();

            public Builder available(boolean available) { response.available = available; return this; }
            public Builder productId(String productId) { response.productId = productId; return this; }
            public Builder availableQuantity(int availableQuantity) { response.availableQuantity = availableQuantity; return this; }
            public InventoryAvailabilityResponse build() { return response; }
        }

        // Getters
        public boolean isAvailable() { return available; }
        public String getProductId() { return productId; }
        public int getAvailableQuantity() { return availableQuantity; }
    }

    public static class PaymentResponse {
        private String id;
        private String orderId;
        private String status;
        private String transactionId;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PaymentResponse response = new PaymentResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder orderId(String orderId) { response.orderId = orderId; return this; }
            public Builder status(String status) { response.status = status; return this; }
            public Builder transactionId(String transactionId) { response.transactionId = transactionId; return this; }
            public PaymentResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public String getOrderId() { return orderId; }
        public String getStatus() { return status; }
        public String getTransactionId() { return transactionId; }
    }

    public static class ShippingResponse {
        private String id;
        private String orderId;
        private String trackingNumber;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ShippingResponse response = new ShippingResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder orderId(String orderId) { response.orderId = orderId; return this; }
            public Builder trackingNumber(String trackingNumber) { response.trackingNumber = trackingNumber; return this; }
            public ShippingResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public String getOrderId() { return orderId; }
        public String getTrackingNumber() { return trackingNumber; }
    }

    public static class AuthResponse {
        private String token;
        private String userId;
        private String expiresAt;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AuthResponse response = new AuthResponse();

            public Builder token(String token) { response.token = token; return this; }
            public Builder userId(String userId) { response.userId = userId; return this; }
            public Builder expiresAt(String expiresAt) { response.expiresAt = expiresAt; return this; }
            public AuthResponse build() { return response; }
        }

        // Getters
        public String getToken() { return token; }
        public String getUserId() { return userId; }
        public String getExpiresAt() { return expiresAt; }
    }

    public static class TokenValidationResponse {
        private boolean valid;
        private String userId;
        private String expiresAt;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private TokenValidationResponse response = new TokenValidationResponse();

            public Builder valid(boolean valid) { response.valid = valid; return this; }
            public Builder userId(String userId) { response.userId = userId; return this; }
            public Builder expiresAt(String expiresAt) { response.expiresAt = expiresAt; return this; }
            public TokenValidationResponse build() { return response; }
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getUserId() { return userId; }
        public String getExpiresAt() { return expiresAt; }
    }

    public static class NotificationResponse {
        private String id;
        private boolean success;
        private String errorMessage;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private NotificationResponse response = new NotificationResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder success(boolean success) { response.success = success; return this; }
            public Builder errorMessage(String errorMessage) { response.errorMessage = errorMessage; return this; }
            public NotificationResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class SendNotificationRequest {
        private String templateId;
        private String channel;
        private String priority;
        private java.util.Map<String, String> templateData;
        private String idempotencyKey;

        // Getters and setters
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public java.util.Map<String, String> getTemplateData() { return templateData; }
        public void setTemplateData(java.util.Map<String, String> templateData) { this.templateData = templateData; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    }

    public static class ReviewResponse {
        private String id;
        private String userId;
        private String productId;
        private int rating;
        private String title;
        private String comment;
        private String createdAt;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ReviewResponse response = new ReviewResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder userId(String userId) { response.userId = userId; return this; }
            public Builder productId(String productId) { response.productId = productId; return this; }
            public Builder rating(int rating) { response.rating = rating; return this; }
            public Builder title(String title) { response.title = title; return this; }
            public Builder comment(String comment) { response.comment = comment; return this; }
            public Builder createdAt(String createdAt) { response.createdAt = createdAt; return this; }
            public ReviewResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public String getUserId() { return userId; }
        public String getProductId() { return productId; }
        public int getRating() { return rating; }
        public String getTitle() { return title; }
        public String getComment() { return comment; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class CreateReviewRequest {
        private String productId;
        private int rating;
        private String title;
        private String comment;

        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
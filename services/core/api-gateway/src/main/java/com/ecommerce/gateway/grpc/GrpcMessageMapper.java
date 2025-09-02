package com.ecommerce.gateway.grpc;

import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.userservice.proto.UserServiceProtos;
import com.ecommerce.productservice.proto.ProductServiceProtos;
import com.ecommerce.cartservice.proto.CartServiceProtos;
import com.ecommerce.orderservice.proto.OrderServiceProtos;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between gRPC messages and REST DTOs
 */
@Component
public class GrpcMessageMapper {

    // Common mappings
    public CommonProtos.TenantContext buildTenantContext(String tenantId, String userId, String correlationId) {
        CommonProtos.TenantContext.Builder builder = CommonProtos.TenantContext.newBuilder();
        
        if (tenantId != null) {
            builder.setTenantId(tenantId);
        }
        if (userId != null) {
            builder.setUserId(userId);
        }
        if (correlationId != null) {
            builder.setCorrelationId(correlationId);
        }
        
        return builder.build();
    }

    public BigDecimal mapMoney(CommonProtos.Money grpcMoney) {
        if (grpcMoney == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(grpcMoney.getAmountCents())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public CommonProtos.Money mapToGrpcMoney(BigDecimal amount, String currency) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (currency == null) {
            currency = "USD";
        }
        
        return CommonProtos.Money.newBuilder()
            .setAmountCents(amount.multiply(BigDecimal.valueOf(100)).longValue())
            .setCurrency(currency)
            .build();
    }

    // User service mappings
    public UserProfileResponse mapToUserProfileResponse(UserServiceProtos.GetUserResponse grpcResponse) {
        UserServiceProtos.User user = grpcResponse.getUser();
        
        return UserProfileResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .active(user.getIsActive())
            .build();
    }

    public List<UserAddressResponse> mapToUserAddressesResponse(UserServiceProtos.GetUserAddressesResponse grpcResponse) {
        return grpcResponse.getAddressesList().stream()
            .map(this::mapToUserAddressResponse)
            .collect(Collectors.toList());
    }

    private UserAddressResponse mapToUserAddressResponse(UserServiceProtos.UserAddress grpcAddress) {
        return UserAddressResponse.builder()
            .id(grpcAddress.getId())
            .type(grpcAddress.getType())
            .streetAddress(grpcAddress.getAddress().getStreetAddress())
            .city(grpcAddress.getAddress().getCity())
            .state(grpcAddress.getAddress().getState())
            .postalCode(grpcAddress.getAddress().getPostalCode())
            .country(grpcAddress.getAddress().getCountry())
            .isDefault(grpcAddress.getIsDefault())
            .build();
    }

    // Product service mappings
    public ProductResponse mapToProductResponse(ProductServiceProtos.GetProductResponse grpcResponse) {
        ProductServiceProtos.Product product = grpcResponse.getProduct();
        
        return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .sku(product.getSku())
            .price(mapMoney(product.getPrice()))
            .category(product.getCategory())
            .brand(product.getBrand())
            .active(product.getIsActive())
            .imageUrls(product.getImageUrlsList())
            .attributes(product.getAttributesMap())
            .build();
    }

    public List<ProductResponse> mapToProductsResponse(ProductServiceProtos.GetProductsByIdsResponse grpcResponse) {
        return grpcResponse.getProductsList().stream()
            .map(product -> ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(mapMoney(product.getPrice()))
                .category(product.getCategory())
                .brand(product.getBrand())
                .active(product.getIsActive())
                .imageUrls(product.getImageUrlsList())
                .attributes(product.getAttributesMap())
                .build())
            .collect(Collectors.toList());
    }

    // Cart service mappings
    public CartResponse mapToCartResponse(CartServiceProtos.GetCartResponse grpcResponse) {
        CartServiceProtos.Cart cart = grpcResponse.getCart();
        
        return CartResponse.builder()
            .id(String.valueOf(cart.getUserId())) // Using userId as cart ID since proto doesn't have separate cart ID
            .userId(cart.getUserId())
            .items(cart.getItemsList().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList()))
            .totalAmount(mapMoney(cart.getTotalAmount()))
            .build();
    }

    public CartResponse mapToCartResponse(CartServiceProtos.Cart cart) {
        return CartResponse.builder()
            .id(String.valueOf(cart.getUserId())) // Using userId as cart ID since proto doesn't have separate cart ID
            .userId(cart.getUserId())
            .items(cart.getItemsList().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList()))
            .totalAmount(mapMoney(cart.getTotalAmount()))
            .build();
    }

    private CartItemResponse mapToCartItemResponse(CartServiceProtos.CartItem grpcItem) {
        return CartItemResponse.builder()
            .id(grpcItem.getProductId()) // Using productId as item ID since proto doesn't have separate item ID
            .productId(grpcItem.getProductId())
            .sku(grpcItem.getSku())
            .quantity(grpcItem.getQuantity())
            .unitPrice(mapMoney(grpcItem.getUnitPrice()))
            .totalPrice(mapMoney(grpcItem.getTotalPrice()))
            .build();
    }

    // Order service mappings
    public OrderResponse mapToOrderResponse(OrderServiceProtos.Order order) {
        return OrderResponse.builder()
            .id(String.valueOf(order.getId()))
            .userId(order.getUserId())
            .status(order.getStatus())
            .totalAmount(mapMoney(order.getTotalAmount()))
            .items(order.getItemsList().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList()))
            .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderServiceProtos.OrderItem grpcItem) {
        return OrderItemResponse.builder()
            .id(String.valueOf(grpcItem.getId()))
            .productId(grpcItem.getProductId())
            .sku(grpcItem.getSku())
            .quantity(grpcItem.getQuantity())
            .unitPrice(mapMoney(grpcItem.getUnitPrice()))
            .totalPrice(mapMoney(grpcItem.getTotalPrice()))
            .build();
    }

    // Response DTOs (these would typically be in a separate package)
    public static class UserProfileResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private boolean active;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private UserProfileResponse response = new UserProfileResponse();

            public Builder id(Long id) { response.id = id; return this; }
            public Builder firstName(String firstName) { response.firstName = firstName; return this; }
            public Builder lastName(String lastName) { response.lastName = lastName; return this; }
            public Builder email(String email) { response.email = email; return this; }
            public Builder phone(String phone) { response.phone = phone; return this; }
            public Builder active(boolean active) { response.active = active; return this; }
            public UserProfileResponse build() { return response; }
        }

        // Getters
        public Long getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public boolean isActive() { return active; }
    }

    public static class UserAddressResponse {
        private Long id;
        private String type;
        private String streetAddress;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private boolean isDefault;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private UserAddressResponse response = new UserAddressResponse();

            public Builder id(Long id) { response.id = id; return this; }
            public Builder type(String type) { response.type = type; return this; }
            public Builder streetAddress(String streetAddress) { response.streetAddress = streetAddress; return this; }
            public Builder city(String city) { response.city = city; return this; }
            public Builder state(String state) { response.state = state; return this; }
            public Builder postalCode(String postalCode) { response.postalCode = postalCode; return this; }
            public Builder country(String country) { response.country = country; return this; }
            public Builder isDefault(boolean isDefault) { response.isDefault = isDefault; return this; }
            public UserAddressResponse build() { return response; }
        }

        // Getters
        public Long getId() { return id; }
        public String getType() { return type; }
        public String getStreetAddress() { return streetAddress; }
        public String getCity() { return city; }
        public String getState() { return state; }
        public String getPostalCode() { return postalCode; }
        public String getCountry() { return country; }
        public boolean isDefault() { return isDefault; }
    }

    public static class ProductResponse {
        private String id;
        private String name;
        private String description;
        private String sku;
        private BigDecimal price;
        private String category;
        private String brand;
        private boolean active;
        private List<String> imageUrls;
        private java.util.Map<String, String> attributes;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ProductResponse response = new ProductResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder name(String name) { response.name = name; return this; }
            public Builder description(String description) { response.description = description; return this; }
            public Builder sku(String sku) { response.sku = sku; return this; }
            public Builder price(BigDecimal price) { response.price = price; return this; }
            public Builder category(String category) { response.category = category; return this; }
            public Builder brand(String brand) { response.brand = brand; return this; }
            public Builder active(boolean active) { response.active = active; return this; }
            public Builder imageUrls(List<String> imageUrls) { response.imageUrls = imageUrls; return this; }
            public Builder attributes(java.util.Map<String, String> attributes) { response.attributes = attributes; return this; }
            public ProductResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getSku() { return sku; }
        public BigDecimal getPrice() { return price; }
        public String getCategory() { return category; }
        public String getBrand() { return brand; }
        public boolean isActive() { return active; }
        public List<String> getImageUrls() { return imageUrls; }
        public java.util.Map<String, String> getAttributes() { return attributes; }
    }

    public static class CartResponse {
        private String id;
        private Long userId;
        private List<CartItemResponse> items;
        private BigDecimal totalAmount;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CartResponse response = new CartResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder userId(Long userId) { response.userId = userId; return this; }
            public Builder items(List<CartItemResponse> items) { response.items = items; return this; }
            public Builder totalAmount(BigDecimal totalAmount) { response.totalAmount = totalAmount; return this; }
            public CartResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public Long getUserId() { return userId; }
        public List<CartItemResponse> getItems() { return items; }
        public BigDecimal getTotalAmount() { return totalAmount; }
    }

    public static class CartItemResponse {
        private String id;
        private String productId;
        private String sku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CartItemResponse response = new CartItemResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder productId(String productId) { response.productId = productId; return this; }
            public Builder sku(String sku) { response.sku = sku; return this; }
            public Builder quantity(int quantity) { response.quantity = quantity; return this; }
            public Builder unitPrice(BigDecimal unitPrice) { response.unitPrice = unitPrice; return this; }
            public Builder totalPrice(BigDecimal totalPrice) { response.totalPrice = totalPrice; return this; }
            public CartItemResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public String getProductId() { return productId; }
        public String getSku() { return sku; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
    }

    public static class OrderResponse {
        private String id;
        private Long userId;
        private String status;
        private BigDecimal totalAmount;
        private List<OrderItemResponse> items;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private OrderResponse response = new OrderResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder userId(Long userId) { response.userId = userId; return this; }
            public Builder status(String status) { response.status = status; return this; }
            public Builder totalAmount(BigDecimal totalAmount) { response.totalAmount = totalAmount; return this; }
            public Builder items(List<OrderItemResponse> items) { response.items = items; return this; }
            public OrderResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public Long getUserId() { return userId; }
        public String getStatus() { return status; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public List<OrderItemResponse> getItems() { return items; }
    }

    public static class OrderItemResponse {
        private String id;
        private String productId;
        private String sku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private OrderItemResponse response = new OrderItemResponse();

            public Builder id(String id) { response.id = id; return this; }
            public Builder productId(String productId) { response.productId = productId; return this; }
            public Builder sku(String sku) { response.sku = sku; return this; }
            public Builder quantity(int quantity) { response.quantity = quantity; return this; }
            public Builder unitPrice(BigDecimal unitPrice) { response.unitPrice = unitPrice; return this; }
            public Builder totalPrice(BigDecimal totalPrice) { response.totalPrice = totalPrice; return this; }
            public OrderItemResponse build() { return response; }
        }

        // Getters
        public String getId() { return id; }
        public String getProductId() { return productId; }
        public String getSku() { return sku; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
    }
}
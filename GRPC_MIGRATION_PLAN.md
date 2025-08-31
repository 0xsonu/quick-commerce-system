# gRPC Migration Plan - Inter-Service Communication

## Current State Analysis

### ✅ Completed gRPC Implementations

1. **Proto Definitions**: All major services have complete proto definitions

   - `user_service.proto` - User management operations
   - `product_service.proto` - Product catalog operations
   - `inventory_service.proto` - Inventory and reservation operations
   - `order_service.proto` - Order management operations
   - `payment_service.proto` - Payment processing operations
   - `auth_service.proto` - Authentication operations (NEW)
   - `shipping_service.proto` - Shipping operations (NEW)
   - `notification_service.proto` - Notification operations (NEW)
   - `review_service.proto` - Review operations (NEW)
   - `cart_service.proto` - Cart operations (NEW)

2. **gRPC Server Implementations**:

   - ✅ **Product Service**: `ProductGrpcService` - Complete implementation
   - ✅ **Order Service**: `OrderGrpcService` - Complete implementation
   - ✅ **User Service**: `UserGrpcService` - NEW implementation created
   - ✅ **Inventory Service**: `InventoryGrpcService` - NEW implementation created
   - ✅ **Payment Service**: `PaymentGrpcService` - NEW implementation created

3. **gRPC Client Implementations**:

   - ✅ **Cart Service**: Uses gRPC clients for Product and Inventory services
   - ✅ **Order Service**: Uses gRPC clients for User, Inventory, and Payment services
   - ✅ **Notification Service**: Uses gRPC client for User service

4. **gRPC Configuration**:
   - ✅ **Product Service**: Port 9084
   - ✅ **User Service**: Port 9083 (UPDATED)
   - ✅ **Inventory Service**: Port 9086 (UPDATED)
   - ✅ **Order Service**: Port 9086 (server) + client configs
   - ✅ **Payment Service**: Port 9088 (NEW)
   - ✅ **Cart Service**: Client configs updated
   - ✅ **Notification Service**: Client config updated

### ❌ Missing gRPC Implementations

1. **Auth Service**: Needs gRPC server implementation
2. **Shipping Service**: Needs gRPC server implementation
3. **Review Service**: Needs gRPC server implementation
4. **Cart Service**: Needs gRPC server implementation (currently only has clients)

### 🔄 Services with Mixed Communication

Currently, some services still expose REST endpoints for inter-service communication alongside gRPC. These need to be migrated to pure gRPC.

## Migration Steps to Complete

### Step 1: Complete Missing gRPC Server Implementations

#### 1.1 Auth Service gRPC Implementation

```bash
# Create: services/core/auth-service/src/main/java/com/ecommerce/authservice/grpc/AuthGrpcService.java
# Update: services/core/auth-service/src/main/resources/application.yml (add grpc.server.port: 9082)
```

#### 1.2 Shipping Service gRPC Implementation

```bash
# Create: services/fulfillment/shipping-service/src/main/java/com/ecommerce/shippingservice/grpc/ShippingGrpcService.java
# Update: services/fulfillment/shipping-service/src/main/resources/application.yml (add grpc.server.port: 9089)
```

#### 1.3 Review Service gRPC Implementation

```bash
# Create: services/engagement/review-service/src/main/java/com/ecommerce/reviewservice/grpc/ReviewGrpcService.java
# Update: services/engagement/review-service/src/main/resources/application.yml (add grpc.server.port: 9091)
```

#### 1.4 Cart Service gRPC Implementation

```bash
# Create: services/commerce/cart-service/src/main/java/com/ecommerce/cartservice/grpc/CartGrpcService.java
# Update: services/commerce/cart-service/src/main/resources/application.yml (add grpc.server.port: 9085)
```

### Step 2: Add Missing gRPC Client Dependencies

Services that need to communicate with other services should have gRPC clients:

#### 2.1 Order Service (COMPLETED)

- ✅ User Service client (for user validation)
- ✅ Inventory Service client (for stock reservation)
- ✅ Payment Service client (for payment processing)
- ✅ Product Service client (for product validation)

#### 2.2 Shipping Service

- ❌ Order Service client (for order details)
- ❌ User Service client (for shipping addresses)

#### 2.3 Notification Service (PARTIALLY COMPLETED)

- ✅ User Service client (for user preferences)
- ❌ Order Service client (for order notifications)

#### 2.4 Review Service

- ❌ Product Service client (for product validation)
- ❌ User Service client (for user validation)
- ❌ Order Service client (for purchase verification)

### Step 3: Update Service Dependencies

#### 3.1 Add gRPC Dependencies to Missing Services

All services need these dependencies in their `pom.xml`:

```xml
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>shared-proto</artifactId>
</dependency>
```

#### 3.2 Update Shared Libraries

Ensure all services depend on:

- `shared-proto` (for gRPC definitions)
- `shared-tracing` (for gRPC tracing interceptors)
- `shared-security` (for tenant context)

### Step 4: Remove REST-based Inter-Service Communication

#### 4.1 Identify REST Controllers Used for Inter-Service Communication

- Review controllers that are called by other services (not external clients)
- Keep only external-facing REST APIs
- Replace internal REST calls with gRPC calls

#### 4.2 Update API Gateway Routes

- Remove routes for internal service-to-service endpoints
- Keep only external client-facing routes

### Step 5: Update Docker Compose and Kubernetes Configurations

#### 5.1 Docker Compose Updates

```yaml
# Update docker-compose.yml to expose gRPC ports
services:
  user-service:
    ports:
      - "8083:8083" # REST (external)
      - "9083:9083" # gRPC (internal)

  inventory-service:
    ports:
      - "8086:8086" # REST (external)
      - "9086:9086" # gRPC (internal)


  # ... similar for all services
```

#### 5.2 Kubernetes Service Definitions

```yaml
# Update Kubernetes services to expose gRPC ports
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  ports:
    - name: http
      port: 8083
      targetPort: 8083
    - name: grpc
      port: 9083
      targetPort: 9083
```

### Step 6: Testing and Validation

#### 6.1 gRPC Health Checks

- Implement gRPC health check services
- Update Kubernetes liveness/readiness probes for gRPC

#### 6.2 Integration Testing

- Test all inter-service gRPC communication
- Verify tenant context propagation
- Test error handling and retries

#### 6.3 Performance Testing

- Compare gRPC vs REST performance
- Validate latency improvements
- Test under load

## Benefits of Pure gRPC Communication

1. **Performance**: Binary protocol, HTTP/2, multiplexing
2. **Type Safety**: Strong typing with Protocol Buffers
3. **Efficiency**: Smaller payload sizes, faster serialization
4. **Streaming**: Support for bidirectional streaming
5. **Language Agnostic**: Easy to add services in other languages
6. **Built-in Features**: Load balancing, retries, timeouts

## gRPC Port Assignments

| Service              | REST Port | gRPC Port | Status        |
| -------------------- | --------- | --------- | ------------- |
| API Gateway          | 8080      | -         | External only |
| Auth Service         | 8082      | 9082      | ❌ Need gRPC  |
| User Service         | 8083      | 9083      | ✅ Complete   |
| Product Service      | 8084      | 9084      | ✅ Complete   |
| Cart Service         | 8085      | 9085      | ❌ Need gRPC  |
| Inventory Service    | 8086      | 9086      | ✅ Complete   |
| Order Service        | 8087      | 9087      | ✅ Complete   |
| Payment Service      | 8088      | 9088      | ✅ Complete   |
| Shipping Service     | 8089      | 9089      | ❌ Need gRPC  |
| Notification Service | 8090      | 9090      | ❌ Need gRPC  |
| Review Service       | 8091      | 9091      | ❌ Need gRPC  |

## Next Actions Required

1. **Immediate**: Complete the 4 missing gRPC server implementations
2. **Short-term**: Add missing gRPC client dependencies and configurations
3. **Medium-term**: Remove REST-based inter-service communication
4. **Long-term**: Optimize gRPC configurations for production (load balancing, retries, etc.)

The foundation is solid with proto definitions and core service implementations complete. The remaining work is primarily implementing the missing gRPC services and updating configurations.

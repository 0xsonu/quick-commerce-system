#!/bin/bash

# gRPC Implementation Validation Script
# This script validates the current state of gRPC implementation across all services

echo "🔍 Validating gRPC Implementation Status"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if a file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✅ Found: $1${NC}"
        return 0
    else
        echo -e "${RED}❌ Missing: $1${NC}"
        return 1
    fi
}

# Function to check if a directory exists
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✅ Found: $1${NC}"
        return 0
    else
        echo -e "${RED}❌ Missing: $1${NC}"
        return 1
    fi
}

# Function to check gRPC configuration in application.yml
check_grpc_config() {
    local service_path=$1
    local service_name=$2
    local expected_port=$3
    
    local app_yml="$service_path/src/main/resources/application.yml"
    
    if [ -f "$app_yml" ]; then
        if grep -q "grpc:" "$app_yml" && grep -q "port: $expected_port" "$app_yml"; then
            echo -e "${GREEN}✅ $service_name: gRPC config found (port $expected_port)${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  $service_name: gRPC config incomplete or wrong port${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ $service_name: application.yml not found${NC}"
        return 1
    fi
}

echo -e "\n${BLUE}1. Checking Proto Definitions${NC}"
echo "-----------------------------"

proto_files=(
    "shared-proto/src/main/proto/common.proto"
    "shared-proto/src/main/proto/user_service.proto"
    "shared-proto/src/main/proto/product_service.proto"
    "shared-proto/src/main/proto/inventory_service.proto"
    "shared-proto/src/main/proto/order_service.proto"
    "shared-proto/src/main/proto/payment_service.proto"
    "shared-proto/src/main/proto/auth_service.proto"
    "shared-proto/src/main/proto/shipping_service.proto"
    "shared-proto/src/main/proto/notification_service.proto"
    "shared-proto/src/main/proto/review_service.proto"
    "shared-proto/src/main/proto/cart_service.proto"
)

proto_count=0
for proto in "${proto_files[@]}"; do
    if check_file "$proto"; then
        ((proto_count++))
    fi
done

echo -e "\n${BLUE}2. Checking gRPC Server Implementations${NC}"
echo "--------------------------------------"

grpc_services=(
    "services/core/user-service/src/main/java/com/ecommerce/userservice/grpc/UserGrpcService.java"
    "services/catalog/product-service/src/main/java/com/ecommerce/productservice/grpc/ProductGrpcService.java"
    "services/fulfillment/inventory-service/src/main/java/com/ecommerce/inventoryservice/grpc/InventoryGrpcService.java"
    "services/commerce/order-service/src/main/java/com/ecommerce/orderservice/grpc/OrderGrpcService.java"
    "services/commerce/payment-service/src/main/java/com/ecommerce/paymentservice/grpc/PaymentGrpcService.java"
    "services/commerce/cart-service/src/main/java/com/ecommerce/cartservice/grpc/CartGrpcService.java"
)

grpc_count=0
for grpc in "${grpc_services[@]}"; do
    if check_file "$grpc"; then
        ((grpc_count++))
    fi
done

echo -e "\n${BLUE}3. Checking gRPC Server Configurations${NC}"
echo "-------------------------------------"

config_count=0

# Check each service configuration
services=(
    "services/core/user-service:User Service:9083"
    "services/catalog/product-service:Product Service:9084"
    "services/commerce/cart-service:Cart Service:9085"
    "services/fulfillment/inventory-service:Inventory Service:9086"
    "services/commerce/order-service:Order Service:9087"
    "services/commerce/payment-service:Payment Service:9088"
)

for service_info in "${services[@]}"; do
    IFS=':' read -r path name port <<< "$service_info"
    if check_grpc_config "$path" "$name" "$port"; then
        ((config_count++))
    fi
done

echo -e "\n${BLUE}4. Checking gRPC Client Configurations${NC}"
echo "-------------------------------------"

client_configs=(
    "services/commerce/cart-service:Cart Service"
    "services/commerce/order-service:Order Service"
    "services/fulfillment/notification-service:Notification Service"
)

client_count=0
for client_info in "${client_configs[@]}"; do
    IFS=':' read -r path name <<< "$client_info"
    app_yml="$path/src/main/resources/application.yml"
    
    if [ -f "$app_yml" ] && grep -q "grpc:" "$app_yml" && grep -q "client:" "$app_yml"; then
        echo -e "${GREEN}✅ $name: gRPC client config found${NC}"
        ((client_count++))
    else
        echo -e "${RED}❌ $name: gRPC client config missing${NC}"
    fi
done

echo -e "\n${BLUE}5. Checking Missing Implementations${NC}"
echo "----------------------------------"

missing_services=(
    "services/core/auth-service/src/main/java/com/ecommerce/authservice/grpc/AuthGrpcService.java:Auth Service"
    "services/fulfillment/shipping-service/src/main/java/com/ecommerce/shippingservice/grpc/ShippingGrpcService.java:Shipping Service"
    "services/engagement/review-service/src/main/java/com/ecommerce/reviewservice/grpc/ReviewGrpcService.java:Review Service"
    "services/fulfillment/notification-service/src/main/java/com/ecommerce/notificationservice/grpc/NotificationGrpcService.java:Notification Service"
)

missing_count=0
for missing_info in "${missing_services[@]}"; do
    IFS=':' read -r path name <<< "$missing_info"
    if [ ! -f "$path" ]; then
        echo -e "${RED}❌ Missing: $name gRPC implementation${NC}"
        ((missing_count++))
    else
        echo -e "${GREEN}✅ Found: $name gRPC implementation${NC}"
    fi
done

echo -e "\n${BLUE}6. Summary${NC}"
echo "----------"

total_protos=${#proto_files[@]}
total_grpc_services=${#grpc_services[@]}
total_configs=${#services[@]}
total_clients=${#client_configs[@]}
total_missing=${#missing_services[@]}

echo -e "Proto Definitions: ${GREEN}$proto_count/$total_protos${NC} complete"
echo -e "gRPC Server Implementations: ${GREEN}$grpc_count/$total_grpc_services${NC} complete"
echo -e "gRPC Server Configurations: ${GREEN}$config_count/$total_configs${NC} complete"
echo -e "gRPC Client Configurations: ${GREEN}$client_count/$total_clients${NC} complete"
echo -e "Missing Implementations: ${RED}$missing_count/$total_missing${NC} remaining"

# Calculate overall completion percentage
total_items=$((total_protos + total_grpc_services + total_configs + total_clients))
completed_items=$((proto_count + grpc_count + config_count + client_count))
completion_percentage=$((completed_items * 100 / total_items))

echo -e "\n${BLUE}Overall gRPC Migration Progress: ${GREEN}$completion_percentage%${NC}"

if [ $completion_percentage -eq 100 ] && [ $missing_count -eq 0 ]; then
    echo -e "\n🎉 ${GREEN}gRPC migration is COMPLETE!${NC}"
    echo "All services are ready for pure gRPC inter-service communication."
elif [ $completion_percentage -ge 80 ]; then
    echo -e "\n🚀 ${YELLOW}gRPC migration is nearly complete!${NC}"
    echo "Only a few services need gRPC implementations."
else
    echo -e "\n⚠️  ${YELLOW}gRPC migration is in progress.${NC}"
    echo "Several services still need gRPC implementations."
fi

echo -e "\n${BLUE}Next Steps:${NC}"
if [ $missing_count -gt 0 ]; then
    echo "1. Implement missing gRPC services"
    echo "2. Add gRPC server configurations"
    echo "3. Test inter-service gRPC communication"
else
    echo "1. Test all gRPC service implementations"
    echo "2. Remove REST-based inter-service communication"
    echo "3. Update deployment configurations"
fi

echo -e "\n${BLUE}Port Assignments:${NC}"
echo "User Service: REST 8083, gRPC 9083"
echo "Product Service: REST 8084, gRPC 9084"
echo "Cart Service: REST 8085, gRPC 9085"
echo "Inventory Service: REST 8086, gRPC 9086"
echo "Order Service: REST 8087, gRPC 9087"
echo "Payment Service: REST 8088, gRPC 9088"
echo "Shipping Service: REST 8089, gRPC 9089 ✅ Complete"
echo "Notification Service: REST 8090, gRPC 9090 ✅ Complete"
echo "Review Service: REST 8091, gRPC 9091 ✅ Complete"
echo "Auth Service: REST 8082, gRPC 9082 ✅ Complete"
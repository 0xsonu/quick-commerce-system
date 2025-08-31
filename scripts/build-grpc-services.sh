#!/bin/bash

# Build script for all gRPC services
# This script builds all services with gRPC implementations to ensure they compile correctly

echo "🔨 Building All gRPC Services"
echo "============================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to build a service
build_service() {
    local service_path=$1
    local service_name=$2
    
    echo -e "\n${BLUE}Building $service_name...${NC}"
    
    if [ -d "$service_path" ]; then
        cd "$service_path"
        
        if mvn clean compile -q; then
            echo -e "${GREEN}✅ $service_name: Build successful${NC}"
            cd - > /dev/null
            return 0
        else
            echo -e "${RED}❌ $service_name: Build failed${NC}"
            cd - > /dev/null
            return 1
        fi
    else
        echo -e "${RED}❌ $service_name: Directory not found${NC}"
        return 1
    fi
}

# Build shared-proto first (required by all services)
echo -e "${BLUE}Building shared-proto (required by all services)...${NC}"
if build_service "shared-proto" "Shared Proto"; then
    echo -e "${GREEN}✅ Shared Proto build successful${NC}"
else
    echo -e "${RED}❌ Shared Proto build failed - stopping build${NC}"
    exit 1
fi

# Services to build
services=(
    "services/core/auth-service:Auth Service"
    "services/core/user-service:User Service"
    "services/catalog/product-service:Product Service"
    "services/commerce/cart-service:Cart Service"
    "services/fulfillment/inventory-service:Inventory Service"
    "services/commerce/order-service:Order Service"
    "services/commerce/payment-service:Payment Service"
    "services/fulfillment/shipping-service:Shipping Service"
    "services/fulfillment/notification-service:Notification Service"
    "services/engagement/review-service:Review Service"
)

successful_builds=0
failed_builds=0

# Build each service
for service_info in "${services[@]}"; do
    IFS=':' read -r path name <<< "$service_info"
    
    if build_service "$path" "$name"; then
        ((successful_builds++))
    else
        ((failed_builds++))
    fi
done

# Summary
echo -e "\n${BLUE}Build Summary${NC}"
echo "============="
echo -e "Successful builds: ${GREEN}$successful_builds${NC}"
echo -e "Failed builds: ${RED}$failed_builds${NC}"

total_services=${#services[@]}
success_percentage=$((successful_builds * 100 / total_services))

if [ $failed_builds -eq 0 ]; then
    echo -e "\n🎉 ${GREEN}All gRPC services built successfully!${NC}"
    echo "The system is ready for deployment with pure gRPC communication."
elif [ $success_percentage -ge 80 ]; then
    echo -e "\n⚠️  ${YELLOW}Most services built successfully ($success_percentage%)${NC}"
    echo "Check the failed services for compilation issues."
else
    echo -e "\n❌ ${RED}Multiple build failures detected${NC}"
    echo "Please fix compilation errors before deployment."
fi

echo -e "\n${BLUE}gRPC Service Status:${NC}"
echo "Auth Service (9082): gRPC server + JWT validation"
echo "User Service (9083): gRPC server + user management"
echo "Product Service (9084): gRPC server + catalog operations"
echo "Cart Service (9085): gRPC server + shopping cart"
echo "Inventory Service (9086): gRPC server + stock management"
echo "Order Service (9087): gRPC server + order processing"
echo "Payment Service (9088): gRPC server + payment processing"
echo "Shipping Service (9089): gRPC server + shipment tracking"
echo "Notification Service (9090): gRPC server + notifications"
echo "Review Service (9091): gRPC server + product reviews"

exit $failed_builds
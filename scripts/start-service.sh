#!/bin/bash

# Script to start individual services
SERVICE_NAME=$1

if [ -z "$SERVICE_NAME" ]; then
    echo "Usage: $0 <service-name>"
    echo ""
    echo "Available services:"
    echo "  api-gateway"
    echo "  auth-service"
    echo "  user-service"
    echo "  product-service"
    echo "  inventory-service"
    echo "  cart-service"
    echo "  order-service"
    echo "  payment-service"
    echo "  shipping-service"
    echo "  notification-service"
    echo "  review-service"
    exit 1
fi

# Map service names to their paths
case $SERVICE_NAME in
    "api-gateway")
        SERVICE_PATH="services/core/api-gateway"
        ;;
    "auth-service")
        SERVICE_PATH="services/core/auth-service"
        ;;
    "user-service")
        SERVICE_PATH="services/core/user-service"
        ;;
    "product-service")
        SERVICE_PATH="services/catalog/product-service"
        ;;
    "inventory-service")
        SERVICE_PATH="services/catalog/inventory-service"
        ;;
    "cart-service")
        SERVICE_PATH="services/commerce/cart-service"
        ;;
    "order-service")
        SERVICE_PATH="services/commerce/order-service"
        ;;
    "payment-service")
        SERVICE_PATH="services/commerce/payment-service"
        ;;
    "shipping-service")
        SERVICE_PATH="services/fulfillment/shipping-service"
        ;;
    "notification-service")
        SERVICE_PATH="services/fulfillment/notification-service"
        ;;
    "review-service")
        SERVICE_PATH="services/engagement/review-service"
        ;;
    *)
        echo "Error: Unknown service '$SERVICE_NAME'"
        exit 1
        ;;
esac

# Check if service directory exists
if [ ! -d "$SERVICE_PATH" ]; then
    echo "Error: Service directory '$SERVICE_PATH' not found!"
    echo "Make sure you're in the project root directory."
    exit 1
fi

# Check if service has a POM file
if [ ! -f "$SERVICE_PATH/pom.xml" ]; then
    echo "Error: No pom.xml found in '$SERVICE_PATH' directory!"
    exit 1
fi

echo "Starting $SERVICE_NAME from $SERVICE_PATH..."
echo "Press Ctrl+C to stop the service"
echo ""

cd "$SERVICE_PATH" && mvn spring-boot:run
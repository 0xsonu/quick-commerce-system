#!/bin/bash

echo "Building Amazon Shopping Backend..."

# Build parent project and shared modules first
echo "Building parent project and shared modules..."
mvn clean install -DskipTests -pl shared-models,shared-utils,shared-security

if [ $? -ne 0 ]; then
    echo "❌ Failed to build shared modules"
    exit 1
fi

echo "✅ Shared modules built successfully"

# Build all services
echo "Building all services..."
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Failed to build services"
    exit 1
fi

echo "✅ All services built successfully"

echo ""
echo "🚀 Build completed!"
echo ""
echo "You can now start the services with:"
echo "  ./scripts/start-services.sh"
echo ""
echo "Or start individual services with:"
echo "  cd services/<domain>/<service-name> && mvn spring-boot:run"
echo ""
echo "Service structure:"
echo "  Core: api-gateway, auth-service, user-service"
echo "  Catalog: product-service, inventory-service"
echo "  Commerce: cart-service, order-service, payment-service"
echo "  Fulfillment: shipping-service, notification-service"
echo "  Engagement: review-service"
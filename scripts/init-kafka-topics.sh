#!/bin/bash

# Kafka Topics Initialization Script
# Creates all required topics for the e-commerce microservices

set -e

KAFKA_CONTAINER="ecommerce-kafka"
KAFKA_BROKER="kafka:29092"

echo "🚀 Initializing Kafka topics for e-commerce backend..."

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
until docker exec $KAFKA_CONTAINER kafka-broker-api-versions --bootstrap-server $KAFKA_BROKER > /dev/null 2>&1; do
    echo "Kafka is not ready yet. Waiting..."
    sleep 5
done

echo "✅ Kafka is ready. Creating topics..."

# Function to create a topic
create_topic() {
    local topic_name=$1
    local partitions=${2:-3}
    local replication_factor=${3:-1}
    local retention_ms=${4:-604800000} # 7 days default
    
    echo "📝 Creating topic: $topic_name (partitions: $partitions, replication: $replication_factor)"
    
    docker exec $KAFKA_CONTAINER kafka-topics \
        --create \
        --bootstrap-server $KAFKA_BROKER \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication_factor \
        --config retention.ms=$retention_ms \
        --config cleanup.policy=delete \
        --if-not-exists
}

# Function to create a compacted topic (for state changes)
create_compacted_topic() {
    local topic_name=$1
    local partitions=${2:-3}
    local replication_factor=${3:-1}
    
    echo "📝 Creating compacted topic: $topic_name (partitions: $partitions, replication: $replication_factor)"
    
    docker exec $KAFKA_CONTAINER kafka-topics \
        --create \
        --bootstrap-server $KAFKA_BROKER \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication_factor \
        --config cleanup.policy=compact \
        --config min.cleanable.dirty.ratio=0.1 \
        --config segment.ms=86400000 \
        --if-not-exists
}

# ===== ORDER EVENTS =====
echo "🛒 Creating order-related topics..."
create_topic "order-events" 6 1 2592000000  # 30 days retention
create_topic "order-created" 6 1 2592000000
create_topic "order-confirmed" 6 1 2592000000
create_topic "order-cancelled" 6 1 2592000000
create_topic "order-shipped" 6 1 2592000000
create_topic "order-delivered" 6 1 2592000000
create_topic "order-refunded" 6 1 2592000000

# Order saga coordination
create_topic "order-saga-events" 6 1 1209600000  # 14 days retention
create_compacted_topic "order-saga-state" 6 1

# ===== PRODUCT EVENTS =====
echo "📦 Creating product-related topics..."
create_topic "product-events" 3 1 2592000000  # 30 days retention
create_topic "product-created" 3 1 2592000000
create_topic "product-updated" 3 1 2592000000
create_topic "product-deleted" 3 1 2592000000
create_topic "product-price-changed" 3 1 2592000000
create_topic "product-inventory-updated" 3 1 604800000  # 7 days retention

# ===== PAYMENT EVENTS =====
echo "💳 Creating payment-related topics..."
create_topic "payment-events" 6 1 5184000000  # 60 days retention (compliance)
create_topic "payment-initiated" 6 1 5184000000
create_topic "payment-succeeded" 6 1 5184000000
create_topic "payment-failed" 6 1 5184000000
create_topic "payment-refunded" 6 1 5184000000
create_topic "payment-webhook" 6 1 2592000000  # 30 days retention

# ===== INVENTORY EVENTS =====
echo "📊 Creating inventory-related topics..."
create_topic "inventory-events" 6 1 1209600000  # 14 days retention
create_topic "inventory-reserved" 6 1 1209600000
create_topic "inventory-released" 6 1 1209600000
create_topic "inventory-updated" 6 1 1209600000
create_topic "inventory-low-stock" 3 1 604800000  # 7 days retention
create_topic "inventory-restock-needed" 3 1 604800000

# Inventory state (compacted for current stock levels)
create_compacted_topic "inventory-state" 6 1

# ===== SHIPPING EVENTS =====
echo "🚚 Creating shipping-related topics..."
create_topic "shipping-events" 3 1 2592000000  # 30 days retention
create_topic "shipment-created" 3 1 2592000000
create_topic "shipment-picked-up" 3 1 2592000000
create_topic "shipment-in-transit" 3 1 2592000000
create_topic "shipment-delivered" 3 1 2592000000
create_topic "shipment-exception" 3 1 2592000000
create_topic "tracking-updated" 3 1 1209600000  # 14 days retention

# ===== USER EVENTS =====
echo "👤 Creating user-related topics..."
create_topic "user-events" 3 1 2592000000  # 30 days retention
create_topic "user-registered" 3 1 2592000000
create_topic "user-updated" 3 1 1209600000  # 14 days retention
create_topic "user-preferences-changed" 3 1 604800000  # 7 days retention

# ===== CART EVENTS =====
echo "🛍️ Creating cart-related topics..."
create_topic "cart-events" 3 1 604800000  # 7 days retention
create_topic "cart-updated" 3 1 604800000
create_topic "cart-abandoned" 3 1 604800000
create_topic "cart-converted" 3 1 1209600000  # 14 days retention

# ===== NOTIFICATION EVENTS =====
echo "📧 Creating notification-related topics..."
create_topic "notification-events" 3 1 604800000  # 7 days retention
create_topic "email-notifications" 3 1 604800000
create_topic "sms-notifications" 3 1 604800000
create_topic "push-notifications" 3 1 604800000
create_topic "notification-delivery-status" 3 1 259200000  # 3 days retention

# ===== REVIEW EVENTS =====
echo "⭐ Creating review-related topics..."
create_topic "review-events" 3 1 2592000000  # 30 days retention
create_topic "review-created" 3 1 2592000000
create_topic "review-updated" 3 1 1209600000  # 14 days retention
create_topic "review-moderated" 3 1 2592000000
create_topic "review-flagged" 3 1 2592000000

# ===== ANALYTICS AND MONITORING =====
echo "📈 Creating analytics and monitoring topics..."
create_topic "analytics-events" 6 1 2592000000  # 30 days retention
create_topic "user-activity" 6 1 604800000  # 7 days retention
create_topic "system-metrics" 3 1 259200000  # 3 days retention
create_topic "audit-logs" 6 1 7776000000  # 90 days retention (compliance)

# ===== DEAD LETTER QUEUES =====
echo "💀 Creating dead letter queue topics..."
create_topic "dlq-order-events" 3 1 2592000000  # 30 days retention
create_topic "dlq-payment-events" 3 1 2592000000
create_topic "dlq-inventory-events" 3 1 2592000000
create_topic "dlq-shipping-events" 3 1 2592000000
create_topic "dlq-notification-events" 3 1 2592000000
create_topic "dlq-general" 3 1 2592000000

# ===== INTEGRATION TOPICS =====
echo "🔗 Creating integration topics..."
create_topic "external-webhook-events" 3 1 604800000  # 7 days retention
create_topic "third-party-sync" 3 1 1209600000  # 14 days retention

echo ""
echo "✅ All Kafka topics created successfully!"
echo ""

# List all topics to verify creation
echo "📋 Listing all created topics:"
docker exec $KAFKA_CONTAINER kafka-topics --list --bootstrap-server $KAFKA_BROKER | sort

echo ""
echo "🎉 Kafka topics initialization completed!"
echo ""
echo "📊 Topic Summary:"
echo "   - Order topics: 8"
echo "   - Product topics: 6" 
echo "   - Payment topics: 6"
echo "   - Inventory topics: 7"
echo "   - Shipping topics: 6"
echo "   - User topics: 4"
echo "   - Cart topics: 4"
echo "   - Notification topics: 5"
echo "   - Review topics: 5"
echo "   - Analytics topics: 4"
echo "   - Dead letter queues: 6"
echo "   - Integration topics: 2"
echo "   - Total topics: ~63"
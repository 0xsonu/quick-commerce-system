-- Shipping Service Database Schema
USE shipping_service;

-- Shipments table
CREATE TABLE shipments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    shipment_number VARCHAR(100) NOT NULL,
    carrier_name VARCHAR(100) NOT NULL,
    carrier_service VARCHAR(100),
    tracking_number VARCHAR(255),
    status ENUM('CREATED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'EXCEPTION', 'RETURNED') NOT NULL DEFAULT 'CREATED',
    shipping_address JSON NOT NULL,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    weight_kg DECIMAL(8,3),
    dimensions_cm JSON, -- {length, width, height}
    shipping_cost DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    shipped_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    UNIQUE KEY unique_shipment_number (shipment_number),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_order_id (order_id),
    INDEX idx_tracking_number (tracking_number),
    INDEX idx_status (status),
    INDEX idx_carrier_name (carrier_name),
    INDEX idx_estimated_delivery (estimated_delivery_date)
);

-- Shipment items table
CREATE TABLE shipment_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    weight_kg DECIMAL(8,3),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    INDEX idx_shipment_id (shipment_id),
    INDEX idx_order_item_id (order_item_id),
    INDEX idx_product_id (product_id)
);

-- Shipment tracking events
CREATE TABLE shipment_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_id BIGINT NOT NULL,
    status VARCHAR(100) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    event_time TIMESTAMP NOT NULL,
    carrier_event_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    INDEX idx_shipment_id (shipment_id),
    INDEX idx_event_time (event_time),
    INDEX idx_status (status),
    INDEX idx_carrier_event_id (carrier_event_id)
);

-- Carrier configurations
CREATE TABLE carrier_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    carrier_name VARCHAR(100) NOT NULL,
    api_endpoint VARCHAR(500) NOT NULL,
    api_key_encrypted VARCHAR(500) NOT NULL,
    api_secret_encrypted VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    supported_services JSON, -- Array of supported shipping services
    rate_table JSON, -- Pricing information
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_carrier (tenant_id, carrier_name),
    INDEX idx_is_active (is_active)
);

-- Shipping rates cache
CREATE TABLE shipping_rates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    carrier_name VARCHAR(100) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    origin_postal_code VARCHAR(20) NOT NULL,
    destination_postal_code VARCHAR(20) NOT NULL,
    weight_kg DECIMAL(8,3) NOT NULL,
    rate DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    transit_days INT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rate_lookup (tenant_id, carrier_name, origin_postal_code, destination_postal_code, weight_kg),
    INDEX idx_expires_at (expires_at)
);


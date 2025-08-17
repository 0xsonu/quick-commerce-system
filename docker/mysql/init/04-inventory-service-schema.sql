-- Inventory Service Database Schema
USE inventory_service;

-- Inventory table with optimistic locking
CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 10,
    reorder_quantity INT NOT NULL DEFAULT 100,
    location VARCHAR(100),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_tenant_product (tenant_id, product_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_sku (sku),
    INDEX idx_product_id (product_id),
    INDEX idx_reorder_level (reorder_level),
    INDEX idx_available_quantity (available_quantity)
);

-- Inventory transactions for audit trail
CREATE TABLE inventory_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inventory_id BIGINT NOT NULL,
    transaction_type ENUM('STOCK_IN', 'STOCK_OUT', 'RESERVED', 'RELEASED', 'ADJUSTMENT') NOT NULL,
    quantity INT NOT NULL,
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    FOREIGN KEY (inventory_id) REFERENCES inventory(id) ON DELETE CASCADE,
    INDEX idx_inventory_id (inventory_id),
    INDEX idx_reference (reference_id, reference_type),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_created_at (created_at)
);

-- Inventory reservations for order processing
CREATE TABLE inventory_reservations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    order_id BIGINT,
    reservation_id VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    status ENUM('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED') DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_reservation_id (reservation_id),
    INDEX idx_tenant_product (tenant_id, product_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
);


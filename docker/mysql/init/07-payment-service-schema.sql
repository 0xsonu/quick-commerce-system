-- Payment Service Database Schema
USE payment_service;

-- Payments table
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    payment_intent_id VARCHAR(255) NOT NULL,
    payment_method ENUM('CREDIT_CARD', 'DEBIT_CARD', 'DIGITAL_WALLET', 'BANK_TRANSFER', 'PAYPAL') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED') NOT NULL DEFAULT 'PENDING',
    gateway_provider VARCHAR(50) NOT NULL,
    gateway_transaction_id VARCHAR(255),
    gateway_response JSON,
    failure_reason VARCHAR(500),
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    UNIQUE KEY unique_payment_intent (payment_intent_id),
    UNIQUE KEY unique_idempotency_key (idempotency_key),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_gateway_transaction_id (gateway_transaction_id),
    INDEX idx_created_at (created_at)
);

-- Payment methods (tokenized for security)
CREATE TABLE payment_methods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    payment_type ENUM('CREDIT_CARD', 'DEBIT_CARD', 'DIGITAL_WALLET', 'BANK_ACCOUNT') NOT NULL,
    token VARCHAR(255) NOT NULL, -- Tokenized payment method
    last_four VARCHAR(4),
    expiry_month INT,
    expiry_year INT,
    card_brand VARCHAR(50),
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_token (token),
    INDEX idx_is_default (is_default),
    INDEX idx_is_active (is_active)
);

-- Payment transactions for detailed tracking
CREATE TABLE payment_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    transaction_type ENUM('AUTHORIZATION', 'CAPTURE', 'REFUND', 'VOID') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    gateway_transaction_id VARCHAR(255),
    gateway_response JSON,
    status ENUM('SUCCESS', 'FAILED', 'PENDING') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    INDEX idx_payment_id (payment_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_status (status),
    INDEX idx_gateway_transaction_id (gateway_transaction_id)
);

-- Payment webhooks for gateway notifications
CREATE TABLE payment_webhooks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    webhook_id VARCHAR(255) NOT NULL,
    gateway_provider VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payment_id BIGINT,
    webhook_data JSON NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    UNIQUE KEY unique_webhook_id (webhook_id),
    INDEX idx_payment_id (payment_id),
    INDEX idx_processed (processed),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at)
);
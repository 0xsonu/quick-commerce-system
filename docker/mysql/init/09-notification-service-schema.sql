-- Notification Service Schema
USE notification_service;

-- Notification templates table
CREATE TABLE notification_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    template_key VARCHAR(100) NOT NULL,
    channel ENUM('EMAIL', 'SMS', 'PUSH') NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_tenant_template_channel (tenant_id, template_key, channel),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_template_key (template_key),
    INDEX idx_channel (channel),
    INDEX idx_is_active (is_active)
);

-- Template variables table
CREATE TABLE template_variables (
    template_id BIGINT NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    variable_description VARCHAR(255),
    PRIMARY KEY (template_id, variable_name),
    FOREIGN KEY (template_id) REFERENCES notification_templates(id) ON DELETE CASCADE
);

-- Notification preferences table
CREATE TABLE notification_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    notification_type ENUM('ORDER_CREATED', 'ORDER_CONFIRMED', 'ORDER_SHIPPED', 'ORDER_DELIVERED', 'ORDER_CANCELLED', 'PAYMENT_SUCCEEDED', 'PAYMENT_FAILED', 'INVENTORY_LOW', 'ACCOUNT_CREATED', 'PASSWORD_RESET', 'PROMOTIONAL', 'NEWSLETTER') NOT NULL,
    channel ENUM('EMAIL', 'SMS', 'PUSH') NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_tenant_user_type_channel (tenant_id, user_id, notification_type, channel),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_user_id (user_id),
    INDEX idx_notification_type (notification_type),
    INDEX idx_channel (channel)
);

-- Notification logs table
CREATE TABLE notification_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    notification_type ENUM('ORDER_CREATED', 'ORDER_CONFIRMED', 'ORDER_SHIPPED', 'ORDER_DELIVERED', 'ORDER_CANCELLED', 'PAYMENT_SUCCEEDED', 'PAYMENT_FAILED', 'INVENTORY_LOW', 'ACCOUNT_CREATED', 'PASSWORD_RESET', 'PROMOTIONAL', 'NEWSLETTER') NOT NULL,
    channel ENUM('EMAIL', 'SMS', 'PUSH') NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT,
    status ENUM('PENDING', 'SENT', 'FAILED', 'RETRYING') NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    sent_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_user_id (user_id),
    INDEX idx_notification_type (notification_type),
    INDEX idx_channel (channel),
    INDEX idx_status (status),
    INDEX idx_sent_at (sent_at),
    INDEX idx_created_at (created_at),
    INDEX idx_retry_status (status, retry_count, updated_at)
);

-- Notification metadata table
CREATE TABLE notification_metadata (
    notification_log_id BIGINT NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500),
    PRIMARY KEY (notification_log_id, metadata_key),
    FOREIGN KEY (notification_log_id) REFERENCES notification_logs(id) ON DELETE CASCADE
);

-- Insert default notification templates
INSERT INTO notification_templates (tenant_id, template_key, channel, subject, content, is_active) VALUES
-- Order templates
('default', 'order_created', 'EMAIL', 'Order Confirmation - Order #[[${orderNumber}]]', 
 'Dear [[${customerName}]],\n\nThank you for your order! Your order #[[${orderNumber}]] has been successfully placed.\n\nOrder Details:\nTotal: [[${totalAmount}]]\nItems: [[${itemCount}]] items\n\nWe will send you another email when your order ships.\n\nThank you for shopping with us!', true),

('default', 'order_created', 'SMS', 'Order Confirmation', 
 'Your order #[[${orderNumber}]] has been placed successfully. Total: [[${totalAmount}]]. Thank you for shopping with us!', true),

('default', 'order_shipped', 'EMAIL', 'Your Order Has Shipped - Order #[[${orderNumber}]]', 
 'Dear [[${customerName}]],\n\nGreat news! Your order #[[${orderNumber}]] has been shipped.\n\nTracking Number: [[${trackingNumber}]]\nCarrier: [[${carrier}]]\nEstimated Delivery: [[${estimatedDelivery}]]\n\nYou can track your package using the tracking number above.\n\nThank you for your business!', true),

('default', 'order_shipped', 'SMS', 'Order Shipped', 
 'Your order #[[${orderNumber}]] has shipped! Tracking: [[${trackingNumber}]]. Estimated delivery: [[${estimatedDelivery}]]', true),

('default', 'order_delivered', 'EMAIL', 'Order Delivered - Order #[[${orderNumber}]]', 
 'Dear [[${customerName}]],\n\nYour order #[[${orderNumber}]] has been delivered!\n\nWe hope you love your purchase. If you have any questions or concerns, please don''t hesitate to contact us.\n\nThank you for choosing us!', true),

('default', 'order_delivered', 'SMS', 'Order Delivered', 
 'Your order #[[${orderNumber}]] has been delivered! Thank you for shopping with us.', true),

-- Payment templates
('default', 'payment_succeeded', 'EMAIL', 'Payment Confirmation - Order #[[${orderNumber}]]', 
 'Dear [[${customerName}]],\n\nYour payment for order #[[${orderNumber}]] has been successfully processed.\n\nPayment Details:\nAmount: [[${amount}]]\nPayment Method: [[${paymentMethod}]]\nTransaction ID: [[${transactionId}]]\n\nThank you for your payment!', true),

('default', 'payment_failed', 'EMAIL', 'Payment Failed - Order #[[${orderNumber}]]', 
 'Dear [[${customerName}]],\n\nWe were unable to process your payment for order #[[${orderNumber}]].\n\nReason: [[${failureReason}]]\n\nPlease update your payment information and try again.\n\nIf you continue to experience issues, please contact our support team.', true),

-- Account templates
('default', 'account_created', 'EMAIL', 'Welcome to Our Store!', 
 'Dear [[${customerName}]],\n\nWelcome to our store! Your account has been successfully created.\n\nEmail: [[${email}]]\n\nYou can now start shopping and enjoy exclusive member benefits.\n\nThank you for joining us!', true),

('default', 'password_reset', 'EMAIL', 'Password Reset Request', 
 'Dear [[${customerName}]],\n\nWe received a request to reset your password.\n\nClick the link below to reset your password:\n[[${resetLink}]]\n\nThis link will expire in 24 hours.\n\nIf you didn''t request this, please ignore this email.', true);

-- Insert default notification preferences (all enabled by default)
-- These will be created dynamically when users register, but we can set system defaults
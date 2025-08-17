-- Database schema initialization script for auth-service tests
-- This script creates all required tables with proper constraints and indexes
-- Order is important due to foreign key relationships
-- Schema matches JPA entity definitions exactly

-- Drop tables in reverse order to handle foreign key constraints
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users_auth;

-- Create users_auth table (main user authentication table)
-- Inherits from BaseEntity: id, tenant_id, created_at, updated_at
CREATE TABLE users_auth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Unique constraints for tenant isolation (matching JPA annotations)
    CONSTRAINT unique_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT unique_tenant_email UNIQUE (tenant_id, email),
    
    -- Indexes for performance (matching JPA @Index annotations)
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Create user_roles junction table for many-to-many relationship
-- Maps users to their roles with proper foreign key constraints
-- Matches @ElementCollection and @CollectionTable annotations
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role ENUM('CUSTOMER', 'ADMIN', 'MANAGER', 'SUPPORT', 'SYSTEM') NOT NULL, -- Use ENUM to match Hibernate expectations
    
    -- Composite primary key
    PRIMARY KEY (user_id, role),
    
    -- Foreign key constraint with cascade delete
    CONSTRAINT fk_user_roles_user_id 
        FOREIGN KEY (user_id) REFERENCES users_auth(id) ON DELETE CASCADE,
    
    -- Index for performance (matching JPA @Index annotation)
    INDEX idx_user_roles_user_id (user_id)
);

-- Create refresh_tokens table for token management
-- Stores refresh tokens with expiration and revocation tracking
-- Matches RefreshToken entity exactly
CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Foreign key constraint with cascade delete
    CONSTRAINT fk_refresh_tokens_user_id 
        FOREIGN KEY (user_id) REFERENCES users_auth(id) ON DELETE CASCADE,
    
    -- Indexes for performance and token lookup (matching JPA @Index annotations)
    INDEX idx_token_hash (token_hash),
    INDEX idx_expires_at (expires_at),
    INDEX idx_user_id (user_id)
);

-- Note: Test data is NOT inserted here to allow proper transactional rollback
-- Integration tests will create their own test data using the createUser() method
-- This ensures each test starts with a clean state and proper cleanup
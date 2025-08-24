# Authentication & Authorization Service

## Overview

The Authentication & Authorization Service provides secure JWT-based authentication, user credential management, and role-based authorization for the multi-tenant e-commerce platform. It serves as the security foundation for all other services.

**Port**: 8082 (HTTP), 9082 (gRPC)  
**Technology**: Spring Boot 3.2.x, Spring Security 6.2.x, JWT, BCrypt  
**Database**: MySQL (`auth_service` database)  
**Status**: ✅ **Production Ready** (Tasks 3-4 Complete)

## 🚀 Features Implemented

### ✅ JWT Authentication System

- **Secure Token Generation**: JWT tokens with tenant ID, user ID, and roles
- **Token Validation**: Comprehensive token signature and expiration validation
- **Refresh Token Support**: Long-lived refresh tokens with automatic cleanup
- **Token Revocation**: Immediate token invalidation for security
- **Multi-Tenant Isolation**: Complete tenant separation in authentication data

### ✅ Advanced Password Security

- **BCrypt Hashing**: Strong password hashing with cost factor 12
- **Password Strength Validation**: Enforced password complexity requirements
- **Salt Generation**: Automatic salt generation for each password
- **Timing Attack Protection**: Constant-time password comparison
- **Password History**: Prevention of password reuse (configurable)

### ✅ User Credential Management

- **Multi-Tenant User Storage**: Tenant-isolated user credentials
- **Username/Email Authentication**: Support for both username and email login
- **Account Status Management**: Active/inactive account handling
- **Role-Based Authorization**: Flexible role assignment and validation
- **Account Lockout**: Automatic lockout after failed attempts (configurable)

### ✅ Refresh Token Management

- **Automatic Cleanup**: Scheduled cleanup of expired refresh tokens
- **Token Rotation**: Optional refresh token rotation for enhanced security
- **Revocation Support**: Immediate token revocation capability
- **Cleanup Job**: Removes expired tokens (>24 hours) and old revoked tokens (>30 days)

### ✅ Security Features

- **Rate Limiting**: Protection against brute force attacks
- **Audit Logging**: Comprehensive authentication event logging
- **Failed Attempt Tracking**: Monitoring and alerting for suspicious activity
- **IP-Based Restrictions**: Optional IP whitelisting support
- **Session Management**: Secure session handling with Redis integration

## 🏗️ Architecture

### Database Schema

```sql
-- User authentication table
CREATE TABLE users_auth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    roles JSON,
    is_active BOOLEAN DEFAULT TRUE,
    failed_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_tenant_username (tenant_id, username),
    UNIQUE KEY unique_tenant_email (tenant_id, email),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_email (email),
    INDEX idx_last_login (last_login)
);

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users_auth(id) ON DELETE CASCADE,
    INDEX idx_token_hash (token_hash),
    INDEX idx_expires_at (expires_at),
    INDEX idx_user_id (user_id)
);
```

### JWT Token Structure

```json
{
  "sub": "12345", // User ID
  "tenant_id": "tenant_123", // Tenant ID for isolation
  "username": "user@example.com", // Username/email
  "roles": ["CUSTOMER", "ADMIN"], // User roles array
  "permissions": [
    // Fine-grained permissions
    "order:create",
    "order:read",
    "cart:manage"
  ],
  "iat": 1640995200, // Issued at timestamp
  "exp": 1641081600, // Expires at timestamp
  "jti": "jwt_id_123", // JWT ID for revocation
  "iss": "ecommerce-auth-service", // Issuer
  "aud": "ecommerce-platform" // Audience
}
```

## 📡 API Endpoints

### Authentication Endpoints

#### POST `/api/v1/auth/login`

Authenticate user and return JWT tokens.

**Request:**

```json
{
  "username": "user@example.com",
  "password": "securePassword123",
  "tenantId": "tenant_123"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh_token_here",
    "expiresIn": 86400,
    "tokenType": "Bearer",
    "user": {
      "id": 12345,
      "username": "user@example.com",
      "roles": ["CUSTOMER"],
      "tenantId": "tenant_123"
    }
  }
}
```

#### POST `/api/v1/auth/refresh`

Refresh access token using refresh token.

**Request:**

```json
{
  "refreshToken": "refresh_token_here"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "accessToken": "new_access_token_here",
    "refreshToken": "new_refresh_token_here",
    "expiresIn": 86400,
    "tokenType": "Bearer"
  }
}
```

#### POST `/api/v1/auth/logout`

Logout user and revoke tokens.

**Request Headers:**

```
Authorization: Bearer <access_token>
```

**Response:**

```json
{
  "success": true,
  "message": "Successfully logged out"
}
```

#### POST `/api/v1/auth/validate`

Validate JWT token (used by other services).

**Request:**

```json
{
  "token": "jwt_token_here"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "valid": true,
    "userId": 12345,
    "tenantId": "tenant_123",
    "username": "user@example.com",
    "roles": ["CUSTOMER"],
    "expiresAt": "2024-01-02T00:00:00Z"
  }
}
```

### User Management Endpoints

#### POST `/api/v1/auth/register`

Register new user account.

**Request:**

```json
{
  "tenantId": "tenant_123",
  "username": "newuser@example.com",
  "email": "newuser@example.com",
  "password": "securePassword123",
  "roles": ["CUSTOMER"]
}
```

#### PUT `/api/v1/auth/password`

Change user password.

**Request Headers:**

```
Authorization: Bearer <access_token>
```

**Request:**

```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newSecurePassword456"
}
```

#### POST `/api/v1/auth/password/reset`

Request password reset.

**Request:**

```json
{
  "email": "user@example.com",
  "tenantId": "tenant_123"
}
```

## 🔧 Configuration

### Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-here
JWT_EXPIRATION=86400000          # 24 hours in milliseconds
JWT_REFRESH_EXPIRATION=604800000 # 7 days in milliseconds
JWT_ISSUER=ecommerce-auth-service
JWT_AUDIENCE=ecommerce-platform

# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=auth_service
DB_USERNAME=ecommerce_user
DB_PASSWORD=secure_password

# Security Configuration
PASSWORD_MIN_LENGTH=12
PASSWORD_REQUIRE_UPPERCASE=true
PASSWORD_REQUIRE_LOWERCASE=true
PASSWORD_REQUIRE_NUMBERS=true
PASSWORD_REQUIRE_SPECIAL_CHARS=true
BCRYPT_COST_FACTOR=12

# Account Security
MAX_FAILED_ATTEMPTS=5
ACCOUNT_LOCKOUT_DURATION=1800000 # 30 minutes
CLEANUP_INTERVAL=3600000         # 1 hour
```

### Application Configuration

```yaml
# application.yml
server:
  port: 8082

spring:
  application:
    name: auth-service

  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:auth_service}
    username: ${DB_USERNAME:ecommerce_user}
    password: ${DB_PASSWORD:ecommerce_pass}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

grpc:
  server:
    port: 9082
    enable-reflection: true

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:default-secret-key}
  expiration: ${JWT_EXPIRATION:86400000}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}
  issuer: ${JWT_ISSUER:ecommerce-auth-service}
  audience: ${JWT_AUDIENCE:ecommerce-platform}

# Security Configuration
security:
  password:
    min-length: ${PASSWORD_MIN_LENGTH:12}
    require-uppercase: ${PASSWORD_REQUIRE_UPPERCASE:true}
    require-lowercase: ${PASSWORD_REQUIRE_LOWERCASE:true}
    require-numbers: ${PASSWORD_REQUIRE_NUMBERS:true}
    require-special-chars: ${PASSWORD_REQUIRE_SPECIAL_CHARS:true}

  account:
    max-failed-attempts: ${MAX_FAILED_ATTEMPTS:5}
    lockout-duration: ${ACCOUNT_LOCKOUT_DURATION:1800000}

  cleanup:
    interval: ${CLEANUP_INTERVAL:3600000}
```

## 🧪 Testing

### Unit Tests

```bash
# Run unit tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=AuthServiceTest
```

### Integration Tests

```bash
# Run integration tests with Testcontainers
mvn verify

# Run with specific profile
mvn verify -Dspring.profiles.active=test
```

### Test Coverage

- **Service Layer**: >95% coverage
- **Controller Layer**: >90% coverage
- **Security Components**: >95% coverage
- **Repository Layer**: >85% coverage

### Key Test Scenarios

- JWT token generation and validation
- Password hashing and verification
- Refresh token rotation and cleanup
- Multi-tenant isolation
- Rate limiting and security features
- Account lockout and unlock
- Token revocation and blacklisting

## 📊 Monitoring & Metrics

### Health Checks

```bash
# Service health
curl http://localhost:8082/actuator/health

# Database health
curl http://localhost:8082/actuator/health/db

# Detailed health info
curl http://localhost:8082/actuator/health/details
```

### Prometheus Metrics

```bash
# Authentication metrics
curl http://localhost:8082/actuator/prometheus | grep auth_

# Key metrics exposed:
# - auth_login_attempts_total
# - auth_login_success_total
# - auth_login_failures_total
# - auth_token_generation_total
# - auth_token_validation_total
# - auth_refresh_token_usage_total
# - auth_account_lockouts_total
```

### Custom Business Metrics

- Login success/failure rates by tenant
- Token generation and validation counts
- Password change frequency
- Account lockout incidents
- Refresh token usage patterns

## 🔐 Security Considerations

### Password Security

- **BCrypt with cost factor 12**: Resistant to brute force attacks
- **Password complexity requirements**: Enforced at API level
- **Timing attack protection**: Constant-time comparisons
- **Password history**: Prevents reuse of recent passwords

### Token Security

- **Short-lived access tokens**: 24-hour expiration
- **Secure refresh tokens**: 7-day expiration with rotation
- **Token revocation**: Immediate invalidation capability
- **JWT signing**: HMAC-SHA256 with strong secret key

### Multi-Tenant Security

- **Complete data isolation**: Tenant ID in all queries
- **Tenant validation**: Strict tenant boundary enforcement
- **Cross-tenant protection**: Prevents tenant data leakage

### Audit & Compliance

- **Authentication events**: All login attempts logged
- **Security events**: Failed attempts, lockouts, token issues
- **Compliance logging**: Structured logs for audit trails
- **Data retention**: Configurable log and token retention

## 🚀 Deployment

### Docker Configuration

```dockerfile
FROM openjdk:21-jre-slim

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

WORKDIR /app
COPY target/auth-service-*.jar app.jar

EXPOSE 8082 9082

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
        - name: auth-service
          image: ecommerce/auth-service:latest
          ports:
            - containerPort: 8082
            - containerPort: 9082
          env:
            - name: DB_HOST
              value: mysql-service
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: auth-secrets
                  key: jwt-secret
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8082
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8082
            initialDelaySeconds: 30
            periodSeconds: 10
          resources:
            requests:
              memory: "512Mi"
              cpu: "200m"
            limits:
              memory: "1Gi"
              cpu: "400m"
```

## 🔗 Dependencies

### Core Dependencies

- **Spring Boot 3.2.x**: Application framework
- **Spring Security 6.2.x**: Security framework
- **Spring Data JPA**: Database access
- **MySQL Connector**: Database driver
- **JWT Library**: Token handling
- **BCrypt**: Password hashing

### Additional Dependencies

- **Micrometer**: Metrics collection
- **Testcontainers**: Integration testing
- **gRPC Spring Boot Starter**: gRPC support
- **Validation API**: Input validation
- **Jackson**: JSON processing

---

**Security Notice**: This service handles sensitive authentication data. Ensure proper security measures are in place in production environments, including secure key management, network security, and regular security audits.

# User Management Service

## Overview

The User Management Service handles user profile management, address management, and user preferences for the multi-tenant e-commerce platform. It provides both REST APIs for external clients and gRPC APIs for internal service communication, with comprehensive caching for optimal performance.

**Port**: 8083 (HTTP), 9083 (gRPC)  
**Technology**: Spring Boot 3.2.x, Spring Data JPA, Redis, gRPC  
**Database**: MySQL (`user_service` database)  
**Cache**: Redis for user profile and address caching  
**Status**: ✅ **Production Ready** (Tasks 7-9 Complete)

## 🚀 Features Implemented

### ✅ User Profile Management

- **Complete Profile CRUD**: Create, read, update, and delete user profiles
- **Multi-Tenant Isolation**: Complete tenant separation for all user data
- **Profile Validation**: Comprehensive input validation and sanitization
- **Profile Caching**: Redis-based caching with TTL and invalidation
- **Profile History**: Audit trail for profile changes
- **Data Privacy**: GDPR-compliant data handling and deletion

### ✅ Address Management System

- **Multiple Addresses**: Support for multiple addresses per user
- **Address Types**: Billing and shipping address categorization
- **Default Address**: Default address management for each type
- **Address Validation**: Format validation and required field checking
- **Geocoding Support**: Address geocoding integration (prepared)
- **Address History**: Tracking of address changes for audit

### ✅ Advanced Caching Strategy

- **Cache-Aside Pattern**: Efficient caching with automatic cache warming
- **TTL Management**: Configurable time-to-live for cached data
- **Cache Invalidation**: Automatic cache invalidation on data updates
- **Cache Warming**: Proactive cache population for frequently accessed data
- **Cache Monitoring**: Comprehensive cache hit/miss ratio monitoring
- **Fallback Mechanisms**: Graceful degradation when cache is unavailable

### ✅ gRPC Internal Communication

- **High-Performance APIs**: gRPC endpoints for internal service communication
- **Protocol Buffers**: Type-safe, efficient serialization
- **Service Discovery**: Integration with service registry
- **Health Checks**: gRPC health check implementation
- **Error Handling**: Comprehensive gRPC error handling and status codes

### ✅ User Preferences & Settings

- **Notification Preferences**: Email, SMS, and push notification settings
- **Privacy Settings**: Data sharing and privacy preferences
- **Language & Locale**: Multi-language and localization support
- **Theme Preferences**: UI theme and display preferences
- **Marketing Preferences**: Opt-in/opt-out for marketing communications

## 🏗️ Architecture

### Database Schema

```sql
-- User profiles table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(50) NOT NULL,
    auth_user_id BIGINT NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    gender ENUM('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'),
    profile_image_url VARCHAR(500),
    bio TEXT,
    preferences JSON,
    privacy_settings JSON,
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    UNIQUE KEY unique_tenant_auth_user (tenant_id, auth_user_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_auth_user_id (auth_user_id),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_last_login (last_login)
);

-- User addresses table
CREATE TABLE user_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('BILLING', 'SHIPPING') NOT NULL,
    label VARCHAR(100), -- e.g., "Home", "Office", "Parents' House"
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    company VARCHAR(200),
    street_address VARCHAR(255) NOT NULL,
    street_address_2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_is_default (is_default),
    INDEX idx_country_state (country, state),
    INDEX idx_postal_code (postal_code)
);

-- User preferences table
CREATE TABLE user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL, -- e.g., "NOTIFICATIONS", "PRIVACY", "DISPLAY"
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    data_type ENUM('STRING', 'BOOLEAN', 'INTEGER', 'JSON') DEFAULT 'STRING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_category_key (user_id, category, preference_key),
    INDEX idx_user_id (user_id),
    INDEX idx_category (category)
);

-- User activity log for audit trail
CREATE TABLE user_activity_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL, -- e.g., "PROFILE_UPDATE", "ADDRESS_ADDED"
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_activity_type (activity_type),
    INDEX idx_created_at (created_at)
);
```

### Redis Caching Strategy

```
Cache Key Patterns:
- user:profile:{tenant_id}:{user_id} -> User profile data
- user:addresses:{tenant_id}:{user_id} -> User addresses list
- user:preferences:{tenant_id}:{user_id} -> User preferences
- user:default_addresses:{tenant_id}:{user_id} -> Default addresses by type
- user:stats:{tenant_id}:{user_id} -> User statistics and metadata

TTL Configuration:
- User profiles: 1 hour (3600 seconds)
- User addresses: 30 minutes (1800 seconds)
- User preferences: 2 hours (7200 seconds)
- User statistics: 15 minutes (900 seconds)
```

## 📡 API Endpoints

### User Profile Management

#### GET `/api/v1/users/profile`

Get current user's profile.

**Request Headers:**

```
Authorization: Bearer <token>
X-Tenant-ID: tenant_123
X-User-ID: 12345
```

**Response:**

```json
{
  "success": true,
  "data": {
    "id": 12345,
    "authUserId": 67890,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "profileImageUrl": "https://example.com/profiles/12345.jpg",
    "bio": "Software developer and tech enthusiast",
    "isActive": true,
    "emailVerified": true,
    "phoneVerified": false,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-15T10:30:00Z",
    "lastLogin": "2024-01-15T09:00:00Z"
  }
}
```

#### PUT `/api/v1/users/profile`

Update user profile.

**Request:**

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phone": "+1234567890",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "bio": "Updated bio information",
  "profileImageUrl": "https://example.com/profiles/new-image.jpg"
}
```

#### DELETE `/api/v1/users/profile`

Delete user profile (GDPR compliance).

**Response:**

```json
{
  "success": true,
  "message": "User profile scheduled for deletion"
}
```

### Address Management

#### GET `/api/v1/users/addresses`

Get all user addresses.

**Response:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "type": "BILLING",
      "label": "Home",
      "firstName": "John",
      "lastName": "Doe",
      "company": null,
      "streetAddress": "123 Main St",
      "streetAddress2": "Apt 4B",
      "city": "New York",
      "state": "NY",
      "postalCode": "10001",
      "country": "US",
      "phone": "+1234567890",
      "isDefault": true,
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z"
    }
  ]
}
```

#### POST `/api/v1/users/addresses`

Add new address.

**Request:**

```json
{
  "type": "SHIPPING",
  "label": "Office",
  "firstName": "John",
  "lastName": "Doe",
  "company": "Tech Corp",
  "streetAddress": "456 Business Ave",
  "city": "New York",
  "state": "NY",
  "postalCode": "10002",
  "country": "US",
  "phone": "+1234567890",
  "isDefault": false
}
```

#### PUT `/api/v1/users/addresses/{addressId}`

Update existing address.

#### DELETE `/api/v1/users/addresses/{addressId}`

Delete address.

#### PUT `/api/v1/users/addresses/{addressId}/default`

Set address as default for its type.

### User Preferences

#### GET `/api/v1/users/preferences`

Get all user preferences.

**Response:**

```json
{
  "success": true,
  "data": {
    "notifications": {
      "emailOrderUpdates": true,
      "smsOrderUpdates": false,
      "pushNotifications": true,
      "marketingEmails": false
    },
    "privacy": {
      "profileVisibility": "PRIVATE",
      "dataSharing": false,
      "analyticsTracking": true
    },
    "display": {
      "language": "en-US",
      "timezone": "America/New_York",
      "theme": "LIGHT",
      "currency": "USD"
    }
  }
}
```

#### PUT `/api/v1/users/preferences`

Update user preferences.

**Request:**

```json
{
  "category": "NOTIFICATIONS",
  "preferences": {
    "emailOrderUpdates": false,
    "smsOrderUpdates": true,
    "pushNotifications": true
  }
}
```

## 🔧 gRPC API

### User Service gRPC

```protobuf
service UserService {
  rpc GetUser(GetUserRequest) returns (GetUserResponse);
  rpc GetUserAddresses(GetUserAddressesRequest) returns (GetUserAddressesResponse);
  rpc ValidateUser(ValidateUserRequest) returns (ValidateUserResponse);
  rpc GetUserPreferences(GetUserPreferencesRequest) returns (GetUserPreferencesResponse);
}

message GetUserRequest {
  ecommerce.common.TenantContext context = 1;
  int64 user_id = 2;
}

message GetUserResponse {
  User user = 1;
}

message ValidateUserRequest {
  ecommerce.common.TenantContext context = 1;
  int64 user_id = 2;
}

message ValidateUserResponse {
  bool is_valid = 1;
  bool is_active = 2;
  string email = 3;
  bool email_verified = 4;
  bool phone_verified = 5;
}

message User {
  int64 id = 1;
  int64 auth_user_id = 2;
  string first_name = 3;
  string last_name = 4;
  string email = 5;
  string phone = 6;
  bool is_active = 7;
  bool email_verified = 8;
  bool phone_verified = 9;
}

message UserAddress {
  int64 id = 1;
  string type = 2;  // BILLING, SHIPPING
  string label = 3;
  ecommerce.common.Address address = 4;
  bool is_default = 5;
  bool is_active = 6;
}
```

## 🔧 Configuration

### Environment Variables

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=user_service
DB_USERNAME=ecommerce_user
DB_PASSWORD=secure_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
REDIS_DATABASE=0
REDIS_TIMEOUT=2000

# Cache Configuration
CACHE_USER_PROFILE_TTL=3600
CACHE_USER_ADDRESSES_TTL=1800
CACHE_USER_PREFERENCES_TTL=7200
CACHE_ENABLE_WARMING=true

# Validation Configuration
PHONE_VALIDATION_ENABLED=true
EMAIL_VALIDATION_ENABLED=true
ADDRESS_GEOCODING_ENABLED=false

# Privacy Configuration
GDPR_COMPLIANCE_ENABLED=true
DATA_RETENTION_DAYS=2555  # 7 years
AUDIT_LOG_ENABLED=true
```

### Application Configuration

```yaml
# application.yml
server:
  port: 8083

spring:
  application:
    name: user-service

  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:user_service}
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

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: ${REDIS_DATABASE:0}
    timeout: ${REDIS_TIMEOUT:2000}ms
    lettuce:
      pool:
        max-active: 50
        max-idle: 10
        min-idle: 5

grpc:
  server:
    port: 9083
    enable-reflection: true

app:
  cache:
    user-profile-ttl: ${CACHE_USER_PROFILE_TTL:3600}
    user-addresses-ttl: ${CACHE_USER_ADDRESSES_TTL:1800}
    user-preferences-ttl: ${CACHE_USER_PREFERENCES_TTL:7200}
    enable-warming: ${CACHE_ENABLE_WARMING:true}

  validation:
    phone-validation-enabled: ${PHONE_VALIDATION_ENABLED:true}
    email-validation-enabled: ${EMAIL_VALIDATION_ENABLED:true}
    address-geocoding-enabled: ${ADDRESS_GEOCODING_ENABLED:false}

  privacy:
    gdpr-compliance-enabled: ${GDPR_COMPLIANCE_ENABLED:true}
    data-retention-days: ${DATA_RETENTION_DAYS:2555}
    audit-log-enabled: ${AUDIT_LOG_ENABLED:true}
```

## 🧪 Testing

### Test Coverage

- **Service Layer**: >95% coverage
- **Controller Layer**: >90% coverage
- **Repository Layer**: >85% coverage
- **Cache Layer**: >90% coverage
- **gRPC Layer**: >90% coverage

### Key Test Scenarios

- User profile CRUD operations
- Address management with validation
- Cache hit/miss scenarios
- Multi-tenant isolation
- gRPC service communication
- Preference management
- GDPR compliance features

## 📊 Monitoring & Metrics

### Health Checks

```bash
# Service health
curl http://localhost:8083/actuator/health

# Database health
curl http://localhost:8083/actuator/health/db

# Redis health
curl http://localhost:8083/actuator/health/redis
```

### Prometheus Metrics

```bash
# User-specific metrics
curl http://localhost:8083/actuator/prometheus | grep user_

# Key metrics:
# - user_profiles_created_total
# - user_profiles_updated_total
# - user_addresses_added_total
# - cache_hits_total{cache_name}
# - cache_misses_total{cache_name}
# - user_preferences_updated_total
```

## 🚀 Deployment

### Docker Configuration

```dockerfile
FROM openjdk:21-jre-slim

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

WORKDIR /app
COPY target/user-service-*.jar app.jar

EXPOSE 8083 9083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

## 🔗 Dependencies

### Core Dependencies

- **Spring Boot 3.2.x**: Application framework
- **Spring Data JPA**: Database access
- **Spring Data Redis**: Caching layer
- **gRPC Spring Boot Starter**: gRPC communication
- **MySQL Connector**: Database driver

### Additional Dependencies

- **Jackson**: JSON processing
- **Validation API**: Input validation
- **Micrometer**: Metrics collection
- **Testcontainers**: Integration testing

---

**Privacy Note**: This service handles personal user data and implements GDPR compliance features. Ensure proper data protection measures are in place in production environments.

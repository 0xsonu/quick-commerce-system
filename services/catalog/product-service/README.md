# Product Catalog Service

## Overview

The Product Catalog Service manages the complete product catalog for the multi-tenant e-commerce platform. Built with MongoDB for flexible schema support and Redis for high-performance caching, it provides comprehensive product management, advanced search capabilities, and event-driven architecture integration.

**Port**: 8084 (HTTP), 9084 (gRPC)  
**Technology**: Spring Boot 3.2.x, Spring Data MongoDB, Redis, Kafka, gRPC  
**Database**: MongoDB (`products` collection)  
**Cache**: Redis for product caching and search optimization  
**Status**: ✅ **Production Ready** (Tasks 10-12 Complete)

## 🚀 Features Implemented

### ✅ Advanced Product Management

- **Flexible Product Schema**: MongoDB-based storage with dynamic attributes
- **Multi-Tenant Isolation**: Complete tenant separation for all product data
- **Product CRUD Operations**: Comprehensive create, read, update, delete operations
- **Product Variants**: Support for product variations (size, color, etc.)
- **Product Categories**: Hierarchical category management
- **Product Images**: Multiple image support with metadata
- **SEO Optimization**: SEO-friendly URLs, meta tags, and structured data

### ✅ High-Performance Search & Discovery

- **Full-Text Search**: MongoDB text indexes for comprehensive product search
- **Advanced Filtering**: Multi-criteria filtering (category, price, brand, attributes)
- **Faceted Search**: Dynamic facet generation for search refinement
- **Search Suggestions**: Auto-complete and search suggestions
- **Relevance Scoring**: Intelligent search result ranking
- **Search Analytics**: Search query tracking and optimization

### ✅ Intelligent Caching Strategy

- **Redis Caching**: High-performance product caching with TTL management
- **Cache-Aside Pattern**: Efficient caching with automatic cache warming
- **Search Result Caching**: Cached search results for popular queries
- **Category Caching**: Hierarchical category structure caching
- **Cache Invalidation**: Smart cache invalidation on product updates
- **Cache Monitoring**: Comprehensive cache performance metrics

### ✅ Event-Driven Architecture

- **Kafka Event Publishing**: Real-time product lifecycle event publishing
- **Event Sourcing**: Complete audit trail through events
- **Async Processing**: Non-blocking event publishing with error handling
- **Event Replay**: Recovery mechanisms for failed event processing
- **Cross-Service Integration**: Events consumed by inventory, cart, and order services

### ✅ gRPC Internal APIs

- **High-Performance Communication**: gRPC endpoints for internal service calls
- **Protocol Buffers**: Type-safe, efficient serialization
- **Product Validation**: Fast product validation for other services
- **Bulk Operations**: Efficient bulk product retrieval
- **Health Monitoring**: gRPC health check implementation

## 🏗️ Architecture

### MongoDB Document Schema

```javascript
// Product document structure
{
  "_id": "product_123",
  "tenant_id": "tenant_abc",
  "name": "Premium Wireless Headphones",
  "description": "High-quality wireless headphones with noise cancellation",
  "short_description": "Premium wireless headphones",
  "category": "electronics",
  "subcategory": "audio",
  "brand": "AudioTech",
  "sku": "AT-WH-001",
  "model": "AudioTech Pro Max",
  "price": {
    "amount": 299.99,
    "currency": "USD",
    "compare_at_price": 399.99,
    "cost_price": 150.00
  },
  "images": [
    {
      "url": "https://cdn.example.com/products/at-wh-001-main.jpg",
      "alt_text": "AudioTech Pro Max - Main View",
      "is_primary": true,
      "sort_order": 1,
      "metadata": {
        "width": 1200,
        "height": 1200,
        "format": "JPEG"
      }
    }
  ],
  "attributes": {
    "color": "Matte Black",
    "connectivity": "Bluetooth 5.0",
    "battery_life": "30 hours",
    "weight": "250g",
    "warranty": "2 years",
    "features": ["Noise Cancellation", "Quick Charge", "Voice Assistant"]
  },
  "variants": [
    {
      "id": "variant_1",
      "sku": "AT-WH-001-BLK",
      "attributes": {"color": "Black"},
      "price": {"amount": 299.99, "currency": "USD"},
      "inventory": {"available": true, "quantity": 50}
    },
    {
      "id": "variant_2",
      "sku": "AT-WH-001-WHT",
      "attributes": {"color": "White"},
      "price": {"amount": 309.99, "currency": "USD"},
      "inventory": {"available": true, "quantity": 30}
    }
  ],
  "seo": {
    "meta_title": "AudioTech Pro Max Wireless Headphones - Premium Audio",
    "meta_description": "Experience premium audio with AudioTech Pro Max wireless headphones featuring noise cancellation and 30-hour battery life.",
    "slug": "audiotech-pro-max-wireless-headphones",
    "keywords": ["wireless headphones", "noise cancellation", "premium audio"],
    "structured_data": {
      "@type": "Product",
      "@context": "https://schema.org/",
      "name": "AudioTech Pro Max",
      "brand": "AudioTech"
    }
  },
  "inventory": {
    "track_inventory": true,
    "available": true,
    "total_quantity": 80,
    "reserved_quantity": 5,
    "low_stock_threshold": 10
  },
  "shipping": {
    "weight": 0.5,
    "dimensions": {
      "length": 20.0,
      "width": 15.0,
      "height": 8.0,
      "unit": "cm"
    },
    "shipping_class": "standard",
    "free_shipping": false
  },
  "status": "ACTIVE", // ACTIVE, INACTIVE, DRAFT, ARCHIVED
  "visibility": "PUBLIC", // PUBLIC, PRIVATE, HIDDEN
  "tags": ["premium", "wireless", "audio", "electronics"],
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-15T10:30:00Z",
  "published_at": "2024-01-02T00:00:00Z",
  "created_by": "admin_user_123",
  "updated_by": "admin_user_456"
}
```

### MongoDB Indexes

```javascript
// Text search index
db.products.createIndex(
  {
    name: "text",
    description: "text",
    short_description: "text",
    brand: "text",
    tags: "text",
    attributes: "text",
  },
  {
    weights: {
      name: 10,
      brand: 8,
      short_description: 5,
      tags: 3,
      description: 2,
      attributes: 1,
    },
    name: "product_text_search",
  }
);

// Compound indexes for efficient queries
db.products.createIndex({ tenant_id: 1, status: 1, category: 1 });
db.products.createIndex({ tenant_id: 1, brand: 1, status: 1 });
db.products.createIndex({ tenant_id: 1, "price.amount": 1, status: 1 });
db.products.createIndex({ tenant_id: 1, created_at: -1 });
db.products.createIndex({ tenant_id: 1, updated_at: -1 });
db.products.createIndex({ sku: 1 }, { unique: true });
db.products.createIndex({ "seo.slug": 1 }, { unique: true });
```

### Redis Caching Strategy

```
Cache Key Patterns:
- product:details:{tenant_id}:{product_id} -> Complete product details
- product:search:{tenant_id}:{search_hash} -> Search results
- product:category:{tenant_id}:{category} -> Products by category
- product:brand:{tenant_id}:{brand} -> Products by brand
- product:featured:{tenant_id} -> Featured products list
- product:new:{tenant_id} -> New arrivals list
- category:tree:{tenant_id} -> Category hierarchy
- search:suggestions:{tenant_id}:{prefix} -> Search suggestions

TTL Configuration:
- Product details: 1 hour (3600 seconds)
- Search results: 15 minutes (900 seconds)
- Category lists: 30 minutes (1800 seconds)
- Featured products: 2 hours (7200 seconds)
- Search suggestions: 1 hour (3600 seconds)
```

## 📡 API Endpoints

### Product Management

#### GET `/api/v1/products`

Search and list products with advanced filtering.

**Query Parameters:**

- `search`: Full-text search query
- `category`: Filter by category
- `subcategory`: Filter by subcategory
- `brand`: Filter by brand
- `minPrice`: Minimum price filter
- `maxPrice`: Maximum price filter
- `tags`: Filter by tags (comma-separated)
- `status`: Filter by status (default: ACTIVE)
- `sortBy`: Sort field (name, price, created_at, updated_at)
- `sortDirection`: Sort direction (asc, desc)
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)

**Request:**

```bash
GET /api/v1/products?search=wireless%20headphones&category=electronics&minPrice=100&maxPrice=500&sortBy=price&sortDirection=asc&page=0&size=20
```

**Response:**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "product_123",
        "name": "Premium Wireless Headphones",
        "shortDescription": "High-quality wireless headphones",
        "brand": "AudioTech",
        "sku": "AT-WH-001",
        "price": {
          "amount": 299.99,
          "currency": "USD",
          "compareAtPrice": 399.99
        },
        "primaryImage": {
          "url": "https://cdn.example.com/products/at-wh-001-main.jpg",
          "altText": "AudioTech Pro Max - Main View"
        },
        "category": "electronics",
        "subcategory": "audio",
        "status": "ACTIVE",
        "availability": {
          "available": true,
          "totalQuantity": 80
        },
        "rating": {
          "average": 4.5,
          "count": 128
        },
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3,
    "first": true,
    "last": false,
    "facets": {
      "categories": [
        { "name": "electronics", "count": 25 },
        { "name": "accessories", "count": 20 }
      ],
      "brands": [
        { "name": "AudioTech", "count": 15 },
        { "name": "SoundPro", "count": 10 }
      ],
      "priceRanges": [
        { "range": "0-100", "count": 5 },
        { "range": "100-300", "count": 25 },
        { "range": "300-500", "count": 15 }
      ]
    }
  }
}
```

#### GET `/api/v1/products/{productId}`

Get detailed product information.

**Response:**

```json
{
  "success": true,
  "data": {
    "id": "product_123",
    "name": "Premium Wireless Headphones",
    "description": "High-quality wireless headphones with advanced noise cancellation...",
    "shortDescription": "Premium wireless headphones",
    "brand": "AudioTech",
    "sku": "AT-WH-001",
    "model": "AudioTech Pro Max",
    "category": "electronics",
    "subcategory": "audio",
    "price": {
      "amount": 299.99,
      "currency": "USD",
      "compareAtPrice": 399.99
    },
    "images": [
      {
        "url": "https://cdn.example.com/products/at-wh-001-main.jpg",
        "altText": "AudioTech Pro Max - Main View",
        "isPrimary": true,
        "sortOrder": 1
      }
    ],
    "attributes": {
      "color": "Matte Black",
      "connectivity": "Bluetooth 5.0",
      "batteryLife": "30 hours",
      "weight": "250g",
      "warranty": "2 years",
      "features": ["Noise Cancellation", "Quick Charge", "Voice Assistant"]
    },
    "variants": [
      {
        "id": "variant_1",
        "sku": "AT-WH-001-BLK",
        "attributes": { "color": "Black" },
        "price": { "amount": 299.99, "currency": "USD" },
        "available": true
      }
    ],
    "seo": {
      "metaTitle": "AudioTech Pro Max Wireless Headphones - Premium Audio",
      "metaDescription": "Experience premium audio with AudioTech Pro Max...",
      "slug": "audiotech-pro-max-wireless-headphones",
      "keywords": ["wireless headphones", "noise cancellation"]
    },
    "inventory": {
      "available": true,
      "totalQuantity": 80,
      "lowStockThreshold": 10
    },
    "shipping": {
      "weight": 0.5,
      "dimensions": {
        "length": 20.0,
        "width": 15.0,
        "height": 8.0,
        "unit": "cm"
      },
      "freeShipping": false
    },
    "status": "ACTIVE",
    "tags": ["premium", "wireless", "audio"],
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
}
```

#### POST `/api/v1/products`

Create new product (Admin only).

**Request:**

```json
{
  "name": "New Wireless Speaker",
  "description": "Portable wireless speaker with excellent sound quality",
  "shortDescription": "Portable wireless speaker",
  "brand": "AudioTech",
  "sku": "AT-SP-001",
  "category": "electronics",
  "subcategory": "audio",
  "price": {
    "amount": 149.99,
    "currency": "USD",
    "compareAtPrice": 199.99,
    "costPrice": 75.0
  },
  "attributes": {
    "color": "Black",
    "connectivity": "Bluetooth 5.0",
    "batteryLife": "12 hours",
    "waterproof": "IPX7"
  },
  "seo": {
    "metaTitle": "AudioTech Wireless Speaker - Portable Audio",
    "metaDescription": "Portable wireless speaker with excellent sound quality and 12-hour battery life.",
    "slug": "audiotech-wireless-speaker-portable"
  },
  "inventory": {
    "trackInventory": true,
    "totalQuantity": 100,
    "lowStockThreshold": 15
  },
  "status": "ACTIVE",
  "tags": ["portable", "wireless", "speaker", "waterproof"]
}
```

#### PUT `/api/v1/products/{productId}`

Update existing product (Admin only).

#### DELETE `/api/v1/products/{productId}`

Delete product (Admin only).

### Search & Discovery

#### GET `/api/v1/products/search/suggestions`

Get search suggestions for auto-complete.

**Query Parameters:**

- `q`: Search query prefix
- `limit`: Maximum suggestions (default: 10)

**Response:**

```json
{
  "success": true,
  "data": {
    "suggestions": [
      "wireless headphones",
      "wireless speaker",
      "wireless earbuds",
      "wireless charger"
    ],
    "products": [
      {
        "id": "product_123",
        "name": "Premium Wireless Headphones",
        "price": { "amount": 299.99, "currency": "USD" },
        "primaryImage": { "url": "...", "altText": "..." }
      }
    ]
  }
}
```

#### GET `/api/v1/products/categories`

Get product category hierarchy.

**Response:**

```json
{
  "success": true,
  "data": [
    {
      "name": "electronics",
      "displayName": "Electronics",
      "productCount": 150,
      "subcategories": [
        {
          "name": "audio",
          "displayName": "Audio & Headphones",
          "productCount": 45
        },
        {
          "name": "mobile",
          "displayName": "Mobile & Accessories",
          "productCount": 65
        }
      ]
    }
  ]
}
```

#### GET `/api/v1/products/featured`

Get featured products.

#### GET `/api/v1/products/new-arrivals`

Get new arrival products.

#### GET `/api/v1/products/trending`

Get trending products based on views/sales.

## 🔧 gRPC API

### Product Service gRPC

```protobuf
service ProductService {
  rpc GetProduct(GetProductRequest) returns (GetProductResponse);
  rpc ValidateProduct(ValidateProductRequest) returns (ValidateProductResponse);
  rpc GetProductsByIds(GetProductsByIdsRequest) returns (GetProductsByIdsResponse);
  rpc SearchProducts(SearchProductsRequest) returns (SearchProductsResponse);
}

message GetProductRequest {
  ecommerce.common.TenantContext context = 1;
  string product_id = 2;
}

message ValidateProductRequest {
  ecommerce.common.TenantContext context = 1;
  string product_id = 2;
  string sku = 3;
}

message ValidateProductResponse {
  bool is_valid = 1;
  string product_id = 2;
  string sku = 3;
  string name = 4;
  ecommerce.common.Money price = 5;
  bool is_active = 6;
  bool is_available = 7;
}

message Product {
  string id = 1;
  string name = 2;
  string description = 3;
  string sku = 4;
  ecommerce.common.Money price = 5;
  string category = 6;
  string brand = 7;
  bool is_active = 8;
  repeated string image_urls = 9;
  map<string, string> attributes = 10;
}
```

## 🔧 Configuration

### Environment Variables

```bash
# MongoDB Configuration
MONGODB_HOST=localhost
MONGODB_PORT=27017
MONGODB_DATABASE=ecommerce_catalog
MONGODB_USERNAME=mongo_user
MONGODB_PASSWORD=mongo_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
REDIS_DATABASE=1

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_PRODUCT_EVENTS=product-events

# Search Configuration
SEARCH_DEFAULT_PAGE_SIZE=20
SEARCH_MAX_PAGE_SIZE=100
SEARCH_CACHE_TTL=900
SEARCH_SUGGESTIONS_LIMIT=10

# Image Configuration
IMAGE_CDN_BASE_URL=https://cdn.example.com
IMAGE_UPLOAD_MAX_SIZE=5242880  # 5MB
IMAGE_ALLOWED_FORMATS=JPEG,PNG,WEBP
```

### Application Configuration

```yaml
# application.yml
server:
  port: 8084

spring:
  application:
    name: product-service

  data:
    mongodb:
      host: ${MONGODB_HOST:localhost}
      port: ${MONGODB_PORT:27017}
      database: ${MONGODB_DATABASE:ecommerce_catalog}
      username: ${MONGODB_USERNAME:}
      password: ${MONGODB_PASSWORD:}
      auto-index-creation: true

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: ${REDIS_DATABASE:1}
    timeout: 2000ms

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

grpc:
  server:
    port: 9084
    enable-reflection: true

app:
  search:
    default-page-size: ${SEARCH_DEFAULT_PAGE_SIZE:20}
    max-page-size: ${SEARCH_MAX_PAGE_SIZE:100}
    cache-ttl: ${SEARCH_CACHE_TTL:900}
    suggestions-limit: ${SEARCH_SUGGESTIONS_LIMIT:10}

  image:
    cdn-base-url: ${IMAGE_CDN_BASE_URL:https://cdn.example.com}
    upload-max-size: ${IMAGE_UPLOAD_MAX_SIZE:5242880}
    allowed-formats: ${IMAGE_ALLOWED_FORMATS:JPEG,PNG,WEBP}

  kafka:
    topics:
      product-events: ${KAFKA_TOPIC_PRODUCT_EVENTS:product-events}
```

## 🧪 Testing

### Test Coverage

- **Service Layer**: >95% coverage
- **Controller Layer**: >90% coverage
- **Repository Layer**: >85% coverage
- **Search Layer**: >90% coverage
- **Cache Layer**: >90% coverage
- **Event Publishing**: >95% coverage

### Key Test Scenarios

- Product CRUD operations
- Advanced search functionality
- Cache hit/miss scenarios
- Event publishing verification
- Multi-tenant isolation
- gRPC service communication
- MongoDB text search
- Redis caching behavior

## 📊 Monitoring & Metrics

### Prometheus Metrics

```bash
# Product-specific metrics
curl http://localhost:8084/actuator/prometheus | grep product_

# Key metrics:
# - products_created_total{tenant_id, category}
# - products_updated_total{tenant_id, category}
# - product_searches_total{tenant_id}
# - search_cache_hits_total
# - search_cache_misses_total
# - product_views_total{tenant_id, product_id}
```

## 🚀 Deployment

### Docker Configuration

```dockerfile
FROM openjdk:21-jre-slim

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

WORKDIR /app
COPY target/product-service-*.jar app.jar

EXPOSE 8084 9084

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

## 🔗 Dependencies

### Core Dependencies

- **Spring Boot 3.2.x**: Application framework
- **Spring Data MongoDB**: Database access
- **Spring Data Redis**: Caching layer
- **Spring Kafka**: Event streaming
- **gRPC Spring Boot Starter**: gRPC communication

---

**Performance Note**: This service is optimized for high-read workloads with comprehensive caching and efficient MongoDB indexing. Search performance is enhanced through Redis caching and MongoDB text indexes.

// Initialize reviews database and collections
db = db.getSiblingDB("reviews");

// Create reviews collection with validation schema
db.createCollection("reviews", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: [
        "tenantId",
        "userId",
        "productId",
        "rating",
        "title",
        "status",
      ],
      properties: {
        tenantId: {
          bsonType: "string",
          description: "Tenant ID is required",
        },
        userId: {
          bsonType: "long",
          description: "User ID is required",
        },
        productId: {
          bsonType: "string",
          description: "Product ID is required",
        },
        rating: {
          bsonType: "int",
          minimum: 1,
          maximum: 5,
          description: "Rating must be between 1 and 5",
        },
        title: {
          bsonType: "string",
          maxLength: 200,
          description: "Title is required and must not exceed 200 characters",
        },
        comment: {
          bsonType: "string",
          maxLength: 2000,
          description: "Comment must not exceed 2000 characters",
        },
        status: {
          bsonType: "string",
          enum: ["PENDING", "APPROVED", "REJECTED", "FLAGGED"],
          description: "Status must be one of the allowed values",
        },
        verified: {
          bsonType: "bool",
          description: "Verified must be a boolean",
        },
        helpfulVotes: {
          bsonType: "int",
          minimum: 0,
          description: "Helpful votes must be non-negative",
        },
        totalVotes: {
          bsonType: "int",
          minimum: 0,
          description: "Total votes must be non-negative",
        },
      },
    },
  },
});

print("Reviews collection created with validation schema");

// Create indexes for the reviews collection
db.reviews.createIndex(
  { tenantId: 1, userId: 1, productId: 1 },
  {
    unique: true,
    name: "unique_user_product_review",
    background: true,
  }
);

db.reviews.createIndex(
  { tenantId: 1, productId: 1 },
  {
    name: "product_tenant_idx",
    background: true,
  }
);

db.reviews.createIndex(
  { tenantId: 1, userId: 1 },
  {
    name: "user_tenant_idx",
    background: true,
  }
);

db.reviews.createIndex(
  { tenantId: 1, status: 1 },
  {
    name: "tenant_status_idx",
    background: true,
  }
);

db.reviews.createIndex(
  { tenantId: 1, productId: 1, status: 1 },
  {
    name: "product_status_idx",
    background: true,
  }
);

db.reviews.createIndex(
  { tenantId: 1, productId: 1, rating: 1, status: 1 },
  {
    name: "product_rating_status_idx",
    background: true,
  }
);

db.reviews.createIndex(
  { createdAt: 1 },
  {
    name: "created_at_idx",
    background: true,
  }
);

db.reviews.createIndex(
  { updatedAt: 1 },
  {
    name: "updated_at_idx",
    background: true,
  }
);

// Text index for search functionality
db.reviews.createIndex(
  {
    title: "text",
    comment: "text",
  },
  {
    name: "text_search_idx",
    background: true,
    weights: {
      title: 10,
      comment: 5,
    },
  }
);

print("All indexes created for reviews collection");

// Create a sample review for testing (optional)
db.reviews.insertOne({
  tenantId: "tenant_1",
  userId: NumberLong(1),
  productId: "product_123",
  rating: 5,
  title: "Excellent Product!",
  comment: "This product exceeded my expectations. Highly recommended!",
  status: "APPROVED",
  verified: true,
  imageUrls: [],
  helpfulVotes: 10,
  totalVotes: 12,
  createdAt: new Date(),
  updatedAt: new Date(),
});

print("Sample review inserted");
print("Reviews database initialization completed");

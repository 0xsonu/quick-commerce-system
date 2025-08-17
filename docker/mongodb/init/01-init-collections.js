// MongoDB Initialization Script for E-commerce Backend
// This script creates databases, collections, and indexes for products and reviews

// Switch to admin database for authentication
db = db.getSiblingDB("admin");

// Create databases and users for each tenant
const tenants = ["tenant_demo", "tenant_test"];

tenants.forEach(function (tenant) {
  // Create tenant-specific database
  const tenantDb = db.getSiblingDB(tenant);

  // Create products collection
  tenantDb.createCollection("products", {
    validator: {
      $jsonSchema: {
        bsonType: "object",
        required: ["tenant_id", "name", "sku", "price", "status"],
        properties: {
          tenant_id: {
            bsonType: "string",
            description: "Tenant ID is required",
          },
          name: {
            bsonType: "string",
            description: "Product name is required",
          },
          sku: {
            bsonType: "string",
            description: "SKU is required",
          },
          price: {
            bsonType: "object",
            required: ["amount", "currency"],
            properties: {
              amount: {
                bsonType: "double",
                minimum: 0,
                description: "Price amount must be a positive number",
              },
              currency: {
                bsonType: "string",
                description: "Currency code is required",
              },
            },
          },
          status: {
            bsonType: "string",
            enum: ["ACTIVE", "INACTIVE", "DISCONTINUED"],
            description:
              "Status must be one of: ACTIVE, INACTIVE, DISCONTINUED",
          },
        },
      },
    },
  });

  // Create reviews collection
  tenantDb.createCollection("reviews", {
    validator: {
      $jsonSchema: {
        bsonType: "object",
        required: ["tenant_id", "product_id", "user_id", "rating"],
        properties: {
          tenant_id: {
            bsonType: "string",
            description: "Tenant ID is required",
          },
          product_id: {
            bsonType: "string",
            description: "Product ID is required",
          },
          user_id: {
            bsonType: "long",
            description: "User ID is required",
          },
          rating: {
            bsonType: "int",
            minimum: 1,
            maximum: 5,
            description: "Rating must be between 1 and 5",
          },
        },
      },
    },
  });

  // Create product categories collection
  tenantDb.createCollection("product_categories", {
    validator: {
      $jsonSchema: {
        bsonType: "object",
        required: ["tenant_id", "name", "slug"],
        properties: {
          tenant_id: {
            bsonType: "string",
            description: "Tenant ID is required",
          },
          name: {
            bsonType: "string",
            description: "Category name is required",
          },
          slug: {
            bsonType: "string",
            description: "Category slug is required",
          },
        },
      },
    },
  });

  print(`Created collections for tenant: ${tenant}`);
});

print("MongoDB collections created successfully");

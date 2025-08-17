// MongoDB Index Creation Script
// Creates optimized indexes for products and reviews collections

const tenants = ["tenant_demo", "tenant_test"];

tenants.forEach(function (tenant) {
  const tenantDb = db.getSiblingDB(tenant);

  print(`Creating indexes for tenant: ${tenant}`);

  // ===== PRODUCTS COLLECTION INDEXES =====

  // Compound index for tenant isolation and basic queries
  tenantDb.products.createIndex(
    { tenant_id: 1, status: 1 },
    { name: "idx_tenant_status" }
  );

  // Unique index for SKU within tenant
  tenantDb.products.createIndex(
    { tenant_id: 1, sku: 1 },
    { name: "idx_tenant_sku", unique: true }
  );

  // Text index for full-text search
  tenantDb.products.createIndex(
    {
      name: "text",
      description: "text",
      category: "text",
      brand: "text",
      "attributes.color": "text",
      "attributes.size": "text",
    },
    {
      name: "idx_text_search",
      weights: {
        name: 10,
        description: 5,
        category: 8,
        brand: 7,
        "attributes.color": 3,
        "attributes.size": 3,
      },
    }
  );

  // Category and subcategory index for filtering
  tenantDb.products.createIndex(
    { tenant_id: 1, category: 1, subcategory: 1 },
    { name: "idx_tenant_category_subcategory" }
  );

  // Price range index for sorting and filtering
  tenantDb.products.createIndex(
    { tenant_id: 1, "price.amount": 1 },
    { name: "idx_tenant_price" }
  );

  // Brand index for filtering
  tenantDb.products.createIndex(
    { tenant_id: 1, brand: 1 },
    { name: "idx_tenant_brand" }
  );

  // Created date index for sorting by newest
  tenantDb.products.createIndex(
    { tenant_id: 1, created_at: -1 },
    { name: "idx_tenant_created_desc" }
  );

  // Updated date index for change tracking
  tenantDb.products.createIndex(
    { tenant_id: 1, updated_at: -1 },
    { name: "idx_tenant_updated_desc" }
  );

  // SEO slug index for URL routing
  tenantDb.products.createIndex(
    { tenant_id: 1, "seo.slug": 1 },
    { name: "idx_tenant_slug", unique: true, sparse: true }
  );

  // Compound index for category + price filtering
  tenantDb.products.createIndex(
    { tenant_id: 1, category: 1, "price.amount": 1, status: 1 },
    { name: "idx_tenant_category_price_status" }
  );

  // ===== REVIEWS COLLECTION INDEXES =====

  // Compound index for tenant and product reviews
  tenantDb.reviews.createIndex(
    { tenant_id: 1, product_id: 1 },
    { name: "idx_tenant_product" }
  );

  // Unique index to prevent duplicate reviews per user per product
  tenantDb.reviews.createIndex(
    { tenant_id: 1, product_id: 1, user_id: 1 },
    { name: "idx_tenant_product_user", unique: true }
  );

  // Rating index for filtering and sorting
  tenantDb.reviews.createIndex(
    { tenant_id: 1, product_id: 1, rating: -1 },
    { name: "idx_tenant_product_rating_desc" }
  );

  // Created date index for sorting by newest reviews
  tenantDb.reviews.createIndex(
    { tenant_id: 1, product_id: 1, created_at: -1 },
    { name: "idx_tenant_product_created_desc" }
  );

  // User reviews index
  tenantDb.reviews.createIndex(
    { tenant_id: 1, user_id: 1, created_at: -1 },
    { name: "idx_tenant_user_created_desc" }
  );

  // Moderation status index
  tenantDb.reviews.createIndex(
    { tenant_id: 1, moderation_status: 1 },
    { name: "idx_tenant_moderation_status" }
  );

  // Helpful votes index for sorting by most helpful
  tenantDb.reviews.createIndex(
    { tenant_id: 1, product_id: 1, helpful_votes: -1 },
    { name: "idx_tenant_product_helpful_desc" }
  );

  // Text index for review content search
  tenantDb.reviews.createIndex(
    { title: "text", content: "text" },
    {
      name: "idx_review_text_search",
      weights: {
        title: 10,
        content: 5,
      },
    }
  );

  // ===== PRODUCT CATEGORIES COLLECTION INDEXES =====

  // Tenant and slug index
  tenantDb.product_categories.createIndex(
    { tenant_id: 1, slug: 1 },
    { name: "idx_tenant_category_slug", unique: true }
  );

  // Parent category index for hierarchical categories
  tenantDb.product_categories.createIndex(
    { tenant_id: 1, parent_id: 1 },
    { name: "idx_tenant_parent_category" }
  );

  // Category name index
  tenantDb.product_categories.createIndex(
    { tenant_id: 1, name: 1 },
    { name: "idx_tenant_category_name" }
  );

  print(`Indexes created successfully for tenant: ${tenant}`);
});

print("All MongoDB indexes created successfully");

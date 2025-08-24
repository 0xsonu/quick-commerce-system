package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // Find by tenant and ID
    Optional<Product> findByTenantIdAndId(String tenantId, String id);

    // Find multiple products by tenant and IDs
    List<Product> findByTenantIdAndIdIn(String tenantId, List<String> ids);

    // Find by tenant and SKU
    Optional<Product> findByTenantIdAndSku(String tenantId, String sku);

    // Find all products for a tenant with pagination
    Page<Product> findByTenantId(String tenantId, Pageable pageable);

    // Find products by tenant and status
    Page<Product> findByTenantIdAndStatus(String tenantId, Product.ProductStatus status, Pageable pageable);

    // Find products by tenant and category
    Page<Product> findByTenantIdAndCategory(String tenantId, String category, Pageable pageable);

    // Find products by tenant, category and subcategory
    Page<Product> findByTenantIdAndCategoryAndSubcategory(String tenantId, String category, String subcategory, Pageable pageable);

    // Find products by tenant and brand
    Page<Product> findByTenantIdAndBrand(String tenantId, String brand, Pageable pageable);

    // Full-text search with tenant isolation
    @Query("{ 'tenantId': ?0, '$text': { '$search': ?1 } }")
    Page<Product> findByTenantIdAndTextSearch(String tenantId, String searchText, Pageable pageable);

    // Advanced search with multiple filters
    @Query("{ 'tenantId': ?0, " +
           "$and: [" +
           "  { $or: [ " +
           "    { 'category': { $regex: ?1, $options: 'i' } }, " +
           "    { 'category': { $exists: false } } " +
           "  ] }, " +
           "  { $or: [ " +
           "    { 'subcategory': { $regex: ?2, $options: 'i' } }, " +
           "    { 'subcategory': { $exists: false } } " +
           "  ] }, " +
           "  { $or: [ " +
           "    { 'brand': { $regex: ?3, $options: 'i' } }, " +
           "    { 'brand': { $exists: false } } " +
           "  ] }, " +
           "  { $or: [ " +
           "    { 'status': ?4 }, " +
           "    { 'status': { $exists: false } } " +
           "  ] } " +
           "] }")
    Page<Product> findByTenantIdWithFilters(String tenantId, String category, String subcategory, 
                                          String brand, Product.ProductStatus status, Pageable pageable);

    // Search with text and filters combined
    @Query("{ 'tenantId': ?0, " +
           "$text: { '$search': ?1 }, " +
           "$and: [" +
           "  { $or: [ " +
           "    { 'category': { $regex: ?2, $options: 'i' } }, " +
           "    { 'category': { $exists: false } } " +
           "  ] }, " +
           "  { $or: [ " +
           "    { 'brand': { $regex: ?3, $options: 'i' } }, " +
           "    { 'brand': { $exists: false } } " +
           "  ] }, " +
           "  { $or: [ " +
           "    { 'status': ?4 }, " +
           "    { 'status': { $exists: false } } " +
           "  ] } " +
           "] }")
    Page<Product> findByTenantIdAndTextSearchWithFilters(String tenantId, String searchText, 
                                                       String category, String brand, 
                                                       Product.ProductStatus status, Pageable pageable);

    // Count products by tenant
    long countByTenantId(String tenantId);

    // Count products by tenant and status
    long countByTenantIdAndStatus(String tenantId, Product.ProductStatus status);

    // Check if SKU exists for tenant
    boolean existsByTenantIdAndSku(String tenantId, String sku);

    // Check if SKU exists for tenant excluding specific product ID
    boolean existsByTenantIdAndSkuAndIdNot(String tenantId, String sku, String id);

    // Find products by tenant and price range
    @Query("{ 'tenantId': ?0, 'price.amount': { $gte: ?1, $lte: ?2 } }")
    Page<Product> findByTenantIdAndPriceRange(String tenantId, Double minPrice, Double maxPrice, Pageable pageable);

    // Get distinct categories for tenant
    @Query(value = "{ 'tenantId': ?0 }", fields = "{ 'category': 1 }")
    List<Product> findDistinctCategoriesByTenantId(String tenantId);

    // Get distinct brands for tenant
    @Query(value = "{ 'tenantId': ?0 }", fields = "{ 'brand': 1 }")
    List<Product> findDistinctBrandsByTenantId(String tenantId);

    // Delete by tenant and ID
    void deleteByTenantIdAndId(String tenantId, String id);
}
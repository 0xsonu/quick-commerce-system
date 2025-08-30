package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.shared.tracing.annotation.Traced;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Traced(operation = "product-service")
public class ProductService {

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;
    private final ProductEventPublisher eventPublisher;

    @Autowired
    public ProductService(ProductRepository productRepository, CacheManager cacheManager, 
                         ProductEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
    }

    @Cacheable(value = "products", key = "#tenantId + ':' + #productId")
    @Traced(value = "get-product", operation = "database-read", includeParameters = true)
    public ProductResponse getProduct(String tenantId, String productId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return new ProductResponse(product);
    }

    /**
     * Get product by ID for internal gRPC calls (returns Optional)
     */
    @Cacheable(value = "products", key = "T(com.ecommerce.shared.utils.TenantContext).getTenantId() + ':' + #productId")
    public Optional<Product> getProductById(String productId) {
        String tenantId = com.ecommerce.shared.utils.TenantContext.getTenantId();
        return productRepository.findByTenantIdAndId(tenantId, productId);
    }

    /**
     * Get multiple products by IDs for internal gRPC calls
     */
    public List<Product> getProductsByIds(List<String> productIds) {
        String tenantId = com.ecommerce.shared.utils.TenantContext.getTenantId();
        return productRepository.findByTenantIdAndIdIn(tenantId, productIds);
    }

    @Cacheable(value = "product-search", key = "#tenantId + ':' + #sku")
    public ProductResponse getProductBySkuCached(String tenantId, String sku) {
        return getProductBySku(tenantId, sku);
    }

    public ProductResponse getProductBySku(String tenantId, String sku) {
        Product product = productRepository.findByTenantIdAndSku(tenantId, sku)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        
        return new ProductResponse(product);
    }

    public PagedResponse<ProductSummaryResponse> getProducts(String tenantId, int page, int size, 
                                                           String category, String subcategory, 
                                                           String brand, String search, 
                                                           Product.ProductStatus status,
                                                           String sortBy, String sortDirection) {
        
        // Validate and set default sorting
        if (!StringUtils.hasText(sortBy)) {
            sortBy = "updatedAt";
        }
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Product> productPage;
        
        if (StringUtils.hasText(search)) {
            // Text search with optional filters
            if (hasFilters(category, brand, status)) {
                productPage = productRepository.findByTenantIdAndTextSearchWithFilters(
                    tenantId, search, category, brand, status, pageable);
            } else {
                productPage = productRepository.findByTenantIdAndTextSearch(tenantId, search, pageable);
            }
        } else if (hasFilters(category, brand, status) || StringUtils.hasText(subcategory)) {
            // Filter-based search
            if (StringUtils.hasText(category) && StringUtils.hasText(subcategory)) {
                productPage = productRepository.findByTenantIdAndCategoryAndSubcategory(
                    tenantId, category, subcategory, pageable);
            } else if (StringUtils.hasText(category)) {
                productPage = productRepository.findByTenantIdAndCategory(tenantId, category, pageable);
            } else if (StringUtils.hasText(brand)) {
                productPage = productRepository.findByTenantIdAndBrand(tenantId, brand, pageable);
            } else if (status != null) {
                productPage = productRepository.findByTenantIdAndStatus(tenantId, status, pageable);
            } else {
                productPage = productRepository.findByTenantIdWithFilters(
                    tenantId, category, subcategory, brand, status, pageable);
            }
        } else {
            // Get all products for tenant
            productPage = productRepository.findByTenantId(tenantId, pageable);
        }
        
        List<ProductSummaryResponse> content = productPage.getContent().stream()
            .map(ProductSummaryResponse::new)
            .collect(Collectors.toList());
        
        return new PagedResponse<>(content, productPage.getNumber(), productPage.getSize(),
                                 productPage.getTotalElements(), productPage.getTotalPages());
    }

    public ProductResponse createProduct(String tenantId, CreateProductRequest request) {
        // Validate SKU uniqueness
        if (productRepository.existsByTenantIdAndSku(tenantId, request.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + request.getSku() + "' already exists");
        }
        
        Product product = mapToProduct(tenantId, request);
        Product savedProduct = productRepository.save(product);
        
        // Publish ProductCreated event
        eventPublisher.publishProductCreatedEvent(savedProduct);
        
        // Evict related caches when new product is created
        evictRelatedCaches(tenantId);
        
        return new ProductResponse(savedProduct);
    }

    @CacheEvict(value = {"products", "product-search", "categories", "brands", "product-recommendations"}, 
                key = "#tenantId + ':' + #productId", allEntries = true)
    public ProductResponse updateProduct(String tenantId, String productId, UpdateProductRequest request) {
        Product existingProduct = productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        // Capture previous values for event publishing
        Map<String, Object> previousValues = captureProductValues(existingProduct);
        
        // Check SKU uniqueness if SKU is being updated
        if (request.getName() != null && !request.getName().equals(existingProduct.getName())) {
            // Generate new SKU if name changed (optional business logic)
        }
        
        // Track which fields are being updated
        Map<String, Object> updatedFields = trackUpdatedFields(request);
        
        updateProductFields(existingProduct, request);
        Product savedProduct = productRepository.save(existingProduct);
        
        // Publish ProductUpdated event
        eventPublisher.publishProductUpdatedEvent(savedProduct, previousValues, updatedFields);
        
        return new ProductResponse(savedProduct);
    }

    @CacheEvict(value = {"products", "product-search", "categories", "brands", "product-recommendations"}, 
                key = "#tenantId + ':' + #productId", allEntries = true)
    public void deleteProduct(String tenantId, String productId) {
        Product existingProduct = productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        // Publish ProductDeleted event before deletion
        eventPublisher.publishProductDeletedEvent(existingProduct, "Manual deletion");
        
        productRepository.deleteByTenantIdAndId(tenantId, productId);
    }

    public PagedResponse<ProductSummaryResponse> searchProducts(String tenantId, String searchText, 
                                                              int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Product> productPage = productRepository.findByTenantIdAndTextSearch(tenantId, searchText, pageable);
        
        List<ProductSummaryResponse> content = productPage.getContent().stream()
            .map(ProductSummaryResponse::new)
            .collect(Collectors.toList());
        
        return new PagedResponse<>(content, productPage.getNumber(), productPage.getSize(),
                                 productPage.getTotalElements(), productPage.getTotalPages());
    }

    @Cacheable(value = "categories", key = "#tenantId")
    public List<String> getCategories(String tenantId) {
        return productRepository.findDistinctCategoriesByTenantId(tenantId).stream()
            .map(Product::getCategory)
            .filter(category -> category != null && !category.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    @Cacheable(value = "brands", key = "#tenantId")
    public List<String> getBrands(String tenantId) {
        return productRepository.findDistinctBrandsByTenantId(tenantId).stream()
            .map(Product::getBrand)
            .filter(brand -> brand != null && !brand.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    public long getProductCount(String tenantId) {
        return productRepository.countByTenantId(tenantId);
    }

    public long getProductCountByStatus(String tenantId, Product.ProductStatus status) {
        return productRepository.countByTenantIdAndStatus(tenantId, status);
    }

    // Private helper methods
    private boolean hasFilters(String category, String brand, Product.ProductStatus status) {
        return StringUtils.hasText(category) || StringUtils.hasText(brand) || status != null;
    }

    private Product mapToProduct(String tenantId, CreateProductRequest request) {
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setSubcategory(request.getSubcategory());
        product.setBrand(request.getBrand());
        product.setSku(request.getSku());
        
        if (request.getPrice() != null) {
            product.setPrice(new Product.Price(request.getPrice().getAmount(), request.getPrice().getCurrency()));
        }
        
        if (request.getImages() != null) {
            List<Product.ProductImage> images = request.getImages().stream()
                .map(img -> new Product.ProductImage(img.getUrl(), img.getAltText(), img.isPrimary()))
                .collect(Collectors.toList());
            product.setImages(images);
        }
        
        product.setAttributes(request.getAttributes());
        
        if (request.getSeo() != null) {
            product.setSeo(new Product.SEO(request.getSeo().getMetaTitle(), 
                                         request.getSeo().getMetaDescription(), 
                                         request.getSeo().getSlug()));
        }
        
        return product;
    }

    private void updateProductFields(Product product, UpdateProductRequest request) {
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getSubcategory() != null) {
            product.setSubcategory(request.getSubcategory());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getPrice() != null) {
            product.setPrice(new Product.Price(request.getPrice().getAmount(), request.getPrice().getCurrency()));
        }
        if (request.getImages() != null) {
            List<Product.ProductImage> images = request.getImages().stream()
                .map(img -> new Product.ProductImage(img.getUrl(), img.getAltText(), img.isPrimary()))
                .collect(Collectors.toList());
            product.setImages(images);
        }
        if (request.getAttributes() != null) {
            product.setAttributes(request.getAttributes());
        }
        if (request.getSeo() != null) {
            product.setSeo(new Product.SEO(request.getSeo().getMetaTitle(), 
                                         request.getSeo().getMetaDescription(), 
                                         request.getSeo().getSlug()));
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
    }

    private void evictRelatedCaches(String tenantId) {
        // Evict tenant-specific caches
        String[] cacheNames = {"categories", "brands", "product-search", "product-recommendations"};
        for (String cacheName : cacheNames) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(tenantId);
            }
        }
    }

    private Map<String, Object> captureProductValues(Product product) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", product.getName());
        values.put("description", product.getDescription());
        values.put("category", product.getCategory());
        values.put("subcategory", product.getSubcategory());
        values.put("brand", product.getBrand());
        values.put("sku", product.getSku());
        if (product.getPrice() != null) {
            values.put("price", product.getPrice().getAmount());
            values.put("currency", product.getPrice().getCurrency());
        }
        values.put("attributes", product.getAttributes());
        values.put("status", product.getStatus() != null ? product.getStatus().name() : null);
        return values;
    }

    private Map<String, Object> trackUpdatedFields(UpdateProductRequest request) {
        Map<String, Object> updatedFields = new HashMap<>();
        if (request.getName() != null) {
            updatedFields.put("name", request.getName());
        }
        if (request.getDescription() != null) {
            updatedFields.put("description", request.getDescription());
        }
        if (request.getCategory() != null) {
            updatedFields.put("category", request.getCategory());
        }
        if (request.getSubcategory() != null) {
            updatedFields.put("subcategory", request.getSubcategory());
        }
        if (request.getBrand() != null) {
            updatedFields.put("brand", request.getBrand());
        }
        if (request.getPrice() != null) {
            updatedFields.put("price", request.getPrice().getAmount());
            updatedFields.put("currency", request.getPrice().getCurrency());
        }
        if (request.getAttributes() != null) {
            updatedFields.put("attributes", request.getAttributes());
        }
        if (request.getStatus() != null) {
            updatedFields.put("status", request.getStatus().name());
        }
        return updatedFields;
    }
}
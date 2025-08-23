package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "products", key = "#tenantId + ':' + #productId")
    public ProductResponse getProduct(String tenantId, String productId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        return new ProductResponse(product);
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
        
        return new ProductResponse(savedProduct);
    }

    @CacheEvict(value = "products", key = "#tenantId + ':' + #productId")
    public ProductResponse updateProduct(String tenantId, String productId, UpdateProductRequest request) {
        Product existingProduct = productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        
        // Check SKU uniqueness if SKU is being updated
        if (request.getName() != null && !request.getName().equals(existingProduct.getName())) {
            // Generate new SKU if name changed (optional business logic)
        }
        
        updateProductFields(existingProduct, request);
        Product savedProduct = productRepository.save(existingProduct);
        
        return new ProductResponse(savedProduct);
    }

    @CacheEvict(value = "products", key = "#tenantId + ':' + #productId")
    public void deleteProduct(String tenantId, String productId) {
        if (!productRepository.findByTenantIdAndId(tenantId, productId).isPresent()) {
            throw new ResourceNotFoundException("Product not found with ID: " + productId);
        }
        
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

    public List<String> getCategories(String tenantId) {
        return productRepository.findDistinctCategoriesByTenantId(tenantId).stream()
            .map(Product::getCategory)
            .filter(category -> category != null && !category.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

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
}
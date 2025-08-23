package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductSummaryResponse;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductRecommendationService {

    private final ProductRepository productRepository;
    private final ProductSearchService productSearchService;

    @Autowired
    public ProductRecommendationService(ProductRepository productRepository, 
                                      ProductSearchService productSearchService) {
        this.productRepository = productRepository;
        this.productSearchService = productSearchService;
    }

    @Cacheable(value = "product-recommendations", key = "#tenantId + ':similar:' + #productId + ':' + #limit")
    public List<ProductSummaryResponse> getSimilarProducts(String tenantId, String productId, int limit) {
        return productSearchService.findSimilarProducts(tenantId, productId, limit);
    }

    @Cacheable(value = "product-recommendations", key = "#tenantId + ':frequently-bought:' + #productId + ':' + #limit")
    public List<ProductSummaryResponse> getFrequentlyBoughtTogether(String tenantId, String productId, int limit) {
        // Simplified implementation - in production, this would use order history analysis
        Product product = productRepository.findByTenantIdAndId(tenantId, productId).orElse(null);
        if (product == null) {
            return List.of();
        }

        // Find products in the same category with similar price range
        BigDecimal productPrice = product.getPrice() != null ? product.getPrice().getAmount() : BigDecimal.ZERO;
        BigDecimal minPrice = productPrice.multiply(BigDecimal.valueOf(0.5)); // 50% of price
        BigDecimal maxPrice = productPrice.multiply(BigDecimal.valueOf(2.0)); // 200% of price

        Pageable pageable = PageRequest.of(0, limit * 2, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        return productRepository.findByTenantIdAndPriceRange(
                tenantId, minPrice.doubleValue(), maxPrice.doubleValue(), pageable)
            .getContent().stream()
            .filter(p -> !p.getId().equals(productId)) // Exclude current product
            .filter(p -> Objects.equals(p.getCategory(), product.getCategory())) // Same category
            .map(ProductSummaryResponse::new)
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "product-recommendations", key = "#tenantId + ':trending:' + #category + ':' + #limit")
    public List<ProductSummaryResponse> getTrendingProducts(String tenantId, String category, int limit) {
        // Simplified trending logic - in production, this would use view/purchase analytics
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        if (StringUtils.hasText(category)) {
            return productRepository.findByTenantIdAndCategory(tenantId, category, pageable)
                .getContent().stream()
                .filter(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
                .map(ProductSummaryResponse::new)
                .collect(Collectors.toList());
        } else {
            return productRepository.findByTenantIdAndStatus(tenantId, Product.ProductStatus.ACTIVE, pageable)
                .getContent().stream()
                .map(ProductSummaryResponse::new)
                .collect(Collectors.toList());
        }
    }

    @Cacheable(value = "product-recommendations", key = "#tenantId + ':personalized:' + #userPreferences.hashCode() + ':' + #limit")
    public List<ProductSummaryResponse> getPersonalizedRecommendations(String tenantId, 
                                                                     UserPreferences userPreferences, 
                                                                     int limit) {
        List<ProductSummaryResponse> recommendations = new ArrayList<>();
        
        // Get products from preferred categories
        if (userPreferences.getPreferredCategories() != null && !userPreferences.getPreferredCategories().isEmpty()) {
            for (String category : userPreferences.getPreferredCategories()) {
                List<ProductSummaryResponse> categoryProducts = productSearchService.findPopularProducts(
                    tenantId, category, limit / userPreferences.getPreferredCategories().size() + 1);
                recommendations.addAll(categoryProducts);
            }
        }
        
        // Get products from preferred brands
        if (userPreferences.getPreferredBrands() != null && !userPreferences.getPreferredBrands().isEmpty()) {
            for (String brand : userPreferences.getPreferredBrands()) {
                Pageable pageable = PageRequest.of(0, limit / userPreferences.getPreferredBrands().size() + 1);
                List<ProductSummaryResponse> brandProducts = productRepository.findByTenantIdAndBrand(
                        tenantId, brand, pageable)
                    .getContent().stream()
                    .filter(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
                    .map(ProductSummaryResponse::new)
                    .collect(Collectors.toList());
                recommendations.addAll(brandProducts);
            }
        }
        
        // Get products in preferred price range
        if (userPreferences.getMinPrice() != null || userPreferences.getMaxPrice() != null) {
            double minPrice = userPreferences.getMinPrice() != null ? 
                userPreferences.getMinPrice().doubleValue() : 0.0;
            double maxPrice = userPreferences.getMaxPrice() != null ? 
                userPreferences.getMaxPrice().doubleValue() : Double.MAX_VALUE;
            
            Pageable pageable = PageRequest.of(0, limit);
            List<ProductSummaryResponse> priceRangeProducts = productRepository.findByTenantIdAndPriceRange(
                    tenantId, minPrice, maxPrice, pageable)
                .getContent().stream()
                .filter(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
                .map(ProductSummaryResponse::new)
                .collect(Collectors.toList());
            recommendations.addAll(priceRangeProducts);
        }
        
        // Remove duplicates and limit results
        return recommendations.stream()
            .collect(Collectors.toMap(
                ProductSummaryResponse::getId, 
                p -> p, 
                (existing, replacement) -> existing,
                LinkedHashMap::new))
            .values()
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "product-recommendations", key = "#tenantId + ':new-arrivals:' + #limit")
    public List<ProductSummaryResponse> getNewArrivals(String tenantId, int limit) {
        return productSearchService.findRecentProducts(tenantId, limit);
    }

    @Cacheable(value = "product-recommendations", key = "#tenantId + ':featured:' + #limit")
    public List<ProductSummaryResponse> getFeaturedProducts(String tenantId, int limit) {
        // In a real implementation, you might have a "featured" flag on products
        // For now, we'll return popular products
        return productSearchService.findPopularProducts(tenantId, null, limit);
    }

    @Cacheable(value = "product-recommendations", key = "#tenantId + ':cross-sell:' + #cartItems.hashCode() + ':' + #limit")
    public List<ProductSummaryResponse> getCrossSellRecommendations(String tenantId, 
                                                                  List<String> cartItems, 
                                                                  int limit) {
        if (cartItems == null || cartItems.isEmpty()) {
            return List.of();
        }

        Set<String> recommendations = new HashSet<>();
        
        // For each item in cart, find similar products
        for (String productId : cartItems) {
            List<ProductSummaryResponse> similar = getSimilarProducts(tenantId, productId, 5);
            similar.forEach(p -> recommendations.add(p.getId()));
        }
        
        // Remove items already in cart
        recommendations.removeAll(cartItems);
        
        // Get product details for recommendations
        return recommendations.stream()
            .limit(limit)
            .map(id -> {
                try {
                    Product product = productRepository.findByTenantIdAndId(tenantId, id).orElse(null);
                    return product != null ? new ProductSummaryResponse(product) : null;
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // User preferences class for personalized recommendations
    public static class UserPreferences {
        private List<String> preferredCategories;
        private List<String> preferredBrands;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private List<String> recentlyViewedProducts;

        // Constructors
        public UserPreferences() {}

        public UserPreferences(List<String> preferredCategories, List<String> preferredBrands,
                             BigDecimal minPrice, BigDecimal maxPrice, List<String> recentlyViewedProducts) {
            this.preferredCategories = preferredCategories;
            this.preferredBrands = preferredBrands;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.recentlyViewedProducts = recentlyViewedProducts;
        }

        // Getters and Setters
        public List<String> getPreferredCategories() {
            return preferredCategories;
        }

        public void setPreferredCategories(List<String> preferredCategories) {
            this.preferredCategories = preferredCategories;
        }

        public List<String> getPreferredBrands() {
            return preferredBrands;
        }

        public void setPreferredBrands(List<String> preferredBrands) {
            this.preferredBrands = preferredBrands;
        }

        public BigDecimal getMinPrice() {
            return minPrice;
        }

        public void setMinPrice(BigDecimal minPrice) {
            this.minPrice = minPrice;
        }

        public BigDecimal getMaxPrice() {
            return maxPrice;
        }

        public void setMaxPrice(BigDecimal maxPrice) {
            this.maxPrice = maxPrice;
        }

        public List<String> getRecentlyViewedProducts() {
            return recentlyViewedProducts;
        }

        public void setRecentlyViewedProducts(List<String> recentlyViewedProducts) {
            this.recentlyViewedProducts = recentlyViewedProducts;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserPreferences that = (UserPreferences) o;
            return Objects.equals(preferredCategories, that.preferredCategories) &&
                   Objects.equals(preferredBrands, that.preferredBrands) &&
                   Objects.equals(minPrice, that.minPrice) &&
                   Objects.equals(maxPrice, that.maxPrice) &&
                   Objects.equals(recentlyViewedProducts, that.recentlyViewedProducts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(preferredCategories, preferredBrands, minPrice, maxPrice, recentlyViewedProducts);
        }
    }
}
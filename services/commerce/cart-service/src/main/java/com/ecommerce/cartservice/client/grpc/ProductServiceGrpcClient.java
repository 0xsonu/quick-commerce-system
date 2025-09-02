package com.ecommerce.cartservice.client.grpc;

import com.ecommerce.cartservice.dto.ProductValidationResponse;
import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.productservice.proto.ProductServiceGrpc;
import com.ecommerce.productservice.proto.ProductServiceProtos.*;
import com.ecommerce.shared.grpc.GrpcContextUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for Product Service
 */
@Component
public class ProductServiceGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceGrpcClient.class);

    @GrpcClient("product-service")
    private ProductServiceGrpc.ProductServiceBlockingStub productServiceStub;

    /**
     * Validate product for cart operations
     */
    @CircuitBreaker(name = "product-service", fallbackMethod = "validateProductFallback")
    @Retry(name = "product-service")
    public ProductValidationResponse validateProduct(String productId, String sku) {
        try {
            logger.debug("Validating product via gRPC: productId={}, sku={}", productId, sku);

            // Create request with tenant context
            ValidateProductRequest request = ValidateProductRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setProductId(productId)
                .setSku(sku != null ? sku : "")
                .build();

            // Make gRPC call with context and timeout
            ValidateProductResponse response = GrpcContextUtils.withCurrentContext(productServiceStub)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .validateProduct(request);

            // Convert to DTO
            ProductValidationResponse validationResponse = new ProductValidationResponse();
            validationResponse.setProductId(response.getProductId());
            validationResponse.setSku(response.getSku());
            validationResponse.setValid(response.getIsValid());
            validationResponse.setName(response.getName());
            validationResponse.setActive(response.getIsActive());
            
            if (response.hasPrice()) {
                // Convert from cents to decimal
                BigDecimal price = BigDecimal.valueOf(response.getPrice().getAmountCents())
                    .divide(BigDecimal.valueOf(100));
                validationResponse.setPrice(price);
                validationResponse.setCurrency(response.getPrice().getCurrency());
            }

            logger.debug("Product validation completed via gRPC: productId={}, valid={}", 
                        productId, validationResponse.isValid());

            return validationResponse;

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error validating product: productId={}, status={}, description={}", 
                        productId, e.getStatus().getCode(), e.getStatus().getDescription());
            
            // Map gRPC errors to appropriate business exceptions
            throw mapGrpcException(e, "Product validation failed for product: " + productId);
            
        } catch (Exception e) {
            logger.error("Unexpected error validating product via gRPC: productId={}", productId, e);
            throw new ProductNotAvailableException("Product validation failed due to unexpected error: " + productId, e);
        }
    }

    /**
     * Map gRPC exceptions to appropriate business exceptions
     */
    private RuntimeException mapGrpcException(StatusRuntimeException e, String operation) {
        Status.Code code = e.getStatus().getCode();
        String message = e.getStatus().getDescription();
        
        return switch (code) {
            case NOT_FOUND -> new ProductNotAvailableException(operation + " - Product not found: " + message);
            case PERMISSION_DENIED -> new ProductNotAvailableException(operation + " - Access denied: " + message);
            case INVALID_ARGUMENT -> new IllegalArgumentException(operation + " - Invalid argument: " + message);
            case UNAVAILABLE -> new ProductNotAvailableException(operation + " - Service unavailable: " + message);
            case DEADLINE_EXCEEDED -> new ProductNotAvailableException(operation + " - Request timeout: " + message);
            case RESOURCE_EXHAUSTED -> new ProductNotAvailableException(operation + " - Rate limit exceeded: " + message);
            default -> new ProductNotAvailableException(operation + " - " + code + ": " + message, e);
        };
    }

    /**
     * Fallback method for validateProduct when circuit breaker is open
     */
    public ProductValidationResponse validateProductFallback(String productId, String sku, Exception ex) {
        logger.warn("Product validation fallback triggered for product: {}, error: {}", productId, ex.getMessage());
        throw new ProductNotAvailableException("Product service is currently unavailable. Please try again later.", ex);
    }

    /**
     * Get multiple products by IDs
     */
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductsByIdsFallback")
    @Retry(name = "product-service")
    public List<ProductValidationResponse> getProductsByIds(List<String> productIds) {
        try {
            logger.debug("Getting products by IDs via gRPC: count={}", productIds.size());

            // Create request with tenant context
            GetProductsByIdsRequest request = GetProductsByIdsRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .addAllProductIds(productIds)
                .build();

            // Make gRPC call with context and timeout
            GetProductsByIdsResponse response = GrpcContextUtils.withCurrentContext(productServiceStub)
                .withDeadlineAfter(10, TimeUnit.SECONDS) // Longer timeout for batch operations
                .getProductsByIds(request);

            // Convert to DTOs
            List<ProductValidationResponse> products = new ArrayList<>();
            for (Product product : response.getProductsList()) {
                ProductValidationResponse validationResponse = new ProductValidationResponse();
                validationResponse.setProductId(product.getId());
                validationResponse.setSku(product.getSku());
                validationResponse.setName(product.getName());
                validationResponse.setValid(true); // If returned, it's valid
                validationResponse.setActive(product.getIsActive());
                
                if (product.hasPrice()) {
                    // Convert from cents to decimal
                    BigDecimal price = BigDecimal.valueOf(product.getPrice().getAmountCents())
                        .divide(BigDecimal.valueOf(100));
                    validationResponse.setPrice(price);
                    validationResponse.setCurrency(product.getPrice().getCurrency());
                }
                
                products.add(validationResponse);
            }

            logger.debug("Retrieved {} products via gRPC", products.size());
            return products;

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error getting products by IDs: status={}, description={}", 
                        e.getStatus().getCode(), e.getStatus().getDescription());
            throw mapGrpcException(e, "Failed to get products by IDs");
            
        } catch (Exception e) {
            logger.error("Unexpected error getting products by IDs via gRPC", e);
            throw new ProductNotAvailableException("Failed to get products by IDs due to unexpected error", e);
        }
    }

    /**
     * Fallback method for getProductsByIds when circuit breaker is open
     */
    public List<ProductValidationResponse> getProductsByIdsFallback(List<String> productIds, Exception ex) {
        logger.warn("Get products by IDs fallback triggered for {} products, error: {}", productIds.size(), ex.getMessage());
        throw new ProductNotAvailableException("Product service is currently unavailable. Please try again later.", ex);
    }

    /**
     * Get single product by ID
     */
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductFallback")
    @Retry(name = "product-service")
    public ProductValidationResponse getProduct(String productId) {
        try {
            logger.debug("Getting product by ID via gRPC: productId={}", productId);

            // Create request with tenant context
            GetProductRequest request = GetProductRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setProductId(productId)
                .build();

            // Make gRPC call with context and timeout
            GetProductResponse response = GrpcContextUtils.withCurrentContext(productServiceStub)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getProduct(request);

            // Convert to DTO
            Product product = response.getProduct();
            ProductValidationResponse validationResponse = new ProductValidationResponse();
            validationResponse.setProductId(product.getId());
            validationResponse.setSku(product.getSku());
            validationResponse.setName(product.getName());
            validationResponse.setValid(true); // If returned, it's valid
            validationResponse.setActive(product.getIsActive());
            
            if (product.hasPrice()) {
                // Convert from cents to decimal
                BigDecimal price = BigDecimal.valueOf(product.getPrice().getAmountCents())
                    .divide(BigDecimal.valueOf(100));
                validationResponse.setPrice(price);
                validationResponse.setCurrency(product.getPrice().getCurrency());
            }

            logger.debug("Retrieved product via gRPC: productId={}, active={}", 
                        productId, validationResponse.isActive());

            return validationResponse;

        } catch (StatusRuntimeException e) {
            logger.error("gRPC error getting product: productId={}, status={}, description={}", 
                        productId, e.getStatus().getCode(), e.getStatus().getDescription());
            throw mapGrpcException(e, "Failed to get product: " + productId);
            
        } catch (Exception e) {
            logger.error("Unexpected error getting product via gRPC: productId={}", productId, e);
            throw new ProductNotAvailableException("Failed to get product due to unexpected error: " + productId, e);
        }
    }

    /**
     * Fallback method for getProduct when circuit breaker is open
     */
    public ProductValidationResponse getProductFallback(String productId, Exception ex) {
        logger.warn("Get product fallback triggered for product: {}, error: {}", productId, ex.getMessage());
        throw new ProductNotAvailableException("Product service is currently unavailable. Please try again later.", ex);
    }
}
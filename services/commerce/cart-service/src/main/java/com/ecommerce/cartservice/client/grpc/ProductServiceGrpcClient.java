package com.ecommerce.cartservice.client.grpc;

import com.ecommerce.cartservice.dto.ProductValidationResponse;
import com.ecommerce.productservice.proto.ProductServiceGrpc;
import com.ecommerce.productservice.proto.ProductServiceProtos.*;
import com.ecommerce.shared.grpc.GrpcContextUtils;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    public ProductValidationResponse validateProduct(String productId, String sku) {
        try {
            logger.debug("Validating product via gRPC: productId={}, sku={}", productId, sku);

            // Create request with tenant context
            ValidateProductRequest request = ValidateProductRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setProductId(productId)
                .setSku(sku != null ? sku : "")
                .build();

            // Make gRPC call with context
            ValidateProductResponse response = GrpcContextUtils.withCurrentContext(productServiceStub)
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
            logger.error("gRPC error validating product: productId={}, error={}", productId, e.getStatus(), e);
            
            // Return invalid response on error
            ProductValidationResponse errorResponse = new ProductValidationResponse();
            errorResponse.setProductId(productId);
            errorResponse.setSku(sku);
            errorResponse.setValid(false);
            errorResponse.setActive(false);
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Unexpected error validating product via gRPC: productId={}", productId, e);
            
            // Return invalid response on error
            ProductValidationResponse errorResponse = new ProductValidationResponse();
            errorResponse.setProductId(productId);
            errorResponse.setSku(sku);
            errorResponse.setValid(false);
            errorResponse.setActive(false);
            return errorResponse;
        }
    }

    /**
     * Get multiple products by IDs
     */
    public List<ProductValidationResponse> getProductsByIds(List<String> productIds) {
        try {
            logger.debug("Getting products by IDs via gRPC: count={}", productIds.size());

            // Create request with tenant context
            GetProductsByIdsRequest request = GetProductsByIdsRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .addAllProductIds(productIds)
                .build();

            // Make gRPC call with context
            GetProductsByIdsResponse response = GrpcContextUtils.withCurrentContext(productServiceStub)
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
            logger.error("gRPC error getting products by IDs: error={}", e.getStatus(), e);
            return new ArrayList<>(); // Return empty list on error
            
        } catch (Exception e) {
            logger.error("Unexpected error getting products by IDs via gRPC", e);
            return new ArrayList<>(); // Return empty list on error
        }
    }

    /**
     * Get single product by ID
     */
    public ProductValidationResponse getProduct(String productId) {
        try {
            logger.debug("Getting product by ID via gRPC: productId={}", productId);

            // Create request with tenant context
            GetProductRequest request = GetProductRequest.newBuilder()
                .setContext(GrpcContextUtils.createTenantContext())
                .setProductId(productId)
                .build();

            // Make gRPC call with context
            GetProductResponse response = GrpcContextUtils.withCurrentContext(productServiceStub)
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
            logger.error("gRPC error getting product: productId={}, error={}", productId, e.getStatus(), e);
            
            // Return invalid response on error
            ProductValidationResponse errorResponse = new ProductValidationResponse();
            errorResponse.setProductId(productId);
            errorResponse.setValid(false);
            errorResponse.setActive(false);
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Unexpected error getting product via gRPC: productId={}", productId, e);
            
            // Return invalid response on error
            ProductValidationResponse errorResponse = new ProductValidationResponse();
            errorResponse.setProductId(productId);
            errorResponse.setValid(false);
            errorResponse.setActive(false);
            return errorResponse;
        }
    }
}
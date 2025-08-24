package com.ecommerce.productservice.grpc;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.service.ProductService;
import com.ecommerce.productservice.proto.ProductServiceProtos.*;
import com.ecommerce.productservice.proto.ProductServiceGrpc;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.grpc.GrpcContextUtils;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * gRPC service implementation for Product Service
 */
@GrpcService
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ProductGrpcService.class);

    @Autowired
    private ProductService productService;

    @Override
    public void getProduct(GetProductRequest request, StreamObserver<GetProductResponse> responseObserver) {
        try {
            logger.debug("gRPC GetProduct called for productId: {}, tenantId: {}", 
                        request.getProductId(), request.getContext().getTenantId());

            // Set tenant context from request
            GrpcContextUtils.setThreadContext(request.getContext());

            // Get product from service
            Optional<Product> productOpt = productService.getProductById(request.getProductId());

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                
                // Convert to protobuf
                com.ecommerce.productservice.proto.ProductServiceProtos.Product protoProduct = 
                    convertToProtoProduct(product);

                GetProductResponse response = GetProductResponse.newBuilder()
                    .setProduct(protoProduct)
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
                logger.debug("gRPC GetProduct completed successfully for productId: {}", request.getProductId());
            } else {
                logger.warn("Product not found for productId: {}", request.getProductId());
                responseObserver.onError(new RuntimeException("Product not found: " + request.getProductId()));
            }

        } catch (Exception e) {
            logger.error("Error in gRPC GetProduct for productId: {}", request.getProductId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void validateProduct(ValidateProductRequest request, StreamObserver<ValidateProductResponse> responseObserver) {
        try {
            logger.debug("gRPC ValidateProduct called for productId: {}, sku: {}, tenantId: {}", 
                        request.getProductId(), request.getSku(), request.getContext().getTenantId());

            // Set tenant context from request
            GrpcContextUtils.setThreadContext(request.getContext());

            // Get product from service
            Optional<Product> productOpt = productService.getProductById(request.getProductId());

            ValidateProductResponse.Builder responseBuilder = ValidateProductResponse.newBuilder()
                .setProductId(request.getProductId())
                .setSku(request.getSku());

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                
                // Validate SKU if provided
                boolean isValid = true;
                if (!request.getSku().isEmpty() && !request.getSku().equals(product.getSku())) {
                    isValid = false;
                    logger.warn("SKU mismatch for productId: {}, expected: {}, got: {}", 
                               request.getProductId(), product.getSku(), request.getSku());
                }

                boolean isProductActive = product.getStatus() == Product.ProductStatus.ACTIVE;
                responseBuilder
                    .setIsValid(isValid && isProductActive)
                    .setName(product.getName())
                    .setPrice(CommonProtos.Money.newBuilder()
                        .setAmountCents(product.getPrice().getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                        .setCurrency(product.getPrice().getCurrency())
                        .build())
                    .setIsActive(isProductActive);

            } else {
                logger.warn("Product not found for validation: {}", request.getProductId());
                responseBuilder.setIsValid(false);
            }

            ValidateProductResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            logger.debug("gRPC ValidateProduct completed for productId: {}, valid: {}", 
                        request.getProductId(), response.getIsValid());

        } catch (Exception e) {
            logger.error("Error in gRPC ValidateProduct for productId: {}", request.getProductId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getProductsByIds(GetProductsByIdsRequest request, StreamObserver<GetProductsByIdsResponse> responseObserver) {
        try {
            logger.debug("gRPC GetProductsByIds called for {} products, tenantId: {}", 
                        request.getProductIdsCount(), request.getContext().getTenantId());

            // Set tenant context from request
            GrpcContextUtils.setThreadContext(request.getContext());

            // Get products from service
            List<Product> products = productService.getProductsByIds(request.getProductIdsList());

            // Convert to protobuf
            GetProductsByIdsResponse.Builder responseBuilder = GetProductsByIdsResponse.newBuilder();
            for (Product product : products) {
                responseBuilder.addProducts(convertToProtoProduct(product));
            }

            GetProductsByIdsResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            logger.debug("gRPC GetProductsByIds completed, returned {} products", products.size());

        } catch (Exception e) {
            logger.error("Error in gRPC GetProductsByIds", e);
            responseObserver.onError(e);
        }
    }

    /**
     * Convert Product entity to protobuf Product
     */
    private com.ecommerce.productservice.proto.ProductServiceProtos.Product convertToProtoProduct(Product product) {
        com.ecommerce.productservice.proto.ProductServiceProtos.Product.Builder builder = 
            com.ecommerce.productservice.proto.ProductServiceProtos.Product.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .setDescription(product.getDescription() != null ? product.getDescription() : "")
                .setSku(product.getSku())
                .setPrice(CommonProtos.Money.newBuilder()
                    .setAmountCents(product.getPrice().getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency(product.getPrice().getCurrency())
                    .build())
                .setCategory(product.getCategory() != null ? product.getCategory() : "")
                .setBrand(product.getBrand() != null ? product.getBrand() : "")
                .setIsActive(product.getStatus() == Product.ProductStatus.ACTIVE);

        // Add image URLs if available
        if (product.getImages() != null) {
            for (Product.ProductImage image : product.getImages()) {
                builder.addImageUrls(image.getUrl());
            }
        }

        // Add attributes if available (convert Object values to String)
        if (product.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : product.getAttributes().entrySet()) {
                builder.putAttributes(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }

        return builder.build();
    }
}
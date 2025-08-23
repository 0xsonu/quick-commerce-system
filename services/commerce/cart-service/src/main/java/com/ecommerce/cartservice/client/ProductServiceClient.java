package com.ecommerce.cartservice.client;

import com.ecommerce.cartservice.dto.ProductValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Client for communicating with Product Catalog Service
 */
@Component
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductServiceClient(RestTemplate restTemplate,
                               @Value("${services.product.url:http://product-service:8080}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    /**
     * Validate product exists and get current price
     */
    public Optional<ProductValidationResponse> validateProduct(String tenantId, String productId) {
        try {
            logger.debug("Validating product {} for tenant {}", productId, tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = productServiceUrl + "/api/v1/products/" + productId;
            ResponseEntity<ProductValidationResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, ProductValidationResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.debug("Product {} validated successfully", productId);
                return Optional.of(response.getBody());
            }

            logger.warn("Product {} validation failed with status {}", productId, response.getStatusCode());
            return Optional.empty();

        } catch (RestClientException e) {
            logger.error("Failed to validate product {} for tenant {}: {}", productId, tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validate product with SKU and get current price
     */
    public Optional<ProductValidationResponse> validateProductWithSku(String tenantId, String productId, String sku) {
        try {
            logger.debug("Validating product {} with SKU {} for tenant {}", productId, sku, tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = productServiceUrl + "/api/v1/products/" + productId + "?sku=" + sku;
            ResponseEntity<ProductValidationResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, ProductValidationResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ProductValidationResponse product = response.getBody();
                // Validate SKU matches
                if (sku.equals(product.getSku())) {
                    logger.debug("Product {} with SKU {} validated successfully", productId, sku);
                    return Optional.of(product);
                } else {
                    logger.warn("SKU mismatch for product {}: expected {}, got {}", productId, sku, product.getSku());
                    return Optional.empty();
                }
            }

            logger.warn("Product {} with SKU {} validation failed with status {}", productId, sku, response.getStatusCode());
            return Optional.empty();

        } catch (RestClientException e) {
            logger.error("Failed to validate product {} with SKU {} for tenant {}: {}", productId, sku, tenantId, e.getMessage());
            return Optional.empty();
        }
    }
}
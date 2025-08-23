package com.ecommerce.cartservice.client;

import com.ecommerce.cartservice.dto.InventoryAvailabilityResponse;
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

import java.util.Optional;

/**
 * Client for communicating with Inventory Service
 */
@Component
public class InventoryServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceClient.class);

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryServiceClient(RestTemplate restTemplate,
                                 @Value("${services.inventory.url:http://inventory-service:8080}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    /**
     * Check inventory availability for a product
     */
    public Optional<InventoryAvailabilityResponse> checkAvailability(String tenantId, String productId, Integer requestedQuantity) {
        try {
            logger.debug("Checking inventory for product {} quantity {} for tenant {}", productId, requestedQuantity, tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = inventoryServiceUrl + "/api/v1/inventory/" + productId + "/availability?quantity=" + requestedQuantity;
            ResponseEntity<InventoryAvailabilityResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, InventoryAvailabilityResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.debug("Inventory check successful for product {}: available={}", 
                           productId, response.getBody().isAvailable());
                return Optional.of(response.getBody());
            }

            logger.warn("Inventory check failed for product {} with status {}", productId, response.getStatusCode());
            return Optional.empty();

        } catch (RestClientException e) {
            logger.error("Failed to check inventory for product {} for tenant {}: {}", productId, tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get current stock level for a product
     */
    public Optional<InventoryAvailabilityResponse> getStockLevel(String tenantId, String productId) {
        try {
            logger.debug("Getting stock level for product {} for tenant {}", productId, tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = inventoryServiceUrl + "/api/v1/inventory/" + productId;
            ResponseEntity<InventoryAvailabilityResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, InventoryAvailabilityResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.debug("Stock level retrieved for product {}: available={}", 
                           productId, response.getBody().getAvailableQuantity());
                return Optional.of(response.getBody());
            }

            logger.warn("Stock level retrieval failed for product {} with status {}", productId, response.getStatusCode());
            return Optional.empty();

        } catch (RestClientException e) {
            logger.error("Failed to get stock level for product {} for tenant {}: {}", productId, tenantId, e.getMessage());
            return Optional.empty();
        }
    }
}
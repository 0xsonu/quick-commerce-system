package com.ecommerce.shippingservice.carrier.impl;

import com.ecommerce.shippingservice.carrier.CarrierService;
import com.ecommerce.shippingservice.dto.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Resilient FedEx carrier service with circuit breaker, retry, and fallback patterns
 */
@Service
public class ResilientFedExCarrierService {

    private static final Logger logger = LoggerFactory.getLogger(ResilientFedExCarrierService.class);
    private static final String CARRIER_NAME = "FedEx";

    private final FedExCarrierService fedExCarrierService;

    @Autowired
    public ResilientFedExCarrierService(FedExCarrierService fedExCarrierService) {
        this.fedExCarrierService = fedExCarrierService;
    }

    @CircuitBreaker(name = "fedex-api", fallbackMethod = "getFallbackShippingRates")
    @Retry(name = "fedex-api")
    @Bulkhead(name = "fedex-api")
    @TimeLimiter(name = "fedex-api")
    public CompletionStage<List<ShippingRateResponse>> getShippingRatesAsync(String tenantId, ShippingRateRequest request) {
        logger.debug("Getting FedEx shipping rates with resilience patterns for tenant: {}", tenantId);
        
        return CompletableFuture.supplyAsync(() -> {
            // Simulate potential network delay or failure for testing
            if (Math.random() < 0.1) { // 10% chance of failure for testing
                throw new RuntimeException("Simulated FedEx API failure");
            }
            return fedExCarrierService.getShippingRates(tenantId, request);
        });
    }

    @CircuitBreaker(name = "fedex-api", fallbackMethod = "getFallbackCreateShipment")
    @Retry(name = "fedex-api")
    @Bulkhead(name = "fedex-api")
    @TimeLimiter(name = "fedex-api")
    public CompletionStage<CreateShipmentResponse> createShipmentAsync(String tenantId, CreateShipmentRequest request) {
        logger.debug("Creating FedEx shipment with resilience patterns for tenant: {}", tenantId);
        
        return CompletableFuture.supplyAsync(() -> {
            // Simulate potential network delay or failure for testing
            if (Math.random() < 0.05) { // 5% chance of failure for testing
                throw new RuntimeException("Simulated FedEx API failure");
            }
            return fedExCarrierService.createShipment(tenantId, request);
        });
    }

    @CircuitBreaker(name = "fedex-api", fallbackMethod = "getFallbackTrackingResponse")
    @Retry(name = "fedex-api")
    @Bulkhead(name = "fedex-api")
    @TimeLimiter(name = "fedex-api")
    public CompletionStage<TrackingResponse> trackShipmentAsync(String tenantId, TrackingRequest request) {
        logger.debug("Tracking FedEx shipment with resilience patterns for tenant: {}", tenantId);
        
        return CompletableFuture.supplyAsync(() -> {
            // Simulate potential network delay or failure for testing
            if (Math.random() < 0.08) { // 8% chance of failure for testing
                throw new RuntimeException("Simulated FedEx API failure");
            }
            return fedExCarrierService.trackShipment(tenantId, request);
        });
    }

    @CircuitBreaker(name = "fedex-api", fallbackMethod = "getFallbackCancelShipment")
    @Retry(name = "fedex-api")
    @Bulkhead(name = "fedex-api")
    @TimeLimiter(name = "fedex-api")
    public CompletionStage<Boolean> cancelShipmentAsync(String tenantId, String trackingNumber) {
        logger.debug("Cancelling FedEx shipment with resilience patterns for tenant: {}", tenantId);
        
        return CompletableFuture.supplyAsync(() -> {
            // Simulate potential network delay or failure for testing
            if (Math.random() < 0.05) { // 5% chance of failure for testing
                throw new RuntimeException("Simulated FedEx API failure");
            }
            return fedExCarrierService.cancelShipment(tenantId, trackingNumber);
        });
    }

    @CircuitBreaker(name = "fedex-api", fallbackMethod = "getFallbackAddressValidation")
    @Retry(name = "fedex-api")
    @Bulkhead(name = "fedex-api")
    @TimeLimiter(name = "fedex-api")
    public CompletionStage<Boolean> validateAddressAsync(String tenantId, String address) {
        logger.debug("Validating address with FedEx resilience patterns for tenant: {}", tenantId);
        
        return CompletableFuture.supplyAsync(() -> {
            // Simulate potential network delay or failure for testing
            if (Math.random() < 0.03) { // 3% chance of failure for testing
                throw new RuntimeException("Simulated FedEx API failure");
            }
            return fedExCarrierService.validateAddress(tenantId, address);
        });
    }

    // Synchronous wrapper methods
    public List<ShippingRateResponse> getShippingRates(String tenantId, ShippingRateRequest request) {
        try {
            return getShippingRatesAsync(tenantId, request).toCompletableFuture().join();
        } catch (Exception e) {
            logger.error("Failed to get FedEx shipping rates, using fallback", e);
            return getFallbackShippingRates(tenantId, request, e).toCompletableFuture().join();
        }
    }

    public CreateShipmentResponse createShipment(String tenantId, CreateShipmentRequest request) {
        try {
            return createShipmentAsync(tenantId, request).toCompletableFuture().join();
        } catch (Exception e) {
            logger.error("Failed to create FedEx shipment, using fallback", e);
            return getFallbackCreateShipment(tenantId, request, e).toCompletableFuture().join();
        }
    }

    public TrackingResponse trackShipment(String tenantId, TrackingRequest request) {
        try {
            return trackShipmentAsync(tenantId, request).toCompletableFuture().join();
        } catch (Exception e) {
            logger.error("Failed to track FedEx shipment, using fallback", e);
            return getFallbackTrackingResponse(tenantId, request, e).toCompletableFuture().join();
        }
    }

    public boolean cancelShipment(String tenantId, String trackingNumber) {
        try {
            return cancelShipmentAsync(tenantId, trackingNumber).toCompletableFuture().join();
        } catch (Exception e) {
            logger.error("Failed to cancel FedEx shipment, using fallback", e);
            return getFallbackCancelShipment(tenantId, trackingNumber, e).toCompletableFuture().join();
        }
    }

    public boolean validateAddress(String tenantId, String address) {
        try {
            return validateAddressAsync(tenantId, address).toCompletableFuture().join();
        } catch (Exception e) {
            logger.error("Failed to validate address with FedEx, using fallback", e);
            return getFallbackAddressValidation(tenantId, address, e).toCompletableFuture().join();
        }
    }

    // Fallback methods
    public CompletionStage<List<ShippingRateResponse>> getFallbackShippingRates(String tenantId, ShippingRateRequest request, Exception ex) {
        logger.warn("FedEx API unavailable, using fallback shipping rates for tenant: {}", tenantId);
        
        List<ShippingRateResponse> fallbackRates = List.of(
            new ShippingRateResponse(
                CARRIER_NAME,
                "FEDEX_GROUND_FALLBACK",
                "FedEx Ground (Estimated)",
                new BigDecimal("15.00"),
                "USD",
                7,
                LocalDate.now().plusDays(7)
            ),
            new ShippingRateResponse(
                CARRIER_NAME,
                "FEDEX_EXPRESS_FALLBACK",
                "FedEx Express (Estimated)",
                new BigDecimal("35.00"),
                "USD",
                3,
                LocalDate.now().plusDays(3)
            )
        );
        
        return CompletableFuture.completedFuture(fallbackRates);
    }

    public CompletionStage<CreateShipmentResponse> getFallbackCreateShipment(String tenantId, CreateShipmentRequest request, Exception ex) {
        logger.warn("FedEx API unavailable, shipment creation failed for tenant: {}", tenantId);
        
        CreateShipmentResponse fallbackResponse = new CreateShipmentResponse(
            false,
            null,
            null,
            null,
            null,
            null,
            "FedEx service temporarily unavailable. Please try again later or use alternative carrier."
        );
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }

    public CompletionStage<TrackingResponse> getFallbackTrackingResponse(String tenantId, TrackingRequest request, Exception ex) {
        logger.warn("FedEx API unavailable, tracking failed for tracking number: {}", request.getTrackingNumber());
        
        TrackingResponse fallbackResponse = new TrackingResponse(
            request.getTrackingNumber(),
            "UNKNOWN",
            "FedEx tracking service temporarily unavailable. Please try again later.",
            "Unknown",
            null,
            null,
            Collections.emptyList()
        );
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }

    public CompletionStage<Boolean> getFallbackCancelShipment(String tenantId, String trackingNumber, Exception ex) {
        logger.warn("FedEx API unavailable, cancellation failed for tracking number: {}", trackingNumber);
        
        // In a real scenario, this could queue the cancellation for later processing
        return CompletableFuture.completedFuture(false);
    }

    public CompletionStage<Boolean> getFallbackAddressValidation(String tenantId, String address, Exception ex) {
        logger.warn("FedEx API unavailable, using basic address validation for tenant: {}", tenantId);
        
        // Basic fallback validation
        boolean isValid = address != null && !address.trim().isEmpty() && address.length() > 10;
        return CompletableFuture.completedFuture(isValid);
    }

    // Delegate methods for CarrierService interface compatibility
    public String getCarrierName() {
        return CARRIER_NAME;
    }

    public boolean isAvailable(String tenantId) {
        return fedExCarrierService.isAvailable(tenantId);
    }

    public List<String> getSupportedServices(String tenantId) {
        return fedExCarrierService.getSupportedServices(tenantId);
    }
}
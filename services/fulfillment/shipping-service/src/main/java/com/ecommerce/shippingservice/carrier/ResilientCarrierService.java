package com.ecommerce.shippingservice.carrier;

import com.ecommerce.shippingservice.dto.*;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Resilient wrapper for carrier services that implements circuit breaker,
 * retry, bulkhead patterns with fallback mechanisms.
 */
@Component
public class ResilientCarrierService {

    private static final Logger logger = LoggerFactory.getLogger(ResilientCarrierService.class);

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;

    public ResilientCarrierService(CircuitBreaker carrierApiCircuitBreaker,
                                   Retry carrierApiRetry,
                                   Bulkhead carrierApiBulkhead) {
        this.circuitBreaker = carrierApiCircuitBreaker;
        this.retry = carrierApiRetry;
        this.bulkhead = carrierApiBulkhead;
    }

    /**
     * Get shipping rates with resilience patterns and fallback
     */
    public List<ShippingRateResponse> getShippingRatesWithResilience(
            CarrierService carrierService, String tenantId, ShippingRateRequest request) {
        
        Supplier<List<ShippingRateResponse>> supplier = 
            () -> carrierService.getShippingRates(tenantId, request);

        supplier = Bulkhead.decorateSupplier(bulkhead, supplier);
        supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            logger.error("Failed to get shipping rates from {}, using fallback", 
                carrierService.getCarrierName(), e);
            return getFallbackShippingRates(carrierService.getCarrierName(), request);
        }
    }

    /**
     * Create shipment with resilience patterns and fallback
     */
    public CreateShipmentResponse createShipmentWithResilience(
            CarrierService carrierService, String tenantId, CreateShipmentRequest request) {
        
        Supplier<CreateShipmentResponse> supplier = 
            () -> carrierService.createShipment(tenantId, request);

        supplier = Bulkhead.decorateSupplier(bulkhead, supplier);
        supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            logger.error("Failed to create shipment with {}, using fallback", 
                carrierService.getCarrierName(), e);
            return getFallbackCreateShipment(carrierService.getCarrierName(), e);
        }
    }

    /**
     * Track shipment with resilience patterns and fallback
     */
    public TrackingResponse trackShipmentWithResilience(
            CarrierService carrierService, String tenantId, TrackingRequest request) {
        
        Supplier<TrackingResponse> supplier = 
            () -> carrierService.trackShipment(tenantId, request);

        supplier = Bulkhead.decorateSupplier(bulkhead, supplier);
        supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            logger.error("Failed to track shipment with {}, using fallback", 
                carrierService.getCarrierName(), e);
            return getFallbackTrackingResponse(request.getTrackingNumber(), e);
        }
    }

    /**
     * Cancel shipment with resilience patterns and fallback
     */
    public boolean cancelShipmentWithResilience(
            CarrierService carrierService, String tenantId, String trackingNumber) {
        
        Supplier<Boolean> supplier = 
            () -> carrierService.cancelShipment(tenantId, trackingNumber);

        supplier = Bulkhead.decorateSupplier(bulkhead, supplier);
        supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            logger.error("Failed to cancel shipment with {}, using fallback", 
                carrierService.getCarrierName(), e);
            return getFallbackCancelShipment(trackingNumber, e);
        }
    }

    /**
     * Validate address with resilience patterns and fallback
     */
    public boolean validateAddressWithResilience(
            CarrierService carrierService, String tenantId, String address) {
        
        Supplier<Boolean> supplier = 
            () -> carrierService.validateAddress(tenantId, address);

        supplier = Bulkhead.decorateSupplier(bulkhead, supplier);
        supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            logger.error("Failed to validate address with {}, using fallback", 
                carrierService.getCarrierName(), e);
            return getFallbackAddressValidation(address, e);
        }
    }

    /**
     * Fallback method for shipping rates when carrier API is unavailable
     */
    private List<ShippingRateResponse> getFallbackShippingRates(String carrierName, ShippingRateRequest request) {
        logger.info("Using fallback shipping rates for carrier: {}", carrierName);
        
        // Return basic fallback rates
        return List.of(
            new ShippingRateResponse(
                carrierName,
                "STANDARD",
                "Standard Shipping (Estimated)",
                new BigDecimal("15.00"),
                "USD",
                7,
                LocalDate.now().plusDays(7)
            ),
            new ShippingRateResponse(
                carrierName,
                "EXPRESS",
                "Express Shipping (Estimated)",
                new BigDecimal("35.00"),
                "USD",
                3,
                LocalDate.now().plusDays(3)
            )
        );
    }

    /**
     * Fallback method for shipment creation when carrier API is unavailable
     */
    private CreateShipmentResponse getFallbackCreateShipment(String carrierName, Exception error) {
        logger.info("Using fallback shipment creation for carrier: {}", carrierName);
        
        return new CreateShipmentResponse(
            false,
            null,
            null,
            null,
            null,
            null,
            "Carrier service temporarily unavailable. Please try again later. Error: " + error.getMessage()
        );
    }

    /**
     * Fallback method for shipment tracking when carrier API is unavailable
     */
    private TrackingResponse getFallbackTrackingResponse(String trackingNumber, Exception error) {
        logger.info("Using fallback tracking response for tracking number: {}", trackingNumber);
        
        return new TrackingResponse(
            trackingNumber,
            "UNKNOWN",
            "Tracking information temporarily unavailable. Please try again later.",
            "Unknown",
            null,
            null,
            Collections.emptyList()
        );
    }

    /**
     * Fallback method for shipment cancellation when carrier API is unavailable
     */
    private boolean getFallbackCancelShipment(String trackingNumber, Exception error) {
        logger.warn("Fallback cancel shipment for tracking number: {} - {}", trackingNumber, error.getMessage());
        
        // In a real scenario, this might queue the cancellation request for later processing
        // or notify administrators about the failed cancellation
        return false;
    }

    /**
     * Fallback method for address validation when carrier API is unavailable
     */
    private boolean getFallbackAddressValidation(String address, Exception error) {
        logger.info("Using fallback address validation for address validation");
        
        // Basic fallback validation - just check if address is not empty
        return address != null && !address.trim().isEmpty() && address.length() > 10;
    }

    /**
     * Check if circuit breaker is open for monitoring purposes
     */
    public boolean isCircuitBreakerOpen() {
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    /**
     * Get circuit breaker metrics for monitoring
     */
    public CircuitBreaker.Metrics getCircuitBreakerMetrics() {
        return circuitBreaker.getMetrics();
    }

    /**
     * Get retry metrics for monitoring
     */
    public Retry.Metrics getRetryMetrics() {
        return retry.getMetrics();
    }

    /**
     * Get bulkhead metrics for monitoring
     */
    public Bulkhead.Metrics getBulkheadMetrics() {
        return bulkhead.getMetrics();
    }
}
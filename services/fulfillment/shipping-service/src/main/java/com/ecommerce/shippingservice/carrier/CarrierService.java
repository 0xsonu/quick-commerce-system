package com.ecommerce.shippingservice.carrier;

import com.ecommerce.shippingservice.dto.ShippingRateRequest;
import com.ecommerce.shippingservice.dto.ShippingRateResponse;
import com.ecommerce.shippingservice.dto.TrackingRequest;
import com.ecommerce.shippingservice.dto.TrackingResponse;
import com.ecommerce.shippingservice.dto.CreateShipmentRequest;
import com.ecommerce.shippingservice.dto.CreateShipmentResponse;

import java.util.List;

/**
 * Interface for carrier-specific shipping operations.
 * Implementations should handle the specific API integration for each carrier.
 */
public interface CarrierService {

    /**
     * Get the carrier name this service handles
     */
    String getCarrierName();

    /**
     * Check if this carrier service is available and configured
     */
    boolean isAvailable(String tenantId);

    /**
     * Get shipping rates for a shipment
     */
    List<ShippingRateResponse> getShippingRates(String tenantId, ShippingRateRequest request);

    /**
     * Create a shipment with the carrier
     */
    CreateShipmentResponse createShipment(String tenantId, CreateShipmentRequest request);

    /**
     * Track a shipment using carrier's tracking API
     */
    TrackingResponse trackShipment(String tenantId, TrackingRequest request);

    /**
     * Cancel a shipment with the carrier
     */
    boolean cancelShipment(String tenantId, String trackingNumber);

    /**
     * Get supported shipping services for this carrier
     */
    List<String> getSupportedServices(String tenantId);

    /**
     * Validate shipping address with carrier
     */
    boolean validateAddress(String tenantId, String address);
}
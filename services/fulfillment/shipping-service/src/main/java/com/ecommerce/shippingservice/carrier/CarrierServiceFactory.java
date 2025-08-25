package com.ecommerce.shippingservice.carrier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for getting the appropriate carrier service implementation
 */
@Component
public class CarrierServiceFactory {

    private final Map<String, CarrierService> carrierServices;

    @Autowired
    public CarrierServiceFactory(List<CarrierService> carrierServiceList) {
        this.carrierServices = carrierServiceList.stream()
            .collect(Collectors.toMap(
                service -> service.getCarrierName().toLowerCase(),
                Function.identity()
            ));
    }

    /**
     * Get carrier service by carrier name
     */
    public Optional<CarrierService> getCarrierService(String carrierName) {
        return Optional.ofNullable(carrierServices.get(carrierName.toLowerCase()));
    }

    /**
     * Get all available carrier services
     */
    public List<CarrierService> getAllCarrierServices() {
        return List.copyOf(carrierServices.values());
    }

    /**
     * Get available carrier services for a tenant
     */
    public List<CarrierService> getAvailableCarrierServices(String tenantId) {
        return carrierServices.values().stream()
            .filter(service -> service.isAvailable(tenantId))
            .collect(Collectors.toList());
    }

    /**
     * Check if a carrier is supported
     */
    public boolean isCarrierSupported(String carrierName) {
        return carrierServices.containsKey(carrierName.toLowerCase());
    }
}
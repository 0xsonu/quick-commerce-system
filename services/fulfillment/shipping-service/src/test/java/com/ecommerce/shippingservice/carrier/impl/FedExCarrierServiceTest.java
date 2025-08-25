package com.ecommerce.shippingservice.carrier.impl;

import com.ecommerce.shippingservice.dto.*;
import com.ecommerce.shippingservice.entity.CarrierConfig;
import com.ecommerce.shippingservice.repository.CarrierConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FedExCarrierServiceTest {

    @Mock
    private CarrierConfigRepository carrierConfigRepository;

    @InjectMocks
    private FedExCarrierService fedExCarrierService;

    private static final String TENANT_ID = "tenant123";
    private static final String CARRIER_NAME = "FedEx";

    private CarrierConfig carrierConfig;

    @BeforeEach
    void setUp() {
        carrierConfig = new CarrierConfig();
        carrierConfig.setTenantId(TENANT_ID);
        carrierConfig.setCarrierName(CARRIER_NAME);
        carrierConfig.setApiEndpoint("https://api.fedex.com");
        carrierConfig.setApiKeyEncrypted("encrypted_key");
        carrierConfig.setIsActive(true);
    }

    @Test
    void getCarrierName_ReturnsCorrectName() {
        // Act
        String carrierName = fedExCarrierService.getCarrierName();

        // Assert
        assertEquals(CARRIER_NAME, carrierName);
    }

    @Test
    void isAvailable_WhenConfigured_ReturnsTrue() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.of(carrierConfig));

        // Act
        boolean available = fedExCarrierService.isAvailable(TENANT_ID);

        // Assert
        assertTrue(available);
        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void isAvailable_WhenNotConfigured_ReturnsFalse() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.empty());

        // Act
        boolean available = fedExCarrierService.isAvailable(TENANT_ID);

        // Assert
        assertFalse(available);
        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void getShippingRates_WhenAvailable_ReturnsRates() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.of(carrierConfig));

        ShippingRateRequest request = new ShippingRateRequest();
        request.setOriginAddress("123 Origin St, City, State 12345");
        request.setDestinationAddress("456 Dest Ave, City, State 67890");
        request.setWeightKg(new BigDecimal("2.5"));

        // Act
        List<ShippingRateResponse> rates = fedExCarrierService.getShippingRates(TENANT_ID, request);

        // Assert
        assertNotNull(rates);
        assertFalse(rates.isEmpty());
        assertEquals(4, rates.size());

        ShippingRateResponse groundRate = rates.stream()
            .filter(rate -> "FEDEX_GROUND".equals(rate.getServiceCode()))
            .findFirst()
            .orElse(null);

        assertNotNull(groundRate);
        assertEquals(CARRIER_NAME, groundRate.getCarrierName());
        assertEquals("FEDEX_GROUND", groundRate.getServiceCode());
        assertEquals("FedEx Ground", groundRate.getServiceName());
        assertEquals(new BigDecimal("12.50"), groundRate.getRate());
        assertEquals("USD", groundRate.getCurrency());
        assertEquals(5, groundRate.getTransitDays());

        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void getShippingRates_WhenNotAvailable_ThrowsException() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.empty());

        ShippingRateRequest request = new ShippingRateRequest();

        // Act & Assert
        assertThrows(IllegalStateException.class, 
            () -> fedExCarrierService.getShippingRates(TENANT_ID, request));

        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void createShipment_WhenAvailable_ReturnsSuccess() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.of(carrierConfig));

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setOrderId(100L);
        request.setCarrierName(CARRIER_NAME);
        request.setServiceType("FEDEX_GROUND");
        request.setShippingAddress("{\"street\":\"123 Main St\"}");
        request.setWeightKg(new BigDecimal("2.5"));
        request.setItems(Arrays.asList(new ShipmentItemRequest()));

        // Act
        CreateShipmentResponse response = fedExCarrierService.createShipment(TENANT_ID, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getTrackingNumber());
        assertNotNull(response.getCarrierShipmentId());
        assertEquals(new BigDecimal("12.50"), response.getShippingCost());
        assertEquals("USD", response.getCurrency());
        assertNotNull(response.getEstimatedDeliveryDate());
        assertNull(response.getErrorMessage());

        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void trackShipment_WhenAvailable_ReturnsTracking() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.of(carrierConfig));

        TrackingRequest request = new TrackingRequest("1234567890", CARRIER_NAME);

        // Act
        TrackingResponse response = fedExCarrierService.trackShipment(TENANT_ID, request);

        // Assert
        assertNotNull(response);
        assertEquals("1234567890", response.getTrackingNumber());
        assertEquals("IN_TRANSIT", response.getStatus());
        assertEquals("Package is in transit", response.getStatusDescription());
        assertEquals("Memphis, TN", response.getCurrentLocation());
        assertNotNull(response.getLastUpdated());
        assertNotNull(response.getEstimatedDeliveryDate());
        assertNotNull(response.getEvents());
        assertEquals(2, response.getEvents().size());

        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void cancelShipment_WhenAvailable_ReturnsTrue() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.of(carrierConfig));

        String trackingNumber = "1234567890";

        // Act
        boolean result = fedExCarrierService.cancelShipment(TENANT_ID, trackingNumber);

        // Assert
        assertTrue(result);
        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void getSupportedServices_ReturnsExpectedServices() {
        // Act
        List<String> services = fedExCarrierService.getSupportedServices(TENANT_ID);

        // Assert
        assertNotNull(services);
        assertEquals(5, services.size());
        assertTrue(services.contains("FEDEX_GROUND"));
        assertTrue(services.contains("FEDEX_EXPRESS_SAVER"));
        assertTrue(services.contains("FEDEX_2_DAY"));
        assertTrue(services.contains("FEDEX_OVERNIGHT"));
        assertTrue(services.contains("FEDEX_PRIORITY_OVERNIGHT"));
    }

    @Test
    void validateAddress_WhenAvailable_ReturnsTrue() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.of(carrierConfig));

        String address = "123 Main St, Anytown, CA 12345";

        // Act
        boolean result = fedExCarrierService.validateAddress(TENANT_ID, address);

        // Assert
        assertTrue(result);
        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void validateAddress_WhenNotAvailable_ReturnsFalse() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.empty());

        String address = "123 Main St, Anytown, CA 12345";

        // Act
        boolean result = fedExCarrierService.validateAddress(TENANT_ID, address);

        // Assert
        assertFalse(result);
        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }

    @Test
    void validateAddress_WhenAddressEmpty_ReturnsFalse() {
        // Arrange
        when(carrierConfigRepository.findActiveCarrierConfig(TENANT_ID, CARRIER_NAME))
            .thenReturn(Optional.of(carrierConfig));

        // Act
        boolean result = fedExCarrierService.validateAddress(TENANT_ID, "");

        // Assert
        assertFalse(result);
        verify(carrierConfigRepository).findActiveCarrierConfig(TENANT_ID, CARRIER_NAME);
    }
}
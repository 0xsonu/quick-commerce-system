package com.ecommerce.shippingservice.resilience;

import com.ecommerce.shippingservice.carrier.CarrierService;
import com.ecommerce.shippingservice.carrier.ResilientCarrierService;
import com.ecommerce.shippingservice.dto.*;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilientCarrierServiceTest {

    @Mock
    private CarrierService mockCarrierService;

    @Mock
    private CircuitBreaker mockCircuitBreaker;

    @Mock
    private Retry mockRetry;

    @Mock
    private Bulkhead mockBulkhead;

    @Mock
    private TimeLimiter mockTimeLimiter;

    private ResilientCarrierService resilientCarrierService;

    @BeforeEach
    void setUp() {
        resilientCarrierService = new ResilientCarrierService(
            mockCircuitBreaker, mockRetry, mockBulkhead
        );
    }

    @Test
    void testGetShippingRatesWithResilience_Success() {
        // Given
        String tenantId = "tenant1";
        ShippingRateRequest request = new ShippingRateRequest();
        List<ShippingRateResponse> expectedRates = List.of(
            new ShippingRateResponse("TestCarrier", "GROUND", "Ground Service", 
                new BigDecimal("15.00"), "USD", 5, LocalDate.now().plusDays(5))
        );

        when(mockCarrierService.getShippingRates(tenantId, request)).thenReturn(expectedRates);
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        List<ShippingRateResponse> result = resilientCarrierService
            .getShippingRatesWithResilience(mockCarrierService, tenantId, request);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getCarrierName()).isEqualTo("TestCarrier");
        verify(mockCarrierService).getShippingRates(tenantId, request);
    }

    @Test
    void testGetShippingRatesWithResilience_FallbackOnFailure() {
        // Given
        String tenantId = "tenant1";
        ShippingRateRequest request = new ShippingRateRequest();

        when(mockCarrierService.getShippingRates(tenantId, request))
            .thenThrow(new RuntimeException("API failure"));
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        List<ShippingRateResponse> result = resilientCarrierService
            .getShippingRatesWithResilience(mockCarrierService, tenantId, request);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getCarrierName()).isEqualTo("TestCarrier");
        assertThat(result.get(0).getServiceName()).contains("Estimated");
    }

    @Test
    void testCreateShipmentWithResilience_Success() {
        // Given
        String tenantId = "tenant1";
        CreateShipmentRequest request = new CreateShipmentRequest();
        CreateShipmentResponse expectedResponse = new CreateShipmentResponse(
            true, "TRACK123", "SHIP456", new BigDecimal("15.00"), "USD", 
            LocalDate.now().plusDays(5), null
        );

        when(mockCarrierService.createShipment(tenantId, request)).thenReturn(expectedResponse);
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        CreateShipmentResponse result = resilientCarrierService
            .createShipmentWithResilience(mockCarrierService, tenantId, request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTrackingNumber()).isEqualTo("TRACK123");
        verify(mockCarrierService).createShipment(tenantId, request);
    }

    @Test
    void testCreateShipmentWithResilience_FallbackOnFailure() {
        // Given
        String tenantId = "tenant1";
        CreateShipmentRequest request = new CreateShipmentRequest();

        when(mockCarrierService.createShipment(tenantId, request))
            .thenThrow(new RuntimeException("API failure"));
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        CreateShipmentResponse result = resilientCarrierService
            .createShipmentWithResilience(mockCarrierService, tenantId, request);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("temporarily unavailable");
    }

    @Test
    void testTrackShipmentWithResilience_Success() {
        // Given
        String tenantId = "tenant1";
        TrackingRequest request = new TrackingRequest("TRACK123", "TestCarrier");
        TrackingResponse expectedResponse = new TrackingResponse(
            "TRACK123", "IN_TRANSIT", "Package in transit", "Memphis, TN", 
            null, null, List.of()
        );

        when(mockCarrierService.trackShipment(tenantId, request)).thenReturn(expectedResponse);
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        TrackingResponse result = resilientCarrierService
            .trackShipmentWithResilience(mockCarrierService, tenantId, request);

        // Then
        assertThat(result.getTrackingNumber()).isEqualTo("TRACK123");
        assertThat(result.getStatus()).isEqualTo("IN_TRANSIT");
        verify(mockCarrierService).trackShipment(tenantId, request);
    }

    @Test
    void testTrackShipmentWithResilience_FallbackOnFailure() {
        // Given
        String tenantId = "tenant1";
        TrackingRequest request = new TrackingRequest("TRACK123", "TestCarrier");

        when(mockCarrierService.trackShipment(tenantId, request))
            .thenThrow(new TimeoutException("API timeout"));
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        TrackingResponse result = resilientCarrierService
            .trackShipmentWithResilience(mockCarrierService, tenantId, request);

        // Then
        assertThat(result.getTrackingNumber()).isEqualTo("TRACK123");
        assertThat(result.getStatus()).isEqualTo("UNKNOWN");
        assertThat(result.getStatusDescription()).contains("temporarily unavailable");
    }

    @Test
    void testCancelShipmentWithResilience_Success() {
        // Given
        String tenantId = "tenant1";
        String trackingNumber = "TRACK123";

        when(mockCarrierService.cancelShipment(tenantId, trackingNumber)).thenReturn(true);
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        boolean result = resilientCarrierService
            .cancelShipmentWithResilience(mockCarrierService, tenantId, trackingNumber);

        // Then
        assertThat(result).isTrue();
        verify(mockCarrierService).cancelShipment(tenantId, trackingNumber);
    }

    @Test
    void testCancelShipmentWithResilience_FallbackOnFailure() {
        // Given
        String tenantId = "tenant1";
        String trackingNumber = "TRACK123";

        when(mockCarrierService.cancelShipment(tenantId, trackingNumber))
            .thenThrow(new RuntimeException("API failure"));
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        boolean result = resilientCarrierService
            .cancelShipmentWithResilience(mockCarrierService, tenantId, trackingNumber);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testValidateAddressWithResilience_Success() {
        // Given
        String tenantId = "tenant1";
        String address = "123 Main St, City, State 12345";

        when(mockCarrierService.validateAddress(tenantId, address)).thenReturn(true);
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        boolean result = resilientCarrierService
            .validateAddressWithResilience(mockCarrierService, tenantId, address);

        // Then
        assertThat(result).isTrue();
        verify(mockCarrierService).validateAddress(tenantId, address);
    }

    @Test
    void testValidateAddressWithResilience_FallbackOnFailure() {
        // Given
        String tenantId = "tenant1";
        String address = "123 Main St, City, State 12345";

        when(mockCarrierService.validateAddress(tenantId, address))
            .thenThrow(new RuntimeException("API failure"));
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        boolean result = resilientCarrierService
            .validateAddressWithResilience(mockCarrierService, tenantId, address);

        // Then
        assertThat(result).isTrue(); // Fallback validation passes for valid-looking address
    }

    @Test
    void testValidateAddressWithResilience_FallbackRejectsInvalidAddress() {
        // Given
        String tenantId = "tenant1";
        String address = ""; // Invalid address

        when(mockCarrierService.validateAddress(tenantId, address))
            .thenThrow(new RuntimeException("API failure"));
        when(mockCarrierService.getCarrierName()).thenReturn("TestCarrier");

        // When
        boolean result = resilientCarrierService
            .validateAddressWithResilience(mockCarrierService, tenantId, address);

        // Then
        assertThat(result).isFalse(); // Fallback validation rejects empty address
    }

    @Test
    void testCircuitBreakerMetrics() {
        // Given
        CircuitBreaker.Metrics mockMetrics = mock(CircuitBreaker.Metrics.class);
        when(mockCircuitBreaker.getMetrics()).thenReturn(mockMetrics);

        // When
        CircuitBreaker.Metrics result = resilientCarrierService.getCircuitBreakerMetrics();

        // Then
        assertThat(result).isEqualTo(mockMetrics);
    }

    @Test
    void testRetryMetrics() {
        // Given
        Retry.Metrics mockMetrics = mock(Retry.Metrics.class);
        when(mockRetry.getMetrics()).thenReturn(mockMetrics);

        // When
        Retry.Metrics result = resilientCarrierService.getRetryMetrics();

        // Then
        assertThat(result).isEqualTo(mockMetrics);
    }

    @Test
    void testBulkheadMetrics() {
        // Given
        Bulkhead.Metrics mockMetrics = mock(Bulkhead.Metrics.class);
        when(mockBulkhead.getMetrics()).thenReturn(mockMetrics);

        // When
        Bulkhead.Metrics result = resilientCarrierService.getBulkheadMetrics();

        // Then
        assertThat(result).isEqualTo(mockMetrics);
    }

    @Test
    void testIsCircuitBreakerOpen() {
        // Given
        when(mockCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);

        // When
        boolean result = resilientCarrierService.isCircuitBreakerOpen();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsCircuitBreakerClosed() {
        // Given
        when(mockCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        // When
        boolean result = resilientCarrierService.isCircuitBreakerOpen();

        // Then
        assertThat(result).isFalse();
    }
}
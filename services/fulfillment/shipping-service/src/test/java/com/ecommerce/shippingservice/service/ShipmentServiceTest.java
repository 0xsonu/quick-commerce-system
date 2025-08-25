package com.ecommerce.shippingservice.service;

import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shippingservice.carrier.CarrierService;
import com.ecommerce.shippingservice.carrier.CarrierServiceFactory;
import com.ecommerce.shippingservice.dto.*;
import com.ecommerce.shippingservice.entity.Shipment;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.exception.CarrierNotAvailableException;
import com.ecommerce.shippingservice.exception.ShipmentNotFoundException;
import com.ecommerce.shippingservice.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private CarrierServiceFactory carrierServiceFactory;

    @Mock
    private ShipmentTrackingService trackingService;

    @Mock
    private CarrierService carrierService;

    @InjectMocks
    private ShipmentService shipmentService;

    private static final String TENANT_ID = "tenant123";
    private static final String CARRIER_NAME = "FedEx";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @Test
    void createShipment_Success() {
        // Arrange
        CreateShipmentRequest request = createShipmentRequest();
        CreateShipmentResponse carrierResponse = CreateShipmentResponse.success(
            "1234567890", "FEDEX_123", new BigDecimal("12.50"), "USD", LocalDate.now().plusDays(3)
        );

        when(carrierServiceFactory.getCarrierService(CARRIER_NAME)).thenReturn(Optional.of(carrierService));
        when(carrierService.isAvailable(TENANT_ID)).thenReturn(true);
        when(carrierService.createShipment(eq(TENANT_ID), any(CreateShipmentRequest.class))).thenReturn(carrierResponse);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipment.setId(1L);
            return shipment;
        });

        // Act
        ShipmentResponse response = shipmentService.createShipment(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(request.getOrderId(), response.getOrderId());
        assertEquals(CARRIER_NAME, response.getCarrierName());
        assertEquals("1234567890", response.getTrackingNumber());
        assertEquals(ShipmentStatus.CREATED, response.getStatus());

        verify(carrierServiceFactory).getCarrierService(CARRIER_NAME);
        verify(carrierService).isAvailable(TENANT_ID);
        verify(carrierService).createShipment(eq(TENANT_ID), any(CreateShipmentRequest.class));
        verify(shipmentRepository, times(2)).save(any(Shipment.class));
    }

    @Test
    void createShipment_CarrierNotSupported() {
        // Arrange
        CreateShipmentRequest request = createShipmentRequest();
        when(carrierServiceFactory.getCarrierService(CARRIER_NAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CarrierNotAvailableException.class, () -> shipmentService.createShipment(request));
        
        verify(carrierServiceFactory).getCarrierService(CARRIER_NAME);
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void createShipment_CarrierNotAvailable() {
        // Arrange
        CreateShipmentRequest request = createShipmentRequest();
        when(carrierServiceFactory.getCarrierService(CARRIER_NAME)).thenReturn(Optional.of(carrierService));
        when(carrierService.isAvailable(TENANT_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(CarrierNotAvailableException.class, () -> shipmentService.createShipment(request));
        
        verify(carrierServiceFactory).getCarrierService(CARRIER_NAME);
        verify(carrierService).isAvailable(TENANT_ID);
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void getShipment_Success() {
        // Arrange
        Long shipmentId = 1L;
        Shipment shipment = createShipment();
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        // Act
        ShipmentResponse response = shipmentService.getShipment(shipmentId);

        // Assert
        assertNotNull(response);
        assertEquals(shipmentId, response.getId());
        assertEquals(shipment.getOrderId(), response.getOrderId());
        assertEquals(shipment.getCarrierName(), response.getCarrierName());

        verify(shipmentRepository).findById(shipmentId);
    }

    @Test
    void getShipment_NotFound() {
        // Arrange
        Long shipmentId = 1L;
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ShipmentNotFoundException.class, () -> shipmentService.getShipment(shipmentId));
        
        verify(shipmentRepository).findById(shipmentId);
    }

    @Test
    void getShipmentByTrackingNumber_Success() {
        // Arrange
        String trackingNumber = "1234567890";
        Shipment shipment = createShipment();
        shipment.setTrackingNumber(trackingNumber);
        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(shipment));

        // Act
        ShipmentResponse response = shipmentService.getShipmentByTrackingNumber(trackingNumber);

        // Assert
        assertNotNull(response);
        assertEquals(trackingNumber, response.getTrackingNumber());

        verify(shipmentRepository).findByTrackingNumber(trackingNumber);
    }

    @Test
    void getShipmentsByOrderId_Success() {
        // Arrange
        Long orderId = 100L;
        List<Shipment> shipments = Arrays.asList(createShipment(), createShipment());
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(shipments);

        // Act
        List<ShipmentResponse> responses = shipmentService.getShipmentsByOrderId(orderId);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());

        verify(shipmentRepository).findByOrderId(orderId);
    }

    @Test
    void updateShipmentStatus_Success() {
        // Arrange
        Long shipmentId = 1L;
        Shipment shipment = createShipment();
        ShipmentStatus newStatus = ShipmentStatus.IN_TRANSIT;
        String reason = "Package in transit";

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);

        // Act
        ShipmentResponse response = shipmentService.updateShipmentStatus(shipmentId, newStatus, reason);

        // Assert
        assertNotNull(response);
        assertEquals(newStatus, response.getStatus());

        verify(shipmentRepository).findById(shipmentId);
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void trackShipment_Success() {
        // Arrange
        String trackingNumber = "1234567890";
        String carrierName = CARRIER_NAME;
        TrackingResponse expectedResponse = new TrackingResponse();
        expectedResponse.setTrackingNumber(trackingNumber);
        expectedResponse.setStatus("IN_TRANSIT");

        when(carrierServiceFactory.getCarrierService(carrierName)).thenReturn(Optional.of(carrierService));
        when(carrierService.trackShipment(eq(TENANT_ID), any(TrackingRequest.class))).thenReturn(expectedResponse);

        // Act
        TrackingResponse response = shipmentService.trackShipment(trackingNumber, carrierName);

        // Assert
        assertNotNull(response);
        assertEquals(trackingNumber, response.getTrackingNumber());
        assertEquals("IN_TRANSIT", response.getStatus());

        verify(carrierServiceFactory).getCarrierService(carrierName);
        verify(carrierService).trackShipment(eq(TENANT_ID), any(TrackingRequest.class));
    }

    @Test
    void trackShipment_WithoutCarrierName_Success() {
        // Arrange
        String trackingNumber = "1234567890";
        Shipment shipment = createShipment();
        shipment.setTrackingNumber(trackingNumber);
        shipment.setCarrierName(CARRIER_NAME);
        
        TrackingResponse expectedResponse = new TrackingResponse();
        expectedResponse.setTrackingNumber(trackingNumber);

        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(shipment));
        when(carrierServiceFactory.getCarrierService(CARRIER_NAME)).thenReturn(Optional.of(carrierService));
        when(carrierService.trackShipment(eq(TENANT_ID), any(TrackingRequest.class))).thenReturn(expectedResponse);

        // Act
        TrackingResponse response = shipmentService.trackShipment(trackingNumber, null);

        // Assert
        assertNotNull(response);
        assertEquals(trackingNumber, response.getTrackingNumber());

        verify(shipmentRepository).findByTrackingNumber(trackingNumber);
        verify(carrierServiceFactory).getCarrierService(CARRIER_NAME);
        verify(carrierService).trackShipment(eq(TENANT_ID), any(TrackingRequest.class));
    }

    @Test
    void cancelShipment_Success() {
        // Arrange
        Long shipmentId = 1L;
        String reason = "Customer requested cancellation";
        Shipment shipment = createShipment();
        shipment.setTrackingNumber("1234567890");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(carrierServiceFactory.getCarrierService(CARRIER_NAME)).thenReturn(Optional.of(carrierService));
        when(carrierService.cancelShipment(TENANT_ID, shipment.getTrackingNumber())).thenReturn(true);
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);

        // Act
        boolean result = shipmentService.cancelShipment(shipmentId, reason);

        // Assert
        assertTrue(result);

        verify(shipmentRepository, times(2)).findById(shipmentId); // Called twice: once in cancelShipment, once in updateShipmentStatus
        verify(carrierServiceFactory).getCarrierService(CARRIER_NAME);
        verify(carrierService).cancelShipment(TENANT_ID, shipment.getTrackingNumber());
    }

    private CreateShipmentRequest createShipmentRequest() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setOrderId(100L);
        request.setCarrierName(CARRIER_NAME);
        request.setServiceType("FEDEX_GROUND");
        request.setShippingAddress("{\"street\":\"123 Main St\",\"city\":\"Anytown\",\"state\":\"CA\",\"zip\":\"12345\"}");
        request.setWeightKg(new BigDecimal("2.5"));
        
        ShipmentItemRequest item = new ShipmentItemRequest();
        item.setOrderItemId(1L);
        item.setProductId("PROD123");
        item.setSku("SKU123");
        item.setProductName("Test Product");
        item.setQuantity(1);
        request.setItems(Arrays.asList(item));
        
        return request;
    }

    private Shipment createShipment() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setTenantId(TENANT_ID);
        shipment.setOrderId(100L);
        shipment.setShipmentNumber("SH123456789");
        shipment.setCarrierName(CARRIER_NAME);
        shipment.setCarrierService("FEDEX_GROUND");
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setShippingAddress("{\"street\":\"123 Main St\",\"city\":\"Anytown\",\"state\":\"CA\",\"zip\":\"12345\"}");
        shipment.setWeightKg(new BigDecimal("2.5"));
        return shipment;
    }
}
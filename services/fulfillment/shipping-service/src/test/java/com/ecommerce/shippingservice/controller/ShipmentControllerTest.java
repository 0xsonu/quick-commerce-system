package com.ecommerce.shippingservice.controller;

import com.ecommerce.shippingservice.dto.*;
import com.ecommerce.shippingservice.entity.ShipmentStatus;
import com.ecommerce.shippingservice.service.ShipmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createShipment_Success() throws Exception {
        // Arrange
        CreateShipmentRequest request = createShipmentRequest();
        ShipmentResponse response = createShipmentResponse();

        when(shipmentService.createShipment(any(CreateShipmentRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.carrierName").value("FedEx"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));

        verify(shipmentService).createShipment(any(CreateShipmentRequest.class));
    }

    @Test
    void createShipment_ValidationError() throws Exception {
        // Arrange
        CreateShipmentRequest request = new CreateShipmentRequest(); // Invalid request

        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(shipmentService, never()).createShipment(any(CreateShipmentRequest.class));
    }

    @Test
    void getShipment_Success() throws Exception {
        // Arrange
        Long shipmentId = 1L;
        ShipmentResponse response = createShipmentResponse();

        when(shipmentService.getShipment(shipmentId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/shipments/{shipmentId}", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderId").value(100));

        verify(shipmentService).getShipment(shipmentId);
    }

    @Test
    void getShipment_NotFound() throws Exception {
        // Arrange
        Long shipmentId = 1L;
        when(shipmentService.getShipment(shipmentId)).thenThrow(new RuntimeException("Shipment not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/shipments/{shipmentId}", shipmentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(shipmentService).getShipment(shipmentId);
    }

    @Test
    void getShipmentByTrackingNumber_Success() throws Exception {
        // Arrange
        String trackingNumber = "1234567890";
        ShipmentResponse response = createShipmentResponse();
        response.setTrackingNumber(trackingNumber);

        when(shipmentService.getShipmentByTrackingNumber(trackingNumber)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/shipments/tracking/{trackingNumber}", trackingNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingNumber").value(trackingNumber));

        verify(shipmentService).getShipmentByTrackingNumber(trackingNumber);
    }

    @Test
    void getShipmentsByOrderId_Success() throws Exception {
        // Arrange
        Long orderId = 100L;
        List<ShipmentResponse> responses = Arrays.asList(createShipmentResponse());

        when(shipmentService.getShipmentsByOrderId(orderId)).thenReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/v1/shipments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].orderId").value(100));

        verify(shipmentService).getShipmentsByOrderId(orderId);
    }

    @Test
    void getShipments_Success() throws Exception {
        // Arrange
        List<ShipmentResponse> shipments = Arrays.asList(createShipmentResponse());
        Page<ShipmentResponse> page = new PageImpl<>(shipments, PageRequest.of(0, 10), 1);

        when(shipmentService.getShipments(any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/shipments")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(shipmentService).getShipments(any());
    }

    @Test
    void updateShipmentStatus_Success() throws Exception {
        // Arrange
        Long shipmentId = 1L;
        ShipmentStatus status = ShipmentStatus.IN_TRANSIT;
        String reason = "Package in transit";
        ShipmentResponse response = createShipmentResponse();
        response.setStatus(status);

        when(shipmentService.updateShipmentStatus(shipmentId, status, reason)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/shipments/{shipmentId}/status", shipmentId)
                .param("status", status.name())
                .param("reason", reason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));

        verify(shipmentService).updateShipmentStatus(shipmentId, status, reason);
    }

    @Test
    void trackShipment_Success() throws Exception {
        // Arrange
        String trackingNumber = "1234567890";
        String carrierName = "FedEx";
        TrackingResponse response = new TrackingResponse();
        response.setTrackingNumber(trackingNumber);
        response.setStatus("IN_TRANSIT");

        when(shipmentService.trackShipment(trackingNumber, carrierName)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/shipments/{trackingNumber}/track", trackingNumber)
                .param("carrierName", carrierName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingNumber").value(trackingNumber))
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));

        verify(shipmentService).trackShipment(trackingNumber, carrierName);
    }

    @Test
    void updateTrackingFromCarrier_Success() throws Exception {
        // Arrange
        Long shipmentId = 1L;
        doNothing().when(shipmentService).updateTrackingFromCarrier(shipmentId);

        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments/{shipmentId}/update-tracking", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(shipmentService).updateTrackingFromCarrier(shipmentId);
    }

    @Test
    void cancelShipment_Success() throws Exception {
        // Arrange
        Long shipmentId = 1L;
        String reason = "Customer requested cancellation";

        when(shipmentService.cancelShipment(shipmentId, reason)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments/{shipmentId}/cancel", shipmentId)
                .param("reason", reason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(shipmentService).cancelShipment(shipmentId, reason);
    }

    @Test
    void cancelShipment_Failed() throws Exception {
        // Arrange
        Long shipmentId = 1L;
        String reason = "Customer requested cancellation";

        when(shipmentService.cancelShipment(shipmentId, reason)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments/{shipmentId}/cancel", shipmentId)
                .param("reason", reason))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(shipmentService).cancelShipment(shipmentId, reason);
    }

    private CreateShipmentRequest createShipmentRequest() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setOrderId(100L);
        request.setCarrierName("FedEx");
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

    private ShipmentResponse createShipmentResponse() {
        ShipmentResponse response = new ShipmentResponse();
        response.setId(1L);
        response.setOrderId(100L);
        response.setShipmentNumber("SH123456789");
        response.setCarrierName("FedEx");
        response.setCarrierService("FEDEX_GROUND");
        response.setTrackingNumber("1234567890");
        response.setStatus(ShipmentStatus.CREATED);
        response.setShippingAddress("{\"street\":\"123 Main St\",\"city\":\"Anytown\",\"state\":\"CA\",\"zip\":\"12345\"}");
        response.setWeightKg(new BigDecimal("2.5"));
        response.setShippingCost(new BigDecimal("12.50"));
        response.setCurrency("USD");
        response.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }
}
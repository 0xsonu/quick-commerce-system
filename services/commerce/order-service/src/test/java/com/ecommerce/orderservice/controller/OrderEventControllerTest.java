package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.service.OrderEventReplayService;
import com.ecommerce.shared.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderEventController.class)
class OrderEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderEventReplayService replayService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("test-tenant");
    }

    @Test
    void replayOrderEvents_ShouldReturnSuccess() throws Exception {
        // Arrange
        Long orderId = 1L;
        when(replayService.replayOrderEvents(orderId)).thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/events/{orderId}/replay", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event replay started successfully"))
                .andExpect(jsonPath("$.data").value("Event replay initiated for order: " + orderId));

        verify(replayService).replayOrderEvents(orderId);
    }

    @Test
    void replayOrderEvents_WithException_ShouldReturnError() throws Exception {
        // Arrange
        Long orderId = 1L;
        when(replayService.replayOrderEvents(orderId)).thenThrow(new RuntimeException("Test error"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/events/{orderId}/replay", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to initiate event replay: Test error"));

        verify(replayService).replayOrderEvents(orderId);
    }

    @Test
    void replayOrderEventsByDateRange_ShouldReturnSuccess() throws Exception {
        // Arrange
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59);
        when(replayService.replayOrderEventsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/events/replay/date-range")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-01-31T23:59:00")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Batch event replay started successfully"));

        verify(replayService).replayOrderEventsByDateRange(startDate, endDate);
    }

    @Test
    void replayOrderEventsByStatus_ShouldReturnSuccess() throws Exception {
        // Arrange
        OrderStatus status = OrderStatus.CONFIRMED;
        when(replayService.replayOrderEventsByStatus(status)).thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/events/replay/status/{status}", status.name())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Status-based event replay started successfully"))
                .andExpect(jsonPath("$.data").value("Event replay initiated for orders with status: " + status));

        verify(replayService).replayOrderEventsByStatus(status);
    }

    @Test
    void recoverMissingEvents_ShouldReturnSuccess() throws Exception {
        // Arrange
        Long orderId = 1L;
        when(replayService.recoverMissingEvents(orderId)).thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/events/{orderId}/recover", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event recovery started successfully"))
                .andExpect(jsonPath("$.data").value("Event recovery initiated for order: " + orderId));

        verify(replayService).recoverMissingEvents(orderId);
    }

    @Test
    void validateEventConsistency_WithValidOrder_ShouldReturnTrue() throws Exception {
        // Arrange
        Long orderId = 1L;
        when(replayService.validateEventConsistency(orderId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/events/{orderId}/validate", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event consistency validation passed"))
                .andExpect(jsonPath("$.data").value(true));

        verify(replayService).validateEventConsistency(orderId);
    }

    @Test
    void validateEventConsistency_WithInvalidOrder_ShouldReturnFalse() throws Exception {
        // Arrange
        Long orderId = 1L;
        when(replayService.validateEventConsistency(orderId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/events/{orderId}/validate", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event consistency validation failed"))
                .andExpect(jsonPath("$.data").value(false));

        verify(replayService).validateEventConsistency(orderId);
    }

    @Test
    void validateEventConsistency_WithException_ShouldReturnError() throws Exception {
        // Arrange
        Long orderId = 1L;
        when(replayService.validateEventConsistency(orderId)).thenThrow(new RuntimeException("Validation error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/events/{orderId}/validate", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to validate event consistency: Validation error"));

        verify(replayService).validateEventConsistency(orderId);
    }

    @Test
    void replayOrderEvents_WithInvalidOrderId_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/events/{orderId}/replay", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(replayService, never()).replayOrderEvents(any());
    }
}
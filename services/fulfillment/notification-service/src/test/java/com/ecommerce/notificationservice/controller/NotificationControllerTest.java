package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.notificationservice.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendNotification_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setContent("Test Content");

        NotificationResponse response = new NotificationResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setNotificationType(NotificationType.ORDER_CREATED);
        response.setChannel(NotificationChannel.EMAIL);
        response.setRecipient("test@example.com");
        response.setSubject("Test Subject");
        response.setStatus(NotificationStatus.SENT);
        response.setCreatedAt(LocalDateTime.now());

        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification processed"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.status").value("SENT"));

        verify(notificationService).sendNotification(any(NotificationRequest.class));
    }

    @Test
    void sendNotification_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).sendNotification(any(NotificationRequest.class));
    }

    @Test
    void sendNotificationAsync_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setRecipient("test@example.com");
        request.setSubject("Test Subject");
        request.setContent("Test Content");

        doNothing().when(notificationService).sendNotificationAsync(any(NotificationRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/notifications/send-async")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification queued successfully"))
                .andExpect(jsonPath("$.data").value("Notification queued for processing"));

        verify(notificationService).sendNotificationAsync(any(NotificationRequest.class));
    }

    @Test
    void health_ShouldReturnHealthy() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification service is healthy"))
                .andExpect(jsonPath("$.data").value("OK"));
    }
}
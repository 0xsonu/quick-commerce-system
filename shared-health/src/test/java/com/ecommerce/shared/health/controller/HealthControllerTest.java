package com.ecommerce.shared.health.controller;

import com.ecommerce.shared.health.indicator.CompositeHealthIndicator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompositeHealthIndicator compositeHealthIndicator;

    @MockBean
    private Map<String, HealthIndicator> healthIndicators;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnDetailedHealth() throws Exception {
        // Given
        Health health = Health.up()
                .withDetail("database", Map.of("status", "UP"))
                .withDetail("redis", Map.of("status", "UP"))
                .build();
        when(compositeHealthIndicator.health()).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/actuator/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.database").exists())
                .andExpect(jsonPath("$.details.redis").exists());
    }

    @Test
    void shouldReturnComponentHealth() throws Exception {
        // Given
        Health componentHealth = Health.up()
                .withDetail("connectionTime", "50ms")
                .build();
        when(compositeHealthIndicator.getComponentHealth("database")).thenReturn(componentHealth);

        // When & Then
        mockMvc.perform(get("/actuator/health/component/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.connectionTime").value("50ms"));
    }

    @Test
    void shouldReturn404ForNonExistentComponent() throws Exception {
        // Given
        Health unknownHealth = Health.unknown()
                .withDetail("error", "Component not found: nonexistent")
                .build();
        when(compositeHealthIndicator.getComponentHealth("nonexistent")).thenReturn(unknownHealth);

        // When & Then
        mockMvc.perform(get("/actuator/health/component/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAvailableComponents() throws Exception {
        // Given
        Set<String> components = Set.of("database", "redis", "jvm");
        when(compositeHealthIndicator.getComponentNames()).thenReturn(components);

        // When & Then
        mockMvc.perform(get("/actuator/health/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*]").value(org.hamcrest.Matchers.containsInAnyOrder("database", "redis", "jvm")));
    }

    @Test
    void shouldReturnLivenessProbe() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnReadinessProbeWhenUp() throws Exception {
        // Given
        Health health = Health.up().build();
        when(compositeHealthIndicator.health()).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnReadinessProbeWhenDegraded() throws Exception {
        // Given
        Health health = Health.status("DEGRADED").build();
        when(compositeHealthIndicator.health()).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnReadinessProbeWhenDown() throws Exception {
        // Given
        Health health = Health.down().build();
        when(compositeHealthIndicator.health()).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnStartupProbeWhenUp() throws Exception {
        // Given
        Health health = Health.up().build();
        when(compositeHealthIndicator.health()).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/actuator/health/startup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.started").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnStartupProbeWhenDegraded() throws Exception {
        // Given
        Health health = Health.status("DEGRADED").build();
        when(compositeHealthIndicator.health()).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/actuator/health/startup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.started").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnStartupProbeWhenDown() throws Exception {
        // Given
        Health health = Health.down().build();
        when(compositeHealthIndicator.health()).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/actuator/health/startup"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.started").value(false))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
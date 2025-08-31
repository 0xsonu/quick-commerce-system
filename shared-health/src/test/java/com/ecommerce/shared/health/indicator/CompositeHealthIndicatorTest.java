package com.ecommerce.shared.health.indicator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeHealthIndicatorTest {

    @Mock
    private HealthIndicator databaseHealthIndicator;

    @Mock
    private HealthIndicator redisHealthIndicator;

    @Mock
    private HealthIndicator jvmHealthIndicator;

    private CompositeHealthIndicator compositeHealthIndicator;
    private Map<String, HealthIndicator> healthIndicators;

    @BeforeEach
    void setUp() {
        healthIndicators = new HashMap<>();
        healthIndicators.put("database", databaseHealthIndicator);
        healthIndicators.put("redis", redisHealthIndicator);
        healthIndicators.put("jvm", jvmHealthIndicator);
        
        compositeHealthIndicator = new CompositeHealthIndicator(healthIndicators);
    }

    @Test
    void shouldReturnUpWhenAllComponentsAreUp() {
        // Given
        when(databaseHealthIndicator.health()).thenReturn(Health.up().build());
        when(redisHealthIndicator.health()).thenReturn(Health.up().build());
        when(jvmHealthIndicator.health()).thenReturn(Health.up().build());

        // When
        Health health = compositeHealthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("summary");
        assertThat(health.getDetails()).containsKey("database");
        assertThat(health.getDetails()).containsKey("redis");
        assertThat(health.getDetails()).containsKey("jvm");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) health.getDetails().get("summary");
        assertThat(summary.get("total")).isEqualTo(3);
        assertThat(summary.get("up")).isEqualTo(3);
        assertThat(summary.get("degraded")).isEqualTo(0);
        assertThat(summary.get("down")).isEqualTo(0);
    }

    @Test
    void shouldReturnDegradedWhenSomeComponentsAreDegraded() {
        // Given
        when(databaseHealthIndicator.health()).thenReturn(Health.up().build());
        when(redisHealthIndicator.health()).thenReturn(Health.status("DEGRADED").build());
        when(jvmHealthIndicator.health()).thenReturn(Health.up().build());

        // When
        Health health = compositeHealthIndicator.health();

        // Then
        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) health.getDetails().get("summary");
        assertThat(summary.get("total")).isEqualTo(3);
        assertThat(summary.get("up")).isEqualTo(2);
        assertThat(summary.get("degraded")).isEqualTo(1);
        assertThat(summary.get("down")).isEqualTo(0);
    }

    @Test
    void shouldReturnDownWhenAnyComponentIsDown() {
        // Given
        when(databaseHealthIndicator.health()).thenReturn(Health.down().build());
        when(redisHealthIndicator.health()).thenReturn(Health.up().build());
        when(jvmHealthIndicator.health()).thenReturn(Health.status("DEGRADED").build());

        // When
        Health health = compositeHealthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) health.getDetails().get("summary");
        assertThat(summary.get("total")).isEqualTo(3);
        assertThat(summary.get("up")).isEqualTo(1);
        assertThat(summary.get("degraded")).isEqualTo(1);
        assertThat(summary.get("down")).isEqualTo(1);
    }

    @Test
    void shouldHandleHealthCheckExceptions() {
        // Given
        when(databaseHealthIndicator.health()).thenThrow(new RuntimeException("Database error"));
        when(redisHealthIndicator.health()).thenReturn(Health.up().build());
        when(jvmHealthIndicator.health()).thenReturn(Health.up().build());

        // When
        Health health = compositeHealthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> databaseHealth = (Map<String, Object>) health.getDetails().get("database");
        assertThat(databaseHealth.get("status")).isEqualTo("DOWN");
    }

    @Test
    void shouldGetComponentHealthIndividually() {
        // Given
        Health expectedHealth = Health.up().withDetail("test", "value").build();
        when(databaseHealthIndicator.health()).thenReturn(expectedHealth);

        // When
        Health health = compositeHealthIndicator.getComponentHealth("database");

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("test");
        assertThat(health.getDetails().get("test")).isEqualTo("value");
    }

    @Test
    void shouldReturnUnknownForNonExistentComponent() {
        // When
        Health health = compositeHealthIndicator.getComponentHealth("nonexistent");

        // Then
        assertThat(health.getStatus().getCode()).isEqualTo("UNKNOWN");
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    void shouldReturnAllComponentNames() {
        // When
        var componentNames = compositeHealthIndicator.getComponentNames();

        // Then
        assertThat(componentNames).containsExactlyInAnyOrder("database", "redis", "jvm");
    }

    @Test
    void shouldIncludeStatusExplanation() {
        // Given
        when(databaseHealthIndicator.health()).thenReturn(Health.up().build());
        when(redisHealthIndicator.health()).thenReturn(Health.status("DEGRADED").build());
        when(jvmHealthIndicator.health()).thenReturn(Health.down().build());

        // When
        Health health = compositeHealthIndicator.health();

        // Then
        assertThat(health.getDetails()).containsKey("status_explanation");
        String explanation = (String) health.getDetails().get("status_explanation");
        assertThat(explanation).contains("DOWN");
        assertThat(explanation).contains("1 component(s) are down");
    }

    @Test
    void shouldHandleTimeouts() {
        // Given - simulate a slow health check
        when(databaseHealthIndicator.health()).thenAnswer(invocation -> {
            Thread.sleep(15000); // Longer than timeout
            return Health.up().build();
        });
        when(redisHealthIndicator.health()).thenReturn(Health.up().build());
        when(jvmHealthIndicator.health()).thenReturn(Health.up().build());

        // When
        Health health = compositeHealthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> databaseHealth = (Map<String, Object>) health.getDetails().get("database");
        assertThat(databaseHealth.get("status")).isEqualTo("DOWN");
    }
}
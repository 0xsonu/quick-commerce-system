package com.ecommerce.shared.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test to verify dependencies are working
 */
class SimpleCompilationTest {

    @Test
    void shouldCompileWithActuatorDependencies() {
        // Test that we can use Spring Boot Actuator classes
        Health health = Health.up().build();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        
        HealthIndicator indicator = () -> Health.up().build();
        Health result = indicator.health();
        assertThat(result.getStatus()).isEqualTo(Status.UP);
    }
}
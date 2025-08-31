package com.ecommerce.shared.health.indicator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisHealthIndicatorTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisConnection connection;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new RedisHealthIndicator(connectionFactory, redisTemplate);
    }

    @Test
    void shouldReturnUpWhenRedisIsHealthy() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("health-check-value");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        
        Properties info = new Properties();
        info.setProperty("redis_version", "7.2.0");
        info.setProperty("connected_clients", "5");
        info.setProperty("keyspace_hits", "100");
        info.setProperty("keyspace_misses", "10");
        when(connection.info()).thenReturn(info);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("connectionTime");
        assertThat(health.getDetails()).containsKey("operationTime");
        assertThat(health.getDetails()).containsKey("redis.version");
        assertThat(health.getDetails()).containsKey("hit_ratio_percent");
    }

    @Test
    void shouldReturnDownWhenCannotConnect() {
        // Given
        when(connectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).isEqualTo("Unable to connect to Redis");
    }

    @Test
    void shouldReturnDownWhenPingFails() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(null);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    void shouldReturnDegradedWhenOperationsAreSlow() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Simulate slow operations
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            Thread.sleep(600); // > 500ms threshold
            return "health-check-value";
        });

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails()).containsKey("warning");
    }

    @Test
    void shouldReturnDegradedWhenOperationsFail() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Operation failed"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails()).containsKey("warning");
    }

    @Test
    void shouldCalculateHitRatioCorrectly() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("health-check-value");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        
        Properties info = new Properties();
        info.setProperty("keyspace_hits", "90");
        info.setProperty("keyspace_misses", "10");
        when(connection.info()).thenReturn(info);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("hit_ratio_percent");
        assertThat(health.getDetails().get("hit_ratio_percent")).isEqualTo("90.00");
    }
}
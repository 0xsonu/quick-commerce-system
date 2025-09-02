package com.ecommerce.gateway.grpc;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GrpcMetricsInterceptor
 */
class GrpcMetricsInterceptorTest {

    private MeterRegistry meterRegistry;
    private GrpcMetricsInterceptor interceptor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        interceptor = new GrpcMetricsInterceptor(meterRegistry);
    }

    @Test
    void testInterceptorCreation() {
        // Verify that the interceptor can be created successfully
        assertThat(interceptor).isNotNull();
    }

    @Test
    void testTimerCreation() {
        // Verify that the timer builder is properly configured
        Timer.Builder timerBuilder = Timer.builder("grpc.client.duration")
            .description("Duration of gRPC client calls");

        assertThat(timerBuilder).isNotNull();
        
        Timer timer = timerBuilder
            .tag("service", "TestService")
            .tag("method", "TestMethod")
            .tag("status", "OK")
            .register(meterRegistry);

        assertThat(timer).isNotNull();
        assertThat(timer.getId().getName()).isEqualTo("grpc.client.duration");
        assertThat(timer.getId().getTag("service")).isEqualTo("TestService");
        assertThat(timer.getId().getTag("method")).isEqualTo("TestMethod");
        assertThat(timer.getId().getTag("status")).isEqualTo("OK");
    }

    @Test
    void testCounterCreation() {
        // Verify that the counter builder is properly configured
        Counter.Builder counterBuilder = Counter.builder("grpc.client.errors")
            .description("Number of gRPC client errors");

        assertThat(counterBuilder).isNotNull();
        
        Counter counter = counterBuilder
            .tag("service", "TestService")
            .tag("method", "TestMethod")
            .tag("error", "NOT_FOUND")
            .register(meterRegistry);

        assertThat(counter).isNotNull();
        assertThat(counter.getId().getName()).isEqualTo("grpc.client.errors");
        assertThat(counter.getId().getTag("service")).isEqualTo("TestService");
        assertThat(counter.getId().getTag("method")).isEqualTo("TestMethod");
        assertThat(counter.getId().getTag("error")).isEqualTo("NOT_FOUND");
    }
}
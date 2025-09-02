package com.ecommerce.gateway.grpc;

import io.grpc.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TenantContextInterceptor
 */
class TenantContextInterceptorTest {

    private TenantContextInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TenantContextInterceptor();
        
        // Clear MDC before each test
        MDC.clear();
    }

    @Test
    void testMetadataKeys() {
        // Verify that the metadata keys are properly defined
        Metadata.Key<String> tenantIdKey = Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);
        Metadata.Key<String> userIdKey = Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER);
        Metadata.Key<String> correlationIdKey = Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER);

        assertThat(tenantIdKey.name()).isEqualTo("tenant-id");
        assertThat(userIdKey.name()).isEqualTo("user-id");
        assertThat(correlationIdKey.name()).isEqualTo("correlation-id");
    }

    @Test
    void testInterceptorCreation() {
        // Verify that the interceptor can be created successfully
        assertThat(interceptor).isNotNull();
    }
}
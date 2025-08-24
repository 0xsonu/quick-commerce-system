package com.ecommerce.shared.grpc;

import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Metadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrpcContextUtilsTest {

    @BeforeEach
    void setUp() {
        // Clear any existing context
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear context after each test
        TenantContext.clear();
    }

    @Test
    void testCreateTenantContext() {
        // Set up thread context
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("user456");
        TenantContext.setCorrelationId("corr789");

        // Create tenant context
        CommonProtos.TenantContext context = GrpcContextUtils.createTenantContext();

        // Verify
        assertEquals("tenant123", context.getTenantId());
        assertEquals("user456", context.getUserId());
        assertEquals("corr789", context.getCorrelationId());
    }

    @Test
    void testCreateTenantContextWithEmptyValues() {
        // Don't set any context values

        // Create tenant context
        CommonProtos.TenantContext context = GrpcContextUtils.createTenantContext();

        // Verify empty values are handled
        assertEquals("", context.getTenantId());
        assertEquals("", context.getUserId());
        assertEquals("", context.getCorrelationId());
    }

    @Test
    void testCreateMetadata() {
        // Create tenant context
        CommonProtos.TenantContext context = CommonProtos.TenantContext.newBuilder()
            .setTenantId("tenant123")
            .setUserId("user456")
            .setCorrelationId("corr789")
            .build();

        // Create metadata
        Metadata metadata = GrpcContextUtils.createMetadata(context);

        // Verify metadata
        assertEquals("tenant123", metadata.get(GrpcContextUtils.TENANT_ID_KEY));
        assertEquals("user456", metadata.get(GrpcContextUtils.USER_ID_KEY));
        assertEquals("corr789", metadata.get(GrpcContextUtils.CORRELATION_ID_KEY));
    }

    @Test
    void testExtractTenantContext() {
        // Create metadata
        Metadata metadata = new Metadata();
        metadata.put(GrpcContextUtils.TENANT_ID_KEY, "tenant123");
        metadata.put(GrpcContextUtils.USER_ID_KEY, "user456");
        metadata.put(GrpcContextUtils.CORRELATION_ID_KEY, "corr789");

        // Extract tenant context
        CommonProtos.TenantContext context = GrpcContextUtils.extractTenantContext(metadata);

        // Verify
        assertEquals("tenant123", context.getTenantId());
        assertEquals("user456", context.getUserId());
        assertEquals("corr789", context.getCorrelationId());
    }

    @Test
    void testSetThreadContext() {
        // Create tenant context
        CommonProtos.TenantContext context = CommonProtos.TenantContext.newBuilder()
            .setTenantId("tenant123")
            .setUserId("user456")
            .setCorrelationId("corr789")
            .build();

        // Set thread context
        GrpcContextUtils.setThreadContext(context);

        // Verify thread context
        assertEquals("tenant123", TenantContext.getTenantId());
        assertEquals("user456", TenantContext.getUserId());
        assertEquals("corr789", TenantContext.getCorrelationId());
    }

    @Test
    void testRoundTripContextHandling() {
        // Set up initial context
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("user456");
        TenantContext.setCorrelationId("corr789");

        // Create tenant context from thread
        CommonProtos.TenantContext context = GrpcContextUtils.createTenantContext();

        // Create metadata from context
        Metadata metadata = GrpcContextUtils.createMetadata(context);

        // Extract context from metadata
        CommonProtos.TenantContext extractedContext = GrpcContextUtils.extractTenantContext(metadata);

        // Clear thread context
        TenantContext.clear();

        // Set thread context from extracted context
        GrpcContextUtils.setThreadContext(extractedContext);

        // Verify round trip
        assertEquals("tenant123", TenantContext.getTenantId());
        assertEquals("user456", TenantContext.getUserId());
        assertEquals("corr789", TenantContext.getCorrelationId());
    }
}
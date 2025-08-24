package com.ecommerce.shared.grpc;

import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.AbstractStub;

/**
 * Utility class for handling gRPC context and metadata
 */
public class GrpcContextUtils {

    // Metadata keys for tenant context
    public static final Metadata.Key<String> TENANT_ID_KEY = 
        Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    
    public static final Metadata.Key<String> USER_ID_KEY = 
        Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER);
    
    public static final Metadata.Key<String> CORRELATION_ID_KEY = 
        Metadata.Key.of("correlation-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * Create TenantContext from current thread context
     */
    public static CommonProtos.TenantContext createTenantContext() {
        return CommonProtos.TenantContext.newBuilder()
            .setTenantId(TenantContext.getTenantId() != null ? TenantContext.getTenantId() : "")
            .setUserId(TenantContext.getUserId() != null ? TenantContext.getUserId() : "")
            .setCorrelationId(TenantContext.getCorrelationId() != null ? TenantContext.getCorrelationId() : "")
            .build();
    }

    /**
     * Create metadata from TenantContext
     */
    public static Metadata createMetadata(CommonProtos.TenantContext context) {
        Metadata metadata = new Metadata();
        if (context.getTenantId() != null && !context.getTenantId().isEmpty()) {
            metadata.put(TENANT_ID_KEY, context.getTenantId());
        }
        if (context.getUserId() != null && !context.getUserId().isEmpty()) {
            metadata.put(USER_ID_KEY, context.getUserId());
        }
        if (context.getCorrelationId() != null && !context.getCorrelationId().isEmpty()) {
            metadata.put(CORRELATION_ID_KEY, context.getCorrelationId());
        }
        return metadata;
    }

    /**
     * Create metadata from current thread context
     */
    public static Metadata createMetadataFromContext() {
        return createMetadata(createTenantContext());
    }

    /**
     * Attach metadata to gRPC stub
     */
    public static <T extends AbstractStub<T>> T withMetadata(T stub, Metadata metadata) {
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    /**
     * Attach current context metadata to gRPC stub
     */
    public static <T extends AbstractStub<T>> T withCurrentContext(T stub) {
        return withMetadata(stub, createMetadataFromContext());
    }

    /**
     * Extract TenantContext from metadata
     */
    public static CommonProtos.TenantContext extractTenantContext(Metadata metadata) {
        String tenantId = metadata.get(TENANT_ID_KEY);
        String userId = metadata.get(USER_ID_KEY);
        String correlationId = metadata.get(CORRELATION_ID_KEY);

        return CommonProtos.TenantContext.newBuilder()
            .setTenantId(tenantId != null ? tenantId : "")
            .setUserId(userId != null ? userId : "")
            .setCorrelationId(correlationId != null ? correlationId : "")
            .build();
    }

    /**
     * Set thread context from TenantContext
     */
    public static void setThreadContext(CommonProtos.TenantContext context) {
        if (context.getTenantId() != null && !context.getTenantId().isEmpty()) {
            TenantContext.setTenantId(context.getTenantId());
        }
        if (context.getUserId() != null && !context.getUserId().isEmpty()) {
            TenantContext.setUserId(context.getUserId());
        }
        if (context.getCorrelationId() != null && !context.getCorrelationId().isEmpty()) {
            TenantContext.setCorrelationId(context.getCorrelationId());
        }
    }
}
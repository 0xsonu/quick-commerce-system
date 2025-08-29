package com.ecommerce.notificationservice.client;

import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.userservice.proto.UserServiceProtos;
import com.ecommerce.userservice.proto.UserServiceGrpc;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    @Value("${grpc.client.user-service.host:user-service}")
    private String userServiceHost;

    @Value("${grpc.client.user-service.port:9090}")
    private int userServicePort;

    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress(userServiceHost, userServicePort)
                .usePlaintext()
                .build();
        userServiceStub = UserServiceGrpc.newBlockingStub(channel);
        logger.info("User Service gRPC client initialized: {}:{}", userServiceHost, userServicePort);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while shutting down User Service gRPC client", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Get user information by user ID
     */
    public UserServiceProtos.User getUser(Long userId) {
        try {
            CommonProtos.TenantContext context = CommonProtos.TenantContext.newBuilder()
                    .setTenantId(TenantContext.getTenantId())
                    .setUserId(String.valueOf(TenantContext.getUserId()))
                    .setCorrelationId(TenantContext.getCorrelationId())
                    .build();

            UserServiceProtos.GetUserRequest request = UserServiceProtos.GetUserRequest.newBuilder()
                    .setContext(context)
                    .setUserId(userId)
                    .build();

            UserServiceProtos.GetUserResponse response = userServiceStub.getUser(request);
            return response.getUser();

        } catch (StatusRuntimeException e) {
            logger.error("Failed to get user from User Service: userId={}", userId, e);
            throw new RuntimeException("Failed to get user information", e);
        }
    }

    /**
     * Validate if user exists and is active
     */
    public boolean validateUser(Long userId) {
        try {
            CommonProtos.TenantContext context = CommonProtos.TenantContext.newBuilder()
                    .setTenantId(TenantContext.getTenantId())
                    .setUserId(String.valueOf(TenantContext.getUserId()))
                    .setCorrelationId(TenantContext.getCorrelationId())
                    .build();

            UserServiceProtos.ValidateUserRequest request = UserServiceProtos.ValidateUserRequest.newBuilder()
                    .setContext(context)
                    .setUserId(userId)
                    .build();

            UserServiceProtos.ValidateUserResponse response = userServiceStub.validateUser(request);
            return response.getIsValid() && response.getIsActive();

        } catch (StatusRuntimeException e) {
            logger.error("Failed to validate user from User Service: userId={}", userId, e);
            return false;
        }
    }

    /**
     * Get user addresses for notification delivery
     */
    public java.util.List<UserServiceProtos.UserAddress> getUserAddresses(Long userId) {
        try {
            CommonProtos.TenantContext context = CommonProtos.TenantContext.newBuilder()
                    .setTenantId(TenantContext.getTenantId())
                    .setUserId(String.valueOf(TenantContext.getUserId()))
                    .setCorrelationId(TenantContext.getCorrelationId())
                    .build();

            UserServiceProtos.GetUserAddressesRequest request = UserServiceProtos.GetUserAddressesRequest.newBuilder()
                    .setContext(context)
                    .setUserId(userId)
                    .build();

            UserServiceProtos.GetUserAddressesResponse response = userServiceStub.getUserAddresses(request);
            return response.getAddressesList();

        } catch (StatusRuntimeException e) {
            logger.error("Failed to get user addresses from User Service: userId={}", userId, e);
            throw new RuntimeException("Failed to get user addresses", e);
        }
    }
}
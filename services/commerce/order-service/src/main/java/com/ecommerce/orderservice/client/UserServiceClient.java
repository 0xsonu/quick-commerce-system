package com.ecommerce.orderservice.client;

import com.ecommerce.userservice.proto.UserServiceGrpc;
import com.ecommerce.userservice.proto.UserServiceProtos.*;
import com.ecommerce.shared.proto.CommonProtos.TenantContext;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);
    
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public ValidateUserResponse validateUser(TenantContext context, long userId) {
        try {
            ValidateUserRequest request = ValidateUserRequest.newBuilder()
                    .setContext(context)
                    .setUserId(userId)
                    .build();

            return userServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .validateUser(request);
        } catch (Exception e) {
            logger.error("Failed to validate user {}: {}", userId, e.getMessage());
            throw new RuntimeException("User validation failed", e);
        }
    }

    public GetUserResponse getUser(TenantContext context, long userId) {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setContext(context)
                    .setUserId(userId)
                    .build();

            return userServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getUser(request);
        } catch (Exception e) {
            logger.error("Failed to get user {}: {}", userId, e.getMessage());
            throw new RuntimeException("User retrieval failed", e);
        }
    }

    public GetUserAddressesResponse getUserAddresses(TenantContext context, long userId) {
        try {
            GetUserAddressesRequest request = GetUserAddressesRequest.newBuilder()
                    .setContext(context)
                    .setUserId(userId)
                    .build();

            return userServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getUserAddresses(request);
        } catch (Exception e) {
            logger.error("Failed to get user addresses for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("User addresses retrieval failed", e);
        }
    }
}
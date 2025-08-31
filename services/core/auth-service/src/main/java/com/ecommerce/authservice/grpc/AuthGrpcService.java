package com.ecommerce.authservice.grpc;

import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.authservice.service.TokenService;
import com.ecommerce.authservice.dto.TokenValidationResult;
import com.ecommerce.authservice.dto.UserInfo;
import com.ecommerce.authservice.dto.RefreshTokenRequest;
import com.ecommerce.authservice.dto.RefreshTokenResponse;
import com.ecommerce.authservice.proto.AuthServiceGrpc;
import com.ecommerce.authservice.proto.AuthServiceProtos.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AuthGrpcService.class);

    private final AuthService authService;
    private final TokenService tokenService;

    @Autowired
    public AuthGrpcService(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        try {
            logger.debug("gRPC ValidateToken request for token: {}", 
                        request.getToken().substring(0, Math.min(20, request.getToken().length())) + "...");

            TokenValidationResult result = tokenService.validateToken(request.getToken());
            
            ValidateTokenResponse.Builder responseBuilder = ValidateTokenResponse.newBuilder()
                .setIsValid(result.isValid());

            if (result.isValid()) {
                responseBuilder
                    .setTenantId(result.getTenantId())
                    .setUserId(result.getUserId())
                    .addAllRoles(result.getRoles())
                    .setExpiresAt(result.getExpiresAt().getEpochSecond());
            }

            ValidateTokenResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug("gRPC ValidateToken completed, valid: {}", result.isValid());

        } catch (Exception e) {
            logger.error("Error validating token", e);
            
            ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setIsValid(false)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUserFromToken(GetUserFromTokenRequest request, StreamObserver<GetUserFromTokenResponse> responseObserver) {
        try {
            logger.debug("gRPC GetUserFromToken request");

            TokenValidationResult result = tokenService.validateToken(request.getToken());
            
            GetUserFromTokenResponse.Builder responseBuilder = GetUserFromTokenResponse.newBuilder()
                .setIsValid(result.isValid());

            if (result.isValid()) {
                UserInfo userInfo = authService.getUserInfo(result.getUserId());
                responseBuilder
                    .setTenantId(result.getTenantId())
                    .setUserId(result.getUserId())
                    .setEmail(userInfo.getEmail())
                    .addAllRoles(result.getRoles());
            }

            GetUserFromTokenResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting user from token", e);
            
            GetUserFromTokenResponse response = GetUserFromTokenResponse.newBuilder()
                .setIsValid(false)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void refreshToken(com.ecommerce.authservice.proto.AuthServiceProtos.RefreshTokenRequest request, 
                            StreamObserver<RefreshTokenResponse> responseObserver) {
        try {
            logger.info("gRPC RefreshToken request");

            RefreshTokenRequest serviceRequest = new RefreshTokenRequest();
            serviceRequest.setRefreshToken(request.getRefreshToken());

            RefreshTokenResponse serviceResponse = tokenService.refreshToken(serviceRequest);
            
            com.ecommerce.authservice.proto.AuthServiceProtos.RefreshTokenResponse response = 
                com.ecommerce.authservice.proto.AuthServiceProtos.RefreshTokenResponse.newBuilder()
                    .setSuccess(serviceResponse.isSuccess())
                    .setAccessToken(serviceResponse.getAccessToken() != null ? serviceResponse.getAccessToken() : "")
                    .setRefreshToken(serviceResponse.getRefreshToken() != null ? serviceResponse.getRefreshToken() : "")
                    .setExpiresAt(serviceResponse.getExpiresAt() != null ? serviceResponse.getExpiresAt().getEpochSecond() : 0)
                    .setErrorMessage(serviceResponse.getErrorMessage() != null ? serviceResponse.getErrorMessage() : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error refreshing token", e);
            
            com.ecommerce.authservice.proto.AuthServiceProtos.RefreshTokenResponse response = 
                com.ecommerce.authservice.proto.AuthServiceProtos.RefreshTokenResponse.newBuilder()
                    .setSuccess(false)
                    .setAccessToken("")
                    .setRefreshToken("")
                    .setExpiresAt(0)
                    .setErrorMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
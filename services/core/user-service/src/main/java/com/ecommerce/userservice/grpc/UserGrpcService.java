package com.ecommerce.userservice.grpc;

import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.dto.AddressResponse;
import com.ecommerce.userservice.service.UserService;
import com.ecommerce.userservice.service.AddressService;
import com.ecommerce.userservice.proto.UserServiceGrpc;
import com.ecommerce.userservice.proto.UserServiceProtos.*;
import com.ecommerce.shared.grpc.TenantContextInterceptor;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@GrpcService(interceptors = {TenantContextInterceptor.class})
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(UserGrpcService.class);

    private final UserService userService;
    private final AddressService addressService;

    @Autowired
    public UserGrpcService(UserService userService, AddressService addressService) {
        this.userService = userService;
        this.addressService = addressService;
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            logger.debug("gRPC GetUser request for user: {} in tenant: {}", 
                        request.getUserId(), TenantContext.getTenantId());

            UserResponse user = userService.getUserById(request.getUserId());
            
            GetUserResponse response = GetUserResponse.newBuilder()
                .setUser(convertToProtoUser(user))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting user: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void getUserAddresses(GetUserAddressesRequest request, StreamObserver<GetUserAddressesResponse> responseObserver) {
        try {
            logger.debug("gRPC GetUserAddresses request for user: {} in tenant: {}", 
                        request.getUserId(), TenantContext.getTenantId());

            List<AddressResponse> addresses = addressService.getUserAddresses(request.getUserId());
            
            GetUserAddressesResponse.Builder responseBuilder = GetUserAddressesResponse.newBuilder();
            for (AddressResponse address : addresses) {
                responseBuilder.addAddresses(convertToProtoUserAddress(address));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting user addresses: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void validateUser(ValidateUserRequest request, StreamObserver<ValidateUserResponse> responseObserver) {
        try {
            logger.debug("gRPC ValidateUser request for user: {} in tenant: {}", 
                        request.getUserId(), TenantContext.getTenantId());

            UserResponse user = userService.getUserById(request.getUserId());
            
            ValidateUserResponse response = ValidateUserResponse.newBuilder()
                .setIsValid(user != null)
                .setIsActive(user != null && user.isActive())
                .setEmail(user != null ? user.getEmail() : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.warn("User validation failed for user: {}", request.getUserId(), e);
            ValidateUserResponse response = ValidateUserResponse.newBuilder()
                .setIsValid(false)
                .setIsActive(false)
                .setEmail("")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private User convertToProtoUser(UserResponse user) {
        return User.newBuilder()
            .setId(user.getId())
            .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
            .setLastName(user.getLastName() != null ? user.getLastName() : "")
            .setEmail(user.getEmail())
            .setPhone(user.getPhone() != null ? user.getPhone() : "")
            .setIsActive(user.isActive())
            .build();
    }

    private UserAddress convertToProtoUserAddress(AddressResponse address) {
        return UserAddress.newBuilder()
            .setId(address.getId())
            .setType(address.getType().name())
            .setAddress(CommonProtos.Address.newBuilder()
                .setStreetAddress(address.getStreetAddress())
                .setCity(address.getCity())
                .setState(address.getState())
                .setPostalCode(address.getPostalCode())
                .setCountry(address.getCountry())
                .build())
            .setIsDefault(address.isDefault())
            .build();
    }
}
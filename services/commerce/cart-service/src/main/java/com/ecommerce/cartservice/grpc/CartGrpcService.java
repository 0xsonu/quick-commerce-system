package com.ecommerce.cartservice.grpc;

import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.UpdateCartItemRequest;
import com.ecommerce.cartservice.service.CartService;
import com.ecommerce.cartservice.proto.CartServiceGrpc;
import com.ecommerce.cartservice.proto.CartServiceProtos.*;
import com.ecommerce.shared.grpc.TenantContextInterceptor;
import com.ecommerce.shared.proto.CommonProtos;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@GrpcService(interceptors = {TenantContextInterceptor.class})
public class CartGrpcService extends CartServiceGrpc.CartServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(CartGrpcService.class);

    private final CartService cartService;

    @Autowired
    public CartGrpcService(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public void getCart(com.ecommerce.cartservice.proto.CartServiceProtos.GetCartRequest request, 
                       StreamObserver<GetCartResponse> responseObserver) {
        try {
            logger.debug("gRPC GetCart request for user: {} in tenant: {}", 
                        request.getUserId(), TenantContext.getTenantId());

            CartResponse cart = cartService.getCart(request.getUserId());
            
            GetCartResponse response = GetCartResponse.newBuilder()
                .setCart(convertToProtoCart(cart))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting cart for user: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    @Override
    public void addToCart(com.ecommerce.cartservice.proto.CartServiceProtos.AddToCartRequest request, 
                         StreamObserver<AddToCartResponse> responseObserver) {
        try {
            logger.info("gRPC AddToCart request for user: {} product: {} quantity: {} in tenant: {}", 
                       request.getUserId(), request.getProductId(), request.getQuantity(), TenantContext.getTenantId());

            AddToCartRequest serviceRequest = new AddToCartRequest();
            serviceRequest.setProductId(request.getProductId());
            serviceRequest.setSku(request.getSku());
            serviceRequest.setQuantity(request.getQuantity());

            CartResponse cart = cartService.addToCart(request.getUserId(), serviceRequest);
            
            AddToCartResponse response = AddToCartResponse.newBuilder()
                .setSuccess(true)
                .setCart(convertToProtoCart(cart))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error adding to cart for user: {} product: {}", 
                        request.getUserId(), request.getProductId(), e);
            
            AddToCartResponse response = AddToCartResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateCartItem(com.ecommerce.cartservice.proto.CartServiceProtos.UpdateCartItemRequest request, 
                              StreamObserver<UpdateCartItemResponse> responseObserver) {
        try {
            logger.info("gRPC UpdateCartItem request for user: {} product: {} quantity: {} in tenant: {}", 
                       request.getUserId(), request.getProductId(), request.getNewQuantity(), TenantContext.getTenantId());

            UpdateCartItemRequest serviceRequest = new UpdateCartItemRequest();
            serviceRequest.setProductId(request.getProductId());
            serviceRequest.setQuantity(request.getNewQuantity());

            CartResponse cart = cartService.updateCartItem(request.getUserId(), serviceRequest);
            
            UpdateCartItemResponse response = UpdateCartItemResponse.newBuilder()
                .setSuccess(true)
                .setCart(convertToProtoCart(cart))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error updating cart item for user: {} product: {}", 
                        request.getUserId(), request.getProductId(), e);
            
            UpdateCartItemResponse response = UpdateCartItemResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void removeFromCart(com.ecommerce.cartservice.proto.CartServiceProtos.RemoveFromCartRequest request, 
                              StreamObserver<RemoveFromCartResponse> responseObserver) {
        try {
            logger.info("gRPC RemoveFromCart request for user: {} product: {} in tenant: {}", 
                       request.getUserId(), request.getProductId(), TenantContext.getTenantId());

            CartResponse cart = cartService.removeFromCart(request.getUserId(), request.getProductId());
            
            RemoveFromCartResponse response = RemoveFromCartResponse.newBuilder()
                .setSuccess(true)
                .setCart(convertToProtoCart(cart))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error removing from cart for user: {} product: {}", 
                        request.getUserId(), request.getProductId(), e);
            
            RemoveFromCartResponse response = RemoveFromCartResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void clearCart(com.ecommerce.cartservice.proto.CartServiceProtos.ClearCartRequest request, 
                         StreamObserver<ClearCartResponse> responseObserver) {
        try {
            logger.info("gRPC ClearCart request for user: {} in tenant: {}", 
                       request.getUserId(), TenantContext.getTenantId());

            cartService.clearCart(request.getUserId());
            
            ClearCartResponse response = ClearCartResponse.newBuilder()
                .setSuccess(true)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error clearing cart for user: {}", request.getUserId(), e);
            
            ClearCartResponse response = ClearCartResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void validateCart(com.ecommerce.cartservice.proto.CartServiceProtos.ValidateCartRequest request, 
                            StreamObserver<ValidateCartResponse> responseObserver) {
        try {
            logger.debug("gRPC ValidateCart request for user: {} in tenant: {}", 
                        request.getUserId(), TenantContext.getTenantId());

            CartResponse cart = cartService.validateCart(request.getUserId());
            
            ValidateCartResponse response = ValidateCartResponse.newBuilder()
                .setIsValid(cart.isValid())
                .setCart(convertToProtoCart(cart))
                .addAllValidationErrors(cart.getValidationErrors())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error validating cart for user: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }

    private Cart convertToProtoCart(CartResponse cart) {
        Cart.Builder cartBuilder = Cart.newBuilder()
            .setUserId(cart.getUserId())
            .setSubtotal(convertToProtoMoney(cart.getSubtotal(), cart.getCurrency()))
            .setTaxAmount(convertToProtoMoney(cart.getTaxAmount(), cart.getCurrency()))
            .setTotalAmount(convertToProtoMoney(cart.getTotalAmount(), cart.getCurrency()))
            .setTotalItems(cart.getTotalItems())
            .setCreatedAt(cart.getCreatedAt().getEpochSecond())
            .setUpdatedAt(cart.getUpdatedAt().getEpochSecond());

        // Add cart items
        if (cart.getItems() != null) {
            for (com.ecommerce.cartservice.dto.CartItemResponse item : cart.getItems()) {
                cartBuilder.addItems(CartItem.newBuilder()
                    .setProductId(item.getProductId())
                    .setSku(item.getSku())
                    .setProductName(item.getProductName())
                    .setQuantity(item.getQuantity())
                    .setUnitPrice(convertToProtoMoney(item.getUnitPrice(), cart.getCurrency()))
                    .setTotalPrice(convertToProtoMoney(item.getTotalPrice(), cart.getCurrency()))
                    .setIsAvailable(item.isAvailable())
                    .setAddedAt(item.getAddedAt().getEpochSecond())
                    .build());
            }
        }

        return cartBuilder.build();
    }

    private CommonProtos.Money convertToProtoMoney(BigDecimal amount, String currency) {
        return CommonProtos.Money.newBuilder()
            .setAmountCents(amount.multiply(new BigDecimal("100")).longValue())
            .setCurrency(currency)
            .build();
    }
}
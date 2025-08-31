package com.ecommerce.paymentservice.grpc;

import com.ecommerce.paymentservice.dto.ProcessPaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.service.PaymentService;
import com.ecommerce.paymentservice.proto.PaymentServiceGrpc;
import com.ecommerce.paymentservice.proto.PaymentServiceProtos.*;
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
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(PaymentGrpcService.class);

    private final PaymentService paymentService;

    @Autowired
    public PaymentGrpcService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void processPayment(com.ecommerce.paymentservice.proto.PaymentServiceProtos.ProcessPaymentRequest request, 
                              StreamObserver<ProcessPaymentResponse> responseObserver) {
        try {
            logger.info("gRPC ProcessPayment request for order: {} amount: {} in tenant: {}", 
                       request.getOrderId(), request.getAmount().getAmountCents(), TenantContext.getTenantId());

            ProcessPaymentRequest serviceRequest = new ProcessPaymentRequest();
            serviceRequest.setOrderId(request.getOrderId());
            serviceRequest.setAmount(convertFromProtoMoney(request.getAmount()));
            serviceRequest.setCurrency(request.getAmount().getCurrency());
            serviceRequest.setPaymentMethod(request.getPaymentMethod());
            serviceRequest.setPaymentToken(request.getPaymentToken());
            serviceRequest.setIdempotencyKey(request.getIdempotencyKey());

            PaymentResponse payment = paymentService.processPayment(serviceRequest);
            
            ProcessPaymentResponse response = ProcessPaymentResponse.newBuilder()
                .setSuccess(payment.isSuccess())
                .setPaymentId(payment.getPaymentId())
                .setTransactionId(payment.getTransactionId() != null ? payment.getTransactionId() : "")
                .setStatus(payment.getStatus().name())
                .setErrorMessage(payment.getErrorMessage() != null ? payment.getErrorMessage() : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error processing payment for order: {}", request.getOrderId(), e);
            
            ProcessPaymentResponse response = ProcessPaymentResponse.newBuilder()
                .setSuccess(false)
                .setPaymentId("")
                .setTransactionId("")
                .setStatus("FAILED")
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getPaymentStatus(GetPaymentStatusRequest request, StreamObserver<GetPaymentStatusResponse> responseObserver) {
        try {
            logger.debug("gRPC GetPaymentStatus request for payment: {} in tenant: {}", 
                        request.getPaymentId(), TenantContext.getTenantId());

            PaymentResponse payment = paymentService.getPaymentById(request.getPaymentId());
            
            GetPaymentStatusResponse response = GetPaymentStatusResponse.newBuilder()
                .setPaymentId(payment.getPaymentId())
                .setStatus(payment.getStatus().name())
                .setAmount(convertToProtoMoney(payment.getAmount(), payment.getCurrency()))
                .setTransactionId(payment.getTransactionId() != null ? payment.getTransactionId() : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting payment status: {}", request.getPaymentId(), e);
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("Payment not found: " + request.getPaymentId())
                .asRuntimeException());
        }
    }

    @Override
    public void refundPayment(com.ecommerce.paymentservice.proto.PaymentServiceProtos.RefundPaymentRequest request, 
                             StreamObserver<RefundPaymentResponse> responseObserver) {
        try {
            logger.info("gRPC RefundPayment request for payment: {} amount: {} in tenant: {}", 
                       request.getPaymentId(), request.getAmount().getAmountCents(), TenantContext.getTenantId());

            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setPaymentId(request.getPaymentId());
            refundRequest.setAmount(convertFromProtoMoney(request.getAmount()));
            refundRequest.setCurrency(request.getAmount().getCurrency());
            refundRequest.setReason(request.getReason());

            PaymentResponse refund = paymentService.refundPayment(refundRequest);
            
            RefundPaymentResponse response = RefundPaymentResponse.newBuilder()
                .setSuccess(refund.isSuccess())
                .setRefundId(refund.getPaymentId())
                .setErrorMessage(refund.getErrorMessage() != null ? refund.getErrorMessage() : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error refunding payment: {}", request.getPaymentId(), e);
            
            RefundPaymentResponse response = RefundPaymentResponse.newBuilder()
                .setSuccess(false)
                .setRefundId("")
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private BigDecimal convertFromProtoMoney(CommonProtos.Money money) {
        return new BigDecimal(money.getAmountCents()).divide(new BigDecimal("100"));
    }

    private CommonProtos.Money convertToProtoMoney(BigDecimal amount, String currency) {
        return CommonProtos.Money.newBuilder()
            .setAmountCents(amount.multiply(new BigDecimal("100")).longValue())
            .setCurrency(currency)
            .build();
    }
}
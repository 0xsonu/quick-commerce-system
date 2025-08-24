package com.ecommerce.orderservice.client;

import com.ecommerce.paymentservice.proto.PaymentServiceGrpc;
import com.ecommerce.paymentservice.proto.PaymentServiceProtos.*;
import com.ecommerce.shared.proto.CommonProtos.TenantContext;
import com.ecommerce.shared.proto.CommonProtos.Money;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Service
public class PaymentServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceClient.class);
    
    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentServiceStub;

    public ProcessPaymentResponse processPayment(TenantContext context, long orderId, Money amount, 
                                               String paymentMethod, String paymentToken, String idempotencyKey) {
        try {
            ProcessPaymentRequest request = ProcessPaymentRequest.newBuilder()
                    .setContext(context)
                    .setOrderId(orderId)
                    .setAmount(amount)
                    .setPaymentMethod(paymentMethod)
                    .setPaymentToken(paymentToken)
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            return paymentServiceStub
                    .withDeadlineAfter(30, TimeUnit.SECONDS)
                    .processPayment(request);
        } catch (Exception e) {
            logger.error("Failed to process payment for order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    public GetPaymentStatusResponse getPaymentStatus(TenantContext context, String paymentId) {
        try {
            GetPaymentStatusRequest request = GetPaymentStatusRequest.newBuilder()
                    .setContext(context)
                    .setPaymentId(paymentId)
                    .build();

            return paymentServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getPaymentStatus(request);
        } catch (Exception e) {
            logger.error("Failed to get payment status for payment {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Payment status check failed", e);
        }
    }

    public RefundPaymentResponse refundPayment(TenantContext context, String paymentId, Money amount, String reason) {
        try {
            RefundPaymentRequest request = RefundPaymentRequest.newBuilder()
                    .setContext(context)
                    .setPaymentId(paymentId)
                    .setAmount(amount)
                    .setReason(reason)
                    .build();

            return paymentServiceStub
                    .withDeadlineAfter(30, TimeUnit.SECONDS)
                    .refundPayment(request);
        } catch (Exception e) {
            logger.error("Failed to refund payment {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Payment refund failed", e);
        }
    }
}
package com.ecommerce.notificationservice.grpc;

import com.ecommerce.notificationservice.dto.SendNotificationRequest;
import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.dto.NotificationPreferenceResponse;
import com.ecommerce.notificationservice.service.NotificationService;
import com.ecommerce.notificationservice.service.NotificationPreferenceService;
import com.ecommerce.notificationservice.proto.NotificationServiceGrpc;
import com.ecommerce.notificationservice.proto.NotificationServiceProtos.*;
import com.ecommerce.shared.grpc.TenantContextInterceptor;
import com.ecommerce.shared.utils.TenantContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@GrpcService(interceptors = {TenantContextInterceptor.class})
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(NotificationGrpcService.class);

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @Autowired
    public NotificationGrpcService(NotificationService notificationService, 
                                  NotificationPreferenceService preferenceService) {
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
    }

    @Override
    public void sendNotification(com.ecommerce.notificationservice.proto.NotificationServiceProtos.SendNotificationRequest request, 
                                StreamObserver<SendNotificationResponse> responseObserver) {
        try {
            logger.info("gRPC SendNotification request for user: {} template: {} channel: {} in tenant: {}", 
                       request.getUserId(), request.getTemplateId(), request.getChannel(), TenantContext.getTenantId());

            SendNotificationRequest serviceRequest = new SendNotificationRequest();
            serviceRequest.setUserId(request.getUserId());
            serviceRequest.setTemplateId(request.getTemplateId());
            serviceRequest.setChannel(request.getChannel());
            serviceRequest.setPriority(request.getPriority());
            serviceRequest.setTemplateData(request.getTemplateDataMap());
            serviceRequest.setIdempotencyKey(request.getIdempotencyKey());

            NotificationResponse notification = notificationService.sendNotification(serviceRequest);
            
            SendNotificationResponse response = SendNotificationResponse.newBuilder()
                .setSuccess(notification != null && notification.isSuccess())
                .setNotificationId(notification != null ? notification.getNotificationId() : "")
                .setErrorMessage(notification != null && !notification.isSuccess() ? notification.getErrorMessage() : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error sending notification for user: {} template: {}", 
                        request.getUserId(), request.getTemplateId(), e);
            
            SendNotificationResponse response = SendNotificationResponse.newBuilder()
                .setSuccess(false)
                .setNotificationId("")
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void sendBulkNotification(com.ecommerce.notificationservice.proto.NotificationServiceProtos.SendBulkNotificationRequest request, 
                                    StreamObserver<SendBulkNotificationResponse> responseObserver) {
        try {
            logger.info("gRPC SendBulkNotification request for {} users template: {} channel: {} in tenant: {}", 
                       request.getUserIdsCount(), request.getTemplateId(), request.getChannel(), TenantContext.getTenantId());

            SendNotificationRequest serviceRequest = new SendNotificationRequest();
            serviceRequest.setTemplateId(request.getTemplateId());
            serviceRequest.setChannel(request.getChannel());
            serviceRequest.setPriority(request.getPriority());
            serviceRequest.setTemplateData(request.getTemplateDataMap());

            int sentCount = 0;
            int failedCount = 0;
            
            for (Long userId : request.getUserIdsList()) {
                try {
                    serviceRequest.setUserId(userId);
                    NotificationResponse notification = notificationService.sendNotification(serviceRequest);
                    if (notification != null && notification.isSuccess()) {
                        sentCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to send notification to user: {}", userId, e);
                    failedCount++;
                }
            }
            
            SendBulkNotificationResponse response = SendBulkNotificationResponse.newBuilder()
                .setSuccess(sentCount > 0)
                .setSentCount(sentCount)
                .setFailedCount(failedCount)
                .setErrorMessage(failedCount > 0 ? "Some notifications failed to send" : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error sending bulk notifications template: {}", request.getTemplateId(), e);
            
            SendBulkNotificationResponse response = SendBulkNotificationResponse.newBuilder()
                .setSuccess(false)
                .setSentCount(0)
                .setFailedCount(request.getUserIdsCount())
                .setErrorMessage(e.getMessage())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getNotificationStatus(GetNotificationStatusRequest request, 
                                     StreamObserver<GetNotificationStatusResponse> responseObserver) {
        try {
            logger.debug("gRPC GetNotificationStatus request for notification: {} in tenant: {}", 
                        request.getNotificationId(), TenantContext.getTenantId());

            NotificationResponse notification = notificationService.getNotificationById(request.getNotificationId());
            
            GetNotificationStatusResponse response = GetNotificationStatusResponse.newBuilder()
                .setNotificationId(notification.getNotificationId())
                .setStatus(notification.getStatus().name())
                .setSentAt(notification.getSentAt() != null ? notification.getSentAt().getEpochSecond() : 0)
                .setDeliveredAt(notification.getDeliveredAt() != null ? notification.getDeliveredAt().getEpochSecond() : 0)
                .setErrorMessage(notification.getErrorMessage() != null ? notification.getErrorMessage() : "")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting notification status: {}", request.getNotificationId(), e);
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("Notification not found: " + request.getNotificationId())
                .asRuntimeException());
        }
    }

    @Override
    public void getUserPreferences(GetUserPreferencesRequest request, 
                                  StreamObserver<GetUserPreferencesResponse> responseObserver) {
        try {
            logger.debug("gRPC GetUserPreferences request for user: {} in tenant: {}", 
                        request.getUserId(), TenantContext.getTenantId());

            NotificationPreferenceResponse preferences = preferenceService.getUserPreferences(request.getUserId());
            
            GetUserPreferencesResponse response = GetUserPreferencesResponse.newBuilder()
                .setUserId(preferences.getUserId())
                .setEmailEnabled(preferences.isEmailEnabled())
                .setSmsEnabled(preferences.isSmsEnabled())
                .setPushEnabled(preferences.isPushEnabled())
                .addAllSubscribedCategories(preferences.getSubscribedCategories())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting user preferences: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException());
        }
    }
}
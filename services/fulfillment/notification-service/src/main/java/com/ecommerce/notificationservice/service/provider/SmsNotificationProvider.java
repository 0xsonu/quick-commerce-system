package com.ecommerce.notificationservice.service.provider;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class SmsNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationProvider.class);

    @Value("${notification.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${notification.sms.twilio.from-number:}")
    private String fromNumber;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    private boolean twilioInitialized = false;

    @PostConstruct
    public void initializeTwilio() {
        if (smsEnabled && accountSid != null && !accountSid.isEmpty() 
            && authToken != null && !authToken.isEmpty()) {
            try {
                Twilio.init(accountSid, authToken);
                twilioInitialized = true;
                logger.info("Twilio SMS provider initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio SMS provider", e);
                twilioInitialized = false;
            }
        } else {
            logger.info("SMS provider is disabled or not configured");
        }
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean sendNotification(String recipient, String subject, String content) throws NotificationException {
        if (!isAvailable()) {
            throw new NotificationException("SMS provider is not available");
        }

        try {
            // For SMS, we combine subject and content since SMS doesn't have a subject field
            String messageBody = subject != null && !subject.isEmpty() 
                ? subject + "\n\n" + content 
                : content;

            Message message = Message.creator(
                new PhoneNumber(recipient),
                new PhoneNumber(fromNumber),
                messageBody
            ).create();

            logger.info("SMS sent successfully to: {} with SID: {}", recipient, message.getSid());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send SMS to: {}", recipient, e);
            throw new NotificationException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return smsEnabled && twilioInitialized && fromNumber != null && !fromNumber.isEmpty();
    }
}
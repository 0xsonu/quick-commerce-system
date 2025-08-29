package com.ecommerce.notificationservice.service.provider;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationProvider.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${notification.email.from:noreply@ecommerce.com}")
    private String fromEmail;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean sendNotification(String recipient, String subject, String content) throws NotificationException {
        if (!isAvailable()) {
            throw new NotificationException("Email provider is not available");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            
            logger.info("Email sent successfully to: {}", recipient);
            return true;
            
        } catch (MailException e) {
            logger.error("Failed to send email to: {}", recipient, e);
            throw new NotificationException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return emailEnabled && mailSender != null;
    }
}
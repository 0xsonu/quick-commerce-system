package com.ecommerce.notificationservice.service.provider;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationProviderTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationProvider emailProvider;

    @BeforeEach
    void setUp() {
        emailProvider = new EmailNotificationProvider();
        ReflectionTestUtils.setField(emailProvider, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailProvider, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailProvider, "emailEnabled", true);
    }

    @Test
    void getChannel_ShouldReturnEmail() {
        // Act & Assert
        assertEquals(NotificationChannel.EMAIL, emailProvider.getChannel());
    }

    @Test
    void isAvailable_WhenEnabledAndMailSenderExists_ShouldReturnTrue() {
        // Act & Assert
        assertTrue(emailProvider.isAvailable());
    }

    @Test
    void isAvailable_WhenDisabled_ShouldReturnFalse() {
        // Arrange
        ReflectionTestUtils.setField(emailProvider, "emailEnabled", false);

        // Act & Assert
        assertFalse(emailProvider.isAvailable());
    }

    @Test
    void isAvailable_WhenMailSenderIsNull_ShouldReturnFalse() {
        // Arrange
        ReflectionTestUtils.setField(emailProvider, "mailSender", null);

        // Act & Assert
        assertFalse(emailProvider.isAvailable());
    }

    @Test
    void sendNotification_WhenSuccessful_ShouldReturnTrue() throws NotificationException {
        // Arrange
        String recipient = "recipient@example.com";
        String subject = "Test Subject";
        String content = "Test Content";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        boolean result = emailProvider.sendNotification(recipient, subject, content);

        // Assert
        assertTrue(result);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotification_WhenNotAvailable_ShouldThrowException() {
        // Arrange
        ReflectionTestUtils.setField(emailProvider, "emailEnabled", false);

        // Act & Assert
        NotificationException exception = assertThrows(NotificationException.class, () -> 
            emailProvider.sendNotification("test@example.com", "Subject", "Content"));
        
        assertEquals("Email provider is not available", exception.getMessage());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotification_WhenMailExceptionThrown_ShouldThrowNotificationException() {
        // Arrange
        String recipient = "recipient@example.com";
        String subject = "Test Subject";
        String content = "Test Content";

        doThrow(new MailException("SMTP server unavailable") {})
            .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        NotificationException exception = assertThrows(NotificationException.class, () -> 
            emailProvider.sendNotification(recipient, subject, content));
        
        assertTrue(exception.getMessage().contains("Failed to send email"));
        assertTrue(exception.getCause() instanceof MailException);
    }

    @Test
    void sendNotification_ShouldSetCorrectMessageProperties() throws NotificationException {
        // Arrange
        String recipient = "recipient@example.com";
        String subject = "Test Subject";
        String content = "Test Content";

        doAnswer(invocation -> {
            SimpleMailMessage message = invocation.getArgument(0);
            assertEquals("test@example.com", message.getFrom());
            assertEquals(recipient, message.getTo()[0]);
            assertEquals(subject, message.getSubject());
            assertEquals(content, message.getText());
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailProvider.sendNotification(recipient, subject, content);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
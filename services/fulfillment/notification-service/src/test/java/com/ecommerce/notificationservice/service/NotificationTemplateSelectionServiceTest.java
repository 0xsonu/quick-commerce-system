package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationTemplate;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateSelectionServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private NotificationTemplateSelectionService templateSelectionService;

    @Test
    void selectTemplate_ShouldReturnTenantSpecificTemplate() {
        // Given
        String tenantId = "tenant1";
        NotificationType type = NotificationType.ORDER_CREATED;
        NotificationChannel channel = NotificationChannel.EMAIL;
        String expectedTemplateKey = "order_created_email";
        
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateKey(expectedTemplateKey);
        template.setIsActive(true);
        
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(tenantId, expectedTemplateKey, channel))
            .thenReturn(Optional.of(template));

        // When
        String result = templateSelectionService.selectTemplate(tenantId, type, channel);

        // Then
        assertThat(result).isEqualTo(expectedTemplateKey);
    }

    @Test
    void selectTemplate_ShouldReturnDefaultTemplateWhenTenantSpecificNotFound() {
        // Given
        String tenantId = "tenant1";
        NotificationType type = NotificationType.ORDER_CREATED;
        NotificationChannel channel = NotificationChannel.EMAIL;
        String expectedTemplateKey = "order_created_email";
        
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(eq(tenantId), any(), eq(channel)))
            .thenReturn(Optional.empty());

        // When
        String result = templateSelectionService.selectTemplate(tenantId, type, channel);

        // Then
        assertThat(result).isEqualTo(expectedTemplateKey);
    }

    @Test
    void selectTemplate_ShouldReturnDefaultTemplateWhenTenantTemplateInactive() {
        // Given
        String tenantId = "tenant1";
        NotificationType type = NotificationType.ORDER_CREATED;
        NotificationChannel channel = NotificationChannel.EMAIL;
        String expectedTemplateKey = "order_created_email";
        
        NotificationTemplate inactiveTemplate = new NotificationTemplate();
        inactiveTemplate.setTemplateKey(expectedTemplateKey);
        inactiveTemplate.setIsActive(false);
        
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(tenantId, expectedTemplateKey, channel))
            .thenReturn(Optional.of(inactiveTemplate));

        // When
        String result = templateSelectionService.selectTemplate(tenantId, type, channel);

        // Then
        assertThat(result).isEqualTo(expectedTemplateKey);
    }

    @Test
    void templateExists_ShouldReturnTrueWhenTemplateExistsAndActive() {
        // Given
        String tenantId = "tenant1";
        String templateKey = "order_created_email";
        NotificationChannel channel = NotificationChannel.EMAIL;
        
        NotificationTemplate template = new NotificationTemplate();
        template.setIsActive(true);
        
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(tenantId, templateKey, channel))
            .thenReturn(Optional.of(template));

        // When
        boolean result = templateSelectionService.templateExists(tenantId, templateKey, channel);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void templateExists_ShouldReturnFalseWhenTemplateNotFound() {
        // Given
        String tenantId = "tenant1";
        String templateKey = "order_created_email";
        NotificationChannel channel = NotificationChannel.EMAIL;
        
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(tenantId, templateKey, channel))
            .thenReturn(Optional.empty());

        // When
        boolean result = templateSelectionService.templateExists(tenantId, templateKey, channel);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void templateExists_ShouldReturnFalseWhenTemplateInactive() {
        // Given
        String tenantId = "tenant1";
        String templateKey = "order_created_email";
        NotificationChannel channel = NotificationChannel.EMAIL;
        
        NotificationTemplate template = new NotificationTemplate();
        template.setIsActive(false);
        
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(tenantId, templateKey, channel))
            .thenReturn(Optional.of(template));

        // When
        boolean result = templateSelectionService.templateExists(tenantId, templateKey, channel);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getTemplatePriorityOrder_ShouldReturnCorrectOrder() {
        // Given
        String tenantId = "tenant1";
        NotificationType type = NotificationType.ORDER_CREATED;
        NotificationChannel channel = NotificationChannel.EMAIL;

        // When
        String[] result = templateSelectionService.getTemplatePriorityOrder(tenantId, type, channel);

        // Then
        assertThat(result).containsExactly(
            "tenant1_order_created_email",
            "order_created_email",
            "generic_email",
            "default_email"
        );
    }

    @Test
    void selectTemplateWithFallback_ShouldReturnFirstAvailableTemplate() {
        // Given
        String tenantId = "tenant1";
        NotificationType type = NotificationType.ORDER_CREATED;
        NotificationChannel channel = NotificationChannel.EMAIL;
        
        // Mock that the second template in priority order exists
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(tenantId, "tenant1_order_created_email", channel))
            .thenReturn(Optional.empty());
        
        NotificationTemplate template = new NotificationTemplate();
        template.setIsActive(true);
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(tenantId, "order_created_email", channel))
            .thenReturn(Optional.of(template));

        // When
        String result = templateSelectionService.selectTemplateWithFallback(tenantId, type, channel);

        // Then
        assertThat(result).isEqualTo("order_created_email");
    }

    @Test
    void selectTemplateWithFallback_ShouldReturnDefaultWhenNoTemplateFound() {
        // Given
        String tenantId = "tenant1";
        NotificationType type = NotificationType.ORDER_CREATED;
        NotificationChannel channel = NotificationChannel.EMAIL;
        
        // Mock that no templates exist
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(eq(tenantId), any(), eq(channel)))
            .thenReturn(Optional.empty());

        // When
        String result = templateSelectionService.selectTemplateWithFallback(tenantId, type, channel);

        // Then
        assertThat(result).isEqualTo("order_created_email");
    }
}
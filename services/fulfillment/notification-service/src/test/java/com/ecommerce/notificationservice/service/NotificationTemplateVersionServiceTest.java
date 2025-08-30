package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationTemplateVersionRequest;
import com.ecommerce.notificationservice.dto.NotificationTemplateVersionResponse;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationTemplate;
import com.ecommerce.notificationservice.entity.NotificationTemplateVersion;
import com.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import com.ecommerce.notificationservice.repository.NotificationTemplateVersionRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateVersionServiceTest {

    @Mock
    private NotificationTemplateVersionRepository templateVersionRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private NotificationTemplateVersionService templateVersionService;

    private String tenantId;
    private String templateKey;
    private NotificationChannel channel;
    private String createdBy;
    private NotificationTemplateVersionRequest testRequest;
    private NotificationTemplateVersion testVersion;
    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        templateKey = "order_created";
        channel = NotificationChannel.EMAIL;
        createdBy = "test-user";

        Map<String, String> variables = new HashMap<>();
        variables.put("orderNumber", "Order number");
        variables.put("customerName", "Customer name");

        testRequest = new NotificationTemplateVersionRequest();
        testRequest.setTemplateKey(templateKey);
        testRequest.setChannel(channel);
        testRequest.setSubject("Order Confirmation - Order #[[${orderNumber}]]");
        testRequest.setContent("Dear [[${customerName}]], your order has been created.");
        testRequest.setChangeDescription("Updated order confirmation message");
        testRequest.setVariables(variables);

        testVersion = new NotificationTemplateVersion(
            tenantId, templateKey, channel, 1,
            "Order Confirmation - Order #[[${orderNumber}]]",
            "Dear [[${customerName}]], your order has been created.",
            createdBy
        );
        testVersion.setId(1L);
        testVersion.setChangeDescription("Updated order confirmation message");
        testVersion.setVariables(variables);

        testTemplate = new NotificationTemplate(
            tenantId, templateKey, channel,
            "Order Confirmation - Order #[[${orderNumber}]]",
            "Dear [[${customerName}]], your order has been created."
        );
        testTemplate.setId(1L);
    }

    @Test
    void createTemplateVersion_ShouldCreateNewVersion() {
        // Given
        when(templateVersionRepository.findMaxVersionNumber(tenantId, templateKey, channel))
            .thenReturn(Optional.of(2));
        when(templateVersionRepository.save(any(NotificationTemplateVersion.class)))
            .thenReturn(testVersion);

        // When
        NotificationTemplateVersionResponse response = templateVersionService
            .createTemplateVersion(tenantId, testRequest, createdBy);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getTemplateKey()).isEqualTo(templateKey);
        assertThat(response.getChannel()).isEqualTo(channel);
        assertThat(response.getCreatedBy()).isEqualTo(createdBy);
        assertThat(response.getChangeDescription()).isEqualTo("Updated order confirmation message");

        verify(templateVersionRepository).findMaxVersionNumber(tenantId, templateKey, channel);
        verify(templateVersionRepository).save(any(NotificationTemplateVersion.class));
    }

    @Test
    void createTemplateVersion_WhenFirstVersion_ShouldCreateVersionOne() {
        // Given
        when(templateVersionRepository.findMaxVersionNumber(tenantId, templateKey, channel))
            .thenReturn(Optional.empty());
        when(templateVersionRepository.save(any(NotificationTemplateVersion.class)))
            .thenReturn(testVersion);

        // When
        NotificationTemplateVersionResponse response = templateVersionService
            .createTemplateVersion(tenantId, testRequest, createdBy);

        // Then
        assertThat(response).isNotNull();
        verify(templateVersionRepository).save(argThat(version -> 
            version.getVersionNumber().equals(1)));
    }

    @Test
    void getTemplateVersions_ShouldReturnAllVersions() {
        // Given
        NotificationTemplateVersion version2 = new NotificationTemplateVersion(
            tenantId, templateKey, channel, 2,
            "Updated subject", "Updated content", createdBy
        );
        version2.setId(2L);

        List<NotificationTemplateVersion> versions = Arrays.asList(version2, testVersion);
        when(templateVersionRepository.findAllVersionsByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel)).thenReturn(versions);

        // When
        List<NotificationTemplateVersionResponse> responses = templateVersionService
            .getTemplateVersions(tenantId, templateKey, channel);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getVersionNumber()).isEqualTo(2);
        assertThat(responses.get(1).getVersionNumber()).isEqualTo(1);

        verify(templateVersionRepository).findAllVersionsByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel);
    }

    @Test
    void getActiveTemplateVersion_WhenExists_ShouldReturnActiveVersion() {
        // Given
        testVersion.setIsActive(true);
        when(templateVersionRepository.findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel)).thenReturn(Optional.of(testVersion));

        // When
        NotificationTemplateVersionResponse response = templateVersionService
            .getActiveTemplateVersion(tenantId, templateKey, channel);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getVersionNumber()).isEqualTo(1);

        verify(templateVersionRepository).findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel);
    }

    @Test
    void getActiveTemplateVersion_WhenNotExists_ShouldThrowException() {
        // Given
        when(templateVersionRepository.findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> templateVersionService
            .getActiveTemplateVersion(tenantId, templateKey, channel))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("No active template version found");

        verify(templateVersionRepository).findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel);
    }

    @Test
    void publishTemplateVersion_ShouldActivateVersionAndDeactivateOld() {
        // Given
        Integer versionNumber = 2;
        String publishedBy = "publisher";

        NotificationTemplateVersion currentActive = new NotificationTemplateVersion(
            tenantId, templateKey, channel, 1, "Old subject", "Old content", createdBy
        );
        currentActive.setIsActive(true);

        NotificationTemplateVersion versionToPublish = new NotificationTemplateVersion(
            tenantId, templateKey, channel, versionNumber, "New subject", "New content", createdBy
        );
        versionToPublish.setId(2L);

        when(templateVersionRepository.findByTenantIdAndTemplateKeyAndChannelAndVersion(
            tenantId, templateKey, channel, versionNumber))
            .thenReturn(Optional.of(versionToPublish));
        when(templateVersionRepository.findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel))
            .thenReturn(Optional.of(currentActive));
        when(templateVersionRepository.save(any(NotificationTemplateVersion.class)))
            .thenReturn(versionToPublish);
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel))
            .thenReturn(Optional.of(testTemplate));

        // When
        NotificationTemplateVersionResponse response = templateVersionService
            .publishTemplateVersion(tenantId, templateKey, channel, versionNumber, publishedBy);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getIsPublished()).isTrue();
        assertThat(response.getPublishedBy()).isEqualTo(publishedBy);
        assertThat(response.getPublishedAt()).isNotNull();

        verify(templateVersionRepository).save(currentActive); // Deactivate old
        verify(templateVersionRepository).save(versionToPublish); // Activate new
        verify(templateRepository).save(testTemplate); // Update main template
        assertThat(currentActive.getIsActive()).isFalse();
    }

    @Test
    void publishTemplateVersion_WhenVersionNotFound_ShouldThrowException() {
        // Given
        Integer versionNumber = 999;
        String publishedBy = "publisher";

        when(templateVersionRepository.findByTenantIdAndTemplateKeyAndChannelAndVersion(
            tenantId, templateKey, channel, versionNumber))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> templateVersionService
            .publishTemplateVersion(tenantId, templateKey, channel, versionNumber, publishedBy))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Template version 999 not found");

        verify(templateVersionRepository).findByTenantIdAndTemplateKeyAndChannelAndVersion(
            tenantId, templateKey, channel, versionNumber);
    }

    @Test
    void rollbackToVersion_ShouldCallPublishTemplateVersion() {
        // Given
        Integer versionNumber = 1;
        String rolledBackBy = "admin";

        NotificationTemplateVersion versionToRollback = new NotificationTemplateVersion(
            tenantId, templateKey, channel, versionNumber, "Old subject", "Old content", createdBy
        );
        versionToRollback.setId(1L);

        when(templateVersionRepository.findByTenantIdAndTemplateKeyAndChannelAndVersion(
            tenantId, templateKey, channel, versionNumber))
            .thenReturn(Optional.of(versionToRollback));
        when(templateVersionRepository.findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel))
            .thenReturn(Optional.empty());
        when(templateVersionRepository.save(any(NotificationTemplateVersion.class)))
            .thenReturn(versionToRollback);
        when(templateRepository.findByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel))
            .thenReturn(Optional.of(testTemplate));

        // When
        NotificationTemplateVersionResponse response = templateVersionService
            .rollbackToVersion(tenantId, templateKey, channel, versionNumber, rolledBackBy);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getPublishedBy()).isEqualTo(rolledBackBy);

        verify(templateVersionRepository).findByTenantIdAndTemplateKeyAndChannelAndVersion(
            tenantId, templateKey, channel, versionNumber);
    }

    @Test
    void getTemplateVersion_WhenExists_ShouldReturnVersion() {
        // Given
        Integer versionNumber = 1;
        when(templateVersionRepository.findByTenantIdAndTemplateKeyAndChannelAndVersion(
            tenantId, templateKey, channel, versionNumber))
            .thenReturn(Optional.of(testVersion));

        // When
        NotificationTemplateVersionResponse response = templateVersionService
            .getTemplateVersion(tenantId, templateKey, channel, versionNumber);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getVersionNumber()).isEqualTo(versionNumber);
        assertThat(response.getTenantId()).isEqualTo(tenantId);

        verify(templateVersionRepository).findByTenantIdAndTemplateKeyAndChannelAndVersion(
            tenantId, templateKey, channel, versionNumber);
    }

    @Test
    void getPublishedVersions_ShouldReturnPublishedVersions() {
        // Given
        testVersion.setIsPublished(true);
        testVersion.setPublishedAt(LocalDateTime.now());
        testVersion.setPublishedBy("publisher");

        List<NotificationTemplateVersion> publishedVersions = Arrays.asList(testVersion);
        when(templateVersionRepository.findPublishedVersionsByTenantId(tenantId))
            .thenReturn(publishedVersions);

        // When
        List<NotificationTemplateVersionResponse> responses = templateVersionService
            .getPublishedVersions(tenantId);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIsPublished()).isTrue();
        assertThat(responses.get(0).getPublishedBy()).isEqualTo("publisher");

        verify(templateVersionRepository).findPublishedVersionsByTenantId(tenantId);
    }
}
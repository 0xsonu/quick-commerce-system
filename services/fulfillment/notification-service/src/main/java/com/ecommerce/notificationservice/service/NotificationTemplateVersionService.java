package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationTemplateVersionRequest;
import com.ecommerce.notificationservice.dto.NotificationTemplateVersionResponse;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationTemplate;
import com.ecommerce.notificationservice.entity.NotificationTemplateVersion;
import com.ecommerce.notificationservice.repository.NotificationTemplateRepository;
import com.ecommerce.notificationservice.repository.NotificationTemplateVersionRepository;
import com.ecommerce.shared.utils.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationTemplateVersionService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationTemplateVersionService.class);

    private final NotificationTemplateVersionRepository templateVersionRepository;
    private final NotificationTemplateRepository templateRepository;

    @Autowired
    public NotificationTemplateVersionService(NotificationTemplateVersionRepository templateVersionRepository,
                                            NotificationTemplateRepository templateRepository) {
        this.templateVersionRepository = templateVersionRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * Create a new template version
     */
    public NotificationTemplateVersionResponse createTemplateVersion(String tenantId, 
                                                                   NotificationTemplateVersionRequest request,
                                                                   String createdBy) {
        logger.info("Creating new template version for tenant: {}, template: {}, channel: {}", 
                   tenantId, request.getTemplateKey(), request.getChannel());

        // Get next version number
        Integer nextVersion = getNextVersionNumber(tenantId, request.getTemplateKey(), request.getChannel());

        NotificationTemplateVersion version = new NotificationTemplateVersion(
            tenantId, request.getTemplateKey(), request.getChannel(),
            nextVersion, request.getSubject(), request.getContent(), createdBy
        );
        
        version.setChangeDescription(request.getChangeDescription());
        version.setVariables(request.getVariables());

        NotificationTemplateVersion savedVersion = templateVersionRepository.save(version);
        logger.debug("Created template version {} for tenant: {}", nextVersion, tenantId);

        return mapToResponse(savedVersion);
    }

    /**
     * Get all versions for a template
     */
    @Transactional(readOnly = true)
    public List<NotificationTemplateVersionResponse> getTemplateVersions(String tenantId, 
                                                                        String templateKey, 
                                                                        NotificationChannel channel) {
        logger.debug("Getting template versions for tenant: {}, template: {}, channel: {}", 
                    tenantId, templateKey, channel);

        List<NotificationTemplateVersion> versions = templateVersionRepository
            .findAllVersionsByTenantIdAndTemplateKeyAndChannel(tenantId, templateKey, channel);

        return versions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active template version
     */
    @Transactional(readOnly = true)
    public NotificationTemplateVersionResponse getActiveTemplateVersion(String tenantId, 
                                                                       String templateKey, 
                                                                       NotificationChannel channel) {
        logger.debug("Getting active template version for tenant: {}, template: {}, channel: {}", 
                    tenantId, templateKey, channel);

        Optional<NotificationTemplateVersion> activeVersion = templateVersionRepository
            .findActiveVersionByTenantIdAndTemplateKeyAndChannel(tenantId, templateKey, channel);

        if (activeVersion.isEmpty()) {
            throw new ResourceNotFoundException(
                String.format("No active template version found for tenant: %s, template: %s, channel: %s", 
                             tenantId, templateKey, channel));
        }

        return mapToResponse(activeVersion.get());
    }

    /**
     * Publish a template version (make it active)
     */
    public NotificationTemplateVersionResponse publishTemplateVersion(String tenantId, 
                                                                     String templateKey, 
                                                                     NotificationChannel channel,
                                                                     Integer versionNumber,
                                                                     String publishedBy) {
        logger.info("Publishing template version {} for tenant: {}, template: {}, channel: {}", 
                   versionNumber, tenantId, templateKey, channel);

        // Find the version to publish
        Optional<NotificationTemplateVersion> versionToPublish = templateVersionRepository
            .findByTenantIdAndTemplateKeyAndChannelAndVersion(tenantId, templateKey, channel, versionNumber);

        if (versionToPublish.isEmpty()) {
            throw new ResourceNotFoundException(
                String.format("Template version %d not found for tenant: %s, template: %s, channel: %s", 
                             versionNumber, tenantId, templateKey, channel));
        }

        // Deactivate current active version
        Optional<NotificationTemplateVersion> currentActive = templateVersionRepository
            .findActiveVersionByTenantIdAndTemplateKeyAndChannel(tenantId, templateKey, channel);
        
        if (currentActive.isPresent()) {
            currentActive.get().setIsActive(false);
            templateVersionRepository.save(currentActive.get());
        }

        // Activate the new version
        NotificationTemplateVersion newActive = versionToPublish.get();
        newActive.setIsActive(true);
        newActive.setIsPublished(true);
        newActive.setPublishedAt(LocalDateTime.now());
        newActive.setPublishedBy(publishedBy);

        NotificationTemplateVersion savedVersion = templateVersionRepository.save(newActive);

        // Update the main template table for backward compatibility
        updateMainTemplate(savedVersion);

        logger.info("Published template version {} for tenant: {}", versionNumber, tenantId);
        return mapToResponse(savedVersion);
    }

    /**
     * Rollback to a previous template version
     */
    public NotificationTemplateVersionResponse rollbackToVersion(String tenantId, 
                                                               String templateKey, 
                                                               NotificationChannel channel,
                                                               Integer versionNumber,
                                                               String rolledBackBy) {
        logger.info("Rolling back to template version {} for tenant: {}, template: {}, channel: {}", 
                   versionNumber, tenantId, templateKey, channel);

        return publishTemplateVersion(tenantId, templateKey, channel, versionNumber, rolledBackBy);
    }

    /**
     * Get a specific template version
     */
    @Transactional(readOnly = true)
    public NotificationTemplateVersionResponse getTemplateVersion(String tenantId, 
                                                                String templateKey, 
                                                                NotificationChannel channel,
                                                                Integer versionNumber) {
        logger.debug("Getting template version {} for tenant: {}, template: {}, channel: {}", 
                    versionNumber, tenantId, templateKey, channel);

        Optional<NotificationTemplateVersion> version = templateVersionRepository
            .findByTenantIdAndTemplateKeyAndChannelAndVersion(tenantId, templateKey, channel, versionNumber);

        if (version.isEmpty()) {
            throw new ResourceNotFoundException(
                String.format("Template version %d not found for tenant: %s, template: %s, channel: %s", 
                             versionNumber, tenantId, templateKey, channel));
        }

        return mapToResponse(version.get());
    }

    /**
     * Get all published versions for a tenant
     */
    @Transactional(readOnly = true)
    public List<NotificationTemplateVersionResponse> getPublishedVersions(String tenantId) {
        logger.debug("Getting published template versions for tenant: {}", tenantId);

        List<NotificationTemplateVersion> publishedVersions = templateVersionRepository
            .findPublishedVersionsByTenantId(tenantId);

        return publishedVersions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Integer getNextVersionNumber(String tenantId, String templateKey, NotificationChannel channel) {
        Optional<Integer> maxVersion = templateVersionRepository
            .findMaxVersionNumber(tenantId, templateKey, channel);
        return maxVersion.orElse(0) + 1;
    }

    private void updateMainTemplate(NotificationTemplateVersion version) {
        Optional<NotificationTemplate> mainTemplate = templateRepository
            .findByTenantIdAndTemplateKeyAndChannel(version.getTenantId(), 
                                                   version.getTemplateKey(), 
                                                   version.getChannel());

        if (mainTemplate.isPresent()) {
            NotificationTemplate template = mainTemplate.get();
            template.setSubject(version.getSubject());
            template.setContent(version.getContent());
            template.setVariables(version.getVariables());
            templateRepository.save(template);
        } else {
            // Create new main template if it doesn't exist
            NotificationTemplate newTemplate = new NotificationTemplate(
                version.getTenantId(), version.getTemplateKey(), version.getChannel(),
                version.getSubject(), version.getContent()
            );
            newTemplate.setVariables(version.getVariables());
            templateRepository.save(newTemplate);
        }
    }

    private NotificationTemplateVersionResponse mapToResponse(NotificationTemplateVersion version) {
        NotificationTemplateVersionResponse response = new NotificationTemplateVersionResponse();
        response.setId(version.getId());
        response.setTenantId(version.getTenantId());
        response.setTemplateKey(version.getTemplateKey());
        response.setChannel(version.getChannel());
        response.setVersionNumber(version.getVersionNumber());
        response.setSubject(version.getSubject());
        response.setContent(version.getContent());
        response.setIsActive(version.getIsActive());
        response.setIsPublished(version.getIsPublished());
        response.setChangeDescription(version.getChangeDescription());
        response.setCreatedBy(version.getCreatedBy());
        response.setPublishedAt(version.getPublishedAt());
        response.setPublishedBy(version.getPublishedBy());
        response.setVariables(version.getVariables());
        response.setCreatedAt(version.getCreatedAt());
        response.setUpdatedAt(version.getUpdatedAt());
        return response;
    }
}
package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationABTestRequest;
import com.ecommerce.notificationservice.dto.NotificationABTestResponse;
import com.ecommerce.notificationservice.entity.NotificationABTest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationTemplateVersion;
import com.ecommerce.notificationservice.repository.NotificationABTestRepository;
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
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationABTestService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationABTestService.class);

    private final NotificationABTestRepository abTestRepository;
    private final NotificationTemplateVersionRepository templateVersionRepository;
    private final Random random = new Random();

    @Autowired
    public NotificationABTestService(NotificationABTestRepository abTestRepository,
                                   NotificationTemplateVersionRepository templateVersionRepository) {
        this.abTestRepository = abTestRepository;
        this.templateVersionRepository = templateVersionRepository;
    }

    /**
     * Create a new A/B test
     */
    public NotificationABTestResponse createABTest(String tenantId, 
                                                 NotificationABTestRequest request,
                                                 String createdBy) {
        logger.info("Creating A/B test '{}' for tenant: {}, template: {}, channel: {}", 
                   request.getTestName(), tenantId, request.getTemplateKey(), request.getChannel());

        // Validate that both template versions exist
        validateTemplateVersionExists(tenantId, request.getControlVersionId());
        validateTemplateVersionExists(tenantId, request.getVariantVersionId());

        // Check if test name already exists for this tenant
        Optional<NotificationABTest> existingTest = abTestRepository
            .findByTenantIdAndTestName(tenantId, request.getTestName());
        if (existingTest.isPresent()) {
            throw new IllegalArgumentException("A/B test with name '" + request.getTestName() + "' already exists");
        }

        // Check if there's already an active test for this template and channel
        Optional<NotificationABTest> activeTest = abTestRepository
            .findActiveTestByTenantIdAndTemplateKeyAndChannel(
                tenantId, request.getTemplateKey(), request.getChannel(), LocalDateTime.now());
        if (activeTest.isPresent()) {
            throw new IllegalArgumentException(
                "There is already an active A/B test for template '" + request.getTemplateKey() + 
                "' and channel '" + request.getChannel() + "'");
        }

        NotificationABTest abTest = new NotificationABTest(
            tenantId, request.getTestName(), request.getTemplateKey(),
            request.getChannel(), request.getControlVersionId(), request.getVariantVersionId(),
            request.getTrafficSplitPercentage(), request.getStartDate(), createdBy
        );

        abTest.setEndDate(request.getEndDate());
        abTest.setDescription(request.getDescription());
        abTest.setSuccessMetric(request.getSuccessMetric());

        NotificationABTest savedTest = abTestRepository.save(abTest);
        logger.info("Created A/B test '{}' with ID: {}", request.getTestName(), savedTest.getId());

        return mapToResponse(savedTest);
    }

    /**
     * Get active A/B test for template selection
     */
    @Transactional(readOnly = true)
    public Optional<NotificationABTest> getActiveABTest(String tenantId, 
                                                       String templateKey, 
                                                       NotificationChannel channel) {
        return abTestRepository.findActiveTestByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel, LocalDateTime.now());
    }

    /**
     * Determine which template version to use based on A/B test
     */
    @Transactional(readOnly = true)
    public Long selectTemplateVersionForABTest(String tenantId, 
                                              String templateKey, 
                                              NotificationChannel channel,
                                              String userId) {
        Optional<NotificationABTest> activeTest = getActiveABTest(tenantId, templateKey, channel);
        
        if (activeTest.isEmpty()) {
            // No active test, use the active template version
            Optional<NotificationTemplateVersion> activeVersion = templateVersionRepository
                .findActiveVersionByTenantIdAndTemplateKeyAndChannel(tenantId, templateKey, channel);
            return activeVersion.map(NotificationTemplateVersion::getId).orElse(null);
        }

        NotificationABTest test = activeTest.get();
        
        // Use user ID hash to ensure consistent assignment
        int userHash = Math.abs(userId.hashCode());
        int bucket = userHash % 100;
        
        if (bucket < test.getTrafficSplitPercentage()) {
            // User gets variant
            logger.debug("User {} assigned to variant version {} for test '{}'", 
                        userId, test.getVariantVersionId(), test.getTestName());
            return test.getVariantVersionId();
        } else {
            // User gets control
            logger.debug("User {} assigned to control version {} for test '{}'", 
                        userId, test.getControlVersionId(), test.getTestName());
            return test.getControlVersionId();
        }
    }

    /**
     * Record A/B test metrics
     */
    public void recordABTestMetric(String tenantId, 
                                  String templateKey, 
                                  NotificationChannel channel,
                                  Long templateVersionId,
                                  String metricType) {
        Optional<NotificationABTest> activeTest = getActiveABTest(tenantId, templateKey, channel);
        
        if (activeTest.isEmpty()) {
            return; // No active test
        }

        NotificationABTest test = activeTest.get();
        
        if (templateVersionId.equals(test.getControlVersionId())) {
            if ("SENT".equals(metricType)) {
                test.setControlSentCount(test.getControlSentCount() + 1);
            } else if ("SUCCESS".equals(metricType)) {
                test.setControlSuccessCount(test.getControlSuccessCount() + 1);
            }
        } else if (templateVersionId.equals(test.getVariantVersionId())) {
            if ("SENT".equals(metricType)) {
                test.setVariantSentCount(test.getVariantSentCount() + 1);
            } else if ("SUCCESS".equals(metricType)) {
                test.setVariantSuccessCount(test.getVariantSuccessCount() + 1);
            }
        }

        test.setLastStatsUpdate(LocalDateTime.now());
        abTestRepository.save(test);
    }

    /**
     * Get A/B test results
     */
    @Transactional(readOnly = true)
    public NotificationABTestResponse getABTestResults(String tenantId, String testName) {
        logger.debug("Getting A/B test results for tenant: {}, test: {}", tenantId, testName);

        Optional<NotificationABTest> test = abTestRepository.findByTenantIdAndTestName(tenantId, testName);
        if (test.isEmpty()) {
            throw new ResourceNotFoundException("A/B test '" + testName + "' not found");
        }

        return mapToResponseWithCalculations(test.get());
    }

    /**
     * Stop an A/B test
     */
    public NotificationABTestResponse stopABTest(String tenantId, 
                                               String testName, 
                                               String stoppedBy,
                                               String stopReason) {
        logger.info("Stopping A/B test '{}' for tenant: {}", testName, tenantId);

        Optional<NotificationABTest> test = abTestRepository.findByTenantIdAndTestName(tenantId, testName);
        if (test.isEmpty()) {
            throw new ResourceNotFoundException("A/B test '" + testName + "' not found");
        }

        NotificationABTest abTest = test.get();
        abTest.setIsActive(false);
        abTest.setStoppedAt(LocalDateTime.now());
        abTest.setStoppedBy(stoppedBy);
        abTest.setStopReason(stopReason);

        NotificationABTest savedTest = abTestRepository.save(abTest);
        logger.info("Stopped A/B test '{}' for tenant: {}", testName, tenantId);

        return mapToResponseWithCalculations(savedTest);
    }

    /**
     * Get all A/B tests for a tenant
     */
    @Transactional(readOnly = true)
    public List<NotificationABTestResponse> getAllABTests(String tenantId) {
        logger.debug("Getting all A/B tests for tenant: {}", tenantId);

        List<NotificationABTest> tests = abTestRepository.findAllTestsByTenantId(tenantId);
        return tests.stream()
                .map(this::mapToResponseWithCalculations)
                .collect(Collectors.toList());
    }

    /**
     * Get active A/B tests for a tenant
     */
    @Transactional(readOnly = true)
    public List<NotificationABTestResponse> getActiveABTests(String tenantId) {
        logger.debug("Getting active A/B tests for tenant: {}", tenantId);

        List<NotificationABTest> activeTests = abTestRepository
            .findActiveTestsByTenantId(tenantId, LocalDateTime.now());
        return activeTests.stream()
                .map(this::mapToResponseWithCalculations)
                .collect(Collectors.toList());
    }

    /**
     * Clean up expired A/B tests
     */
    @Transactional
    public void cleanupExpiredTests() {
        logger.debug("Cleaning up expired A/B tests");

        List<NotificationABTest> expiredTests = abTestRepository
            .findExpiredActiveTests(LocalDateTime.now());

        for (NotificationABTest test : expiredTests) {
            test.setIsActive(false);
            test.setStoppedAt(LocalDateTime.now());
            test.setStopReason("Automatically stopped - test period expired");
            abTestRepository.save(test);
            
            logger.info("Automatically stopped expired A/B test '{}' for tenant: {}", 
                       test.getTestName(), test.getTenantId());
        }
    }

    private void validateTemplateVersionExists(String tenantId, Long versionId) {
        Optional<NotificationTemplateVersion> version = templateVersionRepository.findById(versionId);
        if (version.isEmpty() || !version.get().getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Template version " + versionId + " not found for tenant " + tenantId);
        }
    }

    private NotificationABTestResponse mapToResponse(NotificationABTest test) {
        NotificationABTestResponse response = new NotificationABTestResponse();
        response.setId(test.getId());
        response.setTenantId(test.getTenantId());
        response.setTestName(test.getTestName());
        response.setTemplateKey(test.getTemplateKey());
        response.setChannel(test.getChannel());
        response.setControlVersionId(test.getControlVersionId());
        response.setVariantVersionId(test.getVariantVersionId());
        response.setTrafficSplitPercentage(test.getTrafficSplitPercentage());
        response.setStartDate(test.getStartDate());
        response.setEndDate(test.getEndDate());
        response.setIsActive(test.getIsActive());
        response.setDescription(test.getDescription());
        response.setSuccessMetric(test.getSuccessMetric());
        response.setCreatedBy(test.getCreatedBy());
        response.setStoppedBy(test.getStoppedBy());
        response.setStoppedAt(test.getStoppedAt());
        response.setStopReason(test.getStopReason());
        response.setControlSentCount(test.getControlSentCount());
        response.setVariantSentCount(test.getVariantSentCount());
        response.setControlSuccessCount(test.getControlSuccessCount());
        response.setVariantSuccessCount(test.getVariantSuccessCount());
        response.setLastStatsUpdate(test.getLastStatsUpdate());
        response.setCreatedAt(test.getCreatedAt());
        response.setUpdatedAt(test.getUpdatedAt());
        return response;
    }

    private NotificationABTestResponse mapToResponseWithCalculations(NotificationABTest test) {
        NotificationABTestResponse response = mapToResponse(test);
        
        // Calculate success rates
        if (test.getControlSentCount() > 0) {
            response.setControlSuccessRate(
                (double) test.getControlSuccessCount() / test.getControlSentCount() * 100);
        }
        
        if (test.getVariantSentCount() > 0) {
            response.setVariantSuccessRate(
                (double) test.getVariantSuccessCount() / test.getVariantSentCount() * 100);
        }
        
        // Calculate improvement percentage
        if (response.getControlSuccessRate() != null && response.getVariantSuccessRate() != null 
            && response.getControlSuccessRate() > 0) {
            response.setImprovementPercentage(
                ((response.getVariantSuccessRate() - response.getControlSuccessRate()) 
                 / response.getControlSuccessRate()) * 100);
        }
        
        // Simple statistical significance check (basic implementation)
        response.setIsStatisticallySignificant(
            isStatisticallySignificant(test.getControlSentCount(), test.getControlSuccessCount(),
                                     test.getVariantSentCount(), test.getVariantSuccessCount()));
        
        return response;
    }

    private Boolean isStatisticallySignificant(Long controlSent, Long controlSuccess,
                                             Long variantSent, Long variantSuccess) {
        // Basic statistical significance check using minimum sample size
        // In a real implementation, you would use proper statistical tests like Chi-square
        if (controlSent < 100 || variantSent < 100) {
            return false; // Not enough data
        }
        
        if (controlSent == 0 || variantSent == 0) {
            return false;
        }
        
        double controlRate = (double) controlSuccess / controlSent;
        double variantRate = (double) variantSuccess / variantSent;
        
        // Simple threshold-based significance (in practice, use proper statistical tests)
        return Math.abs(variantRate - controlRate) > 0.02; // 2% difference threshold
    }
}
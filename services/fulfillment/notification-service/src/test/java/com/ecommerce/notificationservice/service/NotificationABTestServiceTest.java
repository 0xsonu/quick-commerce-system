package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.NotificationABTestRequest;
import com.ecommerce.notificationservice.dto.NotificationABTestResponse;
import com.ecommerce.notificationservice.entity.NotificationABTest;
import com.ecommerce.notificationservice.entity.NotificationChannel;
import com.ecommerce.notificationservice.entity.NotificationTemplateVersion;
import com.ecommerce.notificationservice.repository.NotificationABTestRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationABTestServiceTest {

    @Mock
    private NotificationABTestRepository abTestRepository;

    @Mock
    private NotificationTemplateVersionRepository templateVersionRepository;

    @InjectMocks
    private NotificationABTestService abTestService;

    private String tenantId;
    private String templateKey;
    private NotificationChannel channel;
    private String createdBy;
    private NotificationABTestRequest testRequest;
    private NotificationABTest testABTest;
    private NotificationTemplateVersion controlVersion;
    private NotificationTemplateVersion variantVersion;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        templateKey = "order_created";
        channel = NotificationChannel.EMAIL;
        createdBy = "test-user";

        controlVersion = new NotificationTemplateVersion(
            tenantId, templateKey, channel, 1,
            "Control Subject", "Control Content", createdBy
        );
        controlVersion.setId(1L);

        variantVersion = new NotificationTemplateVersion(
            tenantId, templateKey, channel, 2,
            "Variant Subject", "Variant Content", createdBy
        );
        variantVersion.setId(2L);

        testRequest = new NotificationABTestRequest();
        testRequest.setTestName("Email Subject Test");
        testRequest.setTemplateKey(templateKey);
        testRequest.setChannel(channel);
        testRequest.setControlVersionId(1L);
        testRequest.setVariantVersionId(2L);
        testRequest.setTrafficSplitPercentage(50);
        testRequest.setStartDate(LocalDateTime.now().plusHours(1));
        testRequest.setEndDate(LocalDateTime.now().plusDays(7));
        testRequest.setDescription("Testing different email subjects");
        testRequest.setSuccessMetric("OPEN_RATE");

        testABTest = new NotificationABTest(
            tenantId, "Email Subject Test", templateKey, channel,
            1L, 2L, 50, LocalDateTime.now().plusHours(1), createdBy
        );
        testABTest.setId(1L);
        testABTest.setEndDate(LocalDateTime.now().plusDays(7));
        testABTest.setDescription("Testing different email subjects");
        testABTest.setSuccessMetric("OPEN_RATE");
    }

    @Test
    void createABTest_WhenValidRequest_ShouldCreateTest() {
        // Given
        when(templateVersionRepository.findById(1L)).thenReturn(Optional.of(controlVersion));
        when(templateVersionRepository.findById(2L)).thenReturn(Optional.of(variantVersion));
        when(abTestRepository.findByTenantIdAndTestName(tenantId, "Email Subject Test"))
            .thenReturn(Optional.empty());
        when(abTestRepository.findActiveTestByTenantIdAndTemplateKeyAndChannel(
            eq(tenantId), eq(templateKey), eq(channel), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());
        when(abTestRepository.save(any(NotificationABTest.class))).thenReturn(testABTest);

        // When
        NotificationABTestResponse response = abTestService.createABTest(tenantId, testRequest, createdBy);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getTestName()).isEqualTo("Email Subject Test");
        assertThat(response.getTemplateKey()).isEqualTo(templateKey);
        assertThat(response.getChannel()).isEqualTo(channel);
        assertThat(response.getTrafficSplitPercentage()).isEqualTo(50);
        assertThat(response.getCreatedBy()).isEqualTo(createdBy);

        verify(abTestRepository).save(any(NotificationABTest.class));
    }

    @Test
    void createABTest_WhenControlVersionNotFound_ShouldThrowException() {
        // Given
        when(templateVersionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> abTestService.createABTest(tenantId, testRequest, createdBy))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Template version 1 not found");

        verify(templateVersionRepository).findById(1L);
        verify(abTestRepository, never()).save(any());
    }

    @Test
    void createABTest_WhenVariantVersionNotFound_ShouldThrowException() {
        // Given
        when(templateVersionRepository.findById(1L)).thenReturn(Optional.of(controlVersion));
        when(templateVersionRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> abTestService.createABTest(tenantId, testRequest, createdBy))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Template version 2 not found");

        verify(templateVersionRepository).findById(2L);
        verify(abTestRepository, never()).save(any());
    }

    @Test
    void createABTest_WhenTestNameExists_ShouldThrowException() {
        // Given
        when(templateVersionRepository.findById(1L)).thenReturn(Optional.of(controlVersion));
        when(templateVersionRepository.findById(2L)).thenReturn(Optional.of(variantVersion));
        when(abTestRepository.findByTenantIdAndTestName(tenantId, "Email Subject Test"))
            .thenReturn(Optional.of(testABTest));

        // When & Then
        assertThatThrownBy(() -> abTestService.createABTest(tenantId, testRequest, createdBy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("A/B test with name 'Email Subject Test' already exists");

        verify(abTestRepository, never()).save(any());
    }

    @Test
    void createABTest_WhenActiveTestExists_ShouldThrowException() {
        // Given
        when(templateVersionRepository.findById(1L)).thenReturn(Optional.of(controlVersion));
        when(templateVersionRepository.findById(2L)).thenReturn(Optional.of(variantVersion));
        when(abTestRepository.findByTenantIdAndTestName(tenantId, "Email Subject Test"))
            .thenReturn(Optional.empty());
        when(abTestRepository.findActiveTestByTenantIdAndTemplateKeyAndChannel(
            eq(tenantId), eq(templateKey), eq(channel), any(LocalDateTime.class)))
            .thenReturn(Optional.of(testABTest));

        // When & Then
        assertThatThrownBy(() -> abTestService.createABTest(tenantId, testRequest, createdBy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("There is already an active A/B test");

        verify(abTestRepository, never()).save(any());
    }

    @Test
    void getActiveABTest_WhenExists_ShouldReturnTest() {
        // Given
        when(abTestRepository.findActiveTestByTenantIdAndTemplateKeyAndChannel(
            eq(tenantId), eq(templateKey), eq(channel), any(LocalDateTime.class)))
            .thenReturn(Optional.of(testABTest));

        // When
        Optional<NotificationABTest> result = abTestService.getActiveABTest(tenantId, templateKey, channel);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTestName()).isEqualTo("Email Subject Test");

        verify(abTestRepository).findActiveTestByTenantIdAndTemplateKeyAndChannel(
            eq(tenantId), eq(templateKey), eq(channel), any(LocalDateTime.class));
    }

    @Test
    void selectTemplateVersionForABTest_WhenNoActiveTest_ShouldReturnActiveVersion() {
        // Given
        String userId = "user123";
        when(abTestRepository.findActiveTestByTenantIdAndTemplateKeyAndChannel(
            eq(tenantId), eq(templateKey), eq(channel), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());
        when(templateVersionRepository.findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel))
            .thenReturn(Optional.of(controlVersion));

        // When
        Long selectedVersionId = abTestService.selectTemplateVersionForABTest(
            tenantId, templateKey, channel, userId);

        // Then
        assertThat(selectedVersionId).isEqualTo(1L);

        verify(templateVersionRepository).findActiveVersionByTenantIdAndTemplateKeyAndChannel(
            tenantId, templateKey, channel);
    }

    @Test
    void selectTemplateVersionForABTest_WhenActiveTestExists_ShouldSelectBasedOnUserId() {
        // Given
        String userId = "user123"; // This should hash to a consistent bucket
        when(abTestRepository.findActiveTestByTenantIdAndTemplateKeyAndChannel(
            eq(tenantId), eq(templateKey), eq(channel), any(LocalDateTime.class)))
            .thenReturn(Optional.of(testABTest));

        // When
        Long selectedVersionId = abTestService.selectTemplateVersionForABTest(
            tenantId, templateKey, channel, userId);

        // Then
        assertThat(selectedVersionId).isIn(1L, 2L); // Should be either control or variant
        
        // Test consistency - same user should always get same version
        Long selectedVersionId2 = abTestService.selectTemplateVersionForABTest(
            tenantId, templateKey, channel, userId);
        assertThat(selectedVersionId2).isEqualTo(selectedVersionId);
    }

    @Test
    void recordABTestMetric_WhenActiveTestExists_ShouldUpdateMetrics() {
        // Given
        when(abTestRepository.findActiveTestByTenantIdAndTemplateKeyAndChannel(
            eq(tenantId), eq(templateKey), eq(channel), any(LocalDateTime.class)))
            .thenReturn(Optional.of(testABTest));
        when(abTestRepository.save(any(NotificationABTest.class))).thenReturn(testABTest);

        // When
        abTestService.recordABTestMetric(tenantId, templateKey, channel, 1L, "SENT");

        // Then
        verify(abTestRepository).save(testABTest);
        assertThat(testABTest.getControlSentCount()).isEqualTo(1L);
        assertThat(testABTest.getLastStatsUpdate()).isNotNull();
    }

    @Test
    void recordABTestMetric_WhenNoActiveTest_ShouldDoNothing() {
        // Given
        when(abTestRepository.findActiveTestByTenantIdAndTemplateKeyAndChannel(
            eq(tenantId), eq(templateKey), eq(channel), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // When
        abTestService.recordABTestMetric(tenantId, templateKey, channel, 1L, "SENT");

        // Then
        verify(abTestRepository, never()).save(any());
    }

    @Test
    void getABTestResults_WhenTestExists_ShouldReturnResults() {
        // Given
        testABTest.setControlSentCount(100L);
        testABTest.setControlSuccessCount(20L);
        testABTest.setVariantSentCount(100L);
        testABTest.setVariantSuccessCount(25L);

        when(abTestRepository.findByTenantIdAndTestName(tenantId, "Email Subject Test"))
            .thenReturn(Optional.of(testABTest));

        // When
        NotificationABTestResponse response = abTestService.getABTestResults(tenantId, "Email Subject Test");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getControlSuccessRate()).isEqualTo(20.0);
        assertThat(response.getVariantSuccessRate()).isEqualTo(25.0);
        assertThat(response.getImprovementPercentage()).isEqualTo(25.0); // (25-20)/20 * 100
        assertThat(response.getIsStatisticallySignificant()).isTrue();

        verify(abTestRepository).findByTenantIdAndTestName(tenantId, "Email Subject Test");
    }

    @Test
    void getABTestResults_WhenTestNotFound_ShouldThrowException() {
        // Given
        when(abTestRepository.findByTenantIdAndTestName(tenantId, "Nonexistent Test"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> abTestService.getABTestResults(tenantId, "Nonexistent Test"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("A/B test 'Nonexistent Test' not found");

        verify(abTestRepository).findByTenantIdAndTestName(tenantId, "Nonexistent Test");
    }

    @Test
    void stopABTest_WhenTestExists_ShouldStopTest() {
        // Given
        String stoppedBy = "admin";
        String stopReason = "Test completed";
        when(abTestRepository.findByTenantIdAndTestName(tenantId, "Email Subject Test"))
            .thenReturn(Optional.of(testABTest));
        when(abTestRepository.save(any(NotificationABTest.class))).thenReturn(testABTest);

        // When
        NotificationABTestResponse response = abTestService.stopABTest(
            tenantId, "Email Subject Test", stoppedBy, stopReason);

        // Then
        assertThat(response).isNotNull();
        assertThat(testABTest.getIsActive()).isFalse();
        assertThat(testABTest.getStoppedBy()).isEqualTo(stoppedBy);
        assertThat(testABTest.getStopReason()).isEqualTo(stopReason);
        assertThat(testABTest.getStoppedAt()).isNotNull();

        verify(abTestRepository).save(testABTest);
    }

    @Test
    void getAllABTests_ShouldReturnAllTests() {
        // Given
        List<NotificationABTest> tests = Arrays.asList(testABTest);
        when(abTestRepository.findAllTestsByTenantId(tenantId)).thenReturn(tests);

        // When
        List<NotificationABTestResponse> responses = abTestService.getAllABTests(tenantId);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTestName()).isEqualTo("Email Subject Test");

        verify(abTestRepository).findAllTestsByTenantId(tenantId);
    }

    @Test
    void getActiveABTests_ShouldReturnActiveTests() {
        // Given
        List<NotificationABTest> activeTests = Arrays.asList(testABTest);
        when(abTestRepository.findActiveTestsByTenantId(eq(tenantId), any(LocalDateTime.class)))
            .thenReturn(activeTests);

        // When
        List<NotificationABTestResponse> responses = abTestService.getActiveABTests(tenantId);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTestName()).isEqualTo("Email Subject Test");

        verify(abTestRepository).findActiveTestsByTenantId(eq(tenantId), any(LocalDateTime.class));
    }

    @Test
    void cleanupExpiredTests_ShouldDeactivateExpiredTests() {
        // Given
        NotificationABTest expiredTest = new NotificationABTest(
            tenantId, "Expired Test", templateKey, channel,
            1L, 2L, 50, LocalDateTime.now().minusDays(10), createdBy
        );
        expiredTest.setEndDate(LocalDateTime.now().minusDays(1));
        expiredTest.setIsActive(true);

        List<NotificationABTest> expiredTests = Arrays.asList(expiredTest);
        when(abTestRepository.findExpiredActiveTests(any(LocalDateTime.class)))
            .thenReturn(expiredTests);
        when(abTestRepository.save(any(NotificationABTest.class))).thenReturn(expiredTest);

        // When
        abTestService.cleanupExpiredTests();

        // Then
        verify(abTestRepository).save(expiredTest);
        assertThat(expiredTest.getIsActive()).isFalse();
        assertThat(expiredTest.getStoppedAt()).isNotNull();
        assertThat(expiredTest.getStopReason()).contains("Automatically stopped");
    }
}
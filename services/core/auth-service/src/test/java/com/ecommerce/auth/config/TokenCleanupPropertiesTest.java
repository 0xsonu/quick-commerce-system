package com.ecommerce.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TokenCleanupProperties configuration binding
 */
@SpringBootTest(classes = TokenCleanupProperties.class)
@EnableConfigurationProperties(TokenCleanupProperties.class)
@ActiveProfiles("test")
class TokenCleanupPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        // Given
        TokenCleanupProperties properties = new TokenCleanupProperties();

        // Then
        assertThat(properties.getCronExpression()).isEqualTo("0 0 2 * * ?");
        assertThat(properties.getRevokedTokenRetentionPeriod()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBatchSize()).isEqualTo(1000);
    }

    @SpringBootTest(classes = TokenCleanupProperties.class)
    @EnableConfigurationProperties(TokenCleanupProperties.class)
    @TestPropertySource(properties = {
        "auth.token.cleanup.enabled=false",
        "auth.token.cleanup.cron-expression=0 0 3 * * ?",
        "auth.token.cleanup.revoked-token-retention-period=P45D",
        "auth.token.cleanup.batch-size=500"
    })
    static class CustomPropertiesTest {

        @Autowired
        private TokenCleanupProperties properties;

        @Test
        void shouldBindCustomProperties() {
            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getCronExpression()).isEqualTo("0 0 3 * * ?");
            assertThat(properties.getRevokedTokenRetentionPeriod()).isEqualTo(Duration.ofDays(45));
            assertThat(properties.getBatchSize()).isEqualTo(500);
        }
    }
}
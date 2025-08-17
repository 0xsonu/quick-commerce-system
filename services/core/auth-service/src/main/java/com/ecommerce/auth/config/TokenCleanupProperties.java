package com.ecommerce.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for token cleanup job
 */
@ConfigurationProperties(prefix = "auth.token.cleanup")
public class TokenCleanupProperties {

    /**
     * Cron expression for cleanup job scheduling
     * Default: Run every day at 2 AM
     */
    private String cronExpression = "0 0 2 * * ?";

    /**
     * Age threshold for cleaning up revoked tokens
     * Default: 30 days
     */
    private Duration revokedTokenRetentionPeriod = Duration.ofDays(30);

    /**
     * Whether the cleanup job is enabled
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Batch size for cleanup operations to avoid memory issues
     * Default: 1000
     */
    private int batchSize = 1000;

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Duration getRevokedTokenRetentionPeriod() {
        return revokedTokenRetentionPeriod;
    }

    public void setRevokedTokenRetentionPeriod(Duration revokedTokenRetentionPeriod) {
        this.revokedTokenRetentionPeriod = revokedTokenRetentionPeriod;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
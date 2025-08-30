package com.ecommerce.shared.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for metrics collection
 */
@ConfigurationProperties(prefix = "ecommerce.metrics")
public class MetricsProperties {

    /**
     * Whether metrics collection is enabled
     */
    private boolean enabled = true;

    /**
     * Application name for metrics tagging
     */
    private String applicationName = "ecommerce-service";

    /**
     * Application version for metrics tagging
     */
    private String applicationVersion = "1.0.0";

    /**
     * Environment name for metrics tagging
     */
    private String environment = "development";

    /**
     * Whether to collect JVM metrics
     */
    private boolean jvmMetricsEnabled = true;

    /**
     * Whether to collect business metrics
     */
    private boolean businessMetricsEnabled = true;

    /**
     * Whether to collect database metrics
     */
    private boolean databaseMetricsEnabled = true;

    /**
     * Whether to collect cache metrics
     */
    private boolean cacheMetricsEnabled = true;

    /**
     * Whether to collect method-level timing metrics
     */
    private boolean methodTimingEnabled = true;

    /**
     * Percentiles to calculate for timing metrics
     */
    private double[] percentiles = {0.5, 0.75, 0.95, 0.99};

    /**
     * Histogram buckets for timing metrics
     */
    private double[] histogramBuckets = {0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0};

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isJvmMetricsEnabled() {
        return jvmMetricsEnabled;
    }

    public void setJvmMetricsEnabled(boolean jvmMetricsEnabled) {
        this.jvmMetricsEnabled = jvmMetricsEnabled;
    }

    public boolean isBusinessMetricsEnabled() {
        return businessMetricsEnabled;
    }

    public void setBusinessMetricsEnabled(boolean businessMetricsEnabled) {
        this.businessMetricsEnabled = businessMetricsEnabled;
    }

    public boolean isDatabaseMetricsEnabled() {
        return databaseMetricsEnabled;
    }

    public void setDatabaseMetricsEnabled(boolean databaseMetricsEnabled) {
        this.databaseMetricsEnabled = databaseMetricsEnabled;
    }

    public boolean isCacheMetricsEnabled() {
        return cacheMetricsEnabled;
    }

    public void setCacheMetricsEnabled(boolean cacheMetricsEnabled) {
        this.cacheMetricsEnabled = cacheMetricsEnabled;
    }

    public boolean isMethodTimingEnabled() {
        return methodTimingEnabled;
    }

    public void setMethodTimingEnabled(boolean methodTimingEnabled) {
        this.methodTimingEnabled = methodTimingEnabled;
    }

    public double[] getPercentiles() {
        return percentiles;
    }

    public void setPercentiles(double[] percentiles) {
        this.percentiles = percentiles;
    }

    public double[] getHistogramBuckets() {
        return histogramBuckets;
    }

    public void setHistogramBuckets(double[] histogramBuckets) {
        this.histogramBuckets = histogramBuckets;
    }
}
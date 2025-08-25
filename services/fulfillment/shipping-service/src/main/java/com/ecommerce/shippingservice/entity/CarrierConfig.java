package com.ecommerce.shippingservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.ecommerce.shared.models.TenantAware;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "carrier_configs", indexes = {
    @Index(name = "idx_tenant_carrier", columnList = "tenant_id, carrier_name"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
public class CarrierConfig extends BaseEntity implements TenantAware {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotBlank
    @Column(name = "carrier_name", nullable = false, length = 100)
    private String carrierName;

    @NotBlank
    @Column(name = "api_endpoint", nullable = false, length = 500)
    private String apiEndpoint;

    @NotBlank
    @Column(name = "api_key_encrypted", nullable = false, length = 500)
    private String apiKeyEncrypted;

    @Column(name = "api_secret_encrypted", length = 500)
    private String apiSecretEncrypted;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "supported_services", columnDefinition = "JSON")
    private String supportedServices; // JSON array of supported shipping services

    @Column(name = "rate_table", columnDefinition = "JSON")
    private String rateTable; // JSON object with pricing information

    // Constructors
    public CarrierConfig() {}

    public CarrierConfig(String tenantId, String carrierName, String apiEndpoint, String apiKeyEncrypted) {
        this.tenantId = tenantId;
        this.carrierName = carrierName;
        this.apiEndpoint = apiEndpoint;
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    // Business methods
    public boolean isConfigured() {
        return apiKeyEncrypted != null && !apiKeyEncrypted.trim().isEmpty() &&
               apiEndpoint != null && !apiEndpoint.trim().isEmpty();
    }

    // Getters and Setters
    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public String getApiSecretEncrypted() {
        return apiSecretEncrypted;
    }

    public void setApiSecretEncrypted(String apiSecretEncrypted) {
        this.apiSecretEncrypted = apiSecretEncrypted;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getSupportedServices() {
        return supportedServices;
    }

    public void setSupportedServices(String supportedServices) {
        this.supportedServices = supportedServices;
    }

    public String getRateTable() {
        return rateTable;
    }

    public void setRateTable(String rateTable) {
        this.rateTable = rateTable;
    }
}
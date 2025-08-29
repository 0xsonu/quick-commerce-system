package com.ecommerce.notificationservice.entity;

import com.ecommerce.shared.models.BaseEntity;
import com.ecommerce.shared.models.TenantAware;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Entity
@Table(name = "notification_templates", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "template_key", "channel"}))
public class NotificationTemplate extends BaseEntity implements TenantAware {

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @NotBlank
    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @NotBlank
    @Column(name = "subject", length = 255)
    private String subject;

    @NotBlank
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ElementCollection
    @CollectionTable(name = "template_variables", 
                    joinColumns = @JoinColumn(name = "template_id"))
    @MapKeyColumn(name = "variable_name")
    @Column(name = "variable_description")
    private Map<String, String> variables;

    // Constructors
    public NotificationTemplate() {}

    public NotificationTemplate(String tenantId, String templateKey, NotificationChannel channel, 
                              String subject, String content) {
        this.tenantId = tenantId;
        this.templateKey = templateKey;
        this.channel = channel;
        this.subject = subject;
        this.content = content;
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

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }
}
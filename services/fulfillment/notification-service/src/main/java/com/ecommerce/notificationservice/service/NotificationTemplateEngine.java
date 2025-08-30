package com.ecommerce.notificationservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationTemplateEngine {

    private final org.thymeleaf.TemplateEngine thymeleafEngine;
    private final TenantBrandingService tenantBrandingService;

    @Autowired
    public NotificationTemplateEngine(TenantBrandingService tenantBrandingService) {
        this.tenantBrandingService = tenantBrandingService;
        this.thymeleafEngine = new org.thymeleaf.TemplateEngine();
        
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCacheable(false);
        
        this.thymeleafEngine.setTemplateResolver(templateResolver);
    }

    /**
     * Process a template string with the given variables and tenant branding
     * 
     * @param template The template string with Thymeleaf syntax
     * @param variables Map of variables to substitute in the template
     * @param tenantId The tenant ID for branding variables
     * @return Processed template string
     */
    public String processTemplate(String template, Map<String, Object> variables, String tenantId) {
        if (template == null) {
            return null;
        }
        
        Map<String, Object> allVariables = new HashMap<>();
        
        // Add tenant branding variables
        if (tenantId != null) {
            Map<String, Object> brandingVariables = tenantBrandingService.getBrandingVariables(tenantId);
            allVariables.putAll(brandingVariables);
        }
        
        // Add provided variables (these can override branding variables if needed)
        if (variables != null) {
            allVariables.putAll(variables);
        }
        
        if (allVariables.isEmpty()) {
            return template;
        }
        
        Context context = new Context();
        context.setVariables(allVariables);
        
        return thymeleafEngine.process(template, context);
    }

    /**
     * Process a template string with the given variables (backward compatibility)
     * 
     * @param template The template string with Thymeleaf syntax
     * @param variables Map of variables to substitute in the template
     * @return Processed template string
     */
    public String processTemplate(String template, Map<String, Object> variables) {
        return processTemplate(template, variables, null);
    }

    /**
     * Process a subject template with variables and tenant branding
     * 
     * @param subjectTemplate The subject template string
     * @param variables Map of variables to substitute
     * @param tenantId The tenant ID for branding variables
     * @return Processed subject string
     */
    public String processSubject(String subjectTemplate, Map<String, Object> variables, String tenantId) {
        return processTemplate(subjectTemplate, variables, tenantId);
    }

    /**
     * Process a content template with variables and tenant branding
     * 
     * @param contentTemplate The content template string
     * @param variables Map of variables to substitute
     * @param tenantId The tenant ID for branding variables
     * @return Processed content string
     */
    public String processContent(String contentTemplate, Map<String, Object> variables, String tenantId) {
        return processTemplate(contentTemplate, variables, tenantId);
    }

    /**
     * Process a subject template with variables (backward compatibility)
     * 
     * @param subjectTemplate The subject template string
     * @param variables Map of variables to substitute
     * @return Processed subject string
     */
    public String processSubject(String subjectTemplate, Map<String, Object> variables) {
        return processTemplate(subjectTemplate, variables, null);
    }

    /**
     * Process a content template with variables (backward compatibility)
     * 
     * @param contentTemplate The content template string
     * @param variables Map of variables to substitute
     * @return Processed content string
     */
    public String processContent(String contentTemplate, Map<String, Object> variables) {
        return processTemplate(contentTemplate, variables, null);
    }
}
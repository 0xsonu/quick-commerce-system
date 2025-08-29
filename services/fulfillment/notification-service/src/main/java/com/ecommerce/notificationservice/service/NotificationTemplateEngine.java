package com.ecommerce.notificationservice.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

@Service
public class NotificationTemplateEngine {

    private final org.thymeleaf.TemplateEngine thymeleafEngine;

    public NotificationTemplateEngine() {
        this.thymeleafEngine = new org.thymeleaf.TemplateEngine();
        
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCacheable(false);
        
        this.thymeleafEngine.setTemplateResolver(templateResolver);
    }

    /**
     * Process a template string with the given variables
     * 
     * @param template The template string with Thymeleaf syntax
     * @param variables Map of variables to substitute in the template
     * @return Processed template string
     */
    public String processTemplate(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        
        Context context = new Context();
        context.setVariables(variables);
        
        return thymeleafEngine.process(template, context);
    }

    /**
     * Process a subject template with variables
     * 
     * @param subjectTemplate The subject template string
     * @param variables Map of variables to substitute
     * @return Processed subject string
     */
    public String processSubject(String subjectTemplate, Map<String, Object> variables) {
        return processTemplate(subjectTemplate, variables);
    }

    /**
     * Process a content template with variables
     * 
     * @param contentTemplate The content template string
     * @param variables Map of variables to substitute
     * @return Processed content string
     */
    public String processContent(String contentTemplate, Map<String, Object> variables) {
        return processTemplate(contentTemplate, variables);
    }
}
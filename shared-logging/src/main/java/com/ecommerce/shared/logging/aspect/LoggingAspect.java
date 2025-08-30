package com.ecommerce.shared.logging.aspect;

import com.ecommerce.shared.logging.LoggingContext;
import com.ecommerce.shared.logging.annotation.Loggable;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect for method-level logging with performance metrics
 */
@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Around("@annotation(loggable)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String description = loggable.value().isEmpty() ? methodName : loggable.value();
        long startTime = System.currentTimeMillis();
        
        logAtLevel(loggable.level(), "Method execution started: {}", description);
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Log performance warnings for slow methods
            if (duration > 10000) { // >10s
                logger.warn("Very slow method execution: {} - Duration: {}ms", description, duration);
            } else if (duration > 5000) { // >5s
                logger.warn("Slow method execution: {} - Duration: {}ms", description, duration);
            } else {
                logAtLevel(loggable.level(), "Method execution completed: {} - Duration: {}ms", description, duration);
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            logger.error("Method execution failed: {} - Duration: {}ms - Error: {} - CorrelationId: {}", 
                    description, duration, e.getMessage(), LoggingContext.getCorrelationId(), e);
            
            throw e;
        }
    }
    
    @Around("@annotation(logParameters)")
    public Object logParameters(ProceedingJoinPoint joinPoint, 
                               com.ecommerce.shared.logging.annotation.LogParameters logParameters) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        
        // Log parameters if enabled
        if (logParameters.logParameters() && args.length > 0) {
            try {
                String argsJson = objectMapper.writeValueAsString(sanitizeParameters(args));
                logger.debug("Method called: {} with parameters: {} - CorrelationId: {}", 
                        methodName, argsJson, LoggingContext.getCorrelationId());
            } catch (Exception e) {
                logger.debug("Method called: {} with parameters: [serialization failed] - CorrelationId: {}", 
                        methodName, LoggingContext.getCorrelationId());
            }
        } else {
            logger.debug("Method called: {} - CorrelationId: {}", methodName, LoggingContext.getCorrelationId());
        }
        
        try {
            Object result = joinPoint.proceed();
            
            // Log return value if enabled
            if (logParameters.logReturnValue() && result != null) {
                try {
                    String resultJson = objectMapper.writeValueAsString(sanitizeReturnValue(result));
                    logger.debug("Method returned: {} result: {} - CorrelationId: {}", 
                            methodName, resultJson, LoggingContext.getCorrelationId());
                } catch (Exception e) {
                    logger.debug("Method returned: {} result: [serialization failed] - CorrelationId: {}", 
                            methodName, LoggingContext.getCorrelationId());
                }
            } else {
                logger.debug("Method returned: {} - CorrelationId: {}", methodName, LoggingContext.getCorrelationId());
            }
            
            return result;
        } catch (Exception e) {
            if (logParameters.logExceptions()) {
                logger.error("Method failed: {} - Error: {} - CorrelationId: {}", 
                        methodName, e.getMessage(), LoggingContext.getCorrelationId(), e);
            } else {
                logger.error("Method failed: {} - Error: {} - CorrelationId: {}", 
                        methodName, e.getMessage(), LoggingContext.getCorrelationId());
            }
            throw e;
        }
    }
    
    private void logAtLevel(Loggable.LogLevel level, String message, Object... args) {
        switch (level) {
            case DEBUG -> logger.debug(message, args);
            case INFO -> logger.info(message, args);
            case WARN -> logger.warn(message, args);
            case ERROR -> logger.error(message, args);
        }
    }
    
    private Object[] sanitizeParameters(Object[] args) {
        // Remove sensitive data from parameters before logging
        Object[] sanitized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            sanitized[i] = sanitizeValue(args[i]);
        }
        return sanitized;
    }
    
    private Object sanitizeReturnValue(Object value) {
        return sanitizeValue(value);
    }
    
    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        String valueStr = value.toString();
        // Mask potential sensitive data
        if (valueStr.toLowerCase().contains("password") || 
            valueStr.toLowerCase().contains("token") ||
            valueStr.toLowerCase().contains("secret")) {
            return "[REDACTED]";
        }
        
        return value;
    }
}
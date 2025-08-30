package com.ecommerce.shared.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for execution time logging
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    
    /**
     * Custom description for the logged method
     */
    String value() default "";
    
    /**
     * Log level for the execution time log
     */
    LogLevel level() default LogLevel.INFO;
    
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
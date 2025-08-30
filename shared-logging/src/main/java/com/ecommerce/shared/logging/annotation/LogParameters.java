package com.ecommerce.shared.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for parameter and return value logging
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogParameters {
    
    /**
     * Whether to log method parameters
     */
    boolean logParameters() default true;
    
    /**
     * Whether to log return values
     */
    boolean logReturnValue() default true;
    
    /**
     * Whether to log exceptions
     */
    boolean logExceptions() default true;
}
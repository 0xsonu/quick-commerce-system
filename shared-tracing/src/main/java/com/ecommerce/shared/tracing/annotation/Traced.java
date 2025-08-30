package com.ecommerce.shared.tracing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for custom tracing
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    
    /**
     * Custom span name. If not provided, method name will be used.
     */
    String value() default "";
    
    /**
     * Operation name for the span
     */
    String operation() default "";
    
    /**
     * Whether to include method parameters as span attributes
     */
    boolean includeParameters() default false;
    
    /**
     * Whether to include return value as span attribute
     */
    boolean includeReturnValue() default false;
}
package com.ecommerce.shared.metrics.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for business metrics collection
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessMetric {
    
    /**
     * The business event type
     */
    String value();
    
    /**
     * Additional tags to add to the metric in key-value pairs
     */
    String[] tags() default {};
    
    /**
     * Whether to record timing information for this business metric
     */
    boolean recordTiming() default true;
}
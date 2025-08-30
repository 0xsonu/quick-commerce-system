package com.ecommerce.shared.metrics.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for timing metrics collection
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
    
    /**
     * The name of the timer metric. If empty, defaults to "method.execution.time"
     */
    String value() default "";
    
    /**
     * Description of the timer metric
     */
    String description() default "Method execution time";
    
    /**
     * Additional tags to add to the metric in key-value pairs
     */
    String[] extraTags() default {};
}
package com.ecommerce.shared.metrics.aspects;

import com.ecommerce.shared.metrics.annotations.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Aspect for method-level timing metrics
 */
@Aspect
@Component
public class MethodTimingAspect {

    private final MeterRegistry meterRegistry;

    public MethodTimingAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(com.ecommerce.shared.metrics.annotations.Timed)")
    public Object timeMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Timed timedAnnotation = method.getAnnotation(Timed.class);

        String metricName = timedAnnotation.value().isEmpty() ? 
            "method.execution.time" : timedAnnotation.value();
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = true;
        Throwable exception = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            exception = t;
            throw t;
        } finally {
            Timer.Builder timerBuilder = Timer.builder(metricName)
                    .description(timedAnnotation.description())
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("success", String.valueOf(success));

            if (exception != null) {
                timerBuilder.tag("exception", exception.getClass().getSimpleName());
            }

            // Add custom tags from annotation
            String[] tags = timedAnnotation.extraTags();
            for (int i = 0; i < tags.length - 1; i += 2) {
                timerBuilder.tag(tags[i], tags[i + 1]);
            }

            sample.stop(timerBuilder.register(meterRegistry));
        }
    }

    @Around("execution(* com.ecommerce..service.*.*(..))")
    public Object timeServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = true;
        Throwable exception = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            exception = t;
            throw t;
        } finally {
            Timer.Builder timerBuilder = Timer.builder("service.method.duration")
                    .description("Service method execution time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("success", String.valueOf(success));

            if (exception != null) {
                timerBuilder.tag("exception", exception.getClass().getSimpleName());
            }

            sample.stop(timerBuilder.register(meterRegistry));
        }
    }

    @Around("execution(* com.ecommerce..controller.*.*(..))")
    public Object timeControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = true;
        Throwable exception = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            exception = t;
            throw t;
        } finally {
            Timer.Builder timerBuilder = Timer.builder("controller.method.duration")
                    .description("Controller method execution time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("success", String.valueOf(success));

            if (exception != null) {
                timerBuilder.tag("exception", exception.getClass().getSimpleName());
            }

            sample.stop(timerBuilder.register(meterRegistry));
        }
    }

    @Around("execution(* com.ecommerce..repository.*.*(..))")
    public Object timeRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = true;
        Throwable exception = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            exception = t;
            throw t;
        } finally {
            Timer.Builder timerBuilder = Timer.builder("repository.method.duration")
                    .description("Repository method execution time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("success", String.valueOf(success));

            if (exception != null) {
                timerBuilder.tag("exception", exception.getClass().getSimpleName());
            }

            sample.stop(timerBuilder.register(meterRegistry));
        }
    }
}
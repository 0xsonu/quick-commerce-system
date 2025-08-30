package com.ecommerce.shared.tracing.aspect;

import com.ecommerce.shared.tracing.annotation.Traced;
import com.ecommerce.shared.tracing.util.TracingUtils;
import com.ecommerce.shared.utils.TenantContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspect for custom tracing using @Traced annotation
 */
@Aspect
@Component
public class TracingAspect {

    private static final Logger logger = LoggerFactory.getLogger(TracingAspect.class);

    private final Tracer tracer;

    @Autowired
    public TracingAspect(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("ecommerce-custom-tracing");
    }

    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        String spanName = getSpanName(traced, method);
        String operation = traced.operation().isEmpty() ? method.getName() : traced.operation();
        
        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("method.name", method.getName())
                .setAttribute("class.name", method.getDeclaringClass().getSimpleName())
                .setAttribute("operation", operation)
                .startSpan();

        // Add tenant context if available
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            span.setAttribute("tenant.id", tenantId);
        }

        String userId = TenantContext.getUserId();
        if (userId != null) {
            span.setAttribute("user.id", userId);
        }

        String correlationId = TenantContext.getCorrelationId();
        if (correlationId != null) {
            span.setAttribute("correlation.id", correlationId);
        }

        // Add method parameters if requested
        if (traced.includeParameters()) {
            addParameterAttributes(span, signature, joinPoint.getArgs());
        }

        try (Scope scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            
            // Add return value if requested and not null
            if (traced.includeReturnValue() && result != null) {
                span.setAttribute("return.type", result.getClass().getSimpleName());
                span.setAttribute("return.value", TracingUtils.sanitizeValue(result.toString()));
            }
            
            span.setStatus(StatusCode.OK);
            return result;
            
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Around("@within(traced)")
    public Object traceClass(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        // Only trace if method is not already annotated with @Traced
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        if (method.isAnnotationPresent(Traced.class)) {
            return joinPoint.proceed();
        }
        
        return traceMethod(joinPoint, traced);
    }

    private String getSpanName(Traced traced, Method method) {
        if (!traced.value().isEmpty()) {
            return traced.value();
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private void addParameterAttributes(Span span, MethodSignature signature, Object[] args) {
        String[] parameterNames = signature.getParameterNames();
        if (parameterNames != null && args != null) {
            for (int i = 0; i < Math.min(parameterNames.length, args.length); i++) {
                if (args[i] != null) {
                    String paramName = parameterNames[i];
                    String paramValue = TracingUtils.sanitizeValue(args[i].toString());
                    span.setAttribute("param." + paramName, paramValue);
                }
            }
        }
    }
}
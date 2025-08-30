package com.ecommerce.shared.tracing.aspect;

import com.ecommerce.shared.tracing.annotation.Traced;
import com.ecommerce.shared.utils.TenantContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TracingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private InMemorySpanExporter spanExporter;
    private TracingAspect tracingAspect;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        
        tracingAspect = new TracingAspect(openTelemetry);
    }

    @Test
    void testTraceMethodWithDefaultSpanName() throws Throwable {
        // Arrange
        Method method = TestService.class.getMethod("testMethod");
        Traced traced = method.getAnnotation(Traced.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("result");
        
        TenantContext.setTenantId("tenant123");
        TenantContext.setUserId("user456");
        TenantContext.setCorrelationId("corr789");

        // Act
        Object result = tracingAspect.traceMethod(joinPoint, traced);

        // Assert
        assertEquals("result", result);
        
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        SpanData span = spans.get(0);
        assertEquals("TestService.testMethod", span.getName());
        assertEquals("tenant123", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("tenant.id")));
        assertEquals("user456", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("user.id")));
        assertEquals("corr789", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("correlation.id")));
    }

    @Test
    void testTraceMethodWithCustomSpanName() throws Throwable {
        // Arrange
        Method method = TestService.class.getMethod("customNameMethod");
        Traced traced = method.getAnnotation(Traced.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("result");

        // Act
        tracingAspect.traceMethod(joinPoint, traced);

        // Assert
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        assertEquals("custom-span-name", spans.get(0).getName());
    }

    @Test
    void testTraceMethodWithException() throws Throwable {
        // Arrange
        Method method = TestService.class.getMethod("testMethod");
        Traced traced = method.getAnnotation(Traced.class);
        RuntimeException exception = new RuntimeException("Test exception");
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenThrow(exception);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> tracingAspect.traceMethod(joinPoint, traced));
        
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        SpanData span = spans.get(0);
        assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("Test exception", span.getStatus().getDescription());
    }

    @Test
    void testTraceMethodWithParameters() throws Throwable {
        // Arrange
        Method method = TestService.class.getMethod("methodWithParams", String.class, Integer.class);
        Traced traced = method.getAnnotation(Traced.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1", "param2"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 42});
        when(joinPoint.proceed()).thenReturn("result");

        // Act
        tracingAspect.traceMethod(joinPoint, traced);

        // Assert
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        SpanData span = spans.get(0);
        assertEquals("value1", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("param.param1")));
        assertEquals("42", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("param.param2")));
    }

    // Test service class with annotated methods
    public static class TestService {
        
        @Traced
        public String testMethod() {
            return "test";
        }
        
        @Traced("custom-span-name")
        public String customNameMethod() {
            return "custom";
        }
        
        @Traced(includeParameters = true, includeReturnValue = true)
        public String methodWithParams(String param1, Integer param2) {
            return "result";
        }
    }
}
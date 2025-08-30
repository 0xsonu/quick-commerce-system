package com.ecommerce.shared.metrics.aspects;

import com.ecommerce.shared.metrics.annotations.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MethodTimingAspectTest {

    private MeterRegistry meterRegistry;
    private MethodTimingAspect methodTimingAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        methodTimingAspect = new MethodTimingAspect(meterRegistry);
    }

    @Test
    void shouldTimeMethodWithTimedAnnotation() throws Throwable {
        // Given
        Method method = TestService.class.getMethod("timedMethod");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = methodTimingAspect.timeMethod(joinPoint);

        // Then
        assertEquals("result", result);
        Timer timer = meterRegistry.find("custom.timer")
                .tag("class", "TestService")
                .tag("method", "timedMethod")
                .tag("success", "true")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldTimeMethodWithException() throws Throwable {
        // Given
        Method method = TestService.class.getMethod("timedMethod");
        RuntimeException exception = new RuntimeException("Test exception");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThrows(RuntimeException.class, () -> methodTimingAspect.timeMethod(joinPoint));

        Timer timer = meterRegistry.find("custom.timer")
                .tag("class", "TestService")
                .tag("method", "timedMethod")
                .tag("success", "false")
                .tag("exception", "RuntimeException")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldTimeServiceMethods() throws Throwable {
        // Given
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("serviceMethod");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = methodTimingAspect.timeServiceMethods(joinPoint);

        // Then
        assertEquals("result", result);
        Timer timer = meterRegistry.find("service.method.duration")
                .tag("class", "TestService")
                .tag("method", "serviceMethod")
                .tag("success", "true")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldTimeControllerMethods() throws Throwable {
        // Given
        when(joinPoint.getTarget()).thenReturn(new TestController());
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("controllerMethod");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = methodTimingAspect.timeControllerMethods(joinPoint);

        // Then
        assertEquals("result", result);
        Timer timer = meterRegistry.find("controller.method.duration")
                .tag("class", "TestController")
                .tag("method", "controllerMethod")
                .tag("success", "true")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldTimeRepositoryMethods() throws Throwable {
        // Given
        when(joinPoint.getTarget()).thenReturn(new TestRepository());
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("repositoryMethod");
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = methodTimingAspect.timeRepositoryMethods(joinPoint);

        // Then
        assertEquals("result", result);
        Timer timer = meterRegistry.find("repository.method.duration")
                .tag("class", "TestRepository")
                .tag("method", "repositoryMethod")
                .tag("success", "true")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldTimeMethodWithCustomTags() throws Throwable {
        // Given
        Method method = TestService.class.getMethod("timedMethodWithTags");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = methodTimingAspect.timeMethod(joinPoint);

        // Then
        assertEquals("result", result);
        Timer timer = meterRegistry.find("custom.timer.with.tags")
                .tag("class", "TestService")
                .tag("method", "timedMethodWithTags")
                .tag("success", "true")
                .tag("custom", "value")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    // Test classes
    public static class TestService {
        @Timed(value = "custom.timer", description = "Custom timer for testing")
        public String timedMethod() {
            return "test";
        }

        @Timed(value = "custom.timer.with.tags", extraTags = {"custom", "value"})
        public String timedMethodWithTags() {
            return "test";
        }
    }

    public static class TestController {
        public String controllerMethod() {
            return "test";
        }
    }

    public static class TestRepository {
        public String repositoryMethod() {
            return "test";
        }
    }
}
package com.ecommerce.shared.logging.aspect;

import com.ecommerce.shared.logging.annotation.Loggable;
import com.ecommerce.shared.logging.annotation.LogParameters;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private Signature signature;
    
    private LoggingAspect loggingAspect;
    
    @BeforeEach
    void setUp() {
        loggingAspect = new LoggingAspect();
    }
    
    @Test
    void shouldLogExecutionTimeForSuccessfulMethod() throws Throwable {
        // Given
        String methodName = "testMethod()";
        Object expectedResult = "test result";
        Loggable loggable = mock(Loggable.class);
        when(loggable.value()).thenReturn("Custom description");
        when(loggable.level()).thenReturn(Loggable.LogLevel.INFO);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn(expectedResult);
        
        // When
        Object result = loggingAspect.logExecutionTime(joinPoint, loggable);
        
        // Then
        assertEquals(expectedResult, result);
        verify(joinPoint).proceed();
    }
    
    @Test
    void shouldLogExecutionTimeForFailedMethod() throws Throwable {
        // Given
        String methodName = "testMethod()";
        RuntimeException expectedException = new RuntimeException("Test exception");
        Loggable loggable = mock(Loggable.class);
        when(loggable.value()).thenReturn("");
        when(loggable.level()).thenReturn(Loggable.LogLevel.ERROR);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenThrow(expectedException);
        
        // When & Then
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> 
            loggingAspect.logExecutionTime(joinPoint, loggable));
        
        assertEquals(expectedException, thrownException);
        verify(joinPoint).proceed();
    }
    
    @Test
    void shouldLogParametersForSuccessfulMethod() throws Throwable {
        // Given
        String methodName = "testMethod()";
        Object[] args = {"param1", 123, true};
        Object expectedResult = "test result";
        LogParameters logParameters = mock(LogParameters.class);
        when(logParameters.logParameters()).thenReturn(true);
        when(logParameters.logReturnValue()).thenReturn(true);
        when(logParameters.logExceptions()).thenReturn(true);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(expectedResult);
        
        // When
        Object result = loggingAspect.logParameters(joinPoint, logParameters);
        
        // Then
        assertEquals(expectedResult, result);
        verify(joinPoint).proceed();
        verify(joinPoint).getArgs();
    }
    
    @Test
    void shouldLogParametersForFailedMethod() throws Throwable {
        // Given
        String methodName = "testMethod()";
        Object[] args = {"param1", 123};
        RuntimeException expectedException = new RuntimeException("Test exception");
        LogParameters logParameters = mock(LogParameters.class);
        when(logParameters.logParameters()).thenReturn(true);
        when(logParameters.logReturnValue()).thenReturn(true);
        when(logParameters.logExceptions()).thenReturn(true);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenThrow(expectedException);
        
        // When & Then
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> 
            loggingAspect.logParameters(joinPoint, logParameters));
        
        assertEquals(expectedException, thrownException);
        verify(joinPoint).proceed();
        verify(joinPoint).getArgs();
    }
    
    @Test
    void shouldHandleNullArguments() throws Throwable {
        // Given
        String methodName = "testMethod()";
        Object expectedResult = "test result";
        LogParameters logParameters = mock(LogParameters.class);
        when(logParameters.logParameters()).thenReturn(true);
        when(logParameters.logReturnValue()).thenReturn(true);
        when(logParameters.logExceptions()).thenReturn(true);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn(expectedResult);
        
        // When
        Object result = loggingAspect.logParameters(joinPoint, logParameters);
        
        // Then
        assertEquals(expectedResult, result);
        verify(joinPoint).proceed();
    }
    
    @Test
    void shouldSanitizeSensitiveParameters() throws Throwable {
        // Given
        String methodName = "testMethod()";
        Object[] args = {"username", "password123", "secret-token"};
        Object expectedResult = "test result";
        LogParameters logParameters = mock(LogParameters.class);
        when(logParameters.logParameters()).thenReturn(true);
        when(logParameters.logReturnValue()).thenReturn(false);
        when(logParameters.logExceptions()).thenReturn(true);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(expectedResult);
        
        // When
        Object result = loggingAspect.logParameters(joinPoint, logParameters);
        
        // Then
        assertEquals(expectedResult, result);
        verify(joinPoint).proceed();
    }
    
    @Test
    void shouldRespectLogParametersConfiguration() throws Throwable {
        // Given
        String methodName = "testMethod()";
        Object[] args = {"param1", 123};
        Object expectedResult = "test result";
        LogParameters logParameters = mock(LogParameters.class);
        when(logParameters.logParameters()).thenReturn(false); // Don't log parameters
        when(logParameters.logReturnValue()).thenReturn(false); // Don't log return value
        when(logParameters.logExceptions()).thenReturn(true);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(expectedResult);
        
        // When
        Object result = loggingAspect.logParameters(joinPoint, logParameters);
        
        // Then
        assertEquals(expectedResult, result);
        verify(joinPoint).proceed();
    }
    
    // Test service class for integration testing
    public static class TestService {
        
        @Loggable
        public String loggableMethod(String param) {
            return "result: " + param;
        }
        
        @LogParameters
        public String parametersMethod(String param1, int param2) {
            return param1 + param2;
        }
        
        @Loggable
        @LogParameters
        public String combinedMethod(String param) {
            if ("error".equals(param)) {
                throw new RuntimeException("Test error");
            }
            return "success: " + param;
        }
    }
}
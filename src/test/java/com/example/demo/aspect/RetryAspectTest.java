package com.example.demo.aspect;

import com.example.demo.annotation.RetryOnFailure;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessResourceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetryAspectTest {

    private RetryAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private RetryOnFailure threeAttempts;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new RetryAspect();
        joinPoint = mock(ProceedingJoinPoint.class);
        threeAttempts = Stub.class.getDeclaredMethod("retried").getAnnotation(RetryOnFailure.class);
    }

    @Test
    void successOnFirstAttempt_proceedsOnce() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        assertEquals("ok", aspect.retry(joinPoint, threeAttempts));

        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void transientFailureThenSuccess_retriesAndReturnsResult() throws Throwable {
        when(joinPoint.proceed())
                .thenThrow(new TransientDataAccessResourceException("db busy"))
                .thenThrow(new DataAccessResourceFailureException("connection lost"))
                .thenReturn("ok");

        assertEquals("ok", aspect.retry(joinPoint, threeAttempts));

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void allAttemptsFail_rethrowsLastExceptionAfterMaxAttempts() throws Throwable {
        TransientDataAccessResourceException last = new TransientDataAccessResourceException("still busy");
        when(joinPoint.proceed())
                .thenThrow(new TransientDataAccessResourceException("busy"))
                .thenThrow(new TransientDataAccessResourceException("busy again"))
                .thenThrow(last);

        TransientDataAccessResourceException thrown = assertThrows(
                TransientDataAccessResourceException.class, () -> aspect.retry(joinPoint, threeAttempts));

        assertSame(last, thrown);
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void nonRetryableException_isNotRetried() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("bad input"));

        assertThrows(IllegalArgumentException.class, () -> aspect.retry(joinPoint, threeAttempts));

        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void interruptedWhileWaiting_stopsRetryingAndRethrows() throws Throwable {
        RetryOnFailure withDelay = Stub.class.getDeclaredMethod("withDelay").getAnnotation(RetryOnFailure.class);
        when(joinPoint.proceed()).thenThrow(new TransientDataAccessResourceException("busy"));

        Thread.currentThread().interrupt();
        try {
            assertThrows(TransientDataAccessResourceException.class, () -> aspect.retry(joinPoint, withDelay));
            verify(joinPoint, times(1)).proceed();
        } finally {
            Thread.interrupted();
        }
    }

    private static class Stub {

        @RetryOnFailure(maxAttempts = 3, delayMillis = 0)
        void retried() {
        }

        @RetryOnFailure(maxAttempts = 3, delayMillis = 50)
        void withDelay() {
        }
    }
}

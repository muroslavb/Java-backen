package com.example.demo.aspect;

import com.example.demo.annotation.RateLimited;
import com.example.demo.exception.RateLimitExceededException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    private Clock clock;
    private RateLimitAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private RateLimited twoPerMinute;

    @BeforeEach
    void setUp() throws Throwable {
        clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1_000_000L);
        aspect = new RateLimitAspect(clock);

        Signature signature = mock(Signature.class);
        when(signature.toLongString()).thenReturn("ModificationService.createMod");
        joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("created");

        twoPerMinute = Stub.class.getDeclaredMethod("limited").getAnnotation(RateLimited.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void callsWithinLimit_proceedEveryTime() throws Throwable {
        assertEquals("created", aspect.limit(joinPoint, twoPerMinute));
        assertEquals("created", aspect.limit(joinPoint, twoPerMinute));

        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void callOverLimit_throwsAndDoesNotProceed() throws Throwable {
        aspect.limit(joinPoint, twoPerMinute);
        aspect.limit(joinPoint, twoPerMinute);

        assertThrows(RateLimitExceededException.class, () -> aspect.limit(joinPoint, twoPerMinute));

        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void afterWindowPasses_callsAreAllowedAgain() throws Throwable {
        aspect.limit(joinPoint, twoPerMinute);
        aspect.limit(joinPoint, twoPerMinute);

        when(clock.millis()).thenReturn(1_000_000L + 60_001L);

        assertEquals("created", aspect.limit(joinPoint, twoPerMinute));
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void limitIsTrackedPerUser() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("alice", "pw", "ROLE_AUTHOR"));
        aspect.limit(joinPoint, twoPerMinute);
        aspect.limit(joinPoint, twoPerMinute);
        assertThrows(RateLimitExceededException.class, () -> aspect.limit(joinPoint, twoPerMinute));

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("bob", "pw", "ROLE_AUTHOR"));
        assertEquals("created", aspect.limit(joinPoint, twoPerMinute));

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void blockedCall_neverReachesTargetMethod() throws Throwable {
        RateLimited zeroCalls = Stub.class.getDeclaredMethod("blocked").getAnnotation(RateLimited.class);

        assertThrows(RateLimitExceededException.class, () -> aspect.limit(joinPoint, zeroCalls));

        verify(joinPoint, never()).proceed();
    }

    private static class Stub {

        @RateLimited(maxCalls = 2, windowSeconds = 60)
        void limited() {
        }

        @RateLimited(maxCalls = 0, windowSeconds = 60)
        void blocked() {
        }
    }
}

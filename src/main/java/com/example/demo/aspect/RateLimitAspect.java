package com.example.demo.aspect;

import com.example.demo.annotation.RateLimited;
import com.example.demo.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final Clock clock;
    private final Map<String, Deque<Long>> callsByKey = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitAspect() {
        this(Clock.systemUTC());
    }

    RateLimitAspect(Clock clock) {
        this.clock = clock;
    }

    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = currentUser() + "|" + joinPoint.getSignature().toLongString();
        long now = clock.millis();
        long windowStart = now - rateLimited.windowSeconds() * 1000;
        Deque<Long> calls = callsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (calls) {
            while (!calls.isEmpty() && calls.peekFirst() <= windowStart) {
                calls.pollFirst();
            }
            if (calls.size() >= rateLimited.maxCalls()) {
                log.warn("Rate limit exceeded for {}", key);
                throw new RateLimitExceededException(
                        "Перевищено ліміт: " + rateLimited.maxCalls() + " викликів за "
                                + rateLimited.windowSeconds() + " с");
            }
            calls.addLast(now);
        }

        return joinPoint.proceed();
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        return authentication.getName();
    }
}

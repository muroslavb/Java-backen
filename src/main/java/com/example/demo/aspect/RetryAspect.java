package com.example.demo.aspect;

import com.example.demo.annotation.RetryOnFailure;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class RetryAspect {

    @Around("@annotation(retryOnFailure)")
    public Object retry(ProceedingJoinPoint joinPoint, RetryOnFailure retryOnFailure) throws Throwable {
        int maxAttempts = Math.max(1, retryOnFailure.maxAttempts());

        for (int attempt = 1; ; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Throwable ex) {
                if (attempt >= maxAttempts || !isRetryable(ex, retryOnFailure.retryOn())) {
                    throw ex;
                }
                log.warn("Спроба {}/{} не вдалася: {}", attempt, maxAttempts, ex.toString());
                pause(retryOnFailure.delayMillis(), ex);
            }
        }
    }

    private boolean isRetryable(Throwable ex, Class<? extends Throwable>[] retryOn) {
        for (Class<? extends Throwable> type : retryOn) {
            if (type.isInstance(ex)) {
                return true;
            }
        }
        return false;
    }

    private void pause(long delayMillis, Throwable original) throws Throwable {
        if (delayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw original;
        }
    }
}

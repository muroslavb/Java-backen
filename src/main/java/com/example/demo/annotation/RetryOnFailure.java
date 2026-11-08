package com.example.demo.annotation;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryOnFailure {
    int maxAttempts() default 3;

    long delayMillis() default 200;

    Class<? extends Throwable>[] retryOn() default {
            TransientDataAccessException.class,
            DataAccessResourceFailureException.class
    };
}

package com.example.demo.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
public class LoggingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Необроблений виняток в асинхронному методі {}.{} з параметрами {}",
                method.getDeclaringClass().getSimpleName(), method.getName(), Arrays.toString(params), ex);
    }
}

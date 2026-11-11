package com.example.demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestControllerAdvice
public class AsyncExceptionAdvice {

    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<String> handleRejected(TaskRejectedException ex) {
        log.warn("Виконавець переповнений: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body("Сервер перевантажений, спробуйте пізніше");
    }
}

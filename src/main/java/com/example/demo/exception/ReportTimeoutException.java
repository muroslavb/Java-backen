package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class ReportTimeoutException extends RuntimeException {

    public ReportTimeoutException(String message) {
        super(message);
    }
}

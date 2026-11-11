package com.example.demo.async;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.dto.ModificationDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingAsyncExceptionHandlerTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(LoggingAsyncExceptionHandler.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void uncaughtException_isRecordedAsErrorWithMethodNameAndCause() throws Exception {
        Method method = NotificationAsyncService.class.getMethod("sendModCreatedEmail", ModificationDto.class);
        IllegalStateException cause = new IllegalStateException("smtp down");

        new LoggingAsyncExceptionHandler().handleUncaughtException(cause, method, new ModificationDto());

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("sendModCreatedEmail"));
        assertEquals("smtp down", event.getThrowableProxy().getMessage());
    }
}

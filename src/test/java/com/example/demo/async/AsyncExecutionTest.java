package com.example.demo.async;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.dto.ModReportDto;
import com.example.demo.dto.ModificationDto;
import com.example.demo.service.ModReportService;
import com.example.demo.service.ModificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AsyncExecutionTest.Config.class)
class AsyncExecutionTest {

    @Configuration
    @Import(AsyncConfig.class)
    static class Config {

        @Bean
        NotificationAsyncService notificationAsyncService() {
            return new NotificationAsyncService(800);
        }

        @Bean
        PdfReportAsyncService pdfReportAsyncService() {
            return new PdfReportAsyncService(500);
        }

        @Bean
        RatingAsyncService ratingAsyncService() {
            return new RatingAsyncService(500);
        }

        @Bean
        ModificationService modificationService() {
            return mock(ModificationService.class);
        }

        @Bean
        ModReportService modReportService(ModificationService modificationService,
                                          PdfReportAsyncService pdfService,
                                          RatingAsyncService ratingService) {
            return new ModReportService(modificationService, pdfService, ratingService, 5000);
        }

        @Bean
        ProbeTasks probeTasks() {
            return new ProbeTasks();
        }
    }

    public static class ProbeTasks {

        @Async(AsyncConfig.EXECUTOR)
        public CompletableFuture<String> currentThreadName() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }

        @Async(AsyncConfig.EXECUTOR)
        public CompletableFuture<long[]> timedSleep(long millis) throws InterruptedException {
            long start = System.nanoTime();
            Thread.sleep(millis);
            return CompletableFuture.completedFuture(new long[]{start, System.nanoTime()});
        }

        @Async(AsyncConfig.EXECUTOR)
        public CompletableFuture<String> failingFuture() {
            throw new IllegalStateException("boom");
        }

        @Async(AsyncConfig.EXECUTOR)
        public void failingVoid() {
            throw new IllegalStateException("void boom");
        }
    }

    @Autowired
    private NotificationAsyncService notificationService;

    @Autowired
    private ModReportService reportService;

    @Autowired
    private ModificationService modificationService;

    @Autowired
    private ProbeTasks probe;

    @Autowired
    @Qualifier(AsyncConfig.EXECUTOR)
    private ThreadPoolTaskExecutor executor;

    private ListAppender<ILoggingEvent> notificationLog;
    private ListAppender<ILoggingEvent> handlerLog;

    @BeforeEach
    void attachAppenders() {
        notificationLog = attach(NotificationAsyncService.class);
        handlerLog = attach(LoggingAsyncExceptionHandler.class);
    }

    @AfterEach
    void waitUntilExecutorIsIdle() throws InterruptedException {
        TestAwait.until(() -> executor.getActiveCount() == 0
                && executor.getThreadPoolExecutor().getQueue().isEmpty(), 5000);
    }

    @AfterEach
    void detachAppenders() {
        ((Logger) LoggerFactory.getLogger(NotificationAsyncService.class)).detachAppender(notificationLog);
        ((Logger) LoggerFactory.getLogger(LoggingAsyncExceptionHandler.class)).detachAppender(handlerLog);
    }

    private ListAppender<ILoggingEvent> attach(Class<?> type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        logger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    @Test
    void slowMethod_doesNotBlockCallerAndFinishesLaterInExecutorThread() throws Exception {
        ModificationDto dto = new ModificationDto();
        dto.setName("Test mod");

        long start = System.nanoTime();
        notificationService.sendModCreatedEmail(dto);
        long callerBlockedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(callerBlockedMillis < 400, "Виклик заблокував потік на " + callerBlockedMillis + " мс");
        assertTrue(notificationLog.list.isEmpty());

        assertTrue(TestAwait.until(() -> !notificationLog.list.isEmpty(), 3000));
        String threadName = notificationLog.list.get(0).getThreadName();
        assertTrue(threadName.startsWith(AsyncConfig.THREAD_PREFIX), threadName);
    }

    @Test
    void executorIsExplicitlyConfiguredWithBoundedResources() {
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertEquals(10, executor.getQueueCapacity());
        assertEquals(AsyncConfig.THREAD_PREFIX, executor.getThreadNamePrefix());
    }

    @Test
    void asyncMethod_runsInCustomExecutorNotInDefaultPools() throws Exception {
        String threadName = probe.currentThreadName().get(2, TimeUnit.SECONDS);

        assertTrue(threadName.startsWith(AsyncConfig.THREAD_PREFIX), threadName);
        assertFalse(threadName.contains("SimpleAsyncTaskExecutor"));
        assertFalse(threadName.contains("ForkJoinPool"));
        assertFalse(threadName.startsWith("task-"));
    }

    @Test
    void twoIndependentCalls_overlapInTimeAndCombineIntoOneResult() throws Exception {
        CompletableFuture<long[]> first = probe.timedSleep(400);
        CompletableFuture<long[]> second = probe.timedSleep(400);

        long[][] intervals = first.thenCombine(second, (a, b) -> new long[][]{a, b}).get(3, TimeUnit.SECONDS);

        long[] a = intervals[0];
        long[] b = intervals[1];
        assertTrue(a[0] < b[1] && b[0] < a[1], "Інтервали виконання не перетинаються");
    }

    @Test
    void reportBuildsFromTwoParallelSlowCallsFasterThanSequentially() {
        ModificationDto dto = new ModificationDto();
        dto.setId(1L);
        dto.setName("Test mod");
        when(modificationService.getModById(1L)).thenReturn(dto);

        long start = System.nanoTime();
        ModReportDto report = reportService.buildReport(1L);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertFalse(report.degraded());
        assertTrue(report.pdfGenerated());
        assertNotNull(report.rating());
        assertTrue(elapsedMillis < 900, "Виклики виконувалися послідовно: " + elapsedMillis + " мс");
    }

    @Test
    void failedFuture_isHandledByCallerWithoutPropagatingError() throws Exception {
        String result = probe.failingFuture()
                .exceptionally(ex -> "fallback")
                .get(2, TimeUnit.SECONDS);

        assertEquals("fallback", result);
    }

    @Test
    void waitingForSlowFuture_isBoundedByTimeout() throws Exception {
        CompletableFuture<long[]> slow = probe.timedSleep(600);

        assertThrows(TimeoutException.class, () -> slow.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void voidAsyncFailure_isLoggedByUncaughtExceptionHandler() throws Exception {
        probe.failingVoid();

        assertTrue(TestAwait.until(() -> !handlerLog.list.isEmpty(), 3000));
        ILoggingEvent event = handlerLog.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("failingVoid"));
        assertEquals("void boom", event.getThrowableProxy().getMessage());
    }
}

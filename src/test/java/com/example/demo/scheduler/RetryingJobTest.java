package com.example.demo.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetryingJobTest {

    private JobExecutionContext context;
    private AtomicInteger runs;

    @BeforeEach
    void setUp() {
        context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());
        runs = new AtomicInteger();
    }

    private RetryingJob failingJob(int failuresBeforeSuccess) {
        return new RetryingJob() {
            @Override
            protected void run(JobDataMap data) {
                if (runs.incrementAndGet() <= failuresBeforeSuccess) {
                    throw new IllegalStateException("db unavailable");
                }
            }
        };
    }

    private int simulateQuartzRefires(RetryingJob job) {
        int executions = 0;
        for (int refire = 0; refire < 100; refire++) {
            when(context.getRefireCount()).thenReturn(refire);
            executions++;
            try {
                job.execute(context);
                return executions;
            } catch (JobExecutionException ex) {
                assertTrue(ex.refireImmediately());
            }
        }
        throw new AssertionError("Нескінченний цикл повторів");
    }

    @Test
    void persistentFailure_isRetriedLimitedNumberOfTimesThenRecordedAndSwallowed() {
        int executions = simulateQuartzRefires(failingJob(Integer.MAX_VALUE));

        assertEquals(RetryingJob.MAX_REFIRES + 1, executions);
        assertEquals(RetryingJob.MAX_REFIRES + 1, runs.get());
    }

    @Test
    void transientFailure_succeedsOnRetry() {
        int executions = simulateQuartzRefires(failingJob(1));

        assertEquals(2, executions);
    }

    @Test
    void firstSuccess_runsOnce() {
        int executions = simulateQuartzRefires(failingJob(0));

        assertEquals(1, executions);
        assertEquals(1, runs.get());
    }
}

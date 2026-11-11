package com.example.demo.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public abstract class RetryingJob implements Job {

    public static final int MAX_REFIRES = 3;

    @Override
    public final void execute(JobExecutionContext context) throws JobExecutionException {
        String jobName = getClass().getSimpleName();
        try {
            run(context.getMergedJobDataMap());
        } catch (IllegalArgumentException ex) {
            throw failure("Некоректні параметри завдання " + jobName + ": " + ex.getMessage(), ex, false);
        } catch (Exception ex) {
            int refires = context.getRefireCount();
            if (refires < MAX_REFIRES) {
                log.warn("Збій {} (повтор {}/{}): {}", jobName, refires + 1, MAX_REFIRES, ex.toString());
                throw failure("Збій " + jobName, ex, true);
            }
            log.error("Завдання {} не виконано після {} повторів, чекаємо наступного запуску за розкладом",
                    jobName, MAX_REFIRES, ex);
        }
    }

    private static JobExecutionException failure(String message, Throwable cause, boolean refireImmediately) {
        JobExecutionException exception = new JobExecutionException(message, cause);
        exception.setRefireImmediately(refireImmediately);
        return exception;
    }

    protected abstract void run(JobDataMap data) throws Exception;
}

package com.example.demo.scheduler;

import com.example.demo.service.SnapshotCleanupService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;

@DisallowConcurrentExecution
public class SnapshotCleanupJob extends RetryingJob {

    public static final String RETENTION_DAYS = "retentionDays";

    private final SnapshotCleanupService cleanupService;

    public SnapshotCleanupJob(SnapshotCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @Override
    protected void run(JobDataMap data) {
        cleanupService.deleteExpired(JobParameters.requirePositiveInt(data, RETENTION_DAYS));
    }
}

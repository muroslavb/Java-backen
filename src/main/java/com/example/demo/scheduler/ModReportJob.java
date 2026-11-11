package com.example.demo.scheduler;

import com.example.demo.service.ModReportSnapshotService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;

@DisallowConcurrentExecution
public class ModReportJob extends RetryingJob {

    public static final String SCOPE = "scope";
    public static final String REPORT_NAME = "reportName";

    private final ModReportSnapshotService snapshotService;

    public ModReportJob(ModReportSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Override
    protected void run(JobDataMap data) {
        snapshotService.createSnapshot(
                JobParameters.requireString(data, SCOPE),
                JobParameters.requireString(data, REPORT_NAME));
    }
}

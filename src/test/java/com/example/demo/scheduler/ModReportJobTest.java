package com.example.demo.scheduler;

import com.example.demo.service.ModReportSnapshotService;
import com.example.demo.service.SnapshotCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModReportJobTest {

    @Mock
    private ModReportSnapshotService snapshotService;

    @Mock
    private SnapshotCleanupService cleanupService;

    @Mock
    private JobExecutionContext context;

    @Test
    void reportJob_readsParametersAndMakesSingleServiceCall() throws Exception {
        JobDataMap data = new JobDataMap();
        data.put(ModReportJob.SCOPE, "WEEKLY");
        data.put(ModReportJob.REPORT_NAME, "digest");
        when(context.getMergedJobDataMap()).thenReturn(data);

        new ModReportJob(snapshotService).execute(context);

        verify(snapshotService).createSnapshot("WEEKLY", "digest");
        verifyNoMoreInteractions(snapshotService);
    }

    @Test
    void reportJob_misspelledKey_failsExplicitlyWithoutCallingService() {
        JobDataMap data = new JobDataMap();
        data.put("scop", "WEEKLY");
        data.put(ModReportJob.REPORT_NAME, "digest");
        when(context.getMergedJobDataMap()).thenReturn(data);

        JobExecutionException ex = assertThrows(JobExecutionException.class,
                () -> new ModReportJob(snapshotService).execute(context));

        assertFalse(ex.refireImmediately());
        verifyNoInteractions(snapshotService);
    }

    @Test
    void cleanupJob_readsRetentionAndMakesSingleServiceCall() throws Exception {
        JobDataMap data = new JobDataMap();
        data.put(SnapshotCleanupJob.RETENTION_DAYS, 30);
        when(context.getMergedJobDataMap()).thenReturn(data);

        new SnapshotCleanupJob(cleanupService).execute(context);

        verify(cleanupService).deleteExpired(30);
        verifyNoMoreInteractions(cleanupService);
    }

    @Test
    void cleanupJob_missingRetention_failsExplicitly() {
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        assertThrows(JobExecutionException.class, () -> new SnapshotCleanupJob(cleanupService).execute(context));

        verifyNoInteractions(cleanupService);
    }
}

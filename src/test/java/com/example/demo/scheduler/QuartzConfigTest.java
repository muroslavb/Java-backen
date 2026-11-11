package com.example.demo.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.time.DateTimeException;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class QuartzConfigTest {

    private static final String ZONE = "Europe/Kyiv";

    private QuartzConfig config;
    private JobDetail reportJob;
    private JobDetail cleanupJob;
    private Trigger dailyTrigger;
    private Trigger weeklyTrigger;
    private Trigger cleanupTrigger;

    @BeforeEach
    void setUp() {
        config = new QuartzConfig();
        reportJob = config.modReportJobDetail();
        cleanupJob = config.snapshotCleanupJobDetail();
        dailyTrigger = config.modReportDailyTrigger("0 0 2 * * ?", ZONE);
        weeklyTrigger = config.modReportWeeklyTrigger("0 0 3 ? * MON", ZONE);
        cleanupTrigger = config.snapshotCleanupTrigger("0 30 3 * * ?", ZONE);
    }

    @Test
    void everyTriggerPointsToItsJobByJobKey() {
        assertEquals(reportJob.getKey(), dailyTrigger.getJobKey());
        assertEquals(reportJob.getKey(), weeklyTrigger.getJobKey());
        assertEquals(cleanupJob.getKey(), cleanupTrigger.getJobKey());
    }

    @Test
    void jobsAndTriggersHaveExplicitNamesAndGroups() {
        for (JobDetail job : List.of(reportJob, cleanupJob)) {
            assertTrue(!job.getKey().getName().isBlank());
            assertNotEquals("DEFAULT", job.getKey().getGroup());
        }
        for (Trigger trigger : List.of(dailyTrigger, weeklyTrigger, cleanupTrigger)) {
            assertTrue(!trigger.getKey().getName().isBlank());
            assertNotEquals("DEFAULT", trigger.getKey().getGroup());
        }
    }

    @Test
    void reportJobHasTwoTriggers() {
        long triggersOfReportJob = List.of(dailyTrigger, weeklyTrigger, cleanupTrigger).stream()
                .filter(trigger -> trigger.getJobKey().equals(QuartzConfig.REPORT_JOB_KEY))
                .count();

        assertEquals(2, triggersOfReportJob);
    }

    @Test
    void jobsAreDurableRecoverableAndNonConcurrent() {
        for (JobDetail job : List.of(reportJob, cleanupJob)) {
            assertTrue(job.isDurable());
            assertTrue(job.requestsRecovery());
            assertTrue(job.getJobClass().isAnnotationPresent(DisallowConcurrentExecution.class));
        }
    }

    @Test
    void triggerDataOverridesJobDefaultsAndMissingKeysKeepDefaults() throws Exception {
        JobDataMap weeklyMerged = mergedDataMap(reportJob, weeklyTrigger);
        JobDataMap dailyMerged = mergedDataMap(reportJob, dailyTrigger);

        assertEquals("WEEKLY", weeklyMerged.getString(ModReportJob.SCOPE));
        assertEquals(QuartzConfig.DEFAULT_REPORT_NAME, weeklyMerged.getString(ModReportJob.REPORT_NAME));
        assertEquals(QuartzConfig.DEFAULT_SCOPE, dailyMerged.getString(ModReportJob.SCOPE));
        assertEquals(QuartzConfig.DEFAULT_REPORT_NAME, dailyMerged.getString(ModReportJob.REPORT_NAME));
        assertEquals("DAILY", reportJob.getJobDataMap().getString(ModReportJob.SCOPE));
    }

    @Test
    void cleanupJobKeepsDefaultRetention() {
        assertEquals(QuartzConfig.DEFAULT_RETENTION_DAYS,
                cleanupJob.getJobDataMap().getInt(SnapshotCleanupJob.RETENTION_DAYS));
    }

    @Test
    void everyTriggerHasExplicitMisfireInstruction() {
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, dailyTrigger.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, weeklyTrigger.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, cleanupTrigger.getMisfireInstruction());
        for (Trigger trigger : List.of(dailyTrigger, weeklyTrigger, cleanupTrigger)) {
            assertNotEquals(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY, trigger.getMisfireInstruction());
        }
    }

    @Test
    void cronTriggersUseExplicitTimeZone() {
        for (Trigger trigger : List.of(dailyTrigger, weeklyTrigger, cleanupTrigger)) {
            assertEquals(ZONE, ((CronTrigger) trigger).getTimeZone().getID());
        }
    }

    @Test
    void unknownTimeZoneIsRejectedInsteadOfSilentlyBecomingGmt() {
        assertEquals("GMT", TimeZone.getTimeZone("Not/AZone").getID());
        assertThrows(DateTimeException.class, () -> QuartzConfig.resolve("Not/AZone"));
        assertThrows(DateTimeException.class, () -> config.modReportDailyTrigger("0 0 2 * * ?", "Not/AZone"));
    }

    private JobDataMap mergedDataMap(JobDetail job, Trigger trigger) {
        TriggerFiredBundle bundle = new TriggerFiredBundle(
                job, (OperableTrigger) trigger, null, false, null, null, null, null);
        return new JobExecutionContextImpl(mock(Scheduler.class), bundle, null).getMergedJobDataMap();
    }
}

package com.example.demo.scheduler;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class QuartzConfig {

    public static final String REPORT_GROUP = "reports";
    public static final String MAINTENANCE_GROUP = "maintenance";

    public static final JobKey REPORT_JOB_KEY = JobKey.jobKey("modReportJob", REPORT_GROUP);
    public static final JobKey CLEANUP_JOB_KEY = JobKey.jobKey("snapshotCleanupJob", MAINTENANCE_GROUP);

    public static final String DEFAULT_SCOPE = "DAILY";
    public static final String DEFAULT_REPORT_NAME = "mod-catalog-digest";
    public static final int DEFAULT_RETENTION_DAYS = 30;

    @Bean
    public JobDetail modReportJobDetail() {
        return JobBuilder.newJob(ModReportJob.class)
                .withIdentity(REPORT_JOB_KEY)
                .withDescription("Щоденний і щотижневий звіт про каталог модів")
                .usingJobData(ModReportJob.SCOPE, DEFAULT_SCOPE)
                .usingJobData(ModReportJob.REPORT_NAME, DEFAULT_REPORT_NAME)
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger modReportDailyTrigger(
            @Value("${app.scheduler.report-daily-cron:0 0 2 * * ?}") String cron,
            @Value("${app.scheduler.time-zone:Europe/Kyiv}") String timeZone) {
        return TriggerBuilder.newTrigger()
                .forJob(REPORT_JOB_KEY)
                .withIdentity("modReportDailyTrigger", REPORT_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(resolve(timeZone))
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    @Bean
    public Trigger modReportWeeklyTrigger(
            @Value("${app.scheduler.report-weekly-cron:0 0 3 ? * MON}") String cron,
            @Value("${app.scheduler.time-zone:Europe/Kyiv}") String timeZone) {
        return TriggerBuilder.newTrigger()
                .forJob(REPORT_JOB_KEY)
                .withIdentity("modReportWeeklyTrigger", REPORT_GROUP)
                .usingJobData(ModReportJob.SCOPE, "WEEKLY")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(resolve(timeZone))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public JobDetail snapshotCleanupJobDetail() {
        return JobBuilder.newJob(SnapshotCleanupJob.class)
                .withIdentity(CLEANUP_JOB_KEY)
                .withDescription("Видалення застарілих звітів")
                .usingJobData(SnapshotCleanupJob.RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger snapshotCleanupTrigger(
            @Value("${app.scheduler.cleanup-cron:0 30 3 * * ?}") String cron,
            @Value("${app.scheduler.time-zone:Europe/Kyiv}") String timeZone) {
        return TriggerBuilder.newTrigger()
                .forJob(CLEANUP_JOB_KEY)
                .withIdentity("snapshotCleanupTrigger", MAINTENANCE_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(resolve(timeZone))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    static TimeZone resolve(String timeZoneId) {
        return TimeZone.getTimeZone(ZoneId.of(timeZoneId));
    }
}

package com.example.demo.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuartzSchemaConfigTest {

    private static final List<String> QUARTZ_TABLES = List.of(
            "qrtz_job_details", "qrtz_triggers", "qrtz_simple_triggers", "qrtz_cron_triggers",
            "qrtz_simprop_triggers", "qrtz_blob_triggers", "qrtz_calendars", "qrtz_paused_trigger_grps",
            "qrtz_fired_triggers", "qrtz_scheduler_state", "qrtz_locks");

    private String read(String path) throws Exception {
        return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    void liquibaseCreatesAllQuartzTables() throws Exception {
        String sql = read("db/changelog/quartz/tables_quartz_postgres.sql").toLowerCase();
        for (String table : QUARTZ_TABLES) {
            assertTrue(sql.contains("create table " + table), table);
        }
        assertTrue(read("db/changelog/db.changelog-quartz.xml").contains("quartz/tables_quartz_postgres.sql"));
    }

    @Test
    void springBootUsesJdbcStoreAndDoesNotInitializeSchema() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertEquals("jdbc", properties.getProperty("spring.quartz.job-store-type"));
        assertEquals("never", properties.getProperty("spring.quartz.jdbc.initialize-schema"));
        assertEquals("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate",
                properties.getProperty("spring.quartz.properties.org.quartz.jobStore.driverDelegateClass"));
    }
}

package com.example.demo.scheduler;

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobParametersTest {

    @Test
    void presentKey_isReturned() {
        JobDataMap data = new JobDataMap();
        data.put("scope", "WEEKLY");
        data.put("retentionDays", 14);

        assertEquals("WEEKLY", JobParameters.requireString(data, "scope"));
        assertEquals(14, JobParameters.requirePositiveInt(data, "retentionDays"));
    }

    @Test
    void misspelledKey_throwsExplicitException() {
        JobDataMap data = new JobDataMap();
        data.put("scop", "WEEKLY");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JobParameters.requireString(data, "scope"));
        assertEquals("Відсутній параметр завдання: scope", ex.getMessage());
    }

    @Test
    void missingNumericKey_throwsExplicitException() {
        assertThrows(IllegalArgumentException.class,
                () -> JobParameters.requirePositiveInt(new JobDataMap(), "retentionDays"));
    }

    @Test
    void invalidValues_areRejected() {
        JobDataMap data = new JobDataMap();
        data.put("blank", " ");
        data.put("zero", 0);
        data.put("text", "abc");

        assertThrows(IllegalArgumentException.class, () -> JobParameters.requireString(data, "blank"));
        assertThrows(IllegalArgumentException.class, () -> JobParameters.requirePositiveInt(data, "zero"));
        assertThrows(IllegalArgumentException.class, () -> JobParameters.requirePositiveInt(data, "text"));
    }

    @Test
    void numericStringIsAccepted() {
        JobDataMap data = new JobDataMap();
        data.put("retentionDays", "7");

        assertEquals(7, JobParameters.requirePositiveInt(data, "retentionDays"));
    }
}

package com.example.demo.scheduler;

import org.quartz.JobDataMap;

public final class JobParameters {

    private JobParameters() {
    }

    public static String requireString(JobDataMap data, String key) {
        Object value = require(data, key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Параметр '" + key + "' має бути непорожнім рядком");
        }
        return text;
    }

    public static int requirePositiveInt(JobDataMap data, String key) {
        Object value = require(data, key);
        int number;
        if (value instanceof Number n) {
            number = n.intValue();
        } else if (value instanceof String text) {
            try {
                number = Integer.parseInt(text.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Параметр '" + key + "' має бути числом: " + text);
            }
        } else {
            throw new IllegalArgumentException("Параметр '" + key + "' має бути числом");
        }
        if (number <= 0) {
            throw new IllegalArgumentException("Параметр '" + key + "' має бути додатним: " + number);
        }
        return number;
    }

    private static Object require(JobDataMap data, String key) {
        if (!data.containsKey(key)) {
            throw new IllegalArgumentException("Відсутній параметр завдання: " + key);
        }
        return data.get(key);
    }
}

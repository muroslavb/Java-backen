package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.scheduler.time-zone:Europe/Kyiv}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}

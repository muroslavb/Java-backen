package com.example.demo.async;

import com.example.demo.dto.ModificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationAsyncService {

    private final long delayMillis;

    public NotificationAsyncService(@Value("${app.async.email-delay-ms:1000}") long delayMillis) {
        this.delayMillis = delayMillis;
    }

    @Async(AsyncConfig.EXECUTOR)
    public void sendModCreatedEmail(ModificationDto mod) {
        Delays.pause(delayMillis);
        log.info("Лист про створення моду '{}' надіслано автору {}", mod.getName(), mod.getAuthorUsername());
    }
}

package com.example.demo.async;

import com.example.demo.dto.ModificationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class PdfReportAsyncService {

    private final long delayMillis;

    public PdfReportAsyncService(@Value("${app.async.pdf-delay-ms:1000}") long delayMillis) {
        this.delayMillis = delayMillis;
    }

    @Async(AsyncConfig.EXECUTOR)
    public CompletableFuture<byte[]> generatePdf(ModificationDto mod) {
        Delays.pause(delayMillis);
        String document = "%PDF-1.4\n"
                + "mod-id: " + mod.getId() + "\n"
                + "name: " + mod.getName() + "\n"
                + "version: " + mod.getVersion() + "\n"
                + "%%EOF";
        return CompletableFuture.completedFuture(document.getBytes(StandardCharsets.UTF_8));
    }
}

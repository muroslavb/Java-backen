package com.example.demo.service;

import com.example.demo.async.PdfReportAsyncService;
import com.example.demo.async.RatingAsyncService;
import com.example.demo.dto.ModRating;
import com.example.demo.dto.ModReportDto;
import com.example.demo.dto.ModificationDto;
import com.example.demo.exception.ReportTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class ModReportService {

    private final ModificationService modificationService;
    private final PdfReportAsyncService pdfService;
    private final RatingAsyncService ratingService;
    private final long timeoutMillis;

    public ModReportService(ModificationService modificationService,
                            PdfReportAsyncService pdfService,
                            RatingAsyncService ratingService,
                            @Value("${app.async.report-timeout-ms:5000}") long timeoutMillis) {
        this.modificationService = modificationService;
        this.pdfService = pdfService;
        this.ratingService = ratingService;
        this.timeoutMillis = timeoutMillis;
    }

    public ModReportDto buildReport(Long id) {
        ModificationDto mod = modificationService.getModById(id);

        CompletableFuture<byte[]> pdf = pdfService.generatePdf(mod);
        CompletableFuture<ModRating> rating = ratingService.fetchRating(id);

        CompletableFuture<byte[]> safePdf = pdf.exceptionally(ex -> {
            log.warn("Генерація PDF для моду {} не вдалася: {}", id, ex.toString());
            return null;
        });
        CompletableFuture<ModRating> safeRating = rating.exceptionally(ex -> {
            log.warn("Запит рейтингу для моду {} не вдався: {}", id, ex.toString());
            return null;
        });

        CompletableFuture<ModReportDto> report = safePdf.thenCombine(safeRating,
                (pdfBytes, modRating) -> ModReportDto.of(mod, pdfBytes, modRating));

        try {
            return report.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            pdf.cancel(true);
            rating.cancel(true);
            throw new ReportTimeoutException("Звіт не сформовано за " + timeoutMillis + " мс");
        } catch (ExecutionException ex) {
            log.warn("Не вдалося об'єднати результати звіту для моду {}", id, ex);
            return ModReportDto.failed(mod);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ModReportDto.failed(mod);
        }
    }
}

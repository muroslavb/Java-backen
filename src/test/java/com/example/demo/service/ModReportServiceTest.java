package com.example.demo.service;

import com.example.demo.async.PdfReportAsyncService;
import com.example.demo.async.RatingAsyncService;
import com.example.demo.dto.ModRating;
import com.example.demo.dto.ModReportDto;
import com.example.demo.dto.ModificationDto;
import com.example.demo.exception.ReportTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModReportServiceTest {

    @Mock
    private ModificationService modificationService;

    @Mock
    private PdfReportAsyncService pdfService;

    @Mock
    private RatingAsyncService ratingService;

    private ModReportService reportService;
    private ModificationDto mod;

    @BeforeEach
    void setUp() {
        mod = new ModificationDto();
        mod.setId(1L);
        mod.setName("Test mod");
        when(modificationService.getModById(1L)).thenReturn(mod);
        reportService = new ModReportService(modificationService, pdfService, ratingService, 200);
    }

    @Test
    void bothCallsSucceed_reportIsComplete() {
        when(pdfService.generatePdf(any())).thenReturn(CompletableFuture.completedFuture(new byte[]{1, 2, 3}));
        when(ratingService.fetchRating(1L)).thenReturn(CompletableFuture.completedFuture(new ModRating(4.5, 10)));

        ModReportDto report = reportService.buildReport(1L);

        assertFalse(report.degraded());
        assertTrue(report.pdfGenerated());
        assertEquals(3, report.pdfSizeBytes());
        assertEquals(4.5, report.rating());
        assertEquals(10, report.ratingVotes());
    }

    @Test
    void pdfFails_requestStillSucceedsWithDegradedReport() {
        when(pdfService.generatePdf(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("pdf")));
        when(ratingService.fetchRating(1L)).thenReturn(CompletableFuture.completedFuture(new ModRating(4.0, 5)));

        ModReportDto report = reportService.buildReport(1L);

        assertTrue(report.degraded());
        assertFalse(report.pdfGenerated());
        assertEquals(4.0, report.rating());
    }

    @Test
    void ratingFails_requestStillSucceedsWithDegradedReport() {
        when(pdfService.generatePdf(any())).thenReturn(CompletableFuture.completedFuture(new byte[]{1}));
        when(ratingService.fetchRating(1L)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("rating")));

        ModReportDto report = reportService.buildReport(1L);

        assertTrue(report.degraded());
        assertTrue(report.pdfGenerated());
        assertNull(report.rating());
    }

    @Test
    void bothFail_requestStillReturnsReport() {
        when(pdfService.generatePdf(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("pdf")));
        when(ratingService.fetchRating(1L)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("rating")));

        ModReportDto report = reportService.buildReport(1L);

        assertTrue(report.degraded());
        assertEquals("Test mod", report.modName());
    }

    @Test
    void neverCompletingFuture_waitIsBoundedByTimeout() {
        CompletableFuture<byte[]> hangingPdf = new CompletableFuture<>();
        CompletableFuture<ModRating> hangingRating = new CompletableFuture<>();
        when(pdfService.generatePdf(any())).thenReturn(hangingPdf);
        when(ratingService.fetchRating(1L)).thenReturn(hangingRating);

        long start = System.nanoTime();
        assertThrows(ReportTimeoutException.class, () -> reportService.buildReport(1L));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMillis < 2000);
        assertTrue(hangingPdf.isCancelled());
        assertTrue(hangingRating.isCancelled());
    }

    @Test
    void executorRejectsTask_exceptionIsPropagatedForExplicitHandling() {
        when(pdfService.generatePdf(any())).thenThrow(new TaskRejectedException("executor is full"));

        assertThrows(TaskRejectedException.class, () -> reportService.buildReport(1L));
    }
}

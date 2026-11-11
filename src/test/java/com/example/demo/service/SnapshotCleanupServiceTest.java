package com.example.demo.service;

import com.example.demo.repository.ModReportSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SnapshotCleanupServiceTest {

    @Mock
    private ModReportSnapshotRepository snapshotRepository;

    private final List<LocalDate> storedDates = new ArrayList<>(List.of(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 20),
            LocalDate.of(2026, 11, 9)));

    private SnapshotCleanupService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneId.of("Europe/Kyiv"));
        lenient().when(snapshotRepository.deleteOlderThan(any())).thenAnswer(invocation -> {
            LocalDate cutoff = invocation.getArgument(0);
            int before = storedDates.size();
            storedDates.removeIf(date -> date.isBefore(cutoff));
            return before - storedDates.size();
        });
        service = new SnapshotCleanupService(snapshotRepository, clock);
    }

    @Test
    void secondRunRightAfterFirst_deletesNothingMore() {
        int first = service.deleteExpired(30);
        List<LocalDate> afterFirst = List.copyOf(storedDates);
        int second = service.deleteExpired(30);

        assertEquals(2, first);
        assertEquals(0, second);
        assertEquals(afterFirst, storedDates);
        assertEquals(2, storedDates.size());
    }
}

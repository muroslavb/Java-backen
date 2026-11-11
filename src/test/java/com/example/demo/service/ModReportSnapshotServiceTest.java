package com.example.demo.service;

import com.example.demo.entity.ModReportSnapshot;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.ModReportSnapshotRepository;
import com.example.demo.repository.ModificationRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ModReportSnapshotServiceTest {

    @Mock
    private ModReportSnapshotRepository snapshotRepository;

    @Mock
    private ModificationRepository modificationRepository;

    @Mock
    private GameRepository gameRepository;

    private final List<ModReportSnapshot> store = new ArrayList<>();
    private ModReportSnapshotService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneId.of("Europe/Kyiv"));
        lenient().when(snapshotRepository.existsBySnapshotDateAndScope(any(), any())).thenAnswer(invocation ->
                store.stream().anyMatch(s -> s.getSnapshotDate().equals(invocation.getArgument(0))
                        && s.getScope().equals(invocation.getArgument(1))));
        lenient().when(snapshotRepository.save(any(ModReportSnapshot.class))).thenAnswer(invocation -> {
            ModReportSnapshot snapshot = invocation.getArgument(0);
            store.add(snapshot);
            return snapshot;
        });
        lenient().when(modificationRepository.count()).thenReturn(5L);
        lenient().when(gameRepository.count()).thenReturn(2L);
        service = new ModReportSnapshotService(snapshotRepository, modificationRepository, gameRepository, clock);
    }

    @Test
    void secondRunRightAfterFirst_changesNothing() {
        boolean first = service.createSnapshot("DAILY", "digest");
        List<ModReportSnapshot> afterFirst = List.copyOf(store);
        boolean second = service.createSnapshot("DAILY", "digest");

        assertTrue(first);
        assertFalse(second);
        assertEquals(1, store.size());
        assertEquals(afterFirst, store);
    }

    @Test
    void snapshotContainsCountsAndDateInConfiguredZone() {
        service.createSnapshot("DAILY", "digest");

        ModReportSnapshot snapshot = store.get(0);
        assertEquals(LocalDate.of(2026, 11, 9), snapshot.getSnapshotDate());
        assertEquals(5L, snapshot.getTotalMods());
        assertEquals(2L, snapshot.getTotalGames());
        assertEquals("digest", snapshot.getReportName());
    }

    @Test
    void differentScopeSameDay_isSeparateReport() {
        service.createSnapshot("DAILY", "digest");
        service.createSnapshot("WEEKLY", "digest");

        assertEquals(2, store.size());
    }
}

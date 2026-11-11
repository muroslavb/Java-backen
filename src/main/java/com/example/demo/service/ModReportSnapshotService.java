package com.example.demo.service;

import com.example.demo.entity.ModReportSnapshot;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.ModReportSnapshotRepository;
import com.example.demo.repository.ModificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModReportSnapshotService {

    private final ModReportSnapshotRepository snapshotRepository;
    private final ModificationRepository modificationRepository;
    private final GameRepository gameRepository;
    private final Clock clock;

    @Transactional
    public boolean createSnapshot(String scope, String reportName) {
        LocalDate today = LocalDate.now(clock);
        if (snapshotRepository.existsBySnapshotDateAndScope(today, scope)) {
            log.info("Звіт {} за {} уже існує, пропускаємо", scope, today);
            return false;
        }

        ModReportSnapshot snapshot = new ModReportSnapshot();
        snapshot.setSnapshotDate(today);
        snapshot.setScope(scope);
        snapshot.setReportName(reportName);
        snapshot.setTotalMods(modificationRepository.count());
        snapshot.setTotalGames(gameRepository.count());
        snapshot.setCreatedAt(LocalDateTime.now(clock));
        snapshotRepository.save(snapshot);

        log.info("Створено звіт {} за {}: модів {}, ігор {}",
                scope, today, snapshot.getTotalMods(), snapshot.getTotalGames());
        return true;
    }
}

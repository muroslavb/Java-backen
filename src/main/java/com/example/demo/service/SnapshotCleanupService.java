package com.example.demo.service;

import com.example.demo.repository.ModReportSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotCleanupService {

    private final ModReportSnapshotRepository snapshotRepository;
    private final Clock clock;

    @Transactional
    public int deleteExpired(int retentionDays) {
        LocalDate cutoff = LocalDate.now(clock).minusDays(retentionDays);
        int deleted = snapshotRepository.deleteOlderThan(cutoff);
        log.info("Видалено застарілих звітів: {} (старші за {})", deleted, cutoff);
        return deleted;
    }
}

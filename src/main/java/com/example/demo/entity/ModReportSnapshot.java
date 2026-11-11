package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mod_report_snapshots")
@Data
public class ModReportSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private String scope;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Column(name = "total_mods", nullable = false)
    private long totalMods;

    @Column(name = "total_games", nullable = false)
    private long totalGames;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

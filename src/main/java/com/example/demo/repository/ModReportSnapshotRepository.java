package com.example.demo.repository;

import com.example.demo.entity.ModReportSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ModReportSnapshotRepository extends JpaRepository<ModReportSnapshot, Long> {

    boolean existsBySnapshotDateAndScope(LocalDate snapshotDate, String scope);

    @Modifying
    @Query("delete from ModReportSnapshot s where s.snapshotDate < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDate cutoff);
}

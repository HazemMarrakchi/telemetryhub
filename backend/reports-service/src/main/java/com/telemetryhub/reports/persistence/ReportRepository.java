package com.telemetryhub.reports.persistence;

import com.telemetryhub.reports.domain.GeneratedReport;
import com.telemetryhub.reports.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<GeneratedReport, UUID> {

    Page<GeneratedReport> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<GeneratedReport> findByIdAndTenantId(UUID id, UUID tenantId);

    List<GeneratedReport> findByStatusAndCreatedAtBefore(ReportStatus status, Instant before);

    @Query("select distinct r.tenantId from GeneratedReport r")
    List<UUID> findDistinctTenantIds();
}
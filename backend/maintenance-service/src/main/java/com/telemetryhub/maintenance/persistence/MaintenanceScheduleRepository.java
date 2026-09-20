package com.telemetryhub.maintenance.persistence;

import com.telemetryhub.maintenance.domain.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, UUID> {

    List<MaintenanceSchedule> findByTenantIdOrderByNextRunAtAsc(UUID tenantId);

    Optional<MaintenanceSchedule> findByIdAndTenantId(UUID id, UUID tenantId);

    List<MaintenanceSchedule> findByActiveTrueAndNextRunAtBefore(Instant now);
}
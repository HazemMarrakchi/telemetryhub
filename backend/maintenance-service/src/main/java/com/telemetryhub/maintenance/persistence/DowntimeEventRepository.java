package com.telemetryhub.maintenance.persistence;

import com.telemetryhub.maintenance.domain.DowntimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, UUID> {

    Optional<DowntimeEvent> findFirstByTenantIdAndEquipmentIdAndEndedAtIsNullOrderByStartedAtDesc(
            UUID tenantId, UUID equipmentId);

    Optional<DowntimeEvent> findByTenantIdAndAlertIdAndEndedAtIsNull(UUID tenantId, UUID alertId);

    @Query("""
            select d from DowntimeEvent d
            where d.tenantId = :tenantId
              and d.startedAt < :windowEnd
              and (d.endedAt is null or d.endedAt > :windowStart)
              and (:equipmentId is null or d.equipmentId = :equipmentId)
            """)
    List<DowntimeEvent> overlapping(@Param("tenantId") UUID tenantId,
                                    @Param("equipmentId") UUID equipmentId,
                                    @Param("windowStart") Instant windowStart,
                                    @Param("windowEnd") Instant windowEnd);
}
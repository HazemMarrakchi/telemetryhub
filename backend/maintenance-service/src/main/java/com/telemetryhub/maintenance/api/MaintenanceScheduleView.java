package com.telemetryhub.maintenance.api;

import com.telemetryhub.maintenance.domain.MaintenanceSchedule;
import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderType;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceScheduleView(
        UUID id,
        UUID equipmentId,
        String title,
        String description,
        WorkOrderType workType,
        WorkOrderPriority priority,
        int intervalDays,
        Instant nextRunAt,
        Instant lastRunAt,
        boolean active,
        Instant createdAt
) {
    public static MaintenanceScheduleView from(MaintenanceSchedule schedule) {
        return new MaintenanceScheduleView(
                schedule.getId(), schedule.getEquipmentId(), schedule.getTitle(),
                schedule.getDescription(), schedule.getWorkType(), schedule.getPriority(),
                schedule.getIntervalDays(), schedule.getNextRunAt(), schedule.getLastRunAt(),
                schedule.isActive(), schedule.getCreatedAt());
    }
}
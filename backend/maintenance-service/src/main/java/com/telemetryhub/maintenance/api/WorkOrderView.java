package com.telemetryhub.maintenance.api;

import com.telemetryhub.maintenance.domain.WorkOrder;
import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderSource;
import com.telemetryhub.maintenance.domain.WorkOrderStatus;
import com.telemetryhub.maintenance.domain.WorkOrderType;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderView(
        UUID id,
        UUID equipmentId,
        String title,
        String description,
        WorkOrderPriority priority,
        WorkOrderStatus status,
        WorkOrderSource source,
        WorkOrderType workType,
        UUID assignedToUserId,
        UUID alertId,
        UUID scheduleId,
        Instant dueAt,
        Instant startedAt,
        Instant completedAt,
        String spareParts,
        Double costEstimate,
        String completionNotes,
        Instant createdAt,
        Instant updatedAt,
        boolean overdue
) {
    public static WorkOrderView from(WorkOrder order) {
        boolean overdue = order.getDueAt() != null
                && order.getStatus() != WorkOrderStatus.COMPLETED
                && order.getStatus() != WorkOrderStatus.CANCELLED
                && order.getDueAt().isBefore(Instant.now());
        return new WorkOrderView(
                order.getId(), order.getEquipmentId(), order.getTitle(), order.getDescription(),
                order.getPriority(), order.getStatus(), order.getSource(), order.getWorkType(),
                order.getAssignedToUserId(), order.getAlertId(), order.getScheduleId(), order.getDueAt(),
                order.getStartedAt(), order.getCompletedAt(), order.getSpareParts(),
                order.getCostEstimate(), order.getCompletionNotes(), order.getCreatedAt(),
                order.getUpdatedAt(), overdue);
    }
}
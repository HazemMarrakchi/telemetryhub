package com.telemetryhub.maintenance.api;

import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderType;

import java.time.Instant;
import java.util.UUID;

public record UpdateWorkOrderRequest(
        String title,
        String description,
        WorkOrderPriority priority,
        WorkOrderType workType,
        UUID equipmentId,
        Instant dueAt,
        String spareParts,
        Double costEstimate
) {
}
package com.telemetryhub.maintenance.api;

import com.telemetryhub.maintenance.domain.WorkOrderPriority;

import java.time.Instant;
import java.util.UUID;

public record UpdateWorkOrderRequest(
        String title,
        String description,
        WorkOrderPriority priority,
        UUID equipmentId,
        Instant dueAt
) {
}
package com.telemetryhub.maintenance.api;

import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateWorkOrderRequest(
        UUID equipmentId,
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
        String title,
        @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
        String description,
        WorkOrderPriority priority,
        WorkOrderType workType,
        Instant dueAt
) {
}
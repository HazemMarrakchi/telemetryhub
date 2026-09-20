package com.telemetryhub.maintenance.api;

import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateScheduleRequest(
        @NotNull(message = "La machine est obligatoire")
        UUID equipmentId,
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
        String title,
        @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
        String description,
        WorkOrderType workType,
        WorkOrderPriority priority,
        @Min(value = 1, message = "La fréquence doit être d'au moins 1 jour")
        int intervalDays,
        Instant startsAt
) {
}
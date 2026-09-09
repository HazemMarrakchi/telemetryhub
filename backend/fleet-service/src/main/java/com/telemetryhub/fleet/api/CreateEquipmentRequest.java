package com.telemetryhub.fleet.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateEquipmentRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String serialNumber,
        UUID modelId,
        UUID siteId,
        @Size(max = 500) String notes,
        Instant nextMaintenanceAt
) {
}
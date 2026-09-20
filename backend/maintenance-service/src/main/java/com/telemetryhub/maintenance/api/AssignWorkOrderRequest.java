package com.telemetryhub.maintenance.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignWorkOrderRequest(@NotNull(message = "Le technicien est obligatoire") UUID assignedToUserId) {
}
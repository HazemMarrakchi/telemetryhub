package com.telemetryhub.alerting.api;

import com.telemetryhub.alerting.domain.ComparisonOperator;
import com.telemetryhub.alerting.domain.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRuleRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        UUID equipmentId,
        @NotBlank @Size(max = 64) String metric,
        @NotNull ComparisonOperator operator,
        @NotNull Double threshold,
        @Size(max = 24) String unit,
        @NotNull AlertSeverity severity,
        int cooldownMinutes
) {
}
package com.telemetryhub.alerting.api;

import com.telemetryhub.alerting.domain.AlertRule;
import com.telemetryhub.alerting.domain.AlertSeverity;
import com.telemetryhub.alerting.domain.ComparisonOperator;

import java.util.UUID;

public record RuleView(
        UUID id,
        String name,
        String description,
        UUID equipmentId,
        String metric,
        ComparisonOperator operator,
        double threshold,
        String unit,
        AlertSeverity severity,
        boolean enabled,
        int cooldownMinutes
) {
    public static RuleView from(AlertRule rule) {
        return new RuleView(
                rule.getId(),
                rule.getName(),
                rule.getDescription(),
                rule.getEquipmentId(),
                rule.getMetric(),
                rule.getOperator(),
                rule.getThreshold(),
                rule.getUnit(),
                rule.getSeverity(),
                rule.isEnabled(),
                rule.getCooldownMinutes());
    }
}
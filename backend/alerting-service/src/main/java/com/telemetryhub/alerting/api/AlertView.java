package com.telemetryhub.alerting.api;

import com.telemetryhub.alerting.domain.AlertEvent;
import com.telemetryhub.alerting.domain.AlertSeverity;
import com.telemetryhub.alerting.domain.AlertStatus;

import java.time.Instant;
import java.util.UUID;

public record AlertView(
        UUID id,
        UUID ruleId,
        String ruleName,
        UUID equipmentId,
        String metric,
        double value,
        double threshold,
        String operatorSymbol,
        AlertSeverity severity,
        AlertStatus status,
        String message,
        Instant triggeredAt,
        Instant acknowledgedAt
) {
    public static AlertView from(AlertEvent event) {
        return new AlertView(
                event.getId(),
                event.getRuleId(),
                event.getRuleName(),
                event.getEquipmentId(),
                event.getMetric(),
                event.getValue(),
                event.getThreshold(),
                event.getOperator().symbol(),
                event.getSeverity(),
                event.getStatus(),
                event.getMessage(),
                event.getTriggeredAt(),
                event.getAcknowledgedAt());
    }
}
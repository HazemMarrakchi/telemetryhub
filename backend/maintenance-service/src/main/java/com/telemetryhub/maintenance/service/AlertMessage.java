package com.telemetryhub.maintenance.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertMessage(
        UUID id,
        UUID tenantId,
        UUID ruleId,
        String ruleName,
        UUID equipmentId,
        String metric,
        double value,
        double threshold,
        String operator,
        String severity,
        String status,
        String message,
        Instant triggeredAt,
        Instant createdAt
) {
    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status);
    }

    public boolean isSevere() {
        return "CRITICAL".equalsIgnoreCase(severity) || "FATAL".equalsIgnoreCase(severity);
    }
}
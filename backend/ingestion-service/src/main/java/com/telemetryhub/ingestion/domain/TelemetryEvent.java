package com.telemetryhub.ingestion.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record TelemetryEvent(
        UUID equipmentId,
        UUID tenantId,
        String metric,
        double value,
        String unit,
        Instant recordedAt,
        String source,
        String deviceFingerprint
) implements Serializable {
}
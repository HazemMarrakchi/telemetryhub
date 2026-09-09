package com.telemetryhub.ingestion.persistence;

import java.time.Instant;
import java.util.UUID;

public record MetricPoint(
        Instant timestamp,
        double value,
        String metric
) {
}
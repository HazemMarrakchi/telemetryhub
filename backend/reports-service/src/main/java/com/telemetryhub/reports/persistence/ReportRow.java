package com.telemetryhub.reports.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportRow(
        Instant timestamp,
        double value,
        String metric,
        UUID equipmentId
) {
}
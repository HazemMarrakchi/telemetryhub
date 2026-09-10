package com.telemetryhub.maintenance.api;

import java.time.Instant;
import java.util.UUID;

public record AvailabilityView(
        UUID equipmentId,
        Instant from,
        Instant to,
        long totalMinutes,
        long uptimeMinutes,
        long downtimeMinutes,
        double availabilityPercent,
        long downtimeCount
) {
}
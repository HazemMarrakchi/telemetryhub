package com.telemetryhub.maintenance.api;

import java.time.Instant;

public record GlobalOeeView(
        Instant from,
        Instant to,
        long totalMinutes,
        long uptimeMinutes,
        long downtimeMinutes,
        double availabilityPercent,
        long downtimeCount,
        long completedOrders,
        long createdOrders,
        double completionRatePercent
) {
}
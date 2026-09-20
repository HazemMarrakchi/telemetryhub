package com.telemetryhub.maintenance.api;

public record KpiView(
        long created,
        long assigned,
        long inProgress,
        long open,
        long overdue,
        long completedToday,
        long completed,
        long downtimeTodayMinutes
) {
}
package com.telemetryhub.reports.api;

import com.telemetryhub.reports.domain.GeneratedReport;
import com.telemetryhub.reports.domain.ReportKind;
import com.telemetryhub.reports.domain.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ReportView(
        UUID id,
        ReportKind kind,
        ReportStatus status,
        String title,
        Instant rangeFrom,
        Instant rangeTo,
        UUID equipmentId,
        String metric,
        String fileName,
        long sizeBytes,
        String checksum,
        Instant createdAt
) {
    public static ReportView from(GeneratedReport report) {
        return new ReportView(
                report.getId(), report.getKind(), report.getStatus(), report.getTitle(),
                report.getRangeFrom(), report.getRangeTo(), report.getEquipmentId(),
                report.getMetric(), report.getFileName(), report.getSizeBytes(),
                report.getChecksum(), report.getCreatedAt());
    }
}
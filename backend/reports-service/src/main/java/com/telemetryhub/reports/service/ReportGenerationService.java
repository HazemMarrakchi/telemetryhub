package com.telemetryhub.reports.service;

import com.telemetryhub.reports.domain.GeneratedReport;
import com.telemetryhub.reports.domain.ReportKind;
import com.telemetryhub.reports.domain.ReportStatus;
import com.telemetryhub.reports.persistence.ReportRepository;
import com.telemetryhub.reports.persistence.ReportRow;
import com.telemetryhub.reports.persistence.TelemetryReadingsAccess;
import com.telemetryhub.reports.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ReportGenerationService {

    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final int BUCKET_SECONDS = 300;
    private static final int RETENTION_DAYS = 30;

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationService.class);

    private final ReportRepository reportRepository;
    private final TelemetryReadingsAccess readingsAccess;
    private final CsvReportBuilder csvBuilder;
    private final PdfReportBuilder pdfBuilder;

    public ReportGenerationService(ReportRepository reportRepository,
                                   TelemetryReadingsAccess readingsAccess,
                                   CsvReportBuilder csvBuilder,
                                   PdfReportBuilder pdfBuilder) {
        this.reportRepository = reportRepository;
        this.readingsAccess = readingsAccess;
        this.csvBuilder = csvBuilder;
        this.pdfBuilder = pdfBuilder;
    }

    @Transactional
    public GeneratedReport generate(UUID tenantId, UUID userId, ReportKind kind,
                                    String metric, UUID equipmentId,
                                    Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("La période du rapport est invalide");
        }
        if (to.isAfter(Instant.now())) {
            throw new IllegalArgumentException("La fin de période ne peut pas être dans le futur");
        }
        GeneratedReport report = new GeneratedReport(
                tenantId, userId, kind, "Rapport " + metric + " (5 min)", from, to,
                equipmentId, metric);
        report = reportRepository.save(report);
        try {
            build(report);
        } catch (Exception ex) {
            log.error("Échec de génération du rapport {}", report.getId(), ex);
            report.markFailed(ex.getMessage());
        }
        return reportRepository.save(report);
    }

    private void build(GeneratedReport report) {
        List<ReportRow> rows = readingsAccess.query(
                report.getTenantId(), report.getEquipmentId(), report.getMetric(),
                report.getRangeFrom(), report.getRangeTo(), BUCKET_SECONDS);

        byte[] content;
        String extension;
        if (report.getKind() == ReportKind.CSV) {
            content = csvBuilder.build(rows).getBytes(StandardCharsets.UTF_8);
            extension = "csv";
        } else {
            content = pdfBuilder.build(report.getTitle(), rows);
            extension = "pdf";
        }
        String fileName = String.format("telemetryhub-%s-%s.%s",
                report.getTitle().replaceAll("[^a-zA-Z0-9-_]", "_").toLowerCase(),
                FILE_FMT.format(Instant.now()), extension);
        report.markReady(fileName, content, sha256(content));
    }

    public Page<GeneratedReport> list(UUID tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return reportRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public GeneratedReport get(UUID id, UUID tenantId) {
        return reportRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable: " + id));
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de calculer le checksum", ex);
        }
    }

    @Transactional
    public void expireOldReadOnlyReports() {
        List<GeneratedReport> old = reportRepository.findByStatusAndCreatedAtBefore(
                ReportStatus.READY, Instant.now().minusSeconds(RETENTION_DAYS * 86400L));
        for (GeneratedReport report : old) {
            report.setStatus(ReportStatus.RETENTION_EXPIRED);
        }
        reportRepository.saveAll(old);
    }

    public static int retentionDays() {
        return RETENTION_DAYS;
    }
}
package com.telemetryhub.reports.service;

import com.telemetryhub.reports.domain.ReportKind;
import com.telemetryhub.reports.persistence.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);
    private static final UUID SYSTEM_USER =
            UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private final ReportGenerationService generationService;
    private final ReportRepository reportRepository;

    public ReportScheduler(ReportGenerationService generationService,
                           ReportRepository reportRepository) {
        this.generationService = generationService;
        this.reportRepository = reportRepository;
    }

    @Scheduled(cron = "0 45 0 * * *")
    public void dailyDigest() {
        List<UUID> tenants = reportRepository.findDistinctTenantIds();
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        Instant from = yesterday.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = yesterday.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        for (UUID tenantId : tenants) {
            try {
                generationService.generate(tenantId, SYSTEM_USER, ReportKind.CSV,
                        "daily-digest", null, from, to);
            } catch (Exception ex) {
                log.error("Échec du rapport quotidien tenant={}", tenantId, ex);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        int before = reportRepository.findByStatusAndCreatedAtBefore(
                com.telemetryhub.reports.domain.ReportStatus.READY,
                Instant.now().minusSeconds(ReportGenerationService.retentionDays() * 86400L)).size();
        generationService.expireOldReadOnlyReports();
        log.info("Rapports expirés: {} après le TTL de {} jours",
                before, ReportGenerationService.retentionDays());
    }
}
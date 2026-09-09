package com.telemetryhub.reports.api;

import com.telemetryhub.reports.domain.GeneratedReport;
import com.telemetryhub.reports.domain.ReportKind;
import com.telemetryhub.reports.service.ReportGenerationService;
import com.telemetryhub.reports.security.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportGenerationService generationService;

    public ReportController(ReportGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping
    public ResponseEntity<ReportView> generate(@RequestBody @NotNull GenerateReportRequest request) {
        UUID tenantId = TenantContext.get().getTenantId();
        UUID userId = TenantContext.get().getUserId();
        GeneratedReport report = generationService.generate(
                tenantId, userId, request.kind(), request.metric(),
                request.equipmentId(), request.from(), request.to());
        return ResponseEntity.accepted().body(ReportView.from(report));
    }

    @GetMapping
    public ResponseEntity<Page<ReportView>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.get().getTenantId();
        return ResponseEntity.ok(
                generationService.list(tenantId, page, size).map(ReportView::from));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID reportId) {
        UUID tenantId = TenantContext.get().getTenantId();
        GeneratedReport report = generationService.get(reportId, tenantId);
        if (report.getStatus() != com.telemetryhub.reports.domain.ReportStatus.READY) {
            return ResponseEntity.accepted().build();
        }
        MediaType mediaType = report.getKind() == ReportKind.CSV
                ? MediaType.parseMediaType("text/csv")
                : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(report.getFileName()).build().toString())
                .body(report.getArtifact());
    }

    public record GenerateReportRequest(
            @NotNull ReportKind kind,
            @NotBlank String metric,
            UUID equipmentId,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
    }
}
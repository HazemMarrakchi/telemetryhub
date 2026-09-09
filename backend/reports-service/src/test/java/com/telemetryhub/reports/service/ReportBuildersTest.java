package com.telemetryhub.reports.service;

import com.telemetryhub.reports.persistence.ReportRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportBuildersTest {

    private final UUID equipmentId = UUID.randomUUID();
    private final Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
    private final Instant t2 = Instant.parse("2026-09-01T08:05:00Z");

    private List<ReportRow> rows() {
        return List.of(
                new ReportRow(t1, 78.4, "temperature", equipmentId),
                new ReportRow(t2, 79.1, "temperature", equipmentId));
    }

    @Test
    void csvContainsHeaderAndRows() {
        String csv = new CsvReportBuilder().build(rows());

        assertThat(csv).startsWith("timestamp,equipment_id,metric,value\n");
        assertThat(csv).contains(equipmentId.toString());
        assertThat(csv).contains("78.4");
    }

    @Test
    void csvEmptyHasHeaderOnly() {
        String csv = new CsvReportBuilder().build(List.of());
        assertThat(csv).isEqualTo("timestamp,equipment_id,metric,value\n");
    }

    @Test
    void pdfStartsWithPdfMagicBytes() {
        byte[] pdf = new PdfReportBuilder().build("Rapport température", rows());
        assertThat(pdf.length).isGreaterThan(100);
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }

    @Test
    void pdfEmptyRowsStillValid() {
        byte[] pdf = new PdfReportBuilder().build("Sans données", List.of());
        assertThat(pdf.length).isGreaterThan(100);
    }
}
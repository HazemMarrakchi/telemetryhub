package com.telemetryhub.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.telemetryhub.reports.persistence.ReportRow;

import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class PdfReportBuilder {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public byte[] build(String title, List<ReportRow> rows) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph header = new Paragraph(title, titleFont);
            header.setAlignment(Element.ALIGN_LEFT);
            document.add(header);
            document.add(new Paragraph("Généré par TelemetryHub — " + java.time.Instant.now(), 
                    FontFactory.getFont(FontFactory.HELVETICA, 9)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 4, 2.5f, 2.5f, 1.5f});
            addCell(table, "Timestamp", true);
            addCell(table, "Équipement", true);
            addCell(table, "Métrique", true);
            addCell(table, "Valeur", true);
            addCell(table, "Unité", true);

            for (ReportRow row : rows) {
                addCell(table, FMT.format(row.timestamp()), false);
                addCell(table, shortId(row.equipmentId()), false);
                addCell(table, row.metric(), false);
                addCell(table, String.format("%.2f", row.value()), false);
                addCell(table, "", false);
            }
            document.add(table);
        } catch (Exception ex) {
            throw new IllegalStateException("Échec de génération PDF", ex);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private String shortId(UUID id) {
        String s = id.toString();
        return s.substring(0, 8) + "…" + s.substring(s.length() - 4);
    }

    private void addCell(PdfPTable table, String value, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(value,
                header ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)
                        : FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setPadding(4);
        table.addCell(cell);
    }
}
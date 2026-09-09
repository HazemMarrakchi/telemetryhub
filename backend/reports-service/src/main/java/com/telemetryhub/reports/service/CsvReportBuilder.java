package com.telemetryhub.reports.service;

import com.telemetryhub.reports.persistence.ReportRow;

import java.util.List;
import java.util.stream.Collectors;

public class CsvReportBuilder {

    public String build(List<ReportRow> rows) {
        if (rows.isEmpty()) {
            return "timestamp,equipment_id,metric,value\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,equipment_id,metric,value\n");
        for (ReportRow row : rows) {
            sb.append(row.timestamp())
                    .append(',').append(row.equipmentId())
                    .append(',').append(row.metric())
                    .append(',').append(row.value())
                    .append('\n');
        }
        return sb.toString();
    }
}
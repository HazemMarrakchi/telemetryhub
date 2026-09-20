package com.telemetryhub.reports.persistence;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class TelemetryReadingsAccess {

    private final JdbcTemplate jdbcTemplate;

    public TelemetryReadingsAccess(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReportRow> query(UUID tenantId, UUID equipmentId, String metric,
                                 Instant from, Instant to, int bucketSeconds) {
        String sql = """
                SELECT time_bucket('%s seconds', recorded_at) AS ts,
                       avg(value) AS val,
                       metric,
                       equipment_id
                FROM metrics.telemetry_readings
                WHERE tenant_id = ?
                  AND recorded_at BETWEEN ? AND ?
                """.formatted(bucketSeconds);
        List<Object> args = new java.util.ArrayList<>(List.of(
                tenantId,
                new java.sql.Timestamp(from.toEpochMilli()),
                new java.sql.Timestamp(to.toEpochMilli())));
        if (equipmentId != null) {
            sql += "  AND equipment_id = ?\n";
            args.add(equipmentId);
        }
        if (metric != null && !metric.isBlank()) {
            sql += "  AND LOWER(metric) = ?\n";
            args.add(metric.toLowerCase());
        }
        sql += "GROUP BY time_bucket('%s seconds', recorded_at), metric, equipment_id\n"
                .formatted(bucketSeconds);
        sql += "ORDER BY ts ASC";
        return jdbcTemplate.query(sql, args.toArray(), this::mapRow);
    }

    private ReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReportRow(
                rs.getTimestamp("ts").toInstant(),
                rs.getDouble("val"),
                rs.getString("metric"),
                UUID.fromString(rs.getString("equipment_id")));
    }
}
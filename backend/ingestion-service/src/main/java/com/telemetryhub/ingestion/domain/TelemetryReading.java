package com.telemetryhub.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telemetry_readings", schema = "metrics",
        indexes = {
                @Index(name = "idx_readings_equipment_time", columnList = "equipmentId, recordedAt"),
                @Index(name = "idx_readings_tenant_time", columnList = "tenantId, recordedAt"),
                @Index(name = "idx_readings_metric_time", columnList = "metric, recordedAt")
        })
public class TelemetryReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID equipmentId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String metric;

    @Column(nullable = false)
    private double value;

    @Column(nullable = false, length = 24)
    private String unit;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(length = 64)
    private String source;

    @Column(length = 128)
    private String deviceFingerprint;

    protected TelemetryReading() {
    }

    public TelemetryReading(UUID equipmentId, UUID tenantId, String metric, double value,
                            String unit, Instant recordedAt, String source, String deviceFingerprint) {
        this.equipmentId = equipmentId;
        this.tenantId = tenantId;
        this.metric = metric;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
        this.source = source;
        this.deviceFingerprint = deviceFingerprint;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getMetric() {
        return metric;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getSource() {
        return source;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }
}
package com.telemetryhub.maintenance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "downtime_events", schema = "maintenance")
public class DowntimeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "equipment_id", nullable = false)
    private UUID equipmentId;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(nullable = false, length = 64)
    private String metric;

    @Column(length = 2000)
    private String message;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "work_order_id")
    private UUID workOrderId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DowntimeEvent() {
    }

    public DowntimeEvent(UUID tenantId, UUID equipmentId, UUID alertId, String metric,
                         String message, Instant startedAt, UUID workOrderId) {
        this.tenantId = tenantId;
        this.equipmentId = equipmentId;
        this.alertId = alertId;
        this.metric = metric;
        this.message = message;
        this.startedAt = startedAt;
        this.workOrderId = workOrderId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public String getMetric() {
        return metric;
    }

    public String getMessage() {
        return message;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(UUID workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
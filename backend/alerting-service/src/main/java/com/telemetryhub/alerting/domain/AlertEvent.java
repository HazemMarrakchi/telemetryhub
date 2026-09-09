package com.telemetryhub.alerting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_events", schema = "alerts",
        indexes = {
                @Index(name = "idx_alert_tenant_time", columnList = "tenantId, triggeredAt"),
                @Index(name = "idx_alert_equipment_time", columnList = "equipmentId, triggeredAt"),
                @Index(name = "idx_alert_status_time", columnList = "status, triggeredAt")
        })
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID ruleId;

    @Column(nullable = false, length = 100)
    private String ruleName;

    private UUID equipmentId;

    @Column(nullable = false, length = 64)
    private String metric;

    @Column(nullable = false)
    private double value;

    @Column(nullable = false)
    private double threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ComparisonOperator operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status;

    @Column(length = 255)
    private String message;

    @Column(nullable = false)
    private Instant triggeredAt;

    private Instant acknowledgedAt;

    private UUID acknowledgedBy;

    @Column(length = 64)
    private String fingerprint;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AlertEvent() {
    }

    public AlertEvent(UUID tenantId, UUID ruleId, String ruleName, UUID equipmentId,
                      String metric, double value, double threshold,
                      ComparisonOperator operator, AlertSeverity severity,
                      String message, Instant triggeredAt, String fingerprint) {
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.equipmentId = equipmentId;
        this.metric = metric;
        this.value = value;
        this.threshold = threshold;
        this.operator = operator;
        this.severity = severity;
        this.status = AlertStatus.OPEN;
        this.message = message;
        this.triggeredAt = triggeredAt;
        this.fingerprint = fingerprint;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public String getMetric() {
        return metric;
    }

    public double getValue() {
        return value;
    }

    public double getThreshold() {
        return threshold;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void acknowledge(UUID userId) {
        if (this.status == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Une alerte résolue ne peut pas être acquittée");
        }
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = Instant.now();
        this.acknowledgedBy = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
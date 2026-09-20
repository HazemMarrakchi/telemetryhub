package com.telemetryhub.alerting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_rules", schema = "alerts")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    private UUID equipmentId;

    @Column(nullable = false, length = 64)
    private String metric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ComparisonOperator operator;

    @Column(nullable = false)
    private double threshold;

    @Column(length = 24)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int cooldownMinutes = 10;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AlertRule() {
    }

    public AlertRule(UUID tenantId, String name, String description, UUID equipmentId,
                     String metric, ComparisonOperator operator, double threshold,
                     String unit, AlertSeverity severity) {
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.equipmentId = equipmentId;
        this.metric = metric;
        this.operator = operator;
        this.threshold = threshold;
        this.unit = unit;
        this.severity = severity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(UUID equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getMetric() {
        return metric;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public double getThreshold() {
        return threshold;
    }

    public String getUnit() {
        return unit;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
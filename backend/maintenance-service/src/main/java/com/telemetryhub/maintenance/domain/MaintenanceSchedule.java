package com.telemetryhub.maintenance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_schedules", schema = "maintenance")
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "equipment_id", nullable = false)
    private UUID equipmentId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 16)
    private WorkOrderType workType = WorkOrderType.PREVENTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkOrderPriority priority = WorkOrderPriority.MEDIUM;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 30;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MaintenanceSchedule() {
    }

    public MaintenanceSchedule(UUID tenantId, UUID equipmentId, String title, String description,
                               WorkOrderType workType, WorkOrderPriority priority,
                               int intervalDays, Instant nextRunAt) {
        this.tenantId = tenantId;
        this.equipmentId = equipmentId;
        this.title = title;
        this.description = description;
        this.workType = workType != null ? workType : WorkOrderType.PREVENTIVE;
        this.priority = priority != null ? priority : WorkOrderPriority.MEDIUM;
        this.intervalDays = intervalDays > 0 ? intervalDays : 30;
        this.nextRunAt = nextRunAt != null ? nextRunAt : Instant.now();
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public WorkOrderType getWorkType() {
        return workType;
    }

    public WorkOrderPriority getPriority() {
        return priority;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
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
@Table(name = "work_orders", schema = "maintenance")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "equipment_id")
    private UUID equipmentId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkOrderPriority priority = WorkOrderPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkOrderStatus status = WorkOrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkOrderSource source = WorkOrderSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkOrderType workType = WorkOrderType.PREVENTIVE;

    @Column(name = "spare_parts", length = 500)
    private String spareParts;

    @Column(name = "cost_estimate")
    private Double costEstimate;

    @Column(name = "completion_notes", length = 2000)
    private String completionNotes;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkOrder() {
    }

    public WorkOrder(UUID tenantId, UUID equipmentId, String title, String description,
                     WorkOrderPriority priority, WorkOrderSource source, WorkOrderType workType,
                     UUID alertId, Instant dueAt) {
        this.tenantId = tenantId;
        this.equipmentId = equipmentId;
        this.title = title;
        this.description = description;
        this.priority = priority != null ? priority : WorkOrderPriority.MEDIUM;
        this.source = source != null ? source : WorkOrderSource.MANUAL;
        this.workType = workType != null ? workType : WorkOrderType.PREVENTIVE;
        this.alertId = alertId;
        this.dueAt = dueAt;
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

    public void setEquipmentId(UUID equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkOrderPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkOrderPriority priority) {
        this.priority = priority;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public void setStatus(WorkOrderStatus status) {
        this.status = status;
    }

    public WorkOrderSource getSource() {
        return source;
    }

    public WorkOrderType getWorkType() {
        return workType;
    }

    public void setWorkType(WorkOrderType workType) {
        if (workType != null) {
            this.workType = workType;
        }
    }

    public String getSpareParts() {
        return spareParts;
    }

    public void setSpareParts(String spareParts) {
        this.spareParts = spareParts;
    }

    public Double getCostEstimate() {
        return costEstimate;
    }

    public void setCostEstimate(Double costEstimate) {
        this.costEstimate = costEstimate;
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public void setCompletionNotes(String completionNotes) {
        this.completionNotes = completionNotes;
    }

    public UUID getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(UUID assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
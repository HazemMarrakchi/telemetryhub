package com.telemetryhub.reports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports", schema = "reports",
        indexes = {
                @Index(name = "idx_reports_tenant_created", columnList = "tenantId, createdAt")
        })
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ReportKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private Instant rangeFrom;

    @Column(nullable = false)
    private Instant rangeTo;

    private UUID equipmentId;

    @Column(length = 64)
    private String metric;

    @Column(length = 255)
    private String fileName;

    @Column(length = 64)
    private String checksum;

    private long sizeBytes;

    @Lob
    @Column(name = "artifact")
    private byte[] artifact;

    @Column(length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected GeneratedReport() {
    }

    public GeneratedReport(UUID tenantId, UUID generatedBy, ReportKind kind, String title,
                           Instant rangeFrom, Instant rangeTo, UUID equipmentId, String metric) {
        this.tenantId = tenantId;
        this.generatedBy = generatedBy;
        this.kind = kind;
        this.status = ReportStatus.GENERATING;
        this.title = title;
        this.rangeFrom = rangeFrom;
        this.rangeTo = rangeTo;
        this.equipmentId = equipmentId;
        this.metric = metric;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getGeneratedBy() {
        return generatedBy;
    }

    public ReportKind getKind() {
        return kind;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public Instant getRangeFrom() {
        return rangeFrom;
    }

    public Instant getRangeTo() {
        return rangeTo;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public String getMetric() {
        return metric;
    }

    public String getFileName() {
        return fileName;
    }

    public String getChecksum() {
        return checksum;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void markReady(String fileName, byte[] content, String checksum) {
        this.fileName = fileName;
        this.artifact = content;
        this.sizeBytes = content == null ? 0 : content.length;
        this.checksum = checksum;
        this.status = ReportStatus.READY;
    }

    public void markFailed(String message) {
        this.status = ReportStatus.FAILED;
        this.errorMessage = message;
    }

    public byte[] getArtifact() {
        return artifact;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
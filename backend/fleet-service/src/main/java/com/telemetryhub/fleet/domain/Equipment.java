package com.telemetryhub.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "equipments", schema = "fleet",
        indexes = {
                @Index(name = "idx_equipments_tenant", columnList = "tenantId"),
                @Index(name = "idx_equipments_status", columnList = "status"),
                @Index(name = "idx_equipments_site", columnList = "site_id")
        })
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80, unique = true)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private EquipmentModel model;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EquipmentStatus status = EquipmentStatus.ACTIVE;

    @Column(nullable = false)
    private Instant installedAt = Instant.now();

    @Column
    private Instant lastSeenAt;

    @Column
    private Instant nextMaintenanceAt;

    @Column(nullable = false, length = 40)
    private String firmwareVersion = "1.0.0";

    @Column(length = 500)
    private String notes;

    protected Equipment() {
    }

    public Equipment(UUID tenantId, String name, String serialNumber,
                     EquipmentModel model, Site site) {
        this.tenantId = tenantId;
        this.name = name;
        this.serialNumber = serialNumber;
        this.model = model;
        this.site = site;
        this.installedAt = Instant.now();
        this.status = EquipmentStatus.ACTIVE;
    }

    public void markSeen() {
        this.lastSeenAt = Instant.now();
    }

    public void changeStatus(EquipmentStatus newStatus) {
        this.status = newStatus;
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

    public String getSerialNumber() {
        return serialNumber;
    }

    public EquipmentModel getModel() {
        return model;
    }

    public Site getSite() {
        return site;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public Instant getInstalledAt() {
        return installedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getNextMaintenanceAt() {
        return nextMaintenanceAt;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public String getNotes() {
        return notes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setModel(EquipmentModel model) {
        this.model = model;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public void setNextMaintenanceAt(Instant nextMaintenanceAt) {
        this.nextMaintenanceAt = nextMaintenanceAt;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
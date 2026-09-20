package com.telemetryhub.fleet.domain;

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
@Table(name = "equipment_models", schema = "fleet",
        indexes = {
                @Index(name = "idx_eq_models_tenant", columnList = "tenantId"),
                @Index(name = "idx_eq_models_name", columnList = "name")
        })
public class EquipmentModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String manufacturer;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 40)
    private String category = "GENERIC";

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected EquipmentModel() {
    }

    public EquipmentModel(UUID tenantId, String name, String manufacturer,
                          String description, String category) {
        this.tenantId = tenantId;
        this.name = name;
        this.manufacturer = manufacturer;
        this.description = description;
        this.category = category;
        this.createdAt = Instant.now();
        this.active = true;
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

    public String getManufacturer() {
        return manufacturer;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
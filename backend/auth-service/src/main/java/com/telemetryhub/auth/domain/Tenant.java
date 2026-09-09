package com.telemetryhub.auth.domain;

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
@Table(name = "tenants", schema = "auth",
        indexes = {
                @Index(name = "idx_tenants_slug", columnList = "slug", unique = true),
                @Index(name = "idx_tenants_created_at", columnList = "createdAt")
        })
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80, unique = true)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant updatedAt;

    @Column(length = 64)
    private String plan = "STANDARD";

    protected Tenant() {
    }

    public Tenant(String name, String slug, String plan) {
        this.name = name;
        this.slug = slug;
        this.plan = plan == null ? "STANDARD" : plan;
        this.createdAt = Instant.now();
        this.active = true;
    }

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getPlan() {
        return plan;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }
}
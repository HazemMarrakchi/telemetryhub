package com.telemetryhub.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "metric_catalog", schema = "metrics")
public class MetricDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 24)
    private String unit;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private String dataType = "DOUBLE";

    protected MetricDefinition() {
    }

    public MetricDefinition(String name, String unit, String description) {
        this.name = name;
        this.unit = unit;
        this.description = description;
    }
}
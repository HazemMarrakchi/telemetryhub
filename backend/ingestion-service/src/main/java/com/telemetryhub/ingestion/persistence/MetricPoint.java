package com.telemetryhub.ingestion.persistence;

import java.time.Instant;

public interface MetricPoint {

    Instant getTimestamp();

    double getValue();

    String getMetric();
}
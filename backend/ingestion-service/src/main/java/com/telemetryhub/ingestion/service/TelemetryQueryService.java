package com.telemetryhub.ingestion.service;

import com.telemetryhub.ingestion.persistence.MetricPoint;
import com.telemetryhub.ingestion.persistence.TelemetryReadingRepository;
import com.telemetryhub.ingestion.security.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TelemetryQueryService {

    private final TelemetryReadingRepository repository;

    public TelemetryQueryService(TelemetryReadingRepository repository) {
        this.repository = repository;
    }

    private UUID tenantId() {
        return TenantContext.get().getTenantId();
    }

    public List<MetricPoint> raw(Instant from, Instant to) {
        return repository.findRawRange(tenantId(), from, to);
    }

    public List<MetricPoint> rawByEquipment(UUID equipmentId, Instant from, Instant to) {
        return repository.findRawRangeByEquipment(tenantId(), equipmentId, from, to);
    }

    public List<MetricPoint> aggregate(String bucket, Instant from, Instant to) {
        return repository.findAggregated(tenantId(), bucket, from, to);
    }

    public List<MetricPoint> aggregateByEquipment(UUID equipmentId, String bucket, Instant from, Instant to) {
        return repository.findAggregatedByEquipment(tenantId(), equipmentId, bucket, from, to);
    }

    public List<MetricPoint> latest() {
        List<Object[]> rows = repository.findLatest(tenantId(), PageRequest.of(0, 500));
        return rows.stream().map(row -> (MetricPoint) new MetricPoint() {
            @Override
            public Instant getTimestamp() {
                return (Instant) row[0];
            }

            @Override
            public double getValue() {
                return ((Number) row[1]).doubleValue();
            }

            @Override
            public String getMetric() {
                return (String) row[2];
            }
        }).toList();
    }
}